package com.common.dto.wechat;

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
public class WechatSendMsgDto {

  private int type;
  /**
   * 机器人id
   */
  private String robot_wxid;
  /**
   * 接收方id
   */
  private  String to_wxid;
  /**
   * 发送的文字消息（好友或群）
   */
  private String msg;
  /**
   * 发送图片绝对路径
   */
  private String path;

}
