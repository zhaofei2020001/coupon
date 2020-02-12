package com.common.util.jd;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.util.HttpUtils;
import com.google.common.collect.Lists;
import com.xiaoleilu.hutool.json.JSONUtil;
import jd.union.open.goods.jingfen.query.response.Coupon;
import jd.union.open.goods.jingfen.query.response.JFGoodsResp;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * @author zf
 * since 2019/12/20
 */
@Slf4j
public class Utils {
  /**
   * 将连接转为以http开头的完整url
   *
   * @param url
   * @return
   */
  public static String toHttpUrl(String url) throws Exception {
    if (url.startsWith("http")) {
      return (url);
    }
    if (url.startsWith("//")) {
      return ("http:" + url);
    }
    if (url.startsWith("item")) {
      return ("http://" + url);
    }
    return null;
  }

  /**
   * 判断当前时间是否在某个时间区间内
   *
   * @param startTime
   * @param endTime
   * @return
   */
  public static boolean dateInSection(Long startTime, Long endTime) {
    DateTime now = DateTime.now();
    if (now.isAfter(startTime) && now.isBefore(endTime)) {
      return true;
    }
    return false;
  }


  /**
   * 找出最优惠的优惠券组合
   *
   * @param coupons 优惠券列表
   * @param money   商品原价
   * @return
   */
  public static List<Coupon> findMaxCoupon(List<Coupon> coupons, BigDecimal money) {
    List<Coupon> allCoupon = Lists.newArrayList();

    //京东优惠券（全品券，平台券）
    List<Coupon> jdCoupons = coupons.stream()
        .filter(it -> Arrays.asList(AllEnums.couponTypeEnum.QPQ.getCode(), AllEnums.couponTypeEnum.XPL.getCode()).contains(it.getBindType()))
        .filter(it -> new BigDecimal(it.getQuota().toString()).subtract(money).intValue() <= 0)
        .filter(it -> {
          if (Objects.isNull(it) || (Objects.nonNull(it) && Objects.isNull(it.getUseStartTime()) && Objects.isNull(it.getUseEndTime()))) {
            return false;
          }
          return dateInSection(it.getUseStartTime(), it.getUseEndTime());
        }).collect(Collectors.toList());
    //店铺优惠(店铺券，商品券)
    List<Coupon> dpCoupons = coupons.stream()
        .filter(it -> Arrays.asList(AllEnums.couponTypeEnum.XDP.getCode(), AllEnums.couponTypeEnum.DPXSP.getCode()).contains(it.getBindType()))
        .filter(it -> new BigDecimal(it.getQuota().toString()).subtract(money).intValue() <= 0)
        .filter(it -> {
          if (Objects.isNull(it) || (Objects.nonNull(it) && Objects.isNull(it.getUseStartTime())) && Objects.isNull(it.getUseEndTime())) {
            return false;
          }
          return dateInSection(it.getUseStartTime(), it.getUseEndTime());
        }).collect(Collectors.toList());


    if (!CollectionUtils.isEmpty(jdCoupons)) {
      Collections.sort(jdCoupons, new Comparator<Coupon>() {
        @Override
        public int compare(Coupon t1, Coupon t2) {
          // 按照学生的年龄进行升序排列
          if (t1.getDiscount().intValue() > t2.getDiscount().intValue()) {
            return 1;
          }
          if (t1.getDiscount().intValue() == t2.getDiscount().intValue()) {
            return 0;
          }
          return -1;
        }
      });

      allCoupon.add(jdCoupons.get(jdCoupons.size() - 1));
    }

    if (!CollectionUtils.isEmpty(dpCoupons)) {

      Collections.sort(dpCoupons, new Comparator<Coupon>() {
        @Override
        public int compare(Coupon t1, Coupon t2) {
          // 按照学生的年龄进行升序排列
          if (t1.getDiscount().intValue() > t2.getDiscount().intValue()) {
            return 1;
          }
          if (t1.getDiscount().intValue() == t2.getDiscount().intValue()) {
            return 0;
          }
          return -1;
        }
      });

      allCoupon.add(dpCoupons.get(dpCoupons.size() - 1));
    }

    return allCoupon;
  }

  /**
   * 获取商品优惠券二合一连接
   *
   * @param str 商品skuId
   * @return
   */
  public static String getShortUrl(String str) {

    //蚂蚁星球地址
    String URL = Constants.ANT_SERVER_URL;

    HashMap map = new HashMap();
    map.put("apikey", Constants.ANT_APP_KEY);
    map.put("goods_id", str);

    map.put("positionid", Constants.JD_TGW_ID);
    //	type=1 goods_id=商品ID，type=2 goods_id=店铺id，type=3 goods_id=自定义链接(京东活动链接、二合一链接)
    map.put("type", "3");

    String requestResult = HttpUtils.post(URL, JSONUtil.toJsonPrettyStr(map));
    String twoToOneUrl = JSONObject.parseObject(requestResult.replace("\\", "")).getString("data");

    return twoToOneUrl;
  }

