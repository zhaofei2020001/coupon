package com.jd.coupon.Domain;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * @author zf
 * since 2020/2/17
 */
@Component
@Data
public class ConfigDo {

  /**
   * 线报采集群
   */
  @Value("#{'${message.from.group}'.split(',')}")
  private List<String> msgFromGroup;
  /**
   * 线报发送群
   */
  @Value("#{'${message.to.group}'.split(',')}")
  private List<String> msgToGroup;

  /**
   * 自己所管理的群
   */
  @Value("#{'${message.own.group}'.split(',')}")
  private List<String> ownGroup;


  /**
   * 判定违规的关键字
   */
  @Value("#{'${message.key.word}'.split(',')}")
  private List<String> keyWords;

  /**
   * 判定违规后艾特某人的消息模板
   */
  @Value("${message.template}")
  private String template;

  /**
   * 发送线报使用哪个群中的机器人
   */
  @Value("${message.robot.group}")
  private String robotGroup;
  /**
   * 采集线报同一个群中的时间间隔  白天间隔
   */
  @Value("${message.time.dayspace}")
  private int dayspace;
  /**
   * 采集线报同一个群中的时间间隔
   */
  @Value("${message.time.nightspace}")
  private int nightspace;
  /**
   * 线报中提示语
   */
  @Value("${message.reminder}")
  private String reminderTemplate;

  /**
   * 采集线报同一个群中的时间间隔
   */
  @Value("${message.send.space}")
  private int senSpace;
  /**
   * 消除接收线报中的指定字符串
   */
  @Value("#{'${message.remove.tempate}'.split(',')}")
  private List<String> removeStr;

  /**
   * 白名单
   */
  @Value("#{'${message.white.user}'.split(',')}")
  private List<String> whiteUser;

//  /**
//   * 接收淘宝线报的群名称
//   */
//  @Value("#{'${message.taobao.robot}'.split(',')}")
//  private List<String> taobao;

  /**
   * 接收淘宝线报的群名称
   */
  @Value("#{'${message.keyWords}'.split(',')}")
  private List<String> msgKeyWords;
}
