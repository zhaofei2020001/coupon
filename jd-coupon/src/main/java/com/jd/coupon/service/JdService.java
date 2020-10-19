package com.jd.coupon.service;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.account.Account;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.jd.Utf8Util;
import com.common.util.jd.Utils;
import com.common.util.wechat.WechatUtils;
import com.jd.coupon.Domain.ConfigDo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

        synchronized (JdService.class) {
            if (duplicateMessage(receiveMsgDto, redisTemplate)) {
                return;
            }

            String robotId = configDo.getRobotGroup();

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
                    WechatSendMsgDto wsm = new WechatSendMsgDto(AllEnums.loveCatMsgType.DELETE_GROUP_MEMBER.getCode(), robotId, null, null, null, null, null);
                    wsm.setMember_wxid(receiveMsgDto.getFinal_from_wxid());
                    wsm.setGroup_wxid(receiveMsgDto.getFrom_wxid());
                    String s = WechatUtils.sendWechatTextMsg(wsm);
                    log.info("违规将群成员踢出群聊结果----->:{}", s);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    String nick_name = receiveMsgDto.getFinal_from_name();

                    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode(Utf8Util.remove4BytesUTF8Char("@" + (StringUtils.isEmpty(nick_name) ? "" : nick_name) + configDo.getTemplate()), "UTF-8"), null, null, null);
                    String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

                    log.info("判定违规,昵称-->:{},发送的结果--->:{}", nick_name, s1);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return;

            }


            configDo.getMsgFromGroup().forEach(it -> {

                //接收的线报消息来自配置的的线报群
                if (Objects.equals(it, receiveMsgDto.getFrom_wxid())) {

                    //发送的是文字F
                    if ((AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type()) || (AllEnums.wechatMsgType.at_allPerson.getCode() == receiveMsgDto.getMsg_type())) {

                        //获取不同账号京东转链参数
                        String accoutStr = (String) redisTemplate.opsForValue().get("account");
                        List<Account> accounts = JSONObject.parseArray(accoutStr, Account.class);

                        accounts.forEach(accout -> {

                            //转链后的字符串
                            List<String> img_text = Utils.toLinkByDDX(removeTempateStr(receiveMsgDto.getMsg(), receiveMsgDto), configDo.getReminderTemplate(), configDo.getMsgKeyWords(), redisTemplate, receiveMsgDto, accout);

                            if (Objects.isNull(img_text) || (0 == img_text.size())) {
                                //转链失败
                                return;
                            }

                            //将转链后的线报发送到 配置的群中
                            List<String> finalImg_text = img_text;


                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, accout.getGroupId(), finalImg_text.get(0), null, null, null);
                            String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            log.info("{}====>发送文字线报结果----->:{}", accout.getName(), s1);

                            try {
                                Thread.sleep(500L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (StringUtils.isNotBlank(finalImg_text.get(1))) {
                                //发送图片
                                WechatSendMsgDto wechatSendMsgDto_img = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), robotId, accout.getGroupId(), finalImg_text.get(1), null, null, null);
                                String s2 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto_img);
                                log.info("{}====>发送图片结果信息--------------->:{}", accout.getName(), s2);
                            } else {
                                log.info("{}====>,图片为空,不发送----->", accout.getName());
                            }

                        });

                    }
                }
            });
        }
    }

    /**
     * 判断是否违反群规
     *
     * @param receiveMsgDto
     */
    public boolean judgeViolation(WechatReceiveMsgDto receiveMsgDto, String robotId) {

        //1群消息 2 好物线报群 3不是特定人 4发送的是视频、名片、位置信息、分享,图片 判定违规
        if (Objects.equals(AllEnums.loveCatMsgType.GROUP_MSG.getCode(), receiveMsgDto.getType()) &&
                Arrays.asList("17490589131@chatroom", "18949318188@chatroom").contains(receiveMsgDto.getFrom_wxid()) &&
                (!Arrays.asList("du-yannan", "wxid_8sofyhvoo4p322", "wxid_2r8n0q5v38h222", "wxid_pdigq6tu27ag21").contains(receiveMsgDto.getFinal_from_wxid())) &&
                (AllEnums.wechatMsgType.TEXT.getCode() != receiveMsgDto.getMsg_type() && AllEnums.wechatMsgType.YY.getCode() != receiveMsgDto.getMsg_type() && AllEnums.wechatMsgType.Emoticon.getCode() != receiveMsgDto.getMsg_type())) {
            log.info("违规=====>{}", receiveMsgDto.getMsg());
            return true;
        }


        //当有群成员退出群时,通知群主
        sendGroupMasterMemberRelease(receiveMsgDto, configDo.getOwnGroup(), robotId, redisTemplate);

        //接收的不是群消息，不违规
        if (AllEnums.loveCatMsgType.GROUP_MSG.getCode() != receiveMsgDto.getType()) {
            return false;
        }

        //如果是自己人发送,则不违规
        if (Arrays.asList("du-yannan", "wxid_8sofyhvoo4p322", "wxid_2r8n0q5v38h222", "wxid_pdigq6tu27ag21").contains(receiveMsgDto.getFinal_from_wxid())) {
            return false;
        }


        //接收的消息群消息  但不是发送到我们自己管理的群中的,不违规
        if (!configDo.getOwnGroup().contains(receiveMsgDto.getFrom_wxid())) {
            return false;
        }

        //代码走到这里表示：别人发在机器人所管理的群里发的群消息
        try {
            Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(receiveMsgDto.getMsg_type() + receiveMsgDto.getFinal_from_wxid(), "1");
            redisTemplate.expire(receiveMsgDto.getMsg_type() + receiveMsgDto.getFinal_from_wxid(), 2, TimeUnit.SECONDS);
            if (aBoolean) {

                String nick_name = receiveMsgDto.getFinal_from_name();

                String to_groupOwner = "群成员昵称为:【" + (StringUtils.isEmpty(nick_name) ? receiveMsgDto.getFinal_from_wxid() : nick_name) + "】在群里发送了";


                //如果是哥的群 通知他
                if ("18949318188@chatroom".equals(receiveMsgDto.getFrom_wxid())) {
                    try {
                        if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.TEXT.getCode()) {
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_pdigq6tu27ag21", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type()) + ",信息内容:" + receiveMsgDto.getMsg()), "UTF-8"), null, null, null);
                            WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                        } else {
                            log.info("receive---->{}", receiveMsgDto);
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_pdigq6tu27ag21", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type())), "UTF-8"), null, null, null);
                            WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //其余的通知我
                } else {
                    try {
                        if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.TEXT.getCode()) {
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_2r8n0q5v38h222", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type()) + ",信息内容:" + receiveMsgDto.getMsg()), "UTF-8"), null, null, null);
                            WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                        } else {
                            log.info("receive---->{}", receiveMsgDto);
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_2r8n0q5v38h222", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type())), "UTF-8"), null, null, null);
                            WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

            }

        } catch (Exception e) {
            log.info("发消息失败了2------>{}", e);
            e.printStackTrace();
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
    public String removeTempateStr(String str, WechatReceiveMsgDto receiveMsgDto) {

        String replace;
        String removeJdxbStr;
        String sgStr;
        String qyxzStr;
        String jdStr;
        String endStr = null;

        if (str.contains("删除:") && receiveMsgDto.getFrom_wxid().equals("22822365300@chatroom")) {
            log.info("set remove str------------------>{}", str.substring(3));

            redisTemplate.opsForValue().set("remove_str", str.substring(3));

        }


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

        int i1 = removeJdxbStr.indexOf("https://sohu.gg/");
        if (i1 != -1) {
            sgStr = removeJdxbStr.replace(removeJdxbStr.substring(i1, i1 + 23), "");
        } else {
            sgStr = removeJdxbStr;
        }

        int qyxz = sgStr.indexOf("群员须知");
        if (qyxz != -1 && qyxz != 0) {
            qyxzStr = sgStr.replace(sgStr.substring(qyxz - 2), "");
        } else {
            qyxzStr = sgStr;
        }

        int jd_flag = qyxzStr.indexOf("TaoBao线报QQ群");
        if (jd_flag != -1 && jd_flag != 0) {
            jdStr = qyxzStr.replace(qyxzStr.substring(jd_flag - 2), "");
        } else {
            jdStr = qyxzStr;
        }


        if (StringUtils.isEmpty(endStr)) {
            staticStr = jdStr;
        } else {
            staticStr = endStr;
        }

        configDo.getRemoveStr().forEach(it -> staticStr = staticStr.replace(it, ""));

        String remove_str = (String) redisTemplate.opsForValue().get("remove_str");
        if (StringUtils.isNotBlank(remove_str)) {
            int i2 = staticStr.lastIndexOf(remove_str);
            String returnEndStr;
            if (i2 != -1 && i2 != 0) {
                returnEndStr = staticStr.substring(0, i2);
            } else {
                returnEndStr = staticStr;
            }

            return deleteN(returnEndStr);
        } else {
            return deleteN(staticStr);
        }
    }

    /**
     * 如果是群退出群聊 通知群主
     *
     * @param receiveMsgDto receiveMsgDto
     * @param ownGroupIds   ownGroupIds
     * @param robotId       robotId
     */
    public static void sendGroupMasterMemberRelease(WechatReceiveMsgDto receiveMsgDto, List<String> ownGroupIds, String robotId, RedisTemplate<String, Object> redisTemplate) {

        try {
            if (Objects.equals(AllEnums.loveCatMsgType.GROUP_MEMBER_DOWN.getCode(), receiveMsgDto.getType()) && ownGroupIds.contains(receiveMsgDto.getFrom_wxid())) {
                JSONObject jsonObject = JSONObject.parseObject(receiveMsgDto.getMsg());
                String wechat_id = jsonObject.getString("member_wxid");
                String nickName = jsonObject.getString("member_nickname");
                Boolean result = redisTemplate.opsForHash().putIfAbsent("quit_wechat_member", wechat_id, nickName);
                log.info("result===>{}", result);
                if (result) {
                    //如果是哥的群 通知他
                    if ("18949318188@chatroom".equals(receiveMsgDto.getFrom_wxid())) {
                        try {
                            log.info("param1===>{}",AllEnums.loveCatMsgType.PRIVATE_MSG.getCode());
                            log.info("param2===>{}",robotId);
                            log.info("param3===>{}",URLEncoder.encode(Utf8Util.remove4BytesUTF8Char("微信昵称为【" + nickName + "】退出了群")));
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_pdigq6tu27ag21", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char("微信昵称为【" + nickName + "】退出了群"), "UTF-8"), null, null, null);
                            String s = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            log.info("s--->{},receiveMsgDto====>{}", s, receiveMsgDto);
                        } catch (UnsupportedEncodingException e) {
                            log.info("e====>{}", e);
                            e.printStackTrace();
                        }
                        //其余的通知我
                    } else {
                        try {
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_2r8n0q5v38h222", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char("微信昵称为【" + nickName + "】退出了群"), "UTF-8"), null, null, null);
                            String s2 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            log.info("s2--->{},receiveMsgDto====>{}", s2, receiveMsgDto);
                        } catch (UnsupportedEncodingException e) {
                            log.info("e============>{}", e);
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("ee=>{},群退出群聊 通知群主error-------->{}", e, JSONObject.toJSONString(receiveMsgDto));
        }
    }

    /**
     * 是否重复发送消息  和 发送的东西是否违规
     *
     * @param receiveMsgDto
     * @return true 重复消息 false新消息
     */
    public boolean duplicateMessage(WechatReceiveMsgDto receiveMsgDto, RedisTemplate<String, Object> redisTemplate) {

        if (!configDo.getMsgFromGroup().contains(receiveMsgDto.getFrom_wxid())) {
            return true;
        }
        log.info("reciece===>{}", receiveMsgDto);
        //如果是test群发出的删除:【关键字】则放行
        if (receiveMsgDto.getFrom_wxid().equals("22822365300@chatroom") && receiveMsgDto.getMsg().contains("删除:")) {
            return false;
        }

        String key = receiveMsgDto.getMsg();
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1");
        redisTemplate.expire(key, 2, TimeUnit.MINUTES);
        if (result) {
            return false;
        }
        return true;
    }

    public static String deleteN(String str) {

        if (str.endsWith("\n")) {
            int iii = str.lastIndexOf("\n");
            String substring = str.substring(0, iii);
            if (substring.endsWith("\n")) {
                return deleteN(substring).trim();
            } else {
                return substring.trim();
            }
        }
        return str.trim();
    }
}
