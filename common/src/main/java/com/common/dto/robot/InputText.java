package com.common.dto.robot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zf
 * since 2019/12/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputText {
  /**
   * 直接输入文本
   */
  private String text;
}
