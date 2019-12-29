package com.common.util.jd;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import jd.union.open.goods.jingfen.query.response.Coupon;
import jd.union.open.goods.jingfen.query.response.JFGoodsResp;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author zf
 * since 2019/12/28
 */
@Slf4j
public class JdUtil {
  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  /**
   * 将京东商品按不同类型放入缓存
   */
  public void test() {

    for (AllEnums.eliteEnum value : AllEnums.eliteEnum.values()) {
      log.info("TYPE----------------------------->{}", value.getDesc());

      List<JFGoodsResp> jfGoodsRespList = JXGoodsQueryUtil.jxGoodsquery(value);

      Collections.sort(jfGoodsRespList, new Comparator<JFGoodsResp>() {
        @Override
        public int compare(JFGoodsResp t1, JFGoodsResp t2) {
          // 按照学生的年龄进行升序排列
          if (t1.getInOrderCount30DaysSku().intValue() > t2.getInOrderCount30DaysSku().intValue()) {
            return 1;
          }
          if (t1.getInOrderCount30DaysSku().intValue() == t2.getInOrderCount30DaysSku().intValue()) {
            return 0;
          }
          return -1;
        }
      });

      log.info("size---------->{}", jfGoodsRespList.size());

      for (int i = 0; i < jfGoodsRespList.size(); i++) {
        log.info("skuId---->{},skuName---->{},------->{}", jfGoodsRespList.get(i).getSkuId(), jfGoodsRespList.get(i).getSkuName(), i);
        redisTemplate.opsForList().leftPush(value.getDesc(), JSONObject.toJSONString(jfGoodsRespList.get(i)));
      }
    }
  }


