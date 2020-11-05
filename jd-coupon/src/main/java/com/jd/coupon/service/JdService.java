package com.jd.coupon.service;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.account.Account;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.jd.Utils;
import com.common.util.wechat.WechatUtils;
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
import java.util.concurrent.atomic.AtomicReference;


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

            if (Objects.equals(AllEnums.loveCatMsgType.GROUP_MSG.getCode(), receiveMsgDto.getType()) &&
                    configDo.getOwnGroup().contains(receiveMsgDto.getFrom_wxid()) && (receiveMsgDto.getType() == 400) && (receiveMsgDto.getMsg_type() == 0)) {

                if (Arrays.asList("andy8830").contains(receiveMsgDto.getFinal_from_wxid())) {
                    try {
                        WechatSendMsgDto wsm = new WechatSendMsgDto(AllEnums.loveCatMsgType.DELETE_GROUP_MEMBER.getCode(), "wxid_8sofyhvoo4p322", null, null, null, null, null);
                        wsm.setMember_wxid(receiveMsgDto.getFinal_from_wxid());
                        wsm.setGroup_wxid(receiveMsgDto.getFrom_wxid());
                        String s = WechatUtils.sendWechatTextMsg(wsm);
                        log.info("违规将群成员踢出群聊结果----->:{}", s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }


            //判定消息来源,需包含线报来源群(接收线报)和线报发送群(判定违规消息)
            if (!configDo.getMsgFromGroup().contains(receiveMsgDto.getFrom_wxid())) {
                return;
            }
            log.info("receiveMsgDto=======>{}", receiveMsgDto);
            if (duplicateMessage(receiveMsgDto.getRid())) {
                log.info("消息重复=======>");
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

                    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode("@" + (StringUtils.isEmpty(nick_name) ? "" : nick_name) + configDo.getTemplate(), "UTF-8"), null, null, null);
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

                    //发送的是文字
                    if ((AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type()) || (AllEnums.wechatMsgType.at_allPerson.getCode() == receiveMsgDto.getMsg_type())) {

                        //test群 zf发送的
                        if (Objects.equals("22822365300@chatroom", receiveMsgDto.getFrom_wxid()) && Objects.equals(receiveMsgDto.getFinal_from_wxid(), "wxid_2r8n0q5v38h222")) {

                            if (receiveMsgDto.getMsg().contains("注意：") || receiveMsgDto.getMsg().contains("注意:")) {

                                Arrays.asList("17490589131@chatroom", "18949318188@chatroom").forEach(obj -> {
                                    try {
                                        WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, obj, URLEncoder.encode(receiveMsgDto.getMsg(), "UTF-8"), null, null, null);
                                        WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }

                                });
                                //当需要机器人艾特某人时 格式为 【消息内容】艾特某人【某人昵称】艾特某人【微信id】
                            } else if (receiveMsgDto.getMsg().contains("艾特某人")) {

                                String[] atPeopleArray = receiveMsgDto.getMsg().split("艾特某人");
                                try {
                                    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_AT_MSG.getCode(), robotId, "17490589131@chatroom", URLEncoder.encode(atPeopleArray[0], "UTF-8"), null, atPeopleArray[2], URLEncoder.encode(atPeopleArray[1], "UTF-8"));
                                    WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }

                                return;
                                //有【回复消息】四个字时,消息被机器人原样转发到群里
                            } else if (receiveMsgDto.getMsg().contains("回复消息")) {
                                try {
                                    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "17490589131@chatroom", URLEncoder.encode(receiveMsgDto.getMsg().replaceAll("回复消息", ""), "UTF-8"), null, null, null);
                                    WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                return;
                            }
                        }

                        if (receiveMsgDto.getMsg().length() > 500 && (!receiveMsgDto.getMsg().contains("领券汇总")) && (!receiveMsgDto.getMsg().contains("【京东领券"))) {
                            log.info("超过长度=========>{}", receiveMsgDto.getMsg().length());
                            return;
                        }

                        //获取不同账号京东转链参数
                        String accoutStr = (String) redisTemplate.opsForValue().get("account");
                        List<Account> accounts = JSONObject.parseArray(accoutStr, Account.class);

                        AtomicReference<String> havePicUrlAdd = new AtomicReference<>("");

                        accounts.forEach(accout -> {

                            List<String> img_text = Utils.toLinkByDDX(removeTempateStr(receiveMsgDto.getMsg(), receiveMsgDto), configDo.getReminder(), configDo.getMsgKeyWords(), redisTemplate, receiveMsgDto, accout, !StringUtils.isEmpty(havePicUrlAdd.get()));

                            if (Objects.isNull(img_text) || (0 == img_text.size())) {
                                //转链失败
                                return;
                            }

                            //将转链后的线报发送到 配置的群中
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, accout.getGroupId(), img_text.get(0), null, null, null);
                            String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            log.info("{}====>发送文字线报结果----->:{}", accout.getName(), s1);

                            try {
                                Thread.sleep(500L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (img_text.size() == 2) {
                                havePicUrlAdd.set(img_text.get(1));
                            }
                            if (!StringUtils.isEmpty(havePicUrlAdd.get())) {
                                //发送图片
                                WechatSendMsgDto wechatSendMsgDto_img = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), robotId, accout.getGroupId(), havePicUrlAdd.get(), null, null, null);
                                String s2 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto_img);
                                log.info("{}====>发送图片结果信息--------------->:{}", accout.getName(), s2);

                            } else {
                                log.info("{}====>,图片为空,不发送----->", accout.getName());
                            }

                        });

                        //如果是test群 发送的是图片 zf发送
                    } else if (Objects.equals("22822365300@chatroom", receiveMsgDto.getFrom_wxid()) && (AllEnums.wechatMsgType.IMAGE.getCode() == receiveMsgDto.getMsg_type()) && Objects.equals(receiveMsgDto.getFinal_from_wxid(), "wxid_2r8n0q5v38h222")) {


                        Arrays.asList("17490589131@chatroom", "18949318188@chatroom").forEach(obj -> {
                            //发送图片
                            WechatSendMsgDto wechatSendMsgDto_img = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), robotId, obj, receiveMsgDto.getMsg(), null, null, null);
                            WechatUtils.sendWechatTextMsg(wechatSendMsgDto_img);
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

        //1群消息 2 好物线报群 薅羊毛群 3不是特定人
        if (Objects.equals(AllEnums.loveCatMsgType.GROUP_MSG.getCode(), receiveMsgDto.getType()) &&
                configDo.getOwnGroup().contains(receiveMsgDto.getFrom_wxid()) &&
                (!configDo.getWhitename().contains(receiveMsgDto.getFinal_from_wxid()))) {

            //发送的是图片并且包含二维码
            if (AllEnums.wechatMsgType.IMAGE.getCode() == receiveMsgDto.getMsg_type() && (Utils.isHaveQr(receiveMsgDto.getMsg()))) {
                log.info("包含二维码====>");
                return true;
            }
            //发送的不是文字、完成群公告、图片、语音,动态表情 判定违规
            if ((!Arrays.asList(AllEnums.wechatMsgType.TEXT.getCode(), AllEnums.wechatMsgType.qungonggao.getCode(), AllEnums.wechatMsgType.IMAGE.getCode(), AllEnums.wechatMsgType.YY.getCode(), AllEnums.wechatMsgType.ADD_FRIEND.getCode(), AllEnums.wechatMsgType.Emoticon.getCode()).contains(receiveMsgDto.getMsg_type()))) {
                return true;
            }
        }


        //当有群成员退出群时,通知群主
        sendGroupMasterMemberRelease(receiveMsgDto, configDo.getOwnGroup(), robotId, redisTemplate);

        //接收的不是群消息，不违规
        if (AllEnums.loveCatMsgType.GROUP_MSG.getCode() != receiveMsgDto.getType()) {
            return false;
        }

        //如果是自己人发送,则不违规
        if (configDo.getWhitename().contains(receiveMsgDto.getFinal_from_wxid())) {
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

                String to_groupOwner = "群成员昵称为:【" + (StringUtils.isEmpty(nick_name) ? receiveMsgDto.getFinal_from_wxid() : nick_name) + "】(" + receiveMsgDto.getFinal_from_wxid() + ")在群里发送了";


                //如果是zzf的群 通知他
                if ("18949318188@chatroom".equals(receiveMsgDto.getFrom_wxid())) {
                    try {
                        if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.TEXT.getCode()) {
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_pdigq6tu27ag21", URLEncoder.encode(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type()) + ",信息内容:" + receiveMsgDto.getMsg().replace("[@at,nickname=线报助手,wxid=wxid_8sofyhvoo4p322]", ""), "UTF-8"), null, null, null);
                            WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                        } else if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.ADD_FRIEND.getCode()) {
                            log.info("接收请求====>{}", receiveMsgDto.getMsg());
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_pdigq6tu27ag21", URLEncoder.encode(receiveMsgDto.getMsg() + "(" + receiveMsgDto.getFinal_from_wxid() + ")", "UTF-8"), null, null, null);
                            WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                        } else {
                            log.info("receive---->{}", receiveMsgDto);
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_pdigq6tu27ag21", URLEncoder.encode(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type()), "UTF-8"), null, null, null);
                            WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //其余的通知我
                } else {
                    if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.TEXT.getCode()) {

                        Arrays.asList("wxid_2r8n0q5v38h222", "du-yannan").forEach(it -> {
                            try {
                                WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, it, URLEncoder.encode(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type()) + ",信息内容:" + receiveMsgDto.getMsg().replace("[@at,nickname=线报助手,wxid=wxid_8sofyhvoo4p322]", ""), "UTF-8"), null, null, null);
                                WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        });

                    } else if (receiveMsgDto.getMsg_type() == AllEnums.wechatMsgType.ADD_FRIEND.getCode()) {
                        log.info("添加好友请求====>{}", receiveMsgDto.getMsg());

                        Arrays.asList("wxid_2r8n0q5v38h222", "du-yannan").forEach(it -> {
                            try {
                                WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, it, URLEncoder.encode(receiveMsgDto.getMsg() + "(" + receiveMsgDto.getFinal_from_wxid() + ")", "UTF-8"), null, null, null);
                                WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        });

                    } else {
                        log.info("receive------>{}", "111");
                        Arrays.asList("wxid_2r8n0q5v38h222", "du-yannan").forEach(it -> {
                            try {
                                WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, it, URLEncoder.encode(to_groupOwner + AllEnums.wechatMsgType.getStr(receiveMsgDto.getMsg_type()), "UTF-8"), null, null, null);
                                WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        });
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


        int i1 = removeJdxbStr.lastIndexOf("\n");

        if (i1 != -1) {
            String substring = removeJdxbStr.substring(i1);

            if (Objects.equals(receiveMsgDto.getFrom_wxid(), "18172911411@chatroom")) {

                int index = findIndex(substring);

                if (index == -1) {
                    sgStr = removeJdxbStr;
                } else if (index == 0) {
                    sgStr = removeJdxbStr.substring(0, i1);
                } else {
                    sgStr = removeJdxbStr.substring(0, i1 + index);
                }

            } else {
                sgStr = removeJdxbStr;
            }


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
                Boolean result = redisTemplate.opsForHash().putIfAbsent("quit_wechat_member", DateTime.now().toString("yyyy-MM-dd HH:mm:ss"), wechat_id + "=====" + nickName);
                log.info("result===>{}", result);
                if (result) {
                    //如果是zzf的群 通知他
                    if ("18949318188@chatroom".equals(receiveMsgDto.getFrom_wxid())) {
                        try {
                            log.info("param1===>{}", AllEnums.loveCatMsgType.PRIVATE_MSG.getCode());
                            log.info("param2===>{}", robotId);
                            log.info("param3===>{}", nickName);
                            log.info("param4===>{}", URLEncoder.encode("微信昵称为【" + nickName + "】退出了群", "UTF-8"));

                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, "wxid_pdigq6tu27ag21", URLEncoder.encode("微信昵称为【" + nickName + "】退出了群", "UTF-8"), null, null, null);
                            String s = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            log.info("s--->{},receiveMsgDto====>{}", s, receiveMsgDto);
                        } catch (UnsupportedEncodingException e) {
                            log.info("e====>{}", e);
                            e.printStackTrace();
                        }
                        //其余的通知我
                    } else {

                        Arrays.asList("wxid_2r8n0q5v38h222", "du-yannan").forEach(it -> {
                            try {
                                WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, it, URLEncoder.encode("微信昵称为【" + nickName + "】退出了群", "UTF-8"), null, null, null);
                                String s2 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                                log.info("s2--->{},receiveMsgDto====>{}", s2, receiveMsgDto);
                            } catch (UnsupportedEncodingException e) {
                                log.info("e============>{}", e);
                                e.printStackTrace();
                            }

                        });
                    }
                }
            }
        } catch (Exception e) {
            log.info("ee=>{},群退出群聊 通知群主error-------->{}", e, JSONObject.toJSONString(receiveMsgDto));
        }
    }

    /**
     * 消息是否重复发送
     *
     * @param rid
     * @return
     */
    public boolean duplicateMessage(String rid) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(rid, "1");
        redisTemplate.expire(rid, 2, TimeUnit.MINUTES);
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

    /**
     * 判断某个特定字符串的个数
     *
     * @param string
     * @param str
     * @return
     */
    public static int strNum(String string, String str) {
        int i = string.length() - string.replace(str, "").length();
        return i / str.length();
    }


    public static int findIndex(String str) {

        //只有文字不截取
        if (!str.contains("ttp")) {
            return -1;
        }

        if (strNum(str, "u.jd.com") > 0) {
            if (str.contains("https://u.jd.com")) {
                return str.lastIndexOf("https://u.jd.com") + 24;
            } else if (str.contains("http://u.jd.com")) {
                return str.lastIndexOf("http://u.jd.com") + 23;
            }
        } else {
            return 0;
        }
        return 0;
    }
}
