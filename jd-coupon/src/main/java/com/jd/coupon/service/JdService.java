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
import com.jd.coupon.Domain.ConfigDo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  ConfigDo configDo;

  private static String staticStr;


  /**
   * 从love cat上接收微信消息
   *
   * @param receiveMsgDto
   */
  public void receiveWechatMsg(WechatReceiveMsgDto receiveMsgDto) {

    int sendMsgSpace;

    if (nowTimeInNight()) {
      sendMsgSpace = configDo.getNightspace();
    } else {
      sendMsgSpace = configDo.getDayspace();
    }

    //加载各个群的群id和机器人id
    for (AllEnums.wechatGroupEnum value : AllEnums.wechatGroupEnum.values()) {

      if (receiveMsgDto.getFrom_name().contains(value.getDesc())) {
        redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.ROBOT.getDesc(), value.getDesc(), receiveMsgDto.getFinal_from_wxid());
        redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.GROUP.getDesc(), value.getDesc(), receiveMsgDto.getFrom_wxid());
      }
    }

    //自己管理群中发送线报的机器人
    String robotId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.ROBOT.getDesc(), AllEnums.wechatGroupEnum.getStr(configDo.getRobotGroup()));


    //获取淘宝的机器人id和群id  放到一个list中
    List<String> taoBaoIds = getTaoBaoIds(configDo, redisTemplate);


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

        wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_AT_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(configDo.getTemplate()), "UTF-8"), null, receiveMsgDto.getFinal_from_wxid(), receiveMsgDto.getFinal_nickname());
        String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

        if (-1 == JSONObject.parseObject(s1).getInteger("code")) {
          log.info("-----------发送失败重新发送-----------");
          wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_AT_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(configDo.getTemplate()), "UTF-8"), null, receiveMsgDto.getFinal_from_wxid(), null);
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
    configDo.getMsgToGroup().forEach(it -> {
      String msg_will_send_group_id = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.GROUP.getDesc(), AllEnums.wechatGroupEnum.getStr(it));
      message_to_groups.add(msg_will_send_group_id);
    });

    configDo.getMsgFromGroup().forEach(it -> {
      //采集线报群中的机器人
      String jdshxbq_RobotId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.ROBOT.getDesc(), AllEnums.wechatGroupEnum.getStr(it));

      //采集线报群中的机器人
      String jdshxbq_GroupId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.GROUP.getDesc(), AllEnums.wechatGroupEnum.getStr(it));


      //接收的线报消息来自配置的的线报群 中的机器人
      if (Objects.equals(jdshxbq_RobotId, receiveMsgDto.getFinal_from_wxid()) && Objects.equals(jdshxbq_GroupId, receiveMsgDto.getFrom_wxid())) {
        log.info("接收的线报消息:---------->{}", receiveMsgDto);

        //发送的是文字F
        if ((AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type())||(AllEnums.wechatMsgType.at_allPerson.getCode() == receiveMsgDto.getMsg_type())) {

          String time = (String) redisTemplate.opsForHash().get(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid());
          try {
            if (StringUtils.isNotBlank(time)) {
              if (new DateTime(Long.parseLong(time)).plusMillis(sendMsgSpace).isAfter(DateTime.now())) {
                log.info("距离上次发送时间间隔------->:{}秒,-----------------消息不会被发送------------", (System.currentTimeMillis() - Long.parseLong(time)) / 1000);
                redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), time);
                return;
              } else {
                redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), System.currentTimeMillis() + "");
              }
            } else {
              log.info("缓存数据为空,添加时间---------------->");
              redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), System.currentTimeMillis() + "");
            }

          } catch (Exception e) {
            log.info("error--->{}", e);
            return;
          }
          //转链后的字符串
          List<String> img_text;

          String coutStr = (String) redisTemplate.opsForValue().get("msg_count");
          if (StringUtils.isBlank(coutStr)) {
            redisTemplate.opsForValue().set("msg_count", "1");
            //转链后的字符串
            img_text = Utils.toLinkByDDX(removeTempateStr(receiveMsgDto.getMsg()), configDo.getReminderTemplate(), taoBaoIds.contains(receiveMsgDto.getFinal_from_wxid()) && taoBaoIds.contains(receiveMsgDto.getFrom_wxid()) ? "1" : null,configDo.getMsgKeyWords());
          } else {
            redisTemplate.opsForValue().set("msg_count", (Integer.parseInt(coutStr) + 1) + "");
            if (Integer.parseInt(coutStr) % configDo.getSenSpace() == 0) {
              img_text = Utils.toLinkByDDX(removeTempateStr(receiveMsgDto.getMsg()), configDo.getReminderTemplate(), taoBaoIds.contains(receiveMsgDto.getFinal_from_wxid()) && taoBaoIds.contains(receiveMsgDto.getFrom_wxid()) ? "1" : null,configDo.getMsgKeyWords());
            } else {
              img_text = Utils.toLinkByDDX(removeTempateStr(receiveMsgDto.getMsg()), "", taoBaoIds.contains(receiveMsgDto.getFinal_from_wxid()) && taoBaoIds.contains(receiveMsgDto.getFrom_wxid()) ? "1" : null,configDo.getMsgKeyWords());
            }
          }

          if (Objects.isNull(img_text) || (0 == img_text.size())) {
            log.info("-------转链失败-------");
            //转链失败
            redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), (StringUtils.isEmpty(time) ? (System.currentTimeMillis() + "") : time));
            return;
          }

          //将转链后的线报发送到 配置的群中
          List<String> finalImg_text = img_text;
          message_to_groups.forEach(item -> {

            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, item, finalImg_text.get(0), null, null, null);
            String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
            log.info("发送文字线报结果----->:{}", s1);

            //当线报文字发送成功后 该线报文字信息有没有发送过图片信息
            redisTemplate.opsForHash().put(Constants.wechat_msg_send_flag, receiveMsgDto.getFrom_wxid(), System.currentTimeMillis() + "");

            try {
              Thread.sleep(2000L);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }

            if (StringUtils.isNotBlank(finalImg_text.get(1))) {
              //发送图片
              WechatSendMsgDto wechatSendMsgDto_img = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), robotId, item, finalImg_text.get(1), null, null, null);
              String s2 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto_img);
              log.info("发送图片结果信息--------------->:{}", s2);
            } else {
              log.info("图片为空,不发送----->");
            }
          });
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
    //自己所管理的所有群的 群id
    List<String> ownGroupIds = Lists.newArrayList();
    configDo.getOwnGroup().forEach(it -> {
      String groupId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.GROUP.getDesc(), AllEnums.wechatGroupEnum.getStr(it));
      ownGroupIds.add(groupId);

    });

    //当有群成员退出群时,通知群主
    sendGroupMasterMemberRelease(receiveMsgDto, ownGroupIds, robotId, redisTemplate);

    //接收的不是群消息，不违规
    if (AllEnums.loveCatMsgType.GROUP_MSG.getCode() != receiveMsgDto.getType()) {
      return false;
    }

    //如果是自己人发送,则不违规
    if (Arrays.asList("du-yannan", "wxid_o7veppvw5bjn12", "wxid_8sofyhvoo4p322", "wxid_2r8n0q5v38h222", "wxid_pmvco89azbjk22", "wxid_pdigq6tu27ag21", "wxid_3juybqxcizkt22").contains(receiveMsgDto.getFinal_from_wxid())) {
      return false;
    }


    //接收的消息群消息  但不是发送到我们自己管理的群中的,不违规
    if (!ownGroupIds.contains(receiveMsgDto.getFrom_wxid())) {
      return false;
    }


    //代码走到这里表示：别人发在机器人所管理的群里发的群消息
    try {
      Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(receiveMsgDto.getMsg_type() + Constants.wechat_msg_send + receiveMsgDto.getFinal_from_wxid(), receiveMsgDto.getMsg());

      log.info("aBoolean----->{}", aBoolean);
      if (aBoolean) {

        String nick_name = (String) redisTemplate.opsForHash().get("wechat_friends", receiveMsgDto.getFinal_from_wxid());

        String to_groupOwner = "群成员昵称为:【" + nick_name + "】在群里发送了";

        Arrays.asList("du-yannan", "wxid_2r8n0q5v38h222").forEach(it -> {

          try {
            if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.TEXT.getCode()) {
              WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, it, URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type()) + ",信息内容:" + receiveMsgDto.getMsg()), "UTF-8"), null, null, null);
              WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
            } else {
              WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, it, URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type())), "UTF-8"), null, null, null);
              WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
            }
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
        });
      }

    } catch (Exception e) {
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

      for (String keyWord : configDo.getKeyWords()) {
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

  /**
   * 消除多余字符串
   *
   * @return
   */
  public String removeTempateStr(String str) {
    String replace;
    String removeJdxbStr;
    int i = str.indexOf("dl016.kuaizhan.com");
    if (i != -1) {
      String substring = str.substring(i, i + 31);
      replace = str.replace(substring, "");
    } else {
      replace = str;
    }

    int jdxbq = replace.indexOf("京东优质线报群");

    if (jdxbq != -1) {
      removeJdxbStr = replace.replace(replace.substring(jdxbq, jdxbq + 25), "");
    } else {
      removeJdxbStr = replace;
    }

    staticStr = removeJdxbStr;
    configDo.getRemoveStr().forEach(it -> staticStr = staticStr.replace(it, ""));

    return staticStr;
  }

  /**
   * 判断当前时间是否在晚上线报的时间段内
   *
   * @return
   */
  public static boolean nowTimeInNight() {

    int nowHour = DateTime.now().getHourOfDay();

    if (nowHour > 1 && nowHour < 9) {
      return true;
    }
    return false;
  }

  /**
   * 如果是群退出群聊 通知群主
   *
   * @param receiveMsgDto
   * @param ownGroupIds
   * @param robotId
   */
  public static void sendGroupMasterMemberRelease(WechatReceiveMsgDto receiveMsgDto, List<String> ownGroupIds, String robotId, RedisTemplate<String, Object> redisTemplate) {

    try {
      if (Objects.equals(AllEnums.loveCatMsgType.GROUP_MEMBER_DOWN.getCode(), receiveMsgDto.getType()) && ownGroupIds.contains(receiveMsgDto.getFrom_wxid())) {
        String wechat_id = JSONObject.parseObject(receiveMsgDto.getMsg()).getString("member_wxid");
        String nickName = JSONObject.parseObject(receiveMsgDto.getMsg()).getString("member_nickname");
        Boolean result = redisTemplate.opsForHash().putIfAbsent("quit_wechat_member", wechat_id, nickName);

        if (result) {
          Arrays.asList("du-yannan", "wxid_2r8n0q5v38h222").forEach(it -> {
            try {
              WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, it, URLEncoder.encode(Utf8Util.remove4BytesUTF8Char("微信昵称为【" + nickName + "】退出了群"), "UTF-8"), null, null, null);
              WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            }
          });
        }
      }
    } catch (Exception e) {
      log.info("error-------->{}", e);
    }
  }


  /**
   * 获取淘宝的机器人id和群id  放到一个list中
   *
   * @param configDo
   * @param redisTemplate
   * @return
   */
  public static List<String> getTaoBaoIds(ConfigDo configDo, RedisTemplate redisTemplate) {

    List<String> list = Lists.newArrayList();
    List<String> taobao = configDo.getTaobao();

    taobao.forEach(it -> {
      String taoBaoRobotId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.ROBOT.getDesc(), AllEnums.wechatGroupEnum.getStr(it));
      String taoBaoGroupId = (String) redisTemplate.opsForHash().get(AllEnums.wechatMemberFlag.GROUP.getDesc(), AllEnums.wechatGroupEnum.getStr(it));
      list.add(taoBaoRobotId);
      list.add(taoBaoGroupId);
    });
    return list;
  }
}
