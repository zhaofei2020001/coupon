package com.jd.coupon.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author 微信机器人发送消息到微信群
 * since 2019/12/29
 */
@EnableScheduling
@Configuration
@Slf4j
public class RobotSendMsgToWechatGroupJob {
  @Autowired
  RedisTemplate<String, Object> redisTemplate;

//  @Scheduled(cron = "0/1 20 * * * ?")
  public void sendMsg() {

  }



}
