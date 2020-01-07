package com.jd.coupon.service;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.robot.InputText;
import com.common.dto.robot.Perception;
import com.common.dto.robot.TLRobotRequestDto;
import com.common.dto.robot.UserInfo;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.HttpUtils;
import com.common.util.jd.Utf8Util;
import com.common.util.jd.Utils;
import com.common.util.wechat.WechatUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
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
   * 从love cat上接收微信消息
   *
   * @param receiveMsgDto
   */
  public void receiveWechatMsg(WechatReceiveMsgDto receiveMsgDto) {

//    //加载各个群的群id和机器人id
//    for (AllEnums.wechatGroupEnum value : AllEnums.wechatGroupEnum.values()) {
//
//      if (receiveMsgDto.getFrom_name().contains(value.getDesc())) {
//        redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.ROBOT.getDesc(), value.getDesc(), receiveMsgDto.getFinal_from_wxid());
//        redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.GROUP.getDesc(), value.getDesc(), receiveMsgDto.getFrom_wxid());
//      }
//    }
//    log.info("redisTemplate----->{}", receiveMsgDto);
    //机器人
    String robotId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.ROBOT.getDesc(), AllEnums.wechatGroupEnum.getStr(robotGroup));

    //判定是否违规
    boolean b = judgeViolation(receiveMsgDto);

    if (b) {
      String obj = (String) redisTemplate.opsForValue().get(receiveMsgDto.getMsg_type() + Constants.wechat_msg_illegal + receiveMsgDto.getFinal_from_wxid());
      redisTemplate.opsForValue().set(receiveMsgDto.getMsg_type() + Constants.wechat_msg_illegal + receiveMsgDto.getFinal_from_wxid(), "flag",5,TimeUnit.MINUTES);

      log.info("obj----->{}", obj);
      if (StringUtils.isNotBlank(obj)) {
        log.info("-------违规已经警告过了----------");
        redisTemplate.opsForValue().set(receiveMsgDto.getMsg_type() + Constants.wechat_msg_illegal + receiveMsgDto.getFinal_from_wxid(), "flag", 1000, TimeUnit.MILLISECONDS);
        return;
      }
      try {
        WechatSendMsgDto wechatSendMsgDto;
        String nick_name = (String) redisTemplate.opsForHash().get("wechat_friends", receiveMsgDto.getFinal_from_wxid());

        wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_AT_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(template), "UTF-8"), null, receiveMsgDto.getFinal_from_wxid(), nick_name);
        String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        if (-1 == JSONObject.parseObject(s1).getInteger("code")) {
          log.info("-----------发送失败重新发送-----------");
          wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_AT_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(template), "UTF-8"), null, receiveMsgDto.getFinal_from_wxid(), null);
          WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
        }

        String to_groupOwner="群成员昵称为:【"+nick_name+"】发了一条广告,请进群查看是否需要踢出该成员！";
        wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "du-yannan", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner), "UTF-8"), null, null, null);
        String s3 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        log.info("判定违规,昵称-->:{},微信id--->{},发送的结果--->:{},通知群主发广告结果----->{}", nick_name, receiveMsgDto.getFinal_from_wxid(), s1,s3);
        return;
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      return;
    }

