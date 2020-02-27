package com.common.util.jd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author zf
 * since 2020/2/12
 */
@Slf4j
public class MapUtil {
  /**
   * 获取map中第一个非空数据值
   *
   * @param <>  Key的类型
   * @param <>  Value的类型
   * @param map 数据源
   * @return 返回的值
   */
  public static String getFirstNotNull(Map<String, String> map, RedisTemplate<String, Object> redisTemplate) {
    String result = null;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String skuUrl = entry.getValue();

      String skuId = Utils.getSkuIdByUrl(skuUrl);

      if (!StringUtils.isEmpty(skuId)) {
        Boolean jd_skui_send = redisTemplate.opsForValue().setIfAbsent(skuId, skuUrl);
        if (!jd_skui_send) {
          log.info("商品的skuId已经存在------>{},链接----->{}", skuId,skuUrl);
          return "HAD_SEND";
        } else {
          redisTemplate.opsForValue().set(skuId, skuUrl, 1, TimeUnit.HOURS);
        }
      }


      result = Utils.getImgUrlBySkuId(skuId);
      if (!StringUtils.isEmpty(result)) {
        return result;
      }
    }
    return result;
  }
}
