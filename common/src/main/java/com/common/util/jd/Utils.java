package com.common.util.jd;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.Constants;
import com.common.dto.account.Account;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.util.HttpUtils;
import com.google.common.collect.Lists;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.xiaoleilu.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author zf
 * since 2019/12/20
 */
@Slf4j
public class Utils {
    /**
     * 域名 https://xdws20200318.kuaizhan.com
     */
    public static List<String> tklList = Lists.newArrayList();

    /**
     * 获取商品优惠券二合一连接
     *
     * @param str 商品skuId
     * @return
     */
    public static String getShortUrl(String str, Account account) {

        try {
            //蚂蚁星球地址
            String URL = Constants.ANT_SERVER_URL;

            HashMap map = new HashMap();
            map.put("apikey", account.getAntappkey());
            map.put("goods_id", str);

            map.put("positionid", account.getJdtgwid());
            //	type=1 goods_id=商品ID，type=2 goods_id=店铺id，type=3 goods_id=自定义链接(京东活动链接、二合一链接)
            map.put("type", "3");

            String requestResult = HttpUtils.post(URL, JSONUtil.toJsonPrettyStr(map));
            if (Objects.equals("error", requestResult)) {
                return null;
            }
            String twoToOneUrl = JSONObject.parseObject(requestResult.replace("\\", "")).getString("data");
            if (twoToOneUrl.contains("?")) {
                int i = twoToOneUrl.indexOf("?");

                return twoToOneUrl.substring(0, i);
            }
            return twoToOneUrl;
        } catch (Exception e) {
            log.info("转链失败------>{}", e);
            return null;
        }
    }

    /**
     * 递归遍历字符串中所有需要转链的链接
     *
     * @param content
     * @return
     */
    public static LinkedHashMap<String, String> getUrlMap2(String allcontent, String content, LinkedHashMap<String, String> map, int flag, Account account) {


        int i1 = content.indexOf("https://u.jd.com/");

        if (i1 != -1) {
            int start = i1;
            int end = i1 + 23;
            String substring = content.substring(start, end);

            String substring1 = content.substring(end);
            int i = allcontent.indexOf(substring);
            if (!substring.contains("http")) {
                return null;
            }
            String shortUrl = getShortUrl(substring, account);

            if (StringUtils.isEmpty(shortUrl)) {
                return null;
            }
            map.put(substring, shortUrl);
            map.putAll(getUrlMap2(allcontent, substring1, map, i, account));
        }
        return map;
    }

