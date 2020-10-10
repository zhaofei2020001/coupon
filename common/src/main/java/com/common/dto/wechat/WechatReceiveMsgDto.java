package com.common.dto.wechat;

import lombok.Data;

/**
 * @author zf
 * since 2019/12/27
 */
@Data
public class WechatReceiveMsgDto {
  /**
   * 事件类型（事件列表可参考 - 事件列表demo）【事件数值】             【事件描述】
   *
   * 100                       私聊消息
   *
   * 200                       群聊消息
   *
   * 300                       暂无
   *
   * 400                       群成员增加
   *
   * 410                       群成员减少
   *
   * 500                       收到好友请求
   *
   * 600                       二维码收款
   *
   * 700                       收到转账
   *
   * 800                       软件开始启动
   *
   * 900                       新的账号登录完成
   *
   * 910                       账号下线
   */
  private int type;
  /**
   * 消息内容类型（仅在私聊和群消息事件中，代表消息的表现形式，如文字消息、语音、等等）1代表文字
   */
  private int msg_type;
  /**
   * 1级来源id（比如发消息的人的id） 群消息为群id  私聊信息则为发消息人的id
   */
  private String from_wxid;
  /**
   * 1级来源昵称（比如发消息的人昵称）
   */
  private String from_name;
  /**
   * 2级来源id（群消息事件下，1级来源为群id，2级来源为发消息的成员id，私聊事件下都一样）
   */
  private String final_from_wxid;
  /**
   * 2级来源昵称
   */
  private String final_from_name;
  /**
   * 当前登录的账号（机器人）标识id
   */
  private String robot_wxid;
  /**
   * 如果是文件消息（图片、语音、视频、动态表情），这里则是可直接访问的网络地址，非文件消息时为空
   */
  private String file_url;
  /**
   * 消息内容
   */
  private String msg;
  /**
   * 附加参数（暂未用到，请忽略）
   */
  private String parameters;
  /**
   * 请求时间(时间戳10位版本)
   */
  private int time;
}
