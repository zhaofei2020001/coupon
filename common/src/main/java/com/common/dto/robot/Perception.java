package com.common.dto.robot;

import lombok.Data;

/**
 * @author 输入参数必须包含inputText或inputImage或inputMedia！
 * since 2019/12/27
 */
@Data
public class Perception {
  /**
   * 文本信息
   */
  private InputText inputText;
  /**
   * 图片信息
   */
  private InputImage inputImage;
  /**
   * 音频信息
   */
  private InputMedia inputMedia;
  /**
   * 客户端属性
   */
  private SelfInfo selfInfo;
}
