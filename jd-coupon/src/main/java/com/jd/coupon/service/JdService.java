package com.jd.coupon.service;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.dto.robot.TLRobotRequestDto;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.robot.TLRobotUtils;
import com.common.util.robot.Utils;
import com.common.util.wechat.WechatUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author zf
 * since 2019/12/27
 */
@Service
@Slf4j
public class JdService {
  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  public void receiveWechatMsg(WechatReceiveMsgDto receiveMsgDto) {

    redisTemplate.opsForList().leftPush("msg", JSONObject.toJSONString(receiveMsgDto));
    log.info("接受到来自:{}的消息-->:{}", receiveMsgDto.getFrom_name(), receiveMsgDto.getMsg());


    String s = Utils.jqStr(receiveMsgDto.getMsg());

    if (StringUtils.isNotBlank(s)) {

      TLRobotRequestDto robot = new TLRobotRequestDto(receiveMsgDto.getMsg_type());
      //文字
      if (Objects.equals(AllEnums.wechatMsgType.TEXT.getCode(), receiveMsgDto.getMsg_type())) {
        String result = TLRobotUtils.assembleRobotDto(robot, receiveMsgDto.getMsg());
        WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.wechatMsgType.TEXT.getCode(), receiveMsgDto.getRobot_wxid(), receiveMsgDto.getFrom_wxid(), result, null);

        String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
        log.info("result--->{}", s1);
        //图片
      } else if (Objects.equals(AllEnums.wechatMsgType.IMAGE.getCode(), receiveMsgDto.getMsg_type())) {

      }
    }


//    WechatSendMsgDto sendMsgDto = new WechatSendMsgDto();
//    sendMsgDto.setMsg(receiveMsgDto.getMsg());
//    sendMsgDto.setTo_wxid(receiveMsgDto.getFrom_wxid());
//    sendMsgDto.setRobot_wxid(receiveMsgDto.getRobot_wxid());
//    sendMsgDto.setType(receiveMsgDto.getType());
//    log.info("字符串----->{}", JSONObject.toJSONString(sendMsgDto, true));
//    String result = WechatUtils.sendWechatTextMsg(sendMsgDto);
//    log.info("result--->{}", result);
  }
}
