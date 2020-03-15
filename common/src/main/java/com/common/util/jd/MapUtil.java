package com.common.util.jd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.TreeMap;
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
   * @param <>      Key的类型
   * @param <>      Value的类型
   * @param mapCopy 数据源
   * @return 返回的值
   */
  public static String getFirstNotNull(Map<String, String> mapCopy, RedisTemplate<String, Object> redisTemplate, String str) {
    //按键有序输出
    TreeMap<String, String> map = new TreeMap<>();

    for (Map.Entry<String, String> entry : mapCopy.entrySet()) {
      map.put(entry.getKey(), entry.getValue());
    }


    String result = null;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String replace = str.replace(entry.getKey(), "");

      Boolean jd_skui_send = redisTemplate.opsForValue().setIfAbsent(replace.substring(0, 10), "");

      if (jd_skui_send) {
        redisTemplate.opsForValue().set(replace.substring(0, 10), "", 20, TimeUnit.MINUTES);
      } else {
        log.info("京东商品的已经存在------>{}",replace.substring(0, 10));
        return "HAD_SEND";
      }

      String skuUrl = entry.getValue();
      String skuId = Utils.getSkuIdByUrl(skuUrl);
      log.info("京东id---->{}", skuId);
      result = Utils.getSKUInfo(skuId);
      if (!StringUtils.isEmpty(result)) {
        return result;
      }
    }
    return result;
  }

}
