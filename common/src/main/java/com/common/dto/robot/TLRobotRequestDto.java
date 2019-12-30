package com.common.dto.robot;

import lombok.Data;

/**
 * @author 图灵机器人请求体
 * since 2019/12/27
 */
@Data
public class TLRobotRequestDto {
  /**
   * 输入类型:0-文本(默认)、1-图片、2-音频
   */
  private int reqType;

  private Perception perception;

  private UserInfo userInfo;
}
