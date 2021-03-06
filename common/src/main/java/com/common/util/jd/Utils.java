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

    private static String removestr;

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
    public static String getSKUInfo2(List<String> urlList, String antappkey, String rid, String skuId) {
        List<String> urls = Lists.newArrayList();

        if (urlList.size() == 1) {
            String skuUrl = getSKUInfo(skuId, antappkey);
            if (!StringUtils.isEmpty(skuUrl)) {
                FileSplitUtil.downloadPicture(skuUrl, rid);
                return Constants.BASE_URL + rid + ".jpeg";
            }
        }


        for (int i = 0; i < urlList.size(); i++) {
            String skuIdByUrl = Utils.getSkuIdByUrl(urlList.get(i));

            String skuUrl = getSKUInfo(skuIdByUrl, antappkey);
            log.info("sku Id=====>{},图==片====>{}", skuIdByUrl, skuUrl);
            if (!StringUtils.isEmpty(skuUrl)) {

                FileSplitUtil.downloadPicture(skuUrl, rid + i);

                urls.add(Constants.BASE_URL + rid + i + ".jpeg");
            }
        }


        if (CollectionUtils.isEmpty(urls)) {
            return null;
        }
        if (urls.size() == 1) {
            return urls.get(0);
        }

        BufferedImage merge = FileSplitUtil.merge(urls);

        FileSplitUtil.aabase64StringToImage(FileSplitUtil.getImageBinary(merge), rid);

        return Constants.BASE_URL + rid + ".jpeg";
    }

    /**
     * 将原字符串中的所有连接替换为转链之后的连接 ，返回新的字符串
     *
     * @param strString
     * @return
     */
    public static List<String> toLinkByDDX(String strString, String reminder, List<String> msgKeyWords, RedisTemplate<String, Object> redisTemplate, WechatReceiveMsgDto receiveMsgDto, Account account, boolean hadSkuId, boolean had_send, boolean flag) {
        String warn = "";
        //判断是否为淘宝线报
        boolean b = judgeIsTaoBao(strString,receiveMsgDto.getFinal_from_wxid());
        //淘宝转链
        if (b || had_send) {

            try {
                tbMsg(receiveMsgDto, account, redisTemplate);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        if ((!StringUtils.isEmpty(warn = msgContionMsgKeys(strString, msgKeyWords))) || strLengh(strString) || flag) {

            List<String> list = Lists.newArrayList();
            String str;

            try {
                str = strString;
                //所有连接
                List<String> allUrl = getAllUrl(strString);
                if (CollectionUtils.isEmpty(allUrl)&&!Objects.equals(receiveMsgDto.getFinal_from_wxid(),"wxid_0p28wr3n0uh822")) {
                    log.info("无链接==========>");
                    return null;
                }

                if (!hadSkuId && !(strString.contains("【京东领券") || strString.contains("领券汇总"))) {

                    String firstSkuId = MapUtil.getFirstSkuId(allUrl, redisTemplate,receiveMsgDto.getFinal_from_wxid());


                    if (Objects.equals("HAD_SEND", firstSkuId)) {

                        return Arrays.asList("1", "2", "3");
                    }
                    //排除京东40群
                    if (StringUtils.isEmpty(firstSkuId) && MapUtil.hadSendStr(allUrl, str, redisTemplate, account.getName())&&!Objects.equals(receiveMsgDto.getFinal_from_wxid(),"wxid_0p28wr3n0uh822")) {

                        return null;
                    }

                    String returnStr = zlStr(str, account, allUrl,receiveMsgDto.getFinal_from_wxid());
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

                            list.add(URLEncoder.encode(zlStr(str, account, allUrl,receiveMsgDto.getFinal_from_wxid()) + reminder, "UTF-8"));
                        }
                    } else {

                        String returnStr = zlStr(str, account, allUrl,receiveMsgDto.getFinal_from_wxid());
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
    public static boolean judgeIsTaoBao(String msg,String sendMsgRobotId) {
        if (msg.contains("https://u.jd.com/") || msg.contains("https://coupon.m.jd")||Objects.equals(sendMsgRobotId,"wxid_0p28wr3n0uh822")) {
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
    public static String zlStr(String content, Account account, List<String> list,String sendMsgRobotId) {
        int i = 0;
        String content_after = content;
        for (String s : list) {
            i++;
            String shortUrl = getShortUrl(s, account);
            if (StringUtils.isEmpty(shortUrl)) {
                //京东40
                if(Objects.equals(sendMsgRobotId,"wxid_0p28wr3n0uh822")){
                    shortUrl=s;
                }else{
                    log.info("转链失败========>");
                    return null;
                }
            } else {
                log.info("转链前======>{},转链后======>{}", s, shortUrl);
                content_after = content_after.replace(s, shortUrl);
            }
        }

        if (i == 0&&!Objects.equals(sendMsgRobotId,"wxid_0p28wr3n0uh822")) {
            log.info("没有匹配到京东短链接===============>");
            return null;
        }

        return content_after;
    }

    //长度是否符合规定
    public static boolean strLengh(String str) {
        String result = str;
        List<String> allUrl = getAllUrl(str);
        if (CollectionUtils.isEmpty(allUrl)) {
            return false;
        }

        for (String s : allUrl) {
            result = result.replaceAll(s, "");
        }
        if (result.length() > 6 && result.length() < 30) {
            return true;
        }
        return false;
    }

    public static void tbMsg(WechatReceiveMsgDto receiveMsgDto, Account accout, RedisTemplate<String, Object> redisTemplate) {
        String tkl = "";
        boolean flag = false;
        String flag2 = "";

        List<Object> tbmd = redisTemplate.opsForList().range("tbmd", 0, -1);


        for (int i = 0; i < tbmd.size(); i++) {
            //淘宝线报机器人id:群名称
            String zh = (String) tbmd.get(i);
            String[] array = zh.split(":");

            if (array[0].equals(receiveMsgDto.getFinal_from_wxid())) {
                tkl = pp(receiveMsgDto.getMsg());
                flag2 = haveKeyWord(receiveMsgDto.getMsg());
                if ((!StringUtils.isEmpty(tkl)) && !StringUtils.isEmpty(flag2)) {
                    flag = true;
                    break;
                }
            }
        }


        if (flag) {
//            List<Object> tbmd_remove = redisTemplate.opsForList().range("tbmd_remove", 0, -1);
            removestr = receiveMsgDto.getMsg();

//            if (!CollectionUtils.isEmpty(tbmd_remove)) {
//
//                tbmd_remove.forEach(it -> removestr = removestr.replace((String) it, ""));
//            }
////            if (!StringUtils.isEmpty(tkl) && removestr.contains("http")) {
//                removestr = removestr.substring(0, removestr.indexOf("http"));
//            }

            removestr = removestr.trim();
            if (removestr.endsWith(":")) {
                removestr = removestr.substring(0, removestr.length() - 1);
            }

            if (removestr.endsWith("/")) {
                removestr = removestr.substring(0, removestr.length() - 1).trim();
            }


            //===========将特价消息发送给群主===========
            accout.getMsgToPersons().forEach(it -> {
                try {
                    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), "wxid_8sofyhvoo4p322", it, URLEncoder.encode(removestr, "UTF-8"), null, null, null);
                    String s = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                    log.info("发送111========>{}", s);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            });
//发送免单线报到指定群中
//            if ("1".equals(flag2)) {
//
//                //将转链后的线报发送到 配置的群中
//                try {
//                    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), "wxid_8sofyhvoo4p322", accout.getGroupId(), URLEncoder.encode(removestr, "UTF-8"), null, null, null);
//                    String s = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
//                    log.info("发送222========>{}", s);
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
//            }


        }


    }

    //是否含有淘口令或者http
    public static String pp(String str) {

        String result = "";
        String pattern = "\\w{8,12}";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        if (m.find()) {
            result = m.group();
        }

        if (StringUtils.isEmpty(result) && str.contains("http")) {
            result = "1";
        }

        return result;
    }


    /**
     * 喵有券 根据淘宝商品淘口令返回图片地址
     *
     * @return 图片地址
     */
    public static String tbToLink2(String tkl) {
        if (StringUtils.isEmpty(tkl)) {
            return "";
        }

        try {
            String format = String.format(Constants.TKL_TO_SKU_INFO_REQUEST_URL, Constants.MYB_APPKey, Constants.tb_name, Constants.TBLM_PID, tkl);
            String request = HttpUtils.getRequest(format);
            String substring = request.substring(0, request.lastIndexOf("}") + 1);
            log.info("淘口令===>{},result=====>{}", tkl, substring);
            if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {

                String itemId = JSONObject.parseObject(substring).getJSONObject("data").getString("item_id");

                return itemId;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 折淘客高佣转链 （本接口只是返回图片地址）
     *
     * @param skuId
     * @return
     */
    public static String tkzJdToLink(String skuId) {
        if (StringUtils.isEmpty(skuId)) {
            return null;
        }
        try {
            String format = String.format(Constants.ztk_tkl_jd_toLink, skuId);
            String request = HttpUtils.getRequest(format).replace("/n", "").replace("\\", "");
            String string = JSONObject.parseObject(request).getJSONArray("content").getJSONObject(0).getString("pict_url");
            return string;
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }

    public static String haveKeyWord(String str) {
        // List<String> list2 = Arrays.asList("0元", "0.0元", "0.00元", "免单", "0.01元", "0.1", "0.2", "0.3", "0.4", "0.5", "0.10", "价格不对", "0.01","0入","0元入");
        List<String> list = Arrays.asList("0.0元", "0.00元", "免单", "0入", "0元入","0亓");
        List<String> list2 = Arrays.asList("0元", "0.01元", "0.1", "0.2", "0.3", "0.4", "0.5", "0.10", "价格不对", "0.01","0亓");

        if (str.startsWith("0元")) {
            return "1";
        }


        for (String s : list) {
            if (str.contains(s) && !str.contains("原价")) {
                return "1";
            }
        }

        for (String s : list2) {

            if (str.contains(s) && !str.contains("原价")) {
                return "2";
            }
        }
        return "";
    }
}
