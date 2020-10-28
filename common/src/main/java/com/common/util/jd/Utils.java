package com.common.util.jd;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.account.Account;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.HttpUtils;
import com.common.util.wechat.WechatUtils;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author zf
 * since 2019/12/20
 */
@Slf4j
public class Utils {

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
     * 遍历字符串中所有需要转链的链接
     *
     * @param content
     * @return
     */
    public static LinkedHashMap<String, String> getUrlMap2(String content, LinkedHashMap<String, String> map, Account account) {
        int i = 0;
        String content_after = content;
        String pattern = "https://u.jd.com/[0-9A-Za-z]{6,7}";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(content_after);

        while (m.find()) {
            i++;
            String shortUrl = getShortUrl(m.group(), account);
            if (StringUtils.isEmpty(shortUrl)) {
                log.info("链接转换失败,消息不会发送======>{}", shortUrl);
                return null;
            }
            map.put(m.group(), shortUrl);
        }

        if (i == 0) {
            log.info("没有匹配到京东短链接===============>");
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
     * 将原字符串中的所有连接替换为转链之后的连接 ，返回新的字符串
     *
     * @param strString
     * @return
     */
    public static List<String> toLinkByDDX(String strString, String reminder, List<String> msgKeyWords, List<String> owenkeywords, RedisTemplate<String, Object> redisTemplate, WechatReceiveMsgDto receiveMsgDto, Account account) {
        String warn;
        if (StringUtils.isEmpty(warn = msgContionMsgKeys(strString, msgKeyWords, receiveMsgDto, redisTemplate))) {
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
            LinkedHashMap<String, String> map = getUrlMap2(str, urlMap, account);
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


            if (owenkeywords.contains(warn) && (!str2.contains("变价则黄"))) {
                log.info("线报消息为====>{}", str2 + "【变价则黄】" + reminder);
                list.add(URLEncoder.encode(str2 + "【变价则黄】" + reminder, "UTF-8"));

                //===========将特价消息发送给群主===========
                String finalStr = str2;
                Arrays.asList("wxid_2r8n0q5v38h222", "du-yannan").forEach(it -> {
                    try {
                        WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), "wxid_8sofyhvoo4p322", it, URLEncoder.encode(finalStr + "【变价则黄】" + reminder, "UTF-8"), null, null, null);
                        WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                });


            } else {
                list.add(URLEncoder.encode(str2 + reminder, "UTF-8"));
            }

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
            String sku_url = MapUtil.getFirstNotNull(map, redisTemplate, str, account.getName(), account.getAntappkey(), receiveMsgDto.getRid());

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
     * 线报中是否含有我们的关键字,如果含有继续,如果没有线报中的消息不采用
     *
     * @param msg     原线报内容
     * @param msgKeys 线报关键字
     * @return
     */
    public static String msgContionMsgKeys(String msg, List<String> msgKeys, WechatReceiveMsgDto receiveMsgDto, RedisTemplate<String, Object> redisTemplate) {
        AtomicReference<String> result = new AtomicReference<>("");
        msgKeys.forEach(it -> {
            if (it.equals("\\n1")) {
                it = "\n1";
            }
            if (it.equals("1\\n")) {
                it = "1\n";
            }

            if (msg.contains(it) && (!msg.contains("京东价")) && StringUtils.isEmpty(result.get())) {

                if (it.equals("1元") && (msg.contains(".1元") || msg.contains("1元/") || msg.contains("1元,") || msg.contains("1元，") || msg.contains("1元+") || msg.contains("1元\\n") || msg.contains("1元含税"))) {

                } else if (it.equals("秒杀") && (msg.contains("秒杀价") || msg.contains("秒杀 价") || msg.contains("秒 杀 价"))) {

                } else if (it.equals("超值") && (msg.contains("超值价") || msg.contains("超值 价") || msg.contains("超 值 价"))) {

                } else if (it.equals("包邮") && msg.contains("包邮")) {
                    if (msg.contains("@emoji") || msg.contains("\\u2014")) {
                        log.info("关键字1==>{}", it);
                        result.set(it);
                        return;
                    }

                } else if (it.equals("实付") && msg.contains("实付")) {

                    if (msg.contains("[@emoji=\\u2014]")) {
                        log.info("关键字2====>{}", it);
                        result.set(it);
                        return;
                    }

                } else if (it.equals("\n1")) {
                    if (msg.endsWith("\n1")) {
                        log.info("关键字3====>{}", it);
                        result.set(it);
                        return;
                    }
                } else if (it.equals("1\n")) {
                    if (msg.startsWith("1\n")) {
                        log.info("关键字4====>{}", it);
                        result.set(it);
                        return;
                    }
                } else {
                    log.info("关键字5======>{}", it);
                    result.set(it);
                    return;
                }
            }

        });
        return result.get();
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
