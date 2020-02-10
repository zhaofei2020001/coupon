package com.jd.coupon.service;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.jd.Utf8Util;
import com.common.util.jd.Utils;
import com.common.util.wechat.WechatUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


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
   * 采集线报同一个群中的时间间隔
   */
  @Value("${message.time.space}")
  private int sendMsgSpace;

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
   * 发送的线报先文字,再图片
   */
  @Value("${message.send.text_image}")
  private boolean text_image;
  /**
   * 发送的线报先文字,再图片
   */
  @Value("${message.remove.tempate}")
  private String removeStr;

  /**
   * 发送的线报先文字,再图片
   */
  @Value("${message.taobao.robot}")
  private String taobaoRobot;

  /**
   * 从love cat上接收微信消息
   *
   * @param receiveMsgDto
   */
  public void receiveWechatMsg(WechatReceiveMsgDto receiveMsgDto) {
//    log.info("receiveMsgDto---->{}", receiveMsgDto);
//
//    //加载各个群的群id和机器人id
//    for (AllEnums.wechatGroupEnum value : AllEnums.wechatGroupEnum.values()) {
//
//      if (receiveMsgDto.getFrom_name().contains(value.getDesc())) {
//        redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.ROBOT.getDesc(), value.getDesc(), receiveMsgDto.getFinal_from_wxid());
//        redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.GROUP.getDesc(), value.getDesc(), receiveMsgDto.getFrom_wxid());
//      }
//    }
    //机器人
    String robotId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.ROBOT.getDesc(), AllEnums.wechatGroupEnum.getStr(robotGroup));

    //收集淘宝线报的机器人
    String taobaoRobotId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.ROBOT.getDesc(), AllEnums.wechatGroupEnum.getStr(taobaoRobot));


    //判定是否违规
    boolean b = judgeViolation(receiveMsgDto, robotId);

    if (b) {
      String obj = (String) redisTemplate.opsForValue().get(receiveMsgDto.getMsg_type() + Constants.wechat_msg_illegal + receiveMsgDto.getFinal_from_wxid());
      redisTemplate.opsForValue().set(receiveMsgDto.getMsg_type() + Constants.wechat_msg_illegal + receiveMsgDto.getFinal_from_wxid(), "flag", 5, TimeUnit.MINUTES);

      log.info("obj----->{}", obj);
      if (StringUtils.isNotBlank(obj)) {
        log.info("-------违规已经警告过了----------");
        redisTemplate.opsForValue().set(receiveMsgDto.getMsg_type() + Constants.wechat_msg_illegal + receiveMsgDto.getFinal_from_wxid(), "flag", 1000, TimeUnit.MILLISECONDS);
        return;
      }
      try {
        WechatSendMsgDto wechatSendMsgDto;
        String nick_name = (String) redisTemplate.opsForHash().get("wechat_friends", receiveMsgDto.getFinal_from_wxid());

        wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_AT_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(template), "UTF-8"), null, receiveMsgDto.getFinal_from_wxid(), receiveMsgDto.getFinal_nickname());
        String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        if (-1 == JSONObject.parseObject(s1).getInteger("code")) {
          log.info("-----------发送失败重新发送-----------");
          wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_AT_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(template), "UTF-8"), null, receiveMsgDto.getFinal_from_wxid(), null);
          WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
        }

        log.info("判定违规,昵称-->:{},微信id--->{},发送的结果--->:{}", nick_name, receiveMsgDto.getFinal_from_wxid(), s1);
        return;
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      return;
    }

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
        log.info("线报群中的机器人---->:{},消息来自发送则的id----->{}", jdshxbq_obotId, receiveMsgDto.getFinal_from_wxid());
        log.info("wechat receive msg body----------------->{}", receiveMsgDto);
        //发送的是文字F
        if (AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type()) {

          try {
            String timeFlag = (String) redisTemplate.opsForHash().get(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid());

            if (StringUtils.isNotBlank(timeFlag)) {
              String[] split = timeFlag.split(":");
              String s = split[1];
              if (new DateTime(Long.parseLong(s)).plusMillis(sendMsgSpace).isAfter(DateTime.now())) {
                log.info("距离上次发送时间间隔------->:{}秒,-----------------消息不会被发送------------", (System.currentTimeMillis() - Long.parseLong(s)) / 1000);
                redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), AllEnums.wechatXBAddImg.YES.getCode() + ":" + s);
                return;
              } else {
                log.info("更新开始---->");
                redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), AllEnums.wechatXBAddImg.NO.getCode() + ":" + System.currentTimeMillis());
              }
            } else {
              log.info("缓存数据为空,添加时间---------------->");
              redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), AllEnums.wechatXBAddImg.NO.getCode() + ":" + System.currentTimeMillis());
            }

          } catch (Exception e) {
            log.info("----------出错了---------->");
            return;
          }
          //转链后的字符串
          String toLinkStr;

          String coutStr = (String) redisTemplate.opsForValue().get("msg_count");
          if (StringUtils.isBlank(coutStr)) {
            redisTemplate.opsForValue().set("msg_count", "1");
            //转链后的字符串
            toLinkStr = Utils.getHadeplaceUrlStr(receiveMsgDto.getMsg().replace(removeStr, ""), reminderTemplate, ObjectUtils.equals(taobaoRobotId, receiveMsgDto.getFinal_from_wxid()) ? taobaoRobotId : null);
          } else {

            redisTemplate.opsForValue().set("msg_count", (Integer.parseInt(coutStr) + 1) + "");
            if (Integer.parseInt(coutStr) % senSpace == 0) {
              toLinkStr = Utils.getHadeplaceUrlStr(receiveMsgDto.getMsg().replace(removeStr, ""), reminderTemplate, ObjectUtils.equals(taobaoRobotId, receiveMsgDto.getFinal_from_wxid()) ? taobaoRobotId : null);
            } else {
              toLinkStr = Utils.getHadeplaceUrlStr(receiveMsgDto.getMsg().replace(removeStr, ""), "", ObjectUtils.equals(taobaoRobotId, receiveMsgDto.getFinal_from_wxid()) ? taobaoRobotId : null);
            }
          }

          if (StringUtils.isEmpty(toLinkStr)) {
            log.info("-------转链失败-------");
            //转链失败
            redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), AllEnums.wechatXBAddImg.YES.getCode() + ":" + System.currentTimeMillis());
            return;
          }

          //将转链后的线报发送到 配置的群中
          message_to_groups.forEach(item -> {
            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, item, toLinkStr, null, null, null);
            String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
            log.info("微信消息发送结果----->:{},消息发送到群id--->{},信息来自群----->:{}", s1, item, receiveMsgDto.getFrom_name());
            if (Integer.parseInt(JSONObject.parseObject(s1).getString("code")) == 0) {
              log.info("缓存该文本信息暂时没有配图发送------------------------>");
              //当线报文字发送成功后 该线报文字信息有没有发送过图片信息
              redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), AllEnums.wechatXBAddImg.NO.getCode() + ":" + System.currentTimeMillis());
            }

          });

          //发送的是图片
        } else if (AllEnums.wechatMsgType.IMAGE.getCode() == receiveMsgDto.getMsg_type()) {
          try {
            log.info("time1-->{}", System.currentTimeMillis());
            Thread.sleep(4000L);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          log.info("time2-->{}", System.currentTimeMillis());
          log.info("图片来自群id----------->{},消息来自群名称-->{}", receiveMsgDto.getFrom_wxid(), receiveMsgDto.getFrom_name());
          String msgFlag = (String) redisTemplate.opsForHash().get(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid());

          if (!text_image) {
            message_to_groups.forEach(item -> {
              //发送图片
              WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), robotId, item, receiveMsgDto.getMsg(), null, null, null);
              String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
              redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), AllEnums.wechatXBAddImg.YES.getCode() + ":" + System.currentTimeMillis());
              log.info("发送图片结果信息--------------->:{}", s1);
            });
          } else {
            if (StringUtils.isNotBlank(msgFlag)) {
              String[] split = msgFlag.split(":");
              int i = Integer.parseInt(split[0]);
              Long l = Long.parseLong(split[1]);

              log.info("i-->{},l---->{}", i, l);
//              if (i == AllEnums.wechatXBAddImg.NO.getCode() && System.currentTimeMillis() - l < 10000L) {
              if (i == AllEnums.wechatXBAddImg.NO.getCode()) {
                message_to_groups.forEach(item -> {
                  //发送图片
                  WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), robotId, item, receiveMsgDto.getMsg(), null, null, null);
                  String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                  redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), AllEnums.wechatXBAddImg.YES.getCode() + ":" + System.currentTimeMillis());
                  log.info("发送图片结果信息--------------->:{}", s1);
                });

              } else {
                log.info("---------------已发送过图片，本次不会发送图片---------------");
                return;
              }

            } else {
              log.info("---------------还没有发送文字信息,本次不会发送图片");
              return;
            }
          }


        }

      }
    });

  }

  /**
   * 判断是否违反群规
   *
   * @param receiveMsgDto
   */
  public boolean judgeViolation(WechatReceiveMsgDto receiveMsgDto, String robotId) {
    //接收的不是群消息，不违规
    if (AllEnums.loveCatMsgType.GROUP_MSG.getCode() != receiveMsgDto.getType()) {
      return false;
    }

    //如果是自己人发送,则不违规
    if (Arrays.asList("du-yannan", "wxid_o7veppvw5bjn12", "wxid_2r8n0q5v38h222", "wxid_pmvco89azbjk22", "wxid_pdigq6tu27ag21").contains(receiveMsgDto.getFinal_from_wxid())) {
      return false;
    }


    //自己所管理的所有群的 群id
    List<String> ownGroupIds = Lists.newArrayList();
    ownGroup.forEach(it -> {
      String groupId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.GROUP.getDesc(), AllEnums.wechatGroupEnum.getStr(it));
      ownGroupIds.add(groupId);

    });

    //接收的消息群消息  但不是发送到我们自己管理的群中的,不违规
    if (!ownGroupIds.contains(receiveMsgDto.getFrom_wxid())) {
      return false;
    }

    //代码走到这里表示：别人发在机器人所管理的群里发的群消息
    try {
      String obj = (String) redisTemplate.opsForValue().get(receiveMsgDto.getMsg_type() + Constants.wechat_msg_send + receiveMsgDto.getFinal_from_wxid());
      redisTemplate.opsForValue().set(receiveMsgDto.getMsg_type() + Constants.wechat_msg_send + receiveMsgDto.getFinal_from_wxid(), "flag", 5, TimeUnit.MINUTES);

      log.info("obj----->{}", obj);
      if (StringUtils.isNotBlank(obj)) {
        log.info("-------群内有人发消息已经通知过群主了----------");
        redisTemplate.opsForValue().set(receiveMsgDto.getMsg_type() + Constants.wechat_msg_send + receiveMsgDto.getFinal_from_wxid(), "flag", 1000, TimeUnit.MILLISECONDS);
      } else {

        String nick_name = (String) redisTemplate.opsForHash().get("wechat_friends", receiveMsgDto.getFinal_from_wxid());

        String to_groupOwner = "群成员昵称为:【" + nick_name + "】在群里发送了一个";
        String s1 = "";
        if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.IMAGE.getCode()) {

          WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "du-yannan", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.IMAGE.getDesc()), "UTF-8"), null, null, null);
          s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        } else if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.TEXT.getCode()) {

          WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "du-yannan", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.TEXT.getDesc() + ",文本内容为:" + receiveMsgDto.getMsg()), "UTF-8"), null, null, null);
          s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        } else if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.VIDEO.getCode()) {

          WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "du-yannan", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.VIDEO.getDesc()), "UTF-8"), null, null, null);
          s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        } else if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.CARD.getCode()) {

          WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "du-yannan", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.CARD.getDesc()), "UTF-8"), null, null, null);
          s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        } else if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.POSITION.getCode()) {

          WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "du-yannan", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.POSITION.getDesc()), "UTF-8"), null, null, null);
          s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        } else if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.Emoticon.getCode()) {

          WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "du-yannan", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.Emoticon.getDesc()), "UTF-8"), null, null, null);
          s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        } else if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.LINK.getCode()) {

          WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "du-yannan", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.LINK.getDesc()), "UTF-8"), null, null, null);
          s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        } else if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.xcx.getCode()) {

          WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "du-yannan", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.xcx.getDesc()), "UTF-8"), null, null, null);
          s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
        }
        log.info("通知群主发广告结果2----->{}", s1);
      }

    } catch (UnsupportedEncodingException e) {
      log.info("发消息失败了2------>{}", e);
      e.printStackTrace();
    }


    //发送的是视频、名片、位置信息、分享 判定违规
    if (Arrays.asList(AllEnums.wechatMsgType.xcx.getCode(), AllEnums.wechatMsgType.VIDEO.getCode(), AllEnums.wechatMsgType.CARD.getCode(), AllEnums.wechatMsgType.POSITION.getCode(), AllEnums.wechatMsgType.LINK.getCode()).contains(receiveMsgDto.getMsg_type())) {
      return true;
    }
    //如果发送的是文字消息
    if (AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type()) {
      //接收到的信息内容
      String msgContent = receiveMsgDto.getMsg();

      for (String keyWord : keyWords) {
        if (msgContent.contains(keyWord)) {

          //包含关键字：
          WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.DELETE_GROUP_MEMBER.getCode(), robotId, null, null, null, null, null);
          wechatSendMsgDto.setMember_wxid(receiveMsgDto.getFinal_from_wxid());
          wechatSendMsgDto.setGroup_wxid(receiveMsgDto.getFrom_wxid());
          String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

          log.info("违规将群成员踢出群聊结果----->:{}", s1);
          return true;
        }
      }
    }
    return false;
  }
}
