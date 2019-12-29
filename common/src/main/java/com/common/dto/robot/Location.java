package com.common.dto.robot;

import lombok.Data;

/**
 * @author zf
 * since 2019/12/27
 */
@Data
public class Location {
  /**
   * 所在城市
   */
  private String city;
  /**
   * 省份
   */
  private String province;
  /**
   * 街道
   */
  private String street;
}
