package com.common.util.jd;

import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author zf
 * since 2020/2/12
 */
public class MapUtil {
  /**
   * 获取map中第一个非空数据值
   *
   * @param <> Key的类型
   * @param <> Value的类型
   * @param map 数据源
   * @return 返回的值
   */
  public static String getFirstNotNull(Map<String, String> map) {
    String result = null;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String skuUrl = entry.getValue();

      String skuId = Utils.getSkuIdByUrl(skuUrl);
      result = Utils.getImgUrlBySkuId(skuId);
      if (!StringUtils.isEmpty(result)) {
        return result;
      }
    }
    return result;
  }
}
