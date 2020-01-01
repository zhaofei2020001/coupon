package com.jd.coupon.service;

import com.common.constant.AllEnums;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.jd.Utils;
import com.common.util.wechat.WechatUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
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
   * 发送线报使用哪个群中的机器人
   */
  @Value("${message.robot.group}")
  private String robotGroup;


  /**
   * 从love cat上接收微信消息
   *
   * @param receiveMsgDto
   */
  public void receiveWechatMsg(WechatReceiveMsgDto receiveMsgDto) {

//    //加载各个群的群id和机器人id
//    for (AllEnums.wechatGroupEnum value : AllEnums.wechatGroupEnum.values()) {
//      if (receiveMsgDto.getFrom_name().contains(value.getDesc())) {
//        redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.ROBOT.getDesc(), value.getDesc(), receiveMsgDto.getFinal_from_wxid());
//        redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.GROUP.getDesc(), value.getDesc(), receiveMsgDto.getFrom_wxid());
//      }
//    }



      //机器人
      String robotId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.ROBOT.getDesc(), AllEnums.wechatGroupEnum.getStr(robotGroup));

      //收集的线报将要发送到指定的群id
      List<String> message_to_groups = Lists.newArrayList();
      msgToGroup.forEach(it -> {
        String msg_will_send_group_id = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.GROUP.getDesc(), AllEnums.wechatGroupEnum.getStr(it));
        message_to_groups.add(msg_will_send_group_id);
      });

      msgFromGroup.forEach(it -> {
        //采集线报群中的机器人
        String jdshxbq_obotId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.ROBOT.getDesc(), AllEnums.wechatGroupEnum.getStr(it));

        //接收的线报消息来自配置的的线报群 中的机器人
        if (Objects.equals(jdshxbq_obotId, receiveMsgDto.getFinal_from_wxid())) {
          //发送的是文字
          if (AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type()) {

            //转链后的字符串
            String toLinkStr = Utils.getHadeplaceUrlStr(receiveMsgDto.getMsg());

            if (StringUtils.isEmpty(toLinkStr)) {
              //转链失败
              return;
            }

            //将转链后的线报发送到 配置的群中
            message_to_groups.forEach(item -> {
              WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, item, toLinkStr, null);
              String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
              log.info("微信消息发送结果----->:{},发送的群----->:{},发送的内容----->:{}", s1, receiveMsgDto.getFrom_name(), toLinkStr);
            });

            //发送的是图片
          } else if (AllEnums.wechatMsgType.IMAGE.getCode() == receiveMsgDto.getMsg_type()) {
              log.info("图片外网地址--->{}", receiveMsgDto.getFile_url());
          }

        }
      });
    }


}
