package com.common.util.jd;

import com.common.constant.AllEnums;
import com.google.common.collect.Lists;
import jd.union.open.goods.jingfen.query.response.Coupon;
import jd.union.open.goods.jingfen.query.response.JFGoodsResp;
import org.joda.time.DateTime;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zf
 * since 2019/12/20
 */
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

//  /**
//   * 长链接转短链接
//   *
//   * @param longUrl
//   */
//  public static String getShortUrl(String longUrl) throws Exception {
//    String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";
//
//    HashMap map = new HashMap();
//    map.put("action", "long2short");
//    map.put("long_url", longUrl);
//    //发起请求到指定的接口，并且带上菜单json数据
//
//    //appid
//    String APPID = "wxe044566748010063";
//    //appsecret
//    String APPSECRET = "e6931617be5e9f9298501fc21292296d";
//    String request_url = TOKEN_URL.replace("APPID", APPID).replace("APPSECRET", APPSECRET);
//
//    JSONObject get = HttpUtils.tokenUtil(request_url, "GET", null);
//
//    String access_token = get.getString("access_token");
//
//    String requestResult = HttpUtils.post("https://api.weixin.qq.com/cgi-bin/shorturl?access_token=" + access_token, JSONObject.toJSONString(map));
//    JSONObject parse = (JSONObject) JSONObject.parse(requestResult);
//
//    return parse.getString("short_url");
//  }

  /**
   * 如果是秒杀/拼购商品 返回秒杀拼购价,否则返回原价
   *
   * @return
   */
  public static BigDecimal getPriceIfPinGouSeckillInfo(JFGoodsResp jfGoodsResp) {
    //拼购商品
    if (Objects.nonNull(jfGoodsResp.getPinGouInfo()) && Objects.nonNull(jfGoodsResp.getPinGouInfo().getPingouPrice())&&dateInSection(jfGoodsResp.getPinGouInfo().getPingouStartTime(), jfGoodsResp.getPinGouInfo().getPingouEndTime())) {
      if (dateInSection(jfGoodsResp.getPinGouInfo().getPingouStartTime(), jfGoodsResp.getPinGouInfo().getPingouEndTime())) {
        return new BigDecimal(jfGoodsResp.getPinGouInfo().getPingouPrice().toString());
      }
      //秒杀商品
    } else if (Objects.nonNull(jfGoodsResp.getSeckillInfo()) && Objects.nonNull(jfGoodsResp.getSeckillInfo().getSeckillPrice())&&dateInSection(jfGoodsResp.getSeckillInfo().getSeckillStartTime(), jfGoodsResp.getSeckillInfo().getSeckillEndTime())) {
      if (dateInSection(jfGoodsResp.getSeckillInfo().getSeckillStartTime(), jfGoodsResp.getSeckillInfo().getSeckillEndTime())) {
        return new BigDecimal(jfGoodsResp.getSeckillInfo().getSeckillPrice().toString());
      }
    }
    return new BigDecimal(jfGoodsResp.getPriceInfo().getPrice().toString());
  }
}
