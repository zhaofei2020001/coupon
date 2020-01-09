package com.jd.coupon.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
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

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;
  /**
   * 判定违规的关键字
   */
  @Value("#{'${message.key.word}'.split(',')}")
  private List<String> keyWords;

  public RobotSendMsgToWechatGroupJob() {
  }


//  @Scheduled(cron = "*/30 * * * * ?")
  public void test() throws InterruptedException {
    String cout  = (String)redisTemplate.opsForValue().get("msg_count");
    log.info("cout--->{}", cout);
  }


}