    /**
     * 获取商品的图片地址
     *
     * @param skuId 商品skuId
     * @return
     */
    public static String getSKUInfo(String skuId, String antappkey) {

        try {
            if (StringUtils.isEmpty(skuId)) {
                return null;
            }
            //蚂蚁星球地址
            String URL = "http://api-gw.haojingke.com/index.php/v1/api/jd/goodsdetail";

            HashMap map = new HashMap();
            map.put("apikey", antappkey);
            map.put("goods_id", skuId);
            map.put("isunion", "0");

            String requestResult = HttpUtils.post(URL, JSONUtil.toJsonPrettyStr(map));
            if (Objects.equals(JSONObject.parseObject(requestResult).getString("message"), "success")) {
                String string = JSONObject.parseObject(requestResult).getJSONObject("data").getString("picurl").replace("\\", "");
                return string;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("skuId--->{},error--->{}", skuId, e);
            return null;
        }
    }


    /**
     * 喵有券 根据淘宝商品淘口令返回图片地址
     *
     * @return 图片地址
     */
    public static String tbToLink2(String tkl, RedisTemplate<String, Object> redisTemplate) {
        if (StringUtils.isEmpty(tkl)) {
            return "";
        }

        String format = String.format(Constants.TKL_TO_SKU_INFO_REQUEST_URL, Constants.MYB_APPKey, Constants.tb_name, Constants.TBLM_PID, tkl);
        String request = HttpUtils.getRequest(format);
        String substring = request.substring(0, request.lastIndexOf("}") + 1);

        if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {

            String itemId = JSONObject.parseObject(substring).getJSONObject("data").getString("item_id");
            Boolean itme_boolean = redisTemplate.opsForHash().putIfAbsent(itemId, itemId, "1");
            redisTemplate.expire(itemId, 20, TimeUnit.MINUTES);
            if (itme_boolean) {
                String string = JSONObject.parseObject(substring).getJSONObject("data").getJSONObject("item_info").getString("pict_url");
                return string;
            } else {
                log.info("淘宝itemId已经存在了------->{}", itemId);
                return "HAD_SEND";
            }
        } else {

            Boolean itme_boolean = redisTemplate.opsForHash().putIfAbsent(tkl, tkl, "tkl");
            redisTemplate.expire(tkl, 20, TimeUnit.MINUTES);

            if (itme_boolean) {
                return "";
            } else {
                return "HAD_SEND";
            }
        }
    }


    /**
     * 喵有券 根据淘宝商品淘口令转链转为自己的淘口令
     *
     * @param url 由短连接还原后的原网址
     * @return 转链结果内容
     */
    public static String toTaoBaoTkl(String url) {
        if (url.length() < 30) {
            return url;
        }

        //原淘口令
        String old_tkl = null;
        String new_tkl = null;


        String pattern = "([=|(|￥])\\w{8,12}([)|￥|&])";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(url);
        if (m.find()) {
            String substring = m.group();
            int i = url.indexOf(substring);
            old_tkl = url.substring(i + 1, i + 12);
        }
        if (!tklList.contains(old_tkl)) {
            tklList.add(old_tkl);

        } else {

        }


        try {
            String string;
            String format = String.format(Constants.TKL_TO_SKU_INFO_REQUEST_URL, Constants.MYB_APPKey, Constants.tb_name, Constants.TBLM_PID, old_tkl);
            String request = HttpUtils.getRequest(format);
            String substring = request.substring(0, request.lastIndexOf("}") + 1);

            if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {
                string = JSONObject.parseObject(substring).getJSONObject("data").getString("tpwd");
                //转换为自己的淘口令不含￥符号
                new_tkl = string.substring(1, (string.length() - 1));
                log.info("原淘口令-->{},转换的淘口令-->{}", old_tkl, new_tkl);
            } else {
                log.info("由原淘口令转换自己的淘口令失败了,请求服务器地址报错----->{}", substring);
                return url;
            }
        } catch (Exception e) {
            log.info("由原淘口令转换自己的淘口令失败了,由短连接还原后的原网址----->{},url");
            return url;
        }

        try {
            int i2 = url.indexOf("kuaizhan.com");
            String substring = url.substring(0, i2 + 12);
            return substring + "/?" + new_tkl;
        } catch (Exception e) {
            e.printStackTrace();
            return url;
        }
    }


    /**
     * 将原字符串中的所有连接替换为转链之后的连接 ，返回新的字符串
     *
     * @param strString
     * @return
     */
    public static List<String> toLinkByDDX(String strString, String reminder, List<String> msgKeyWords, RedisTemplate<String, Object> redisTemplate, WechatReceiveMsgDto receiveMsgDto, Account account) {

        if (!msgContionMsgKeys(strString, msgKeyWords, receiveMsgDto, redisTemplate)) {
            return null;
        }

        //判断是否为淘宝线报
        boolean b = judgeIsTaoBao(strString);

        List<String> list = Lists.newArrayList();
        String str;
        //淘宝转链
        if (b) {
            return null;
        }
        try {
            str = strString;
            //京东转链
            LinkedHashMap<String, String> urlMap = new LinkedHashMap<>();
            LinkedHashMap<String, String> map = getUrlMap2(str, str, urlMap, 0, account);
            if (Objects.equals(map, null) || map.size() == 0) {
                return null;
            }
            String str2 = str;
            for (Map.Entry<String, String> entry : map.entrySet()) {

                if (Objects.isNull(entry.getValue())) {
                    log.info("京东转链失败----------------------->");
                    return null;
                }
                log.info("京东转链前:---->{},转链后---->{}", entry.getKey(), entry.getValue());
                str2 = str2.replace(entry.getKey(), entry.getValue());
            }


            log.info("消息长度----->{}", str2.length());
            if (str2.length() > 350 && (!str2.contains("【京东领券")) && (!str2.contains("领券汇总"))) {
                log.info("超出长度--------------->{}", str2.length());
                return Lists.newArrayList();
            }
            list.add(URLEncoder.encode(str2, "UTF-8"));

            if (str2.contains("【京东领券") || str2.contains("领券汇总")) {
                list.add("");
                //防止一天内发多次京东领券的线报
                Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(account.getName() + DateTime.now().toString("yyyy-MM-dd"), "1");
                if (aBoolean) {
                    redisTemplate.expire(account.getName() + DateTime.now().toString("yyyy-MM-dd"), DateTime.now().plusDays(1).toLocalDate().toDate().getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    return list;
                } else {
                    return null;
                }
            }


            //购买京东商品的图片链接
            String sku_url = MapUtil.getFirstNotNull(map, redisTemplate, str, account.getName(), account.getAntappkey());

            if (Objects.equals("HAD_SEND", sku_url)) {
                return Lists.newArrayList();
            }

            list.add(sku_url);
            return list;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取商品skuId
     *
     * @return
     */
    public static String getSkuIdByUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }

        try {
            String request = HttpUtils.getRequest(url);
            String substring = request.substring(request.indexOf("var hrl='") + 9, request.indexOf("';var ua="));
            String redirectUrl = getRedirectUrl(substring);
            log.info("redirectUrl---->{}", redirectUrl);
            String pattern = "([/|=])\\d{6,15}([&|.])";

            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(redirectUrl);
            if (m.find()) {
                String st = m.group();
                String skuId = st.substring(1, st.length() - 1);
                if ("shopId".equals(redirectUrl.substring((m.start() - 6), m.start()))) {
                    return null;
                } else {
                    log.info("skuId---->{}", skuId);
                    return skuId;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * suo.im 短网址地址
     *
     * @param url 由原短链还原后的原网址长链接(淘口令已经换成自己的了)
     * @return 短链接
     */
    public static String yunHomeToshortLink(String url) {

        if (!url.contains("kuaizhan.com")) {
            return url;
        }


        String request = null;
        try {
            String requestUrl = "http://suo.im/api.htm?format=json&key=5ee6e6e3b1b63c29d6cdc3e0@1a756646538af28b9cb13bd86562c065&url=%s";
            String format = String.format(requestUrl, url);
            request = HttpUtils.getRequest(format).replace("/n", "");
            return JSONObject.parseObject(request).getString("url");
        } catch (Exception e) {
            log.info("将长链接转换为短链接失败----->{}", e);
            return url;
        }
    }


    /**
     * 短链接还原 （api:官网：http://www.alapi.cn/  ）
     *
     * @param shortUrl 短链接
     * @return 原网址
     */
    public static String shortToLong2(String shortUrl) {

        if (tklList.contains(shortUrl)) {
            return "";
        } else {
            tklList.add(shortUrl);
        }

        String url = "https://v1.alapi.cn/api/url/query?url=%s";
        String format = String.format(url, shortUrl);

        try {
            String request = HttpUtils.getRequest(format).replace("/n", "");
            String longUrl = JSONObject.parseObject(request).getJSONObject("data").getString("long_url");
            return longUrl;
        } catch (Exception e) {
            log.info("短链接还原淘口令出错了,原样返回短链接----->{}", shortUrl);
            return shortUrl;
        }

    }


    /**
     * 递归获取短链接的（如http://t.cn/A677uwls) 的淘口令
     *
     * @param str
     * @param map
     * @return
     */
    public static Map<String, String> dgGetTkl(String str, Map<String, String> map) {
        int i = str.indexOf("https://url.cn/");

        int m = str.indexOf("https://t.cn/");

        int n = str.indexOf("https://w.url.cn/");

        if (i != -1) {
            int start = i;
            int end = i + 23;
            String substring = str.substring(start, end);
            String s = shortToLong2(substring);
            if (!StringUtils.isEmpty(s)) {
                map.put(substring, toTaoBaoTkl(s));
            }
            String substring1 = str.substring(end);
            dgGetTkl(substring1, map);
        }

        if (m != -1) {
            int start = m;
            int end = m + 21;
            String substring = str.substring(start, end);
            String s = shortToLong2(substring);
            if (!StringUtils.isEmpty(s)) {
                map.put(substring, toTaoBaoTkl(s));
            }
            String substring1 = str.substring(end);
            dgGetTkl(substring1, map);
        }

        if (n != -1) {
            int start = n;
            int end = n + 26;
            String substring = str.substring(start, end);
            String s = shortToLong2(substring);
            if (!StringUtils.isEmpty(s)) {
                map.put(substring, toTaoBaoTkl(s));
            }
            String substring1 = str.substring(end);
            dgGetTkl(substring1, map);
        }


        return map;
    }

    /**
     * 递归获取短链接的（如http://t.cn/A677uwls) 的淘口令
     *
     * @param str
     * @param map
     * @return
     */
    public static Map<String, String> dgGetTkl2(String str, Map<String, String> map) {
        String pattern = "([(|￥])\\w{8,12}([)|￥])";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        if (m.find()) {
            String substring = m.group();
            int i = str.indexOf(substring);
            String substring1 = str.substring(i, i + 13);
            map.put(substring1, tkl_to_gy(substring));
            String flag = str.replace(substring, "");
            dgGetTkl2(flag, map);
        }
        return map;
    }

    /**
     * 将带有短链接的字符串替换为淘口令
     *
     * @param str
     * @return
     */
    public static List<String> getTBUrlMap(String str, RedisTemplate<String, Object> redisTemplate) {

        try {
            Map<String, String> map = new HashMap<>();
            List<String> list = Lists.newArrayList();
            Map<String, String> tklMapResult = new HashMap<>();
            Map<String, String> tklMap = dgGetTkl(str, map);

            if (tklMap.size() == 0) {
                tklMapResult = dgGetTkl2(str, map);
            } else {
                tklMapResult = tklMap;
            }

            if (tklMapResult.size() == 0) {
                return Lists.newArrayList();
            }

            for (Map.Entry<String, String> entry : tklMapResult.entrySet()) {

                str = str.replace(entry.getKey(), " " + yunHomeToshortLink(entry.getValue()) + " ");

            }
            list.add(str);
            return list;

        } catch (Exception e) {
            log.info("exception------------->{}", e);
            return Lists.newArrayList();
        }
    }

    /**
     * 线报中是否含有我们的关键字,如果含有继续,如果没有线报中的消息不采用
     *
     * @param msg     原线报内容
     * @param msgKeys 线报关键字
     * @return
     */
    public static boolean msgContionMsgKeys(String msg, List<String> msgKeys, WechatReceiveMsgDto receiveMsgDto, RedisTemplate<String, Object> redisTemplate) {
        AtomicBoolean msgFlag = new AtomicBoolean(false);

        msgKeys.forEach(it -> {

            if (msg.contains(it) && (!msgFlag.get())) {

                if (it.equals("1元") && (msg.contains(".1元") || msg.contains("1元/") || msg.contains("1元,") || msg.contains("1元，") || msg.contains("1元+"))) {

                } else if (it.equals("秒杀") && (msg.contains("秒杀价") || msg.contains("秒杀 价") || msg.contains("秒 杀 价"))) {

                } else if (it.equals("超值") && (msg.contains("超值价") || msg.contains("超值 价") || msg.contains("超 值 价"))) {

                } else if (it.equals("包邮") && msg.contains("包邮")) {
                    if (!msg.contains("京东价") && !msg.contains("内购价")) {
                        log.info("关键字--->{}", it);
                        msgFlag.set(true);
                        return;
                    }

                } else if (it.equals("实付") && msg.contains("实付")) {

                    if (msg.contains("[@emoji=\\u2014]") && (!msg.contains("京东价")) && (!msg.contains("内购价"))) {
                        log.info("关键字--->{}", it);
                        msgFlag.set(true);
                        return;
                    }

                } else {
                    log.info("关键字--->{}", it);
                    msgFlag.set(true);
                    return;
                }
            }
        });
        return msgFlag.get();
    }

    /**
     * 图片中是否含有二维码
     *
     * @param path 图片的地址
     * @return
     */
    public static boolean isHaveQr(String path) {

        try {
            log.info("path---->{}", path);
            BufferedImage image = ImageIO.read(new File(path));
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            Binarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);
            System.out.println("图片中的内容-->" + result.getText());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为淘宝线报
     *
     * @param msg
     * @return
     */
    public static boolean judgeIsTaoBao(String msg) {
        if (msg.contains("https://u.jd.com/") || msg.contains("https://coupon.m.jd")) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 如果是淘宝线报 每隔一段时间就可原样输出 是否拦截
     *
     * @param str
     * @param
     * @param openIsNo 是否拦截开关
     * @return
     */
    public static boolean taobaoInterval(String str, RedisTemplate<String, Object> redisTemplate, boolean openIsNo) {
        if (!openIsNo) {
            return true;
        }


        int time;

        String flag;
        boolean b = judgeIsTaoBao(str);
        if (b) {
            //淘宝
            flag = "tbtime";
            time = 30;
        } else {
            //京东
            flag = "jdtime";
            time = 3000;
        }

        String tbtime = (String) redisTemplate.opsForValue().get(flag);
        if (StringUtils.isEmpty(tbtime)) {
            redisTemplate.opsForValue().set(flag, System.currentTimeMillis() + "");
            return false;
        } else {

            if (new DateTime(Long.parseLong(tbtime)).plusMinutes(time).toDate().getTime() - System.currentTimeMillis() < 0L) {
                redisTemplate.opsForValue().set(flag, System.currentTimeMillis() + "");
                return false;
            } else {
                redisTemplate.opsForValue().set(flag, tbtime);
                return true;
            }
        }
    }

    /**
     * 由淘口令直接转链为自己的淘口令（折淘客接口：http://www.zhetaoke.com/user/open/open_gaoyongzhuanlian_tkl.aspx ）
     *
     * @param tkl
     * @return
     */
    public static String tkl_to_gy(String tkl) {

        try {
            String str = String.format(Constants.ztk_gy_zl, tkl);
            String request = HttpUtils.getRequest(str).replace("/n", "");
            String string = JSONObject.parseObject(request).getJSONArray("content").getJSONObject(0).getString("tkl");
            log.info("原淘口令-->{},转换的淘口令-->{}", tkl, string);
            return string;
        } catch (Exception e) {
            return tkl;
        }
    }

    /**
     * 淘口令转新浪链接
     *
     * @param tkl
     * @return
     */
    public static String tkl_toLink(String tkl) {

        try {
            String format = String.format(Constants.tkl_toLink, tkl);
            String request = HttpUtils.getRequest(format).replace("/n", "").replace("\\", "");
            String url = JSONObject.parseObject(request).getString("url");

            String str = null;
            try {
                str = String.format(Constants.ztk_tkl_toLink, URLEncoder.encode(url, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String short_rul = HttpUtils.getRequest(str).replace("/n", "");
            return short_rul;
        } catch (Exception e) {
            log.info("error here------->{}", e);
            return tkl;
        }
    }

    /**
     * 折淘客高佣转链 （本接口只是返回图片地址）
     *
     * @param skuId
     * @return
     */
    public static String tkzJdToLink(String skuId) {
        String format = String.format(Constants.ztk_tkl_jd_toLink, skuId);
        String request = HttpUtils.getRequest(format).replace("/n", "").replace("\\", "");
        return request;
    }

    /**
     * 免单群中是否包含关键字
     *
     * @return
     */
    public static boolean miandanGroupMsgContainKeyWords(String msg) {
        AtomicBoolean flag = new AtomicBoolean(false);
        Arrays.asList("0", "免单", "免费").stream()
                .forEach(it -> {
                    if (msg.contains(it)) {
                        flag.set(true);
                    }
                });

        return flag.get();
    }


    /**
     * 域名在微信中是否正常使用
     *
     * @param domain
     * @return
     */
    public static boolean checkDomainNormal(String domain) {

        String url = "http://www.360kan.cn/wxcheck/?url=" + domain;
        String request = HttpUtils.getRequest(url).replace("/n", "").replace("\\", "");
        System.out.println("request----->:" + request);
        return false;
    }


    /**
     * 获取重定向地址
     *
     * @param path
     * @return
     * @throws Exception
     */
    private static String getRedirectUrl(String path) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(path)
                .openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(5000);
        return conn.getHeaderField("Location");
    }


//  public static void main(String[] args) {
//    String url="http://cms.api.com/api/jd/detail?url=https://u.jd.com/cyVAm1";
//    String request = HttpUtils.getRequest(url);
//    System.out.println(request);
//  }


//  public static void main(String[] args) throws Exception {
//
//
//    String request = HttpUtils.getRequest("https://u.jd.com/Q4yhAg");
//    String substring = request.substring(request.indexOf("var hrl='") + 9, request.indexOf("';var ua="));
//    System.out.println("sub----->" + substring);
//    String redirectUrl = getRedirectUrl(substring);
//    System.out.println("redirectUrl--->" + redirectUrl);
//
//    String pattern = "([/|=])\\d{7,12}([&|.])";
//
//    Pattern r = Pattern.compile(pattern);
//    Matcher m = r.matcher(redirectUrl);
//    if (m.find()) {
//      String st = m.group();
//      String skuId = st.substring(1, st.length() - 1);
//      System.out.println("skuId-->" + skuId);
//    } else {
//      return;
//    }
//
//
////    String skuInfo = getSKUInfo("4079999");
////    System.out.println(skuInfo);
//
//
//  }
}