//    //如果是向机器人发出文本提问,则返回文机器人回复的内容
//    String robotResponseStr = atRobotMsg(receiveMsgDto);
//
//    if (StringUtils.isNotBlank(robotResponseStr)) {
//      WechatSendMsgDto wechatSendMsgDto = null;
//      try {
//        wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(robotResponseStr), "UTF-8"), null, null, null);
//        String s = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
//        log.info("艾特机器人的时候,机器人回复消息发送短信的结果------>{}", s);
//        return;
//      } catch (UnsupportedEncodingException e) {
//        e.printStackTrace();
//      }
//
//    }


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
            log.info("timeFlag------>{},----->{}", timeFlag, StringUtils.isNotBlank(timeFlag));
            if (StringUtils.isNotBlank(timeFlag)) {

              String[] split = timeFlag.split(":");
              String s = split[1];
              log.info("s---->{},now--->{}", s, new DateTime(Long.parseLong(s)).plusMinutes(sendMsgSpace).isAfter(DateTime.now()));
              if (new DateTime(Long.parseLong(s)).plusMinutes(sendMsgSpace).isAfter(DateTime.now())) {
                log.info("距离上次发送时间间隔------->:{}分钟,-----------------消息不会被发送------------", new Date(System.currentTimeMillis() - Long.parseLong(s)).getMinutes());
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
          String toLinkStr = Utils.getHadeplaceUrlStr(receiveMsgDto.getMsg(), reminderTemplate);

          if (StringUtils.isEmpty(toLinkStr) || !toLinkStr.contains("http")) {
            //转链失败
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

          log.info("图片来自群id----------->{},消息来自群名称-->{}", receiveMsgDto.getFrom_wxid(), receiveMsgDto.getFrom_name());
          String msgFlag = (String) redisTemplate.opsForHash().get(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid());

          if (StringUtils.isNotBlank(msgFlag)) {
            String[] split = msgFlag.split(":");
            int i = Integer.parseInt(split[0]);
            Long l = Long.parseLong(split[1]);

            log.info("i-->{},l---->{}", i, l);
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
    });

  }

  /**
   * 判断是否违反群规
   *
   * @param receiveMsgDto
   */
  public boolean judgeViolation(WechatReceiveMsgDto receiveMsgDto) {
    //接收的不是群消息，不违规
    if (AllEnums.loveCatMsgType.GROUP_MSG.getCode() != receiveMsgDto.getType()) {
      return false;
    }

    //如果是自己人发送,则不违规
    if (Arrays.asList("du-yannan", "wxid_o7veppvw5bjn12", "wxid_2r8n0q5v38h222").contains(receiveMsgDto.getFinal_from_wxid())) {
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
    //发送的是视频、名片、位置信息、分享 判定违规
    if (Arrays.asList(AllEnums.wechatMsgType.xcx.getCode(),AllEnums.wechatMsgType.VIDEO.getCode(), AllEnums.wechatMsgType.CARD.getCode(), AllEnums.wechatMsgType.POSITION.getCode(), AllEnums.wechatMsgType.LINK.getCode()).contains(receiveMsgDto.getMsg_type())) {
      return true;
    }
    //如果发送的是文字消息
    if (AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type()) {
      //接收到的信息内容
      String msgContent = receiveMsgDto.getMsg();

      for (String keyWord : keyWords) {
        if (msgContent.contains(keyWord)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 如果是@机器人的消息则返回机器人回复的内容
   *
   * @param receiveMsgDto
   * @return
   */
  public String atRobotMsg(WechatReceiveMsgDto receiveMsgDto) {

    try {
      //接收的是否是群消息
      boolean flag1 = AllEnums.loveCatMsgType.GROUP_MSG.getCode() == receiveMsgDto.getType();
      //接收的是否是文字
      boolean flag2 = AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type();
      //是否是艾特机器人
      boolean flag3 = receiveMsgDto.getMsg().contains("[@at,nickname=京东小助手,wxid=wxid_o7veppvw5bjn12]");


      //自己所管理的所有群的 群id
      List<String> ownGroupIds = Lists.newArrayList();
      ownGroup.forEach(it -> {
        String groupId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.GROUP.getDesc(), AllEnums.wechatGroupEnum.getStr(it));
        ownGroupIds.add(groupId);

      });
      //消息是够来源我们自己管理的群
      boolean flag4 = ownGroupIds.contains(receiveMsgDto.getFrom_wxid());

      //接收的不是群消息， 并且是文字
      if (flag1 && flag2 && flag3 && flag4) {

        int i = receiveMsgDto.getMsg().indexOf("]");

        //发送给机器人的文本信息
        String substring = receiveMsgDto.getMsg().substring(i + 1);

        TLRobotRequestDto tlRobotRequestDto = new TLRobotRequestDto();
        Perception perception = new Perception();

        InputText inputText = new InputText(substring.trim());
        perception.setInputText(inputText);
        UserInfo userInfo = new UserInfo(Constants.ROBOT_API_KEY, "abc123");
        tlRobotRequestDto.setUserInfo(userInfo);
        tlRobotRequestDto.setPerception(perception);
        String robotResponseStr = HttpUtils.post(Constants.TL_ROBOT_URL, JSONObject.toJSONString(tlRobotRequestDto));

        int code = Integer.parseInt(JSONObject.parseObject(robotResponseStr).getJSONObject("intent").getString("code"));
        if (Arrays.asList(5000, 6000, 4000, 4001, 4002, 4005, 4007, 4100, 4200, 4300, 4400, 4500, 4600, 7002, 8008).contains(code)) {
          return "我还在学习中呢,还没有学到你提问的内容呢";
        } else if (4003 == code) {
          return "我今天已经回答好多问题了,我已经向主人申请调休了,请不要再艾特我了!";
        } else if (10008 == code) {
          String string = JSONObject.parseObject(robotResponseStr).getJSONArray("results").getJSONObject(0).getJSONObject("values").getString("text");

          log.info("向robot提问的内容------>:{},robot回复的内容------>:{}", substring, string);
          return string;
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
      return "我还在学习中呢,还没有学到你提问的内容呢";
    }

    return null;


  }


  public boolean setGroupRobotId(String groupName, String groupId, String robotId) {
    Boolean b1 = redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.GROUP.getDesc(), AllEnums.wechatGroupEnum.getStr(groupName), groupId);
    Boolean b2 = redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.ROBOT.getDesc(), AllEnums.wechatGroupEnum.getStr(groupName), robotId);
    return b1 && b2;
  }

//  public static void main(String[] args) throws UnsupportedEncodingException {
//    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_FRIEND_MEMBER.getCode(), "du-yannan", null, null, null, null, null);
//    wechatSendMsgDto.setGroup_wxid("17490589131@chatroom");
//    String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
//    String str = JSONObject.parseObject(s1).getString("data");


//    System.out.println("----------------------------------一下发送微信好友抢红包------------------------------");
//    String str = "%5B%7B%22wxid%22%3A%22wxid_k5d40s15sff622%22%2C%22nickname%22%3A%22%E7%88%B1%E5%A5%BD%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_5kg4uv7u932m12%22%2C%22nickname%22%3A%22%E4%BB%98%E8%8A%B3%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_o188y99i9p5q22%22%2C%22nickname%22%3A%22%E9%83%AD%E7%88%BD%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_u0tpyg51pmzq21%22%2C%22nickname%22%3A%22A%E4%B8%BD%E5%8D%8E%E5%BF%AB%E9%A4%90%26%E5%90%B4%E7%8E%89%E9%BE%99~15121190861%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ah4otvr0klnb22%22%2C%22nickname%22%3A%22%E9%AB%98%E7%BA%A7%E5%82%AC%E4%B9%B3%E5%B8%88%E3%80%81%E6%AF%8D%E4%B9%B3%E5%96%82%E5%85%BB%E6%8C%87%E5%AF%BC%E5%B8%88%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_xtopnxt323e422%22%2C%22nickname%22%3A%22%E6%9D%9C%E8%89%B3%E8%B6%8518736139957%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_xowel9m71dn121%22%2C%22nickname%22%3A%22%E9%99%88%E6%82%A6%3F%3FAnna%E7%BE%8E%E5%B9%B4%E5%9B%A2%E6%A3%80%2B%E4%B8%AA%E6%A3%80%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_a0v9u0sksyz622%22%2C%22nickname%22%3A%22%E5%B8%B8%E5%B7%9E~%E7%A5%81%E7%95%99%E5%A8%A5%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22du-yannan%22%2C%22nickname%22%3A%22%E6%9D%9C%E8%89%B3%E6%A5%A0%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_vp74lmurliyg22%22%2C%22nickname%22%3A%22%E6%A2%B5%E6%81%A9%E8%AF%97%3F%3F%20%E9%A3%9E%E9%B8%BD%E5%B0%8F%E5%8A%A9%E7%90%86%20%E5%8F%AF%E5%8F%AF%E8%B1%86%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_7er8ubv3rt6221%22%2C%22nickname%22%3A%22%E6%9D%8E%E6%B5%B7%E9%9C%9E%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_7u6orgl9dvo22%22%2C%22nickname%22%3A%22%E5%88%98%E9%9D%92%E4%BC%9F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_x9u4po8crnya22%22%2C%22nickname%22%3A%22%E7%BA%A2%E6%A2%85%E8%8A%B1%E5%84%BF%E5%BC%80%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_4897728976221%22%2C%22nickname%22%3A%22%E9%81%87%E8%A7%81%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_1znyldvog0mv11%22%2C%22nickname%22%3A%22CT%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_u318ejchnnb622%22%2C%22nickname%22%3A%22%E4%B8%8A%E5%96%84%E8%8B%A5%E6%B0%B4%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_mbiftw8f0oc222%22%2C%22nickname%22%3A%22Dream%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_nc7yzya5yprl12%22%2C%22nickname%22%3A%22%E5%88%AB%E5%90%8E%E6%9D%A5%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22beidi--%22%2C%22nickname%22%3A%22%E7%9F%AD%E5%8F%91%3F%3F%3F%3F%3F%3F%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_weey0iy180t722%22%2C%22nickname%22%3A%22Answer%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_rz801tzivrzt22%22%2C%22nickname%22%3A%22AI~%E5%BC%A0%E7%A7%8B%E4%B8%BD%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_2r8n0q5v38h222%22%2C%22nickname%22%3A%22zf%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_tqnohw9169di22%22%2C%22nickname%22%3A%22AAA%E4%B8%8D%E7%A6%BB%E4%B8%8D%E5%BC%83(%E5%85%B0%E8%8B%B1)%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_pdigq6tu27ag21%22%2C%22nickname%22%3A%22flysmilezhao%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_3lnk6evgezr521%22%2C%22nickname%22%3A%22%E9%BB%98%E9%BB%98%E8%80%95%E8%80%98%E3%80%81%E9%9D%99%E7%AD%89%E8%8A%B1%E5%BC%80o%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_bf6010lx87wl52%22%2C%22nickname%22%3A%22%20%20%20%E6%A9%99%E5%AD%90%20%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_vltu4e2dhpsx22%22%2C%22nickname%22%3A%22%E9%82%93%E6%85%A7%E6%B0%B8%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_9xp021iizzv212%22%2C%22nickname%22%3A%22%E8%8A%92%E6%9E%9C%E5%85%88%E7%94%9F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ypfqzptn7zod21%22%2C%22nickname%22%3A%22%E6%8A%8A%E6%89%8B%E7%BB%99%E6%88%91%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_bzfiasvj9ph222%22%2C%22nickname%22%3A%22%E5%B0%8F%E7%99%BD%E7%98%A6%E5%AD%90%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22bf-462%22%2C%22nickname%22%3A%22%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_bm5s0d1ezwv221%22%2C%22nickname%22%3A%22%E7%9A%AE%E5%8D%A1%E4%B8%98%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22gaoyan917036%22%2C%22nickname%22%3A%22%E9%AB%98%E6%9C%A8%E7%93%9C%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22duhuiqin002%22%2C%22nickname%22%3A%22%E5%B2%81%E6%9C%88%E9%9D%99%E5%A5%BD%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_tz268jo794rz21%22%2C%22nickname%22%3A%22%E5%8F%AA%E9%99%AA%E4%BB%96%E9%97%B9J%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_uwnbptimrm8122%22%2C%22nickname%22%3A%22%E4%BC%9A%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_bp94g3uo1i1p22%22%2C%22nickname%22%3A%22%E6%A2%93%E6%9B%A6%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22tf969317924%22%2C%22nickname%22%3A%22%E8%85%BE%E9%A3%9E%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_sp969ao05lvm52%22%2C%22nickname%22%3A%22%E7%94%B3%E7%94%B3%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22w494399583%22%2C%22nickname%22%3A%22%E7%90%BC%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wanghao890812%22%2C%22nickname%22%3A%22%E9%98%BF%E6%B5%A9%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_utnrzjr8bqe022%22%2C%22nickname%22%3A%22%E5%B9%B3%E5%87%A1%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_rsyzx76584m222%22%2C%22nickname%22%3A%22%E7%BA%A2%E5%94%87%E4%BD%B3%E4%BA%BA%E6%97%A5%E5%8C%96%E5%95%86%E8%A1%8C13653950879%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_d9y61n5u9zro22%22%2C%22nickname%22%3A%22%E5%AE%88%E6%8A%A4%E4%B8%BD%E4%BA%BA%3F%3F%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_s0gizxmkyehs12%22%2C%22nickname%22%3A%22%E9%9B%AA%E6%99%B4%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22z845698105%22%2C%22nickname%22%3A%22%3F%3F%20%20%E6%85%A7%E6%85%A7%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_yp9d53ntlrn122%22%2C%22nickname%22%3A%22%E5%BD%A6%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_3iwxgck1yxhb12%22%2C%22nickname%22%3A%22%E5%8D%AB%E6%99%AF%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_y62rbmf66kgz21%22%2C%22nickname%22%3A%22%E5%81%9A%E6%9C%80%E7%BE%8E%E7%9A%84%E8%87%AA%E5%B7%B1%E3%80%82%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_cl5ghdaefhxu21%22%2C%22nickname%22%3A%22%E5%91%A8%E4%BA%9A%E7%90%BC%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22yi68675901%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22chenghuan19931128%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_8sofyhvoo4p322%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_7kw0cr62g0gz22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_qvvs192gd5b622%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_6gy3rne2syhf21%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_s2pvgj6c358822%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_f687t8t2xcry22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22zhmsdzw521%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_d8llagvoee7v22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_obvxtrn2nezm22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_lcdqvjptm7v621%22%2C%22nickname%22%3A%220707%3F%3F%3F%3F%3F%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22zwt19920212%22%2C%22nickname%22%3A%22%3F%3F%E9%99%88%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_d6ahnei33mjn22%22%2C%22nickname%22%3A%22%E7%8F%8D%E7%88%B1%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_iq17wk93uuse22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22an8459%22%2C%22nickname%22%3A%22%E5%AE%89%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22cc260707%22%2C%22nickname%22%3A%22%E9%98%BFc%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_jvl6zfj3raut22%22%2C%22nickname%22%3A%22%E3%80%82%E3%80%82%E3%80%82%E3%80%82%E3%80%82%E3%80%82%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_sey0l6rbs6sl21%22%2C%22nickname%22%3A%22%E9%98%BF%E7%BE%8E%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_nb8s7xerfd8o21%22%2C%22nickname%22%3A%22summer%E6%9D%9C%E7%BA%A2%E8%89%B3%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22leroywind%22%2C%22nickname%22%3A%22%E8%94%A1%E5%AE%8F%E7%8E%89%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_cl6fc38fr7kd51%22%2C%22nickname%22%3A%22%E8%BD%A9%E7%84%95%E7%84%95%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_n61xmac89n2l41%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22weixin1354644532qq%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_docyignbqgok22%22%2C%22nickname%22%3A%22%E5%B0%8F%E6%98%8E%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_kw8kpd9yztfz12%22%2C%22nickname%22%3A%22%E6%95%AC%E6%95%AC%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22lyh852479063%22%2C%22nickname%22%3A%22%E5%80%9A%E6%A5%BC%E5%90%AC%E9%A3%8E%E9%9B%A8%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_nuwp68kx84gq22%22%2C%22nickname%22%3A%22%E9%87%8E%E9%85%92%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_9377163771012%22%2C%22nickname%22%3A%22%E8%B7%AF%E6%98%93%20.g%E4%B8%9C%E6%96%B9%E7%A5%9E%E9%9F%B5%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_7qne3m2t1dxr21%22%2C%22nickname%22%3A%22shining%20day%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_y9jl5s3yl2hc22%22%2C%22nickname%22%3A%22%E9%AB%98%E5%86%AC%E9%9B%AA%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_2f3rf1rbiqd822%22%2C%22nickname%22%3A%22%3F%3F%20L.S.H%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_6jng4cznoigj22%22%2C%22nickname%22%3A%22jy%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ff6ytsh6mxm522%22%2C%22nickname%22%3A%22%E6%9D%8E%E5%8D%A0%E4%B8%9C15993699960%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_jvy8kq9rsln722%22%2C%22nickname%22%3A%22%E9%BB%84%E5%AE%89%E6%98%BE%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_7jpu4nsw8gpq22%22%2C%22nickname%22%3A%22%E6%9D%8E%E5%85%B5%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_r8b55g4f1rdo22%22%2C%22nickname%22%3A%22%E9%A3%8E%E4%B8%AD%E6%9C%89%E6%A2%A6%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22longna55%22%2C%22nickname%22%3A%22A%20%3F%3F%E7%8C%AB%E7%8C%ABnala%3F%3F%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22liyi904%22%2C%22nickname%22%3A%22Dear_%E5%BC%BA%E5%A7%86%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_fgkr7f0ep1bj32%22%2C%22nickname%22%3A%22%E5%86%B0.%E5%87%9D%E6%AA%AC%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_u6mmer3zw3ut12%22%2C%22nickname%22%3A%22%E5%90%8D%E5%AD%97%E9%83%BD%E6%87%92%E5%BE%97%E8%B5%B7%E4%BA%86%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22jiwenyu1990%22%2C%22nickname%22%3A%22steven%E5%A7%AC%E4%BF%8A%E5%AE%87%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_lw9fexrgmpuj21%22%2C%22nickname%22%3A%22%E9%9F%A9%E6%99%93%E8%8B%B1%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22qidqorui472942147%22%2C%22nickname%22%3A%22%E6%B7%87%E9%94%90%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_v1q4587d2aqp31%22%2C%22nickname%22%3A%22TK%E6%B2%88%E5%AD%9D%E5%A8%81%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_gaxtef6f4jf022%22%2C%22nickname%22%3A%22A%20%E4%B8%AD%E4%BF%A1%E5%AD%99%E6%96%87%E8%BD%A9%2015618615528%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_qaobu9j7ddmi22%22%2C%22nickname%22%3A%22%E5%9B%BD%E5%AE%9D%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_v9ce8sihwp6921%22%2C%22nickname%22%3A%22%E7%8E%8B%E6%B0%B8%E4%BC%9F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_fi20tvaw82vc51%22%2C%22nickname%22%3A%22%E5%8F%B2%E5%9B%BD%E5%BC%BA%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wzy262168011%22%2C%22nickname%22%3A%22%E7%8E%8B%E5%BF%97%E8%BF%9C%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_jbyyg7h50han22%22%2C%22nickname%22%3A%22%E9%99%B6%E4%B8%BD%E5%90%9B%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22heiseqiqiu001%22%2C%22nickname%22%3A%22%E9%BB%91%E8%89%B2%26%E6%B0%94%E7%90%83%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22chuxingzhemao%22%2C%22nickname%22%3A%22%3F%3FMaoMao%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_fegj3k5tsch821%22%2C%22nickname%22%3A%22%E7%BE%8E%E7%BE%8E%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_oksud6wvx8qt22%22%2C%22nickname%22%3A%22%E6%AE%B7%E8%8A%AC13627246790%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22yezi516699%22%2C%22nickname%22%3A%22%E7%A9%BF%E8%B6%8A%E6%97%B6%E7%A9%BA%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ho8yrwzustgw21%22%2C%22nickname%22%3A%22%E6%9D%A8%E6%96%87%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_3311533115511%22%2C%22nickname%22%3A%22%E6%96%BD%E6%98%A5%E8%89%B3%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22yexiuhong4388%22%2C%22nickname%22%3A%22%E5%8F%B6%E7%A7%80%E7%BA%A2%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_5s3djboebs0622%22%2C%22nickname%22%3A%22cy940418%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22guoguo761538%22%2C%22nickname%22%3A%22%E6%9E%9C%E6%9E%9C%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ikuweas61nv522%22%2C%22nickname%22%3A%22%E6%9F%AF%E7%BE%8E%E7%A7%801%3F221%3F11%3F%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_5kce1y3g2r2l22%22%2C%22nickname%22%3A%22%E6%AF%8D%E5%A9%B4%E6%97%A0%E7%97%9B%E5%82%AC%E4%B9%B3%E5%A2%9E%E7%94%9F%E8%B0%83%E7%90%8615800622505%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_90z3j7bitic521%22%2C%22nickname%22%3A%22%E6%B5%85%E7%AC%91%EF%BC%86%EF%BF%A1%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_melx4qa36xk322%22%2C%22nickname%22%3A%22%E5%AD%99%E5%8F%8C%E6%9C%89%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_4ijiajo09tr841%22%2C%22nickname%22%3A%22Origin%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_kar8n11fu2tr22%22%2C%22nickname%22%3A%22%E6%87%BF%E6%A2%A6%E6%99%97%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_dlk6xymk369h22%22%2C%22nickname%22%3A%22%E8%B4%BA%E5%85%B0%E6%9C%B1%E7%8E%B2%E5%A8%A3%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_yhdockxwzu7s22%22%2C%22nickname%22%3A%22LX%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_md6pk8b4jiie22%22%2C%22nickname%22%3A%22%E6%9D%9C%E5%AD%A6%E8%B6%8515936068685%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_z4vvbtpbmf1m31%22%2C%22nickname%22%3A%22%E5%AD%90%E8%BD%A9%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_8tg41ijzhuj722%22%2C%22nickname%22%3A%22%E4%BC%9F%E4%BC%9F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_qp9jhwlg5kj411%22%2C%22nickname%22%3A%22%E7%98%A6%E5%AD%90%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22bm5201314999%22%2C%22nickname%22%3A%22%E9%95%81%E6%9B%9C%E8%90%A5%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22ZHY213713%22%2C%22nickname%22%3A%22Milky%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_giv2mg853q6p22%22%2C%22nickname%22%3A%22%E5%85%B0%E8%8A%AC%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_pmvco89azbjk22%22%2C%22nickname%22%3A%22%E5%B0%8F%E9%92%A2%E7%8E%89%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_uezk8al6akag21%22%2C%22nickname%22%3A%22WHY%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_jis8arumjhjn21%22%2C%22nickname%22%3A%22%E5%97%AF%E5%97%AF%E5%97%AF%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_5134701346612%22%2C%22nickname%22%3A%22%E5%8D%A2%E6%B2%9F%E6%99%93%E6%9C%88%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22hupo543256%22%2C%22nickname%22%3A%22%E9%99%88%E5%B0%8F%E8%99%8E%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22sweetstar918%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_6e78jkzymjne31%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_oovwct68wui122%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_4iayrriqn3ea22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_cwrcubglvgzg22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22hankangyu168%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_xrjbvjk8h7xo22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_g6lvq51du1oe21%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22lilulu9869%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22chacha831762%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22dengmengyao123%22%2C%22nickname%22%3A%22%E6%9E%97%E5%A4%95%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_6o51i1ahkdpd22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_6od8284k7eo422%22%2C%22nickname%22%3A%22%E6%B0%B4%E6%B8%85%E6%A2%A6%E8%93%9D%5EO%5E%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_gs6tinuk9fcl22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_99z2h8fbzh8o22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_rggeesd0wriu22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_wxzx6ya1hjzw22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_6cuog6931h3e21%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ckjii6ozx9aq22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_cwdw4ivo4jjk12%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_s53e6rnewwwb22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_p1to6oovu5yu22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_5yykj7aa8slc22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_xmlxck1d8no722%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22anshihaibo%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_z7gbiivdf2a312%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_29xd2mcmvwzj22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_n1b5t62g8drs22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22Xl554024756%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_03cn9f0f8c9r22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_2e555waew9ou21%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_34hhved9klcs22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22kangyong82%22%2C%22nickname%22%3A%22%E6%9F%A0%E6%AA%AC%E8%8C%B6%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_1qnkz227nnut22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_eaj9dhf8x7dl12%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_bn4mv4uaj68822%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_gtr6w7wpff4k22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_53o3315pfpsh22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wang2160492%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_cd5kjct9wxsk22%22%2C%22nickname%22%3A%22%E6%98%A5%E7%BA%A2%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_qcrxg70nl26w22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ibp5503xpdno22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_m85mbs56n04122%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_no8028zks57s22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_kmvcr1bifa1322%22%2C%22nickname%22%3A%22%E6%8B%9B%E6%89%8B%E6%9C%BA%E8%92%B9%E8%81%8C%E7%9A%84%E5%B0%8F%E8%8A%AC%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_k465qrgamhy022%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_xseuohmtekoh22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22gouchenyi%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22menjuan842535067%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_c6gye6ixcgf312%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_55niexfxm1mv22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_rurkabhaq04y22%22%2C%22nickname%22%3A%22%E7%B3%AF%E7%B3%AF%E7%9A%84%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_r1qptmf9f61922%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_mhc4rv6aw1bz12%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_b0voiymp7iso12%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22duhailong6619%22%2C%22nickname%22%3A%22%E6%97%A7%E5%A4%8F%E5%A4%A9OLd%20summe%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_27nbybnns7t721%22%2C%22nickname%22%3A%22A%E4%B8%B6%E6%9D%9C%E8%BE%9B%E6%B1%9F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_4fs94nylx42s22%22%2C%22nickname%22%3A%22%E6%A5%8A%E6%9E%9D%E7%94%98%E9%9C%B2%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_tvje7dp8i7u012%22%2C%22nickname%22%3A%22%E6%9F%A0%E6%AA%AC%E5%95%8A%E6%9F%A0%E6%AA%AC%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_qe3oiy7c8v3e22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_pjz6ue6qklse22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_lldv8y7yw22i12%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_kd28xx0sxgvs22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_2qr7ethboqtq22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ohhwal7a0gb922%22%2C%22nickname%22%3A%22%E5%88%AB%E7%86%AC%E5%A4%9C%E4%BA%86%EF%BC%8C%E4%BD%A0%E5%96%9C%E6%AC%A2%E7%9A%84%E4%BA%BA%E5%B7%B2%E7%BB%8F%E7%9D%A1%E4%BA%86%3F%3F%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_eyprdndxnhir22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_siegg13yax4k21%22%2C%22nickname%22%3A%22%E6%9E%9C%E5%AD%90%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22zhy494876405%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_nmcj0tmdhlzg22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_hm8v3vmgv0lc12%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ntj5kzdrcc9421%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_qnzrq5gk9u8e22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_h2oknvqlsv7i22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_mo2yyq1aqatu22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_vin40hm9dgm822%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_b7h4nafvinsu22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_rpyj43o6iqxo22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_kpj8zyp7c3ox22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22Love920808888%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_7xqqklld581322%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_lop6kzbuqb7z21%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ros0c39f4emf22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_59yojvl6b9cc22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_fjzhyqcshp2o22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_4242h4575yhv22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_2ukimubdjjyn22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_csm6parjs9sb22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_1209c7ihijx122%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_uarke22z6ek022%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_aot4lhu7ru9h11%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_o7veppvw5bjn12%22%2C%22nickname%22%3A%22%E4%BA%AC%E4%B8%9C%E5%B0%8F%E5%8A%A9%E6%89%8B%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_7susrobzapse22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_qarjx9u09dr521%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_bhwscceyj6si12%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_idenklcivwpw22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_0ij1w3ybmrwo22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_h1xnrgl4h0h222%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22qian07107%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_sudlf9hdv6ci21%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_oqpm8isk0by722%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_5h1ub81yh8i121%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_gyavgp3gvwy312%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_zzvuwu3txvgz22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_747kceu6oxb221%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_77emjise1n9u12%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_8atvqi562wwy22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_777qd5kii5z322%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22witchxue%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_oj2wlvq0uts422%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_zvgej3kbeu8122%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_nn7djdkrjlrv31%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_5exwvqmzday722%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_8z5mvd2sqrmo22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_9wik37gvtmzt22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_t7oiuthmwr8k21%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_jz0ye8d4hrg212%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_uwc08cpufb1m22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_ymmmhdxn1rtd22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_r1g5taecfcp22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22mickey571997%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22lvpingshouxi%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_lkyelu6zbrgz22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_v8kgfc0q3dth12%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_pf03mu9a1d0x22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_xho38m0glpi122%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%2C%7B%22wxid%22%3A%22wxid_2lalb1l386cc22%22%2C%22nickname%22%3A%22%22%2C%22robot_wxid%22%3A%22du-yannan%22%7D%5D";
//    String encode = URLUtil.decode(str, "UTF-8");
//    List<RobotFriend> robotFriends = JSONObject.parseArray(encode).toJavaList(RobotFriend.class);
//    System.out.println(robotFriends.size());
//
//    String msg = "年货节京享红包\n" +
//        "京东年货节火力全开，瓜分千万京享红包！另有1月1日、7日奖池加倍，祝您福运满满！\n" +
//        "https://u.jd.com/33yDBs\n" +
//        "吉鼠报喜，纳新红包！发个红包给你，最高888元 ，1月1日 、1月7日奖池加码财运满满哦！fυ製@yFVfe2URZb@这段话后咑開最新版【京d0ng】";
//
//
//    robotFriends.forEach(it -> {
//      WechatSendMsgDto text_mssage = null;
//      try {
//        text_mssage = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), "du-yannan", it.getWxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(msg), "UTF-8"), null, null, null);
//      } catch (UnsupportedEncodingException e) {
//        e.printStackTrace();
//      }
//      String s1 = WechatUtils.sendWechatTextMsg(text_mssage);
//      log.info("文字:name----->{},s1---->{}", it.getNickname(), s1);
//
//
//      if (0 == JSONObject.parseObject(s1).getInteger("code")) {
//        WechatSendMsgDto image_message = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), "du-yannan", it.getWxid(), "C:\\Users\\Mac\\Desktop\\love\\cat\\tmp.jpg", null, null, null);
//        String s2 = WechatUtils.sendWechatTextMsg(image_message);
//        log.info("图片:name----->{},s1---->{}", it.getNickname(), s2);
//      }
//
//    });
//


//  }
}