  /**
   * 根据商品类型从redis中取出一件商品并组装成可以发送到微信群字符串的模式
   *
   * @return
   */
  public static String getJFGoodsRespByType(AllEnums.eliteEnum eliteEnum, RedisTemplate<String, Object> redisTemplate) throws Exception {
    StringBuilder sbTemplate = new StringBuilder();

    //9.9专区
//    if (Objects.equals(AllEnums.eliteEnum.NINENINEZQ.getDesc(), eliteEnum.getDesc())) {
    String ninezq;
    try {
      ninezq = (String) redisTemplate.opsForList().leftPop(eliteEnum.getDesc());
    } catch (Exception e) {
      return null;
    }


    JFGoodsResp jfGoodsResps = JSONObject.parseObject(ninezq, JFGoodsResp.class);

    if(Objects.isNull(jfGoodsResps)){
      return "*******************************"+eliteEnum.getDesc()+"已从缓存中取空*******************************";
    }

    List<Coupon> list = CollectionUtils.arrayToList(jfGoodsResps.getCouponInfo().getCouponList());

    StringBuilder str = new StringBuilder();

    //如果是秒杀/拼购商品 返回秒杀拼购价,否则返回原价
    BigDecimal priceIfPinGouSeckillInfo = Utils.getPriceIfPinGouSeckillInfo(jfGoodsResps);


    //最优优惠完组合
    List<Coupon> maxCoupon = Utils.findMaxCoupon(list, new BigDecimal(jfGoodsResps.getPriceInfo().getPrice().toString()));

    //优惠券组合金额
    Double count = maxCoupon.stream().mapToDouble(Coupon::getDiscount).sum();


    //实际价格
    BigDecimal totalMoney = priceIfPinGouSeckillInfo.subtract(new BigDecimal(count.toString()));

    //有优惠券
    if (maxCoupon.size() > 0) {
      //有优惠券并且 将来会有秒杀信息
      if (Objects.nonNull(jfGoodsResps.getSeckillInfo()) && Objects.nonNull(jfGoodsResps.getSeckillInfo().getSeckillStartTime()) && (DateTime.now().isBefore(jfGoodsResps.getSeckillInfo().getSeckillStartTime()))) {

        if (maxCoupon.size() == 1) {
          str.append("领券:").append("\r\n").append(Utils.toHttpUrl(maxCoupon.get(0).getLink())).append("\r\n")
              .append("———").append("\r\n").append("预告：该产品将于【").append(new DateTime(jfGoodsResps.getSeckillInfo().getSeckillStartTime()).toString("yyyy-MM-dd HH:mm:ss")).append("】参与秒杀更优惠！！！");
        }
        if (maxCoupon.size() > 1) {
          for (int i = 1; i <= maxCoupon.size(); i++) {
            str.append("第").append(i).append("张券:").append("\r\n").append(Utils.toHttpUrl(maxCoupon.get(i - 1).getLink())).append("\r\n");
          }
          str.append("———").append("\r\n").append("预告：该产品将于【").append(new DateTime(jfGoodsResps.getSeckillInfo().getSeckillStartTime()).toString("yyyy-MM-dd HH:mm:ss")).append("】参与秒杀更优惠！！！");

        }

      }


      //有优惠券并且 将来会有拼团购信息
      if (Objects.nonNull(jfGoodsResps.getPinGouInfo()) && Objects.nonNull(jfGoodsResps.getPinGouInfo().getPingouStartTime()) && (DateTime.now().isBefore(jfGoodsResps.getPinGouInfo().getPingouStartTime()))) {

        if (maxCoupon.size() == 1) {
          str.append("领券:").append("\r\n").append(Utils.toHttpUrl(maxCoupon.get(0).getLink())).append("\r\n")
              .append("———").append("\r\n").append("预告：该产品将于【").append(new DateTime(jfGoodsResps.getPinGouInfo().getPingouStartTime()).toString("yyyy-MM-dd HH:mm:ss")).append("】参与拼团购更优惠！！！");
        }
        if (maxCoupon.size() > 1) {
          for (int i = 1; i <= maxCoupon.size(); i++) {
            str.append("第").append(i).append("张券:").append("\r\n").append(Utils.toHttpUrl(maxCoupon.get(i - 1).getLink())).append("\r\n");
          }
          str.append("———").append("\r\n").append("预告：该产品将于【").append(new DateTime(jfGoodsResps.getPinGouInfo().getPingouStartTime()).toString("yyyy-MM-dd HH:mm:ss")).append("】参与拼团购更优惠！！！");
        }
      }

      //有优惠券 未来即无秒杀又无拼团
      if (!(Objects.nonNull(jfGoodsResps.getSeckillInfo()) && Objects.nonNull(jfGoodsResps.getSeckillInfo().getSeckillStartTime()) && (DateTime.now().isBefore(jfGoodsResps.getSeckillInfo().getSeckillStartTime()))) && !(Objects.nonNull(jfGoodsResps.getPinGouInfo()) && Objects.nonNull(jfGoodsResps.getPinGouInfo().getPingouStartTime()) && (DateTime.now().isBefore(jfGoodsResps.getPinGouInfo().getPingouStartTime())))) {
        if (maxCoupon.size() == 1) {
          str.append("领券:").append("\r\n").append(Utils.toHttpUrl(maxCoupon.get(0).getLink())).append("\r\n");
        }
        if (maxCoupon.size() > 1) {
          for (int i = 1; i <= maxCoupon.size(); i++) {
            str.append("第").append(i).append("张券:").append("\r\n").append(Utils.toHttpUrl(maxCoupon.get(i - 1).getLink())).append("\r\n");
          }
        }
      }

      //无优惠
    } else {
      //无优惠券但 将来会有秒杀信息
      if (Objects.nonNull(jfGoodsResps.getSeckillInfo()) && Objects.nonNull(jfGoodsResps.getSeckillInfo().getSeckillStartTime()) && (DateTime.now().isBefore(jfGoodsResps.getSeckillInfo().getSeckillStartTime()))) {
        str.append("———").append("\r\n").append("预告：该产品将于【").append(new DateTime(jfGoodsResps.getSeckillInfo().getSeckillStartTime()).toString("yyyy-MM-dd HH:mm:ss")).append("】参与秒杀更优惠！！！");
      }


      //无优惠券但 将来会有拼团购信息
      if (Objects.nonNull(jfGoodsResps.getPinGouInfo()) && Objects.nonNull(jfGoodsResps.getPinGouInfo().getPingouStartTime()) && (DateTime.now().isBefore(jfGoodsResps.getPinGouInfo().getPingouStartTime()))) {
        str.append("———").append("\r\n").append("预告：该产品将于【").append(new DateTime(jfGoodsResps.getPinGouInfo().getPingouStartTime()).toString("yyyy-MM-dd HH:mm:ss")).append("】参与拼团购更优惠！！！");
      }
    }


    sbTemplate.append(AllEnums.ownerEnum.getStr(jfGoodsResps.getOwner())).append(jfGoodsResps.getSkuName())
        .append("\r\n")
        .append("原价:").append(new BigDecimal(jfGoodsResps.getPriceInfo().getPrice().toString())).append("元,")
        .append("券后：").append(totalMoney).append("元")
        .append("\r\n")
        .append(Utils.toHttpUrl(jfGoodsResps.getMaterialUrl())).append("\r\n").append(str);


    return sbTemplate.toString();
//    }

//    return null;
  }


}
