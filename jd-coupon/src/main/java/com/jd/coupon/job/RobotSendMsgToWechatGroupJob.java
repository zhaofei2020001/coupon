package com.jd.coupon.job;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.wechat.WechatUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

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

  @Value("${robot.send.message.to.wechat.group}")
  private String groupName;

  public RobotSendMsgToWechatGroupJob() {
  }

  /**
   * 定时让机器人发送商品信息到群指定群
   */
  @Scheduled(cron = "* */2 * * * ?")
  public void sendMsg() {

    String wechatGroupName = AllEnums.wechatGroupEnum.getStr(groupName);
    if (StringUtils.isEmpty(wechatGroupName)) {
      log.info("没有找到群:{}----------------------->", groupName);
      return;
    }

    //将要发送到指定微信群中的商品信息
    String msg = (String) redisTemplate.opsForList().leftPop(wechatGroupName);
    //指定群的群id
    String grupuId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.GROUP.getDesc(), wechatGroupName);
    String robotId = (String) redisTemplate.opsForValue().get(AllEnums.wechatMemberFlag.ROBOT.getDesc());

    if (StringUtils.isEmpty(msg) || StringUtils.isEmpty(grupuId) || StringUtils.isEmpty(robotId)) {
      return;
    }

    if (msg.contains(Constants.SPLIT_FLAG)) {


    } else {
      WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, grupuId, msg, null);
      log.info("机器人发送参数------>{}",JSONObject.toJSONString(wechatSendMsgDto));
      String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
      log.info("微信发送结果:------>{}", s1);


    }


  }


}
