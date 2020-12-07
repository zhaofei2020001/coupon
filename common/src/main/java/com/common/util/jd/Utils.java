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
import org.springframework.util.CollectionUtils;
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


    public static List<String> getAllUrl(String content) {

        List<String> list = new ArrayList<>();

        String content_after = content;
        String pattern = "https://u.jd.com/[0-9A-Za-z]{6,7}";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(content_after);

        while (m.find()) {
            list.add(m.group());
        }

        return list;
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
     * 获取商品的图片地址
     *
     * @param urlList 商品skuId
     * @return
     */
    public static String getSKUInfo2(List<String> urlList, String antappkey, String rid) {

        List<String> urls = Lists.newArrayList();

        for (int i = 0; i < urlList.size(); i++) {
            String skuIdByUrl = Utils.getSkuIdByUrl(urlList.get(i));

            String skuUrl = getSKUInfo(skuIdByUrl, antappkey);
            log.info("skuId=====>{},图片====>{}", skuIdByUrl, skuUrl);
            if (!StringUtils.isEmpty(skuUrl)) {

                FileSplitUtil.downloadPicture(skuUrl, rid + i);

                urls.add("/Users/mac/" + rid + i + ".jpeg");
            }
        }


        if (CollectionUtils.isEmpty(urlList)) {
            return null;
        }
        if (urlList.size() == 1) {
            return urlList.get(0);
        }


        String[] array = urls.toArray(new String[urls.size()]);

        BufferedImage merge = FileSplitUtil.merge(array);

        FileSplitUtil.aabase64StringToImage(FileSplitUtil.getImageBinary(merge), rid);

        return "/Users/mac/" + rid + ".jpeg";
    }

    /**
     * 将原字符串中的所有连接替换为转链之后的连接 ，返回新的字符串
     *
     * @param strString
     * @return
     */
    public static List<String> toLinkByDDX(String strString, String reminder, List<String> msgKeyWords, RedisTemplate<String, Object> redisTemplate, WechatReceiveMsgDto receiveMsgDto, Account account, boolean hadSkuId, boolean had_send,boolean flag) {
        String warn = "";

        if (!StringUtils.isEmpty(warn = msgContionMsgKeys(strString, msgKeyWords)) || strLengh(strString)||flag) {

            //判断是否为淘宝线报
            boolean b = judgeIsTaoBao(strString);

            List<String> list = Lists.newArrayList();
            String str;
            //淘宝转链
            if (b || had_send) {
                return null;
            }
            try {
                str = strString;
                //所有连接
                List<String> allUrl = getAllUrl(strString);
                if (CollectionUtils.isEmpty(allUrl)) {
                    log.info("无链接==========>");
                    return null;
                }

                if (!hadSkuId && !(strString.contains("【京东领券") || strString.contains("领券汇总"))) {

                    String firstSkuId = MapUtil.getFirstSkuId(allUrl, redisTemplate);


                    if (Objects.equals("HAD_SEND", firstSkuId)) {

                        return Arrays.asList("1", "2", "3");
                    }

                    if (StringUtils.isEmpty(firstSkuId) && MapUtil.hadSendStr(allUrl, str, redisTemplate, account.getName())) {

                        return null;
                    }

                    String returnStr = zlStr(str, account, allUrl);
                    if (StringUtils.isEmpty(returnStr)) {
                        return null;
                    }

                    if (Arrays.asList("一元", "1元", "【1】", "1亓", "\n1", "1\n", "1+u", "0元单", "0元购", "免单", "0撸").contains(warn) && (!returnStr.contains("变价则黄")) && Objects.equals("ddy", account.getName())) {

                        list.add(URLEncoder.encode(returnStr + " 变价则无" + reminder, "UTF-8"));
                        list.add(firstSkuId);
                        //===========将特价消息发送给群主===========
                        account.getMsgToPersons().forEach(it -> {
                            try {
                                WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), "wxid_8sofyhvoo4p322", it, URLEncoder.encode(returnStr, "UTF-8"), null, null, null);
                                WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        });

                    } else {
                        list.add(URLEncoder.encode(returnStr + reminder, "UTF-8"));
                        list.add(firstSkuId);
                    }

                } else {

                    if (strString.contains("【京东领券") || strString.contains("领券汇总")) {
                        //防止一天内发多次京东领券的线报
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent("JDLQ" + account.getName() + DateTime.now().toString("yyyy-MM-dd"), "1");
                        if (aBoolean) {
                            redisTemplate.expire("JDLQ" + account.getName() + DateTime.now().toString("yyyy-MM-dd"), DateTime.now().plusDays(1).toLocalDate().toDate().getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

                            list.add(URLEncoder.encode(zlStr(str, account, allUrl) + reminder, "UTF-8"));
                        }
                    } else {

                        String returnStr = zlStr(str, account, allUrl);
                        if (StringUtils.isEmpty(returnStr)) {
                            return null;
                        }


                        if (Arrays.asList("一元", "1元", "【1】", "1亓", "\n1", "1\n", "1+u", "0元单", "0元购", "免单", "0撸").contains(warn) && (!returnStr.contains("变价则无"))) {

                            list.add(URLEncoder.encode(returnStr + " 变价则无" + reminder, "UTF-8"));
                        } else {
                            list.add(URLEncoder.encode(returnStr + reminder, "UTF-8"));
                        }
                    }
                }

                return list;

            } catch (Exception e) {
                log.info("出错了=======>");
                e.printStackTrace();

            }

        } else {
            return null;
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
            String pattern = "([/|=])\\d{6,15}([&|.])";

            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(redirectUrl);

            boolean flag = false;
            String skuId = "";


            while (m.find()) {
                String st = m.group();
                skuId = st.substring(1, st.length() - 1);
                if ("shopId".equals(redirectUrl.substring((m.start() - 6), m.start()))) {
                    flag = true;
                }
            }


            if (flag) {
                return null;
            } else {
                return skuId;
            }

        } catch (Exception e) {
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
    public static String msgContionMsgKeys(String msg, List<String> msgKeys) {
        AtomicReference<String> result = new AtomicReference<>("");
        msgKeys.forEach(it -> {
            if (it.equals("\\n1")) {
                it = "\n1";
            }
            if (it.equals("1\\n")) {
                it = "1\n";
            }
            if (it.equals("1亓")) {
                it = "1元";
            }


            if (msg.contains(it) && (!msg.contains("京东价")) && StringUtils.isEmpty(result.get()) && !msg.contains("at,nickname")) {

                if (it.equals("1元") &&
                        ((msg.contains(".1元") ||
                                msg.contains("91元") ||
                                msg.contains("81元") ||
                                msg.contains("71元") ||
                                msg.contains("61元") ||
                                msg.contains("51元") ||
                                msg.contains("41元") ||
                                msg.contains("31元") ||
                                msg.contains("21元") ||
                                msg.contains("11元") ||
                                msg.contains(".1元") ||
                                msg.contains("01元") ||
                                msg.contains("1元/")

                        ) &&
                                (!msg.contains("11.1"))
                        )
                ) {

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


    /**
     * 订单侠根据商品链接获取商品skuId
     *
     * @return
     */
    public static String getSkuIdByUrl2(String url) {
        try {
            String str = Constants.DDX_GET_SKUID;
            String format = String.format(str, Constants.DDX_APIKEY, url);
            String request = HttpUtils.getRequest(format);
            String substring = request.substring(0, request.lastIndexOf("}") + 1);

            if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {
                String string = JSONObject.parseObject(substring).getString("data");
                return string;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    //转链后消息内容
    public static String zlStr(String content, Account account, List<String> list) {
        int i = 0;
        String content_after = content;
        for (String s : list) {
            i++;
            String shortUrl = getShortUrl(s, account);
            if (StringUtils.isEmpty(shortUrl)) {
                log.info("转链失败========>");
                return null;
            } else {
                log.info("转链前======>{},转链后======>{}", s, shortUrl);
                content_after = content_after.replace(s, shortUrl);
            }
        }

        if (i == 0) {
            log.info("没有匹配到京东短链接===============>");
            return null;
        }

        return content_after;
    }

    public static boolean strLengh(String str) {
        String result = str;
        List<String> allUrl = getAllUrl(str);
        if (CollectionUtils.isEmpty(allUrl)) {
            return false;
        }

        for (String s : allUrl) {
            result = result.replaceAll(s, "");
        }
        if (result.length() > 6 && result.length() < 40) {
            return true;
        }
        return false;
    }
}