  /**
   * 如果是秒杀/拼购商品 返回秒杀拼购价,否则返回原价
   *
   * @return
   */
  public static BigDecimal getPriceIfPinGouSeckillInfo(JFGoodsResp jfGoodsResp) {
    //拼购商品
    if (Objects.nonNull(jfGoodsResp.getPinGouInfo()) && Objects.nonNull(jfGoodsResp.getPinGouInfo().getPingouPrice()) && dateInSection(jfGoodsResp.getPinGouInfo().getPingouStartTime(), jfGoodsResp.getPinGouInfo().getPingouEndTime())) {
      if (dateInSection(jfGoodsResp.getPinGouInfo().getPingouStartTime(), jfGoodsResp.getPinGouInfo().getPingouEndTime())) {
        return new BigDecimal(jfGoodsResp.getPinGouInfo().getPingouPrice().toString());
      }
      //秒杀商品
    } else if (Objects.nonNull(jfGoodsResp.getSeckillInfo()) && Objects.nonNull(jfGoodsResp.getSeckillInfo().getSeckillPrice()) && dateInSection(jfGoodsResp.getSeckillInfo().getSeckillStartTime(), jfGoodsResp.getSeckillInfo().getSeckillEndTime())) {
      if (dateInSection(jfGoodsResp.getSeckillInfo().getSeckillStartTime(), jfGoodsResp.getSeckillInfo().getSeckillEndTime())) {
        return new BigDecimal(jfGoodsResp.getSeckillInfo().getSeckillPrice().toString());
      }
    }
    return new BigDecimal(jfGoodsResp.getPriceInfo().getPrice().toString());
  }

