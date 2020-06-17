package com.common.util.jd;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
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
//  public static String domain_name = "https://xdws20200322.kuaizhan.com/?taowords=";
//                                      https://32g01.kuaizhan.com/?sid=n6Bnam

  public static String domain = "20200322";
  public static String domain_name;


  /**
   * 获取商品优惠券二合一连接
   *
   * @param str 商品skuId
   * @return
   */
  public static String getShortUrl(String str) {

    try {
      //蚂蚁星球地址
      String URL = Constants.ANT_SERVER_URL;

      HashMap map = new HashMap();
      map.put("apikey", Constants.ANT_APP_KEY);
      map.put("goods_id", str);

      map.put("positionid", Constants.JD_TGW_ID);
      //	type=1 goods_id=商品ID，type=2 goods_id=店铺id，type=3 goods_id=自定义链接(京东活动链接、二合一链接)
      map.put("type", "3");

      String requestResult = HttpUtils.post(URL, JSONUtil.toJsonPrettyStr(map));
      if (Objects.equals("error", requestResult)) {
        return null;
      }
      String twoToOneUrl = JSONObject.parseObject(requestResult.replace("\\", "")).getString("data");

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
  public static Map<String, String> getUrlMap2(String allcontent, String content, Map<String, String> map, int flag) {


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
      String shortUrl = getShortUrl(substring);

      if (StringUtils.isEmpty(shortUrl)) {
        return null;
      }
      map.put(substring, shortUrl);
      map.putAll(getUrlMap2(allcontent, substring1, map, i));
    }
    return map;
  }

  /**
   * 获取商品的图片地址
   *
   * @param skuId 商品skuId
   * @return
   */
  public static String getSKUInfo(String skuId) {

    try {
      if (StringUtils.isEmpty(skuId)) {
        return null;
      }
      //蚂蚁星球地址
      String URL = "http://api-gw.haojingke.com/index.php/v1/api/jd/goodsdetail";

      HashMap map = new HashMap();
      map.put("apikey", Constants.ANT_APP_KEY);
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

    int i = url.indexOf("?word=");
    int i1 = url.indexOf("&image=");
    //原淘口令
    String old_tkl = url.substring(i + 6, i1);
    String new_tkl = null;

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
   * 将原字符串中的所有连接替换为转链之后的连接 ，返回新的字符串 (订单侠)
   *
   * @param strString
   * @return
   */
  public static List<String> toLinkByDDX(String strString, String reminder, List<String> msgKeyWords, RedisTemplate<String, Object> redisTemplate, String tbshopurl, WechatReceiveMsgDto receiveMsgDto) {

    if (!msgContionMsgKeys(strString, msgKeyWords, receiveMsgDto, redisTemplate)) {
      boolean flag = taobaoInterval(strString, redisTemplate, false);
      if (flag) {
        return Lists.newArrayList();
      }
    }

    //判断是否为淘宝线报
    boolean b = judgeIsTaoBao(strString);

    List<String> list = Lists.newArrayList();
    String str;
    //淘宝转链
    if (b) {

//      //---------如果是免单群直接返回---------
//      if (Objects.equals(receiveMsgDto.getFrom_wxid(), "23205855791@chatroom")) {
//
//        if (strString.contains("￥") || strString.contains("http") || strString.contains("红包口令")) {
//          try {
//            list.add(URLEncoder.encode(Utf8Util.remove4BytesUTF8Char("----免单线报(变价则失效)----\n" + strString), "UTF-8"));
//            list.add("");
//            return list;
//          } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//          }
//        }
//      }
//      //---------如果是免单群直接返回---------


      //---------如果是免单群直接返回---------
      if (Arrays.asList("23205855791@chatroom","23676378446@chatroom").contains(receiveMsgDto.getFrom_wxid())) {

        if ((strString.contains("0元") || strString.contains("免单") || strString.contains("红包口令"))&&((strString.contains("(")&&strString.contains(")"))||(strString.contains("￥")))) {
          try {
            list.add(URLEncoder.encode(Utf8Util.remove4BytesUTF8Char("----免单线报(变价则失效)----\n" + strString), "UTF-8"));
            list.add("");
            return list;
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
        }
      }
      //---------如果是免单群直接返回---------




      String replace;
      List<String> strList = getTBUrlMap(strString, redisTemplate);

      str = strList.get(0);

      if (str.contains("http://t.uc.cn")) {
        replace = str.replace(str.substring(str.indexOf("http://t.uc.cn"), str.indexOf("http://t.uc.cn") + 22), "");

      } else {
        replace = str;
      }

      if (!replace.contains("【淘宝") && !replace.contains("[淘宝")) {
        replace = "【淘宝】" + replace;
      }

//      if (!replace.contains("http")) {
//        return null;
//      }

      try {
        list.add(URLEncoder.encode(Utf8Util.remove4BytesUTF8Char("----免单线报(变价则失效)----\n" + replace + tbshopurl), "UTF-8"));
        list.add("");
        return list;
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }
    try {
      str = strString;
      //京东转链
      Map<String, String> urlMap = new HashMap<>();
      Map<String, String> map = getUrlMap2(str, str, urlMap, 0);
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

      if (!str2.contains("【京东") && !str2.contains("[京东")) {
        str2 = "【京东】" + str2;
      }
      log.info("消息长度----->{}", str2.length());
      if (str2.length() > 400 && (!str2.contains("京东领券")) && (!str2.contains("领券汇总"))) {
        return Lists.newArrayList();
      }


      list.add(URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(str2 + reminder), "UTF-8"));

      //购买京东商品的图片链接
      String sku_url = MapUtil.getFirstNotNull(map, redisTemplate, str);

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
   * 根据订单侠对京东链接转链
   *
   * @param link
   * @return
   */
  public static String toLink_ddx(String link) {
    try {
      String str = Constants.DDX_TOLINK_URL;
      String format = String.format(str, Constants.DDX_APIKEY, link, Constants.JDLM_ID);
      String request = HttpUtils.getRequest(format);
      String substring = request.substring(0, request.lastIndexOf("}") + 1);
      if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {
        return JSONObject.parseObject(substring).getJSONObject("data").getString("shortURL");
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }


  /**
   * 订单侠根据商品链接获取商品skuId
   *
   * @return
   */
  public static String getSkuIdByUrl(String url) {
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

  /**
   * 订单侠根据根据京东skuId获取商品图片的url
   *
   * @param skuId 京东商品skuId
   * @return
   */
  public static String getImgUrlBySkuId(String skuId) {
    if (StringUtils.isEmpty(skuId)) {
      return null;
    }
    try {
      String str = Constants.DDX_SKU_INFO;
      String format = String.format(str, Constants.DDX_APIKEY, skuId);
      String request = HttpUtils.getRequest(format);
      String substring = request.substring(0, request.lastIndexOf("}") + 1);

      if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {
        String string = JSONObject.parseObject(substring).getJSONArray("data").getJSONObject(0).getString("imgUrl");
        return string;
      } else {
        return null;
      }
    } catch (Exception e) {
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

    String url = "https://v1.alapi.cn/api/url/query?url=%s";
    String format = String.format(url, shortUrl);

    try {
      String request = HttpUtils.getRequest(format).replace("/n", "");
      String longUrl = JSONObject.parseObject(request).getJSONObject("data").getString("long_url");
      return longUrl;
    } catch (Exception e) {
      log.info("短链接还原淘口令出错了,原样返回短链接----->{}", e);
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
      map.put(substring, toTaoBaoTkl(shortToLong2(substring)));
      String substring1 = str.substring(end);
      dgGetTkl(substring1, map);
    }

    if (m != -1) {
      int start = m;
      int end = m + 21;
      String substring = str.substring(start, end);
      map.put(substring, toTaoBaoTkl(shortToLong2(substring)));
      String substring1 = str.substring(end);
      dgGetTkl(substring1, map);
    }

    if (n != -1) {
      int start = n;
      int end = n + 26;
      String substring = str.substring(start, end);
      map.put(substring, toTaoBaoTkl(shortToLong2(substring)));
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
    //如果是【禁言】淘礼金免单八群 直接返回truebia表示包含关键字
    if (Objects.equals(receiveMsgDto.getFrom_wxid(), "23205855791@chatroom") || checkMsgFrom(receiveMsgDto)) {
      return true;
    }else if(Objects.equals(receiveMsgDto.getFrom_wxid(), "23676378446@chatroom")){
      return false;
    }


//    //拦截所有tb 线报
//    if (judgeIsTaoBao(msg)) {
//      //排除"买+"
//      if (!msg.substring(0, 1).equals("买")) {
//        return false;
//      }
//    }
    String substring = msg.substring(0, 1);

    if (substring.equals("买") && "17490589131@chatroom".equals(receiveMsgDto.getFrom_wxid()) && msg.length() < 15) {
      String substring1 = msg.substring(1);

      redisTemplate.opsForList().leftPush("coustom_buy_goods_key", substring1);
      redisTemplate.expire("coustom_buy_goods_key", 5, TimeUnit.DAYS);


      try {
        String nick_name = (String) redisTemplate.opsForHash().get("wechat_friends", receiveMsgDto.getFinal_from_wxid());

        WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), "wxid_8sofyhvoo4p322", receiveMsgDto.getFrom_wxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char("@" + (StringUtils.isEmpty(nick_name) ? "" : nick_name) + "您的购买需求已知晓,小助将为您留意近5日的线报信息,如果有该类型商品出现促销活动,将会在线报群中发送,请留意线报信息！"), "UTF-8"), null, null, null);
        String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        log.info("记录用户的购买需求-->:{},发送的结果--->:{}", nick_name, s1);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

    }

    try {
      String coustom_buy_goods_key = (String) redisTemplate.opsForList().leftPop("coustom_buy_goods_key");
      if (!StringUtils.isEmpty(coustom_buy_goods_key)) {
        msgKeys.add(coustom_buy_goods_key);
      }
    } catch (Exception e) {

    }


    msgKeys.forEach(it -> {

      if (msg.contains(it) && (!msgFlag.get())) {

        if (it.equals("1元") && (msg.contains(".1元") || msg.contains("1元/"))) {

        } else {
          log.info("关键字--->{},原消息--->{}", it, receiveMsgDto);
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
   * @param path_ago 图片的地址
   * @return
   */
  public static boolean isHaveQr(String path_ago) {

    //http://:8073/static/1112031028.jpg
    String[] split = path_ago.split("/");
    String pic_Name = split[split.length - 1];

    ImageDown.saveToFile(pic_Name);

    String path = Constants.PIC_SAVE_PATH + pic_Name;

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


  public static boolean checkMsgFrom(WechatReceiveMsgDto receiveMsgDto) {
    boolean equals = Objects.equals(receiveMsgDto.getFrom_wxid(), "23676378446@chatroom");
    boolean b = (Arrays.asList("wxid_k76wy0x5zu6z22").contains(receiveMsgDto.getFinal_from_wxid())) && (receiveMsgDto.getMsg().contains("免单") || receiveMsgDto.getMsg().contains("0元"));
    return equals && b;
  }
}
