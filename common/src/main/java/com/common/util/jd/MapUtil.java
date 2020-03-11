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
      String skuUrl = entry.getValue();

      String skuId = Utils.getSkuIdByUrl(skuUrl);

      if (!StringUtils.isEmpty(skuId)) {
        Boolean jd_skui_send = redisTemplate.opsForValue().setIfAbsent(skuId, skuUrl);
        if (!jd_skui_send) {
          log.info("商品的skuId已经存在------>{},链接----->{}", skuId, skuUrl);
          return "HAD_SEND";
        } else {
          log.info("skuId-->{}", skuId);
          redisTemplate.opsForValue().set(skuId, skuUrl, 80, TimeUnit.MINUTES);
        }
      } else {
        String replace = str.replace(entry.getKey(), "");
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(replace, entry.getKey());

        if (!aBoolean) {
          log.info("msg已存在----->{}", replace);
          return "HAD_SEND";
        } else {
          redisTemplate.opsForValue().set(replace, entry.getKey(), 30, TimeUnit.MINUTES);
        }
      }


      result = Utils.getSKUInfo(skuId);
      if (!StringUtils.isEmpty(result)) {
        return result;
      }
    }
    return result;
  }

}