  /**
   * 递归遍历字符串中所有需要转链的链接
   *
   * @param content
   * @return
   */
  public static Map<String, String> getUrlMap(String allcontent, String content, Map<String, String> map, int flag) {

    Matcher matcher = Patterns.WEB_URL.matcher(content);
    if (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      String substring = content.substring(start, end);
      int index = getFirstHz(substring);
      if (index > -1) {
        substring = content.substring(start, start + index);
        end = index;
      }

      if (StringUtils.isEmpty(substring) || (!substring.contains("http"))) {
        return null;
      }

      String substring1 = content.substring(end);
      int i = allcontent.indexOf(substring);
      if (!substring.contains("http")) {
        return null;
      }
      map.put(substring, getShortUrl(substring));
      map.putAll(getUrlMap(allcontent, substring1, map, i));
    }
    return map;
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
      String shortUrl = toLink_ddx(substring);

      if (StringUtils.isEmpty(shortUrl)) {
        return null;
      }
      map.put(substring, shortUrl);
      map.putAll(getUrlMap2(allcontent, substring1, map, i));
    }
    return map;
  }

  /**
   * 获取字符串第一个汉字的位置
   *
   * @param s
   * @return
   */
  public static int getFirstHz(String s) {
    for (int index = 0; index <= s.length() - 1; index++) {
      //将字符串拆开成单个的字符
      String w = s.substring(index, index + 1);
      if (w.compareTo("\u4e00") > 0 && w.compareTo("\u9fa5") < 0) {
        return index;
      }
    }
    return -1;
  }

  /**
   * 将原字符串中的所有连接替换为转链之后的连接 ，返回新的字符串
   *
   * @param str
   * @return
   */
  public static String getHadeplaceUrlStr2(String str, String reminder) {
    Map<String, String> urlMap = new HashMap<>();
    Map<String, String> map = getUrlMap(str, str, urlMap, 0);
    if (Objects.equals(map, null)) {
      return null;
    }
    String str2 = str;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      str2 = str2.replace(entry.getKey(), entry.getValue());
    }

    try {
      return URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(str2 + reminder), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 获取商品的图片地址
   *
   * @param skuId 商品skuId
   * @return
   */
  public static String getSKUInfo(String skuId) {
    //蚂蚁星球地址
    String URL = "http://api-gw.haojingke.com/index.php/v1/api/jd/goodsdetail";

    HashMap map = new HashMap();
    map.put("apikey", Constants.ANT_APP_KEY);
    map.put("goods_id", skuId);
    map.put("isunion", "0");

    String requestResult = HttpUtils.post(URL, JSONUtil.toJsonPrettyStr(map));

    return requestResult;
  }

//  /**
//   * 将原字符串中的所有连接替换为转链之后的连接 ，返回新的字符串
//   *
//   * @param str
//   * @return
//   */
//  public static String getHadeplaceUrlStr(String str, String reminder, String taobaoRobotId) {
//    //淘宝转链
//    if (!StringUtils.isEmpty(taobaoRobotId)) {
//      String substring = "";
//      int i = str.indexOf("(");
//      int rmb = str.indexOf("￥");
//      if (i != -1 && Objects.equals(str.charAt(i + 12) + "", ")")) {
//        int i1 = str.indexOf(")") + 1;
//        substring = str.substring(i, i1);
//      } else if (rmb != -1 && Objects.equals(str.charAt(rmb + 12) + "", "￥")) {
//        int i1 = str.lastIndexOf("￥") + 1;
//        substring = str.substring(rmb, i1);
//      }
//      String s = tbToLink(substring);
//      if (StringUtils.isEmpty(s)) {
//        return null;
//      }
//      String replace;
//      if (str.contains("http://t.uc.cn")) {
//        replace = str.replace(substring, s).replace(str.substring(str.indexOf("http"), str.indexOf("http") + 22), "");
//
//      } else {
//        replace = str.replace(substring, s);
//      }
//      try {
//        return URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(replace), "UTF-8");
//      } catch (UnsupportedEncodingException e) {
//        return null;
//      }
//    }
//    try {
//      //京东转链
//      Map<String, String> urlMap = new HashMap<>();
//      Map<String, String> map = getUrlMap2(str, str, urlMap, 0);
//      if (Objects.equals(map, null) || map.size() == 0) {
//        return null;
//      }
//      String str2 = str;
//      for (Map.Entry<String, String> entry : map.entrySet()) {
//        str2 = str2.replace(entry.getKey(), entry.getValue());
//      }
//      return URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(str2 + reminder), "UTF-8");
//    } catch (UnsupportedEncodingException e) {
//      e.printStackTrace();
//    }
//    return null;
//  }

  /**
   * 喵有券 根据淘宝商品淘口令转链转为自己的淘口令
   *
   * @return 转链结果内容
   */
  public static List<String> tbToLink(String tkl) {
    if (StringUtils.isEmpty(tkl)) {
      return null;
    }
    List<String> list=Lists.newArrayList();

    String format = String.format(Constants.TKL_TO_SKU_INFO_REQUEST_URL, Constants.MYB_APPKey, Constants.tb_name, Constants.TBLM_PID, tkl);
    String request = HttpUtils.getRequest(format);
    String substring = request.substring(0, request.lastIndexOf("}") + 1);
    log.info("taobao转链后的字符串------------>:{}", substring);

    if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {
      String string = JSONObject.parseObject(substring).getJSONObject("data").getString("tpwd");
      if (StringUtils.isEmpty(string)) {
        return null;
      }

      list.add(string + "\n\n-------淘宝线报-------\n" + "复制文本信息打开淘宝app");
      String img_url = JSONObject.parseObject(substring).getJSONObject("data").getJSONObject("item_info").getString("pict_url");
      list.add(img_url);
      return list;
    } else {
      return null;
    }
  }


  /**
   * 将原字符串中的所有连接替换为转链之后的连接 ，返回新的字符串 (订单侠)
   *
   * @param str
   * @return
   */
  public static List<String> toLinkByDDX(String str, String reminder, String taobaoRobotId) {
    List<String> list = Lists.newArrayList();

    //淘宝转链
    if (!StringUtils.isEmpty(taobaoRobotId)) {
      String substring = "";
      int i = str.indexOf("(");
      int rmb = str.indexOf("￥");
      if (i != -1 && Objects.equals(str.charAt(i + 12) + "", ")")) {
        int i1 = str.indexOf(")") + 1;
        substring = str.substring(i, i1);
      } else if (rmb != -1 && Objects.equals(str.charAt(rmb + 12) + "", "￥")) {
        int i1 = str.lastIndexOf("￥") + 1;
        substring = str.substring(rmb, i1);
      }
      list = tbToLink(substring);
      if (Objects.isNull(list)) {
        return null;
      }
      String replace;
      if (str.contains("http://t.uc.cn")) {
        replace = str.replace(substring, list.get(0)).replace(str.substring(str.indexOf("http"), str.indexOf("http") + 22), "");

      } else {
        replace = str.replace(substring, list.get(0));
      }
      try {
        list.set(0, URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(replace), "UTF-8"));
        return list;
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }
    try {
      //京东转链
      Map<String, String> urlMap = new HashMap<>();
      Map<String, String> map = getUrlMap2(str, str, urlMap, 0);
      if (Objects.equals(map, null) || map.size() == 0) {
        return null;
      }
      String str2 = str;
      for (Map.Entry<String, String> entry : map.entrySet()) {
        str2 = str2.replace(entry.getKey(), entry.getValue());
      }
      list.add(URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(str2 + reminder), "UTF-8"));
      //京东商品的购买链接
      String sku_url = MapUtil.getFirstNotNull(map);
      String skuId = getSkuIdByUrl(sku_url);
      String sku_img_url = getImgUrlBySkuId(skuId);

      list.add(sku_img_url);
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
    if(StringUtils.isEmpty(skuId)){
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

//  public static void main(String[] args) {
//    System.out.println(getImgUrlBySkuId("50073471468"));
//  }
}
