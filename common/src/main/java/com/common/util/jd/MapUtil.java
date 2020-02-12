package com.common.util.jd;

import java.util.Map;

/**
 * @author zf
 * since 2020/2/12
 */
public class MapUtil {
  /**
   * 获取map中第一个非空数据值
   *
   * @param <K> Key的类型
   * @param <V> Value的类型
   * @param map 数据源
   * @return 返回的值
   */
  public static <K, V> V getFirstNotNull(Map<K, V> map) {
    V obj = null;
    for (Map.Entry<K, V> entry : map.entrySet()) {
      obj = entry.getValue();
      if (obj != null) {
        break;
      }
    }
    return obj;
  }
}
