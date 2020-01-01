package com.jd.coupon.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * @author 微信机器人发送消息到微信群
 * since 2019/12/29
 */
@EnableScheduling
@Configuration
@Slf4j
public class RobotSendMsgToWechatGroupJob {

  @Value("#{'${message.from.group}'.split(',')}")
  private List<String> groupName;
  @Value("#{'${message.to.group}'.split(',')}")
  private List<String> toName;

  public RobotSendMsgToWechatGroupJob() {
  }


//  @Scheduled(cron = "*/3 * * * * ?")
  public void test() {
    log.info("groupName--->{},{}", groupName,toName);
  }


}
