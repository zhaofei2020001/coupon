package com.jd.coupon.service;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.account.Account;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.jd.TextWatermarking;
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
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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


        String array = null;
        List<String> msg_group= Lists.newArrayList();
        try {
            array = (String) redisTemplate.opsForValue().get("msg_group");
            msg_group = new ArrayList<>(Arrays.asList(array.split(",")));
        } catch (Exception e) {
            Jedis jedis = new Jedis("39.98.77.98");
            log.info("连接本地的 Redis 服务成功！");
            // 查看服务是否运行
            log.info("服务 正在运行:{} " + jedis.ping());

            jedis.set("msg_group","22822365300@chatroom,21874856168@chatroom,19933485573@chatroom,17490589131@chatroom,18949318188@chatroom,5013506060@chatroom,23765777130@chatroom,23336882997@chatroom,23216907002@chatroom,18172911411@chatroom");
            jedis.set("account","[{\"antappkey\":\"872ea5798e8746d0\",\"groupId\":\"17490589131@chatroom\",\"jdtgwid\":\"1987045755\",\"msgToPersons\":[\"wxid_2r8n0q5v38h222\",\"du-yannan\",\"wxid_pdigq6tu27ag21\"],\"name\":\"ddy\"},{\"antappkey\":\"5862cd52a87a1914\",\"groupId\":\"18949318188@chatroom\",\"jdtgwid\":\"3002800583\",\"msgToPersons\":[],\"name\":\"zzf\"}]");
            jedis.lpush("tbmd","wxid_qj37xlvrt9t422:A02【小文】线报冕単分享??","wxid_zlhgrhsx42sb22:淘礼金免单②群","wxid_j2h1kopuoqlc12:阿涛福利社-04A15","wxid_qj37xlvrt9t422:【小K】捡漏??B16群");

            array = "22822365300@chatroom,21874856168@chatroom,19933485573@chatroom,17490589131@chatroom,18949318188@chatroom,5013506060@chatroom,23765777130@chatroom,23336882997@chatroom,23216907002@chatroom,18172911411@chatroom";
            msg_group = new ArrayList<>(Arrays.asList(array.split(",")));
        }



        //判定消息来源,需包含线报来源群(接收线报)和线报发送群(判定违规消息)
        if (!msg_group.contains(receiveMsgDto.getFrom_wxid())) {
            return;
        }
        log.info("receiveMsgDto=======>{}", receiveMsgDto);

        String robotId = configDo.getRobotGroup();

        //判定是否违规
        boolean b = judgeViolation(receiveMsgDto, robotId);

        if (b) {
            String obj = (String) redisTemplate.opsForValue().get(receiveMsgDto.getMsg_type() + Constants.wechat_msg_illegal + receiveMsgDto.getFinal_from_wxid());
            redisTemplate.opsForValue().set(receiveMsgDto.getMsg_type() + Constants.wechat_msg_illegal + receiveMsgDto.getFinal_from_wxid(), "flag", 5, TimeUnit.MINUTES);

            if (StringUtils.isNotBlank(obj)) {
                log.info("-------违规已经警告过了----------");
                redisTemplate.opsForValue().set(receiveMsgDto.getMsg_type() + Constants.wechat_msg_illegal + receiveMsgDto.getFinal_from_wxid(), "flag", 1000, TimeUnit.MILLISECONDS);
                return;
            }


            try {
                String nick_name = receiveMsgDto.getFinal_from_name();
                WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, receiveMsgDto.getFrom_wxid(), URLEncoder.encode("@" + (StringUtils.isEmpty(nick_name) ? "" : nick_name) + configDo.getTemplate(), "UTF-8"), null, null, null);
                String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);

                log.info("判定违规,昵称-->:{},发送的结果--->:{}", nick_name, s1);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //有人发送违规消息通知群主有人发送消息
            tzQunZhu(receiveMsgDto, robotId);
            log.info("有人发送违规消息通知群主========>");
            return;

        }


        msg_group.forEach(it -> {

            //接收的线报消息来自配置的的线报群
            if (Objects.equals(it, receiveMsgDto.getFrom_wxid())) {

                //发送的是文字
                if ((AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type()) || (AllEnums.wechatMsgType.at_allPerson.getCode() == receiveMsgDto.getMsg_type())) {

                    //test群 zf发送的
                    if (Objects.equals("22822365300@chatroom", it) && Objects.equals(receiveMsgDto.getFinal_from_wxid(), "wxid_2r8n0q5v38h222")) {

                        if (receiveMsgDto.getMsg().contains("注意：") || receiveMsgDto.getMsg().contains("注意:")) {

                            Arrays.asList("17490589131@chatroom", "18949318188@chatroom").forEach(obj -> {
                                try {
                                    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, obj, URLEncoder.encode(receiveMsgDto.getMsg(), "UTF-8"), null, null, null);
                                    WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }

                            });
                            return;
                            //当需要机器人艾特某人时 格式为 【消息内容】艾特某人【某人昵称】艾特某人【微信id】
                        } else if (receiveMsgDto.getMsg().contains("艾特某人")) {

                            String[] atPeopleArray = receiveMsgDto.getMsg().split("艾特某人");
                            try {
                                WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_AT_MSG.getCode(), robotId, "17490589131@chatroom", URLEncoder.encode(atPeopleArray[0], "UTF-8"), null, atPeopleArray[2], atPeopleArray[1]);
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
                        } else if (receiveMsgDto.getMsg().contains("踢人")) {
                            deleteMember(receiveMsgDto.getMsg().replace("踢人", ""), "17490589131@chatroom", robotId);
                        }
                    }

                    if (receiveMsgDto.getMsg().length() > 350 && (!receiveMsgDto.getMsg().contains("领券汇总")) && (!receiveMsgDto.getMsg().contains("【京东领券")) && !qunzhuSendMsg(it)&&(!Objects.equals(receiveMsgDto.getFrom_wxid(),"21874856168@chatroom"))) {
                        log.info("超过长度=========>{}", receiveMsgDto.getMsg().length());
                        return;
                    }

                    //获取不同账号京东转链参数
                    String accoutStr = (String) redisTemplate.opsForValue().get("account");
                    List<Account> accounts = JSONObject.parseArray(accoutStr, Account.class);

                    AtomicReference<String> hadSkuId = new AtomicReference<>("");

                    AtomicReference<String> hadPic = new AtomicReference<>("");

                    AtomicReference<Boolean> had_send = new AtomicReference<>(false);

                    accounts.forEach(accout -> {
                        //不是京东生活40群
                        if(!Objects.equals(receiveMsgDto.getFrom_wxid(),"21874856168@chatroom")){
                            if (had_send.get()) {
                                return;
                            }
                            String s = removeTempateStr(receiveMsgDto.getMsg(), receiveMsgDto);
                            List<String> img_text = Utils.toLinkByDDX(s, configDo.getReminder(), configDo.getMsgKeyWords(), redisTemplate, receiveMsgDto, accout, !StringUtils.isEmpty(hadSkuId.get()), had_send.get(), qunzhuSendMsg(it));


                            if (Objects.isNull(img_text) || (0 == img_text.size())) {
                                //转链失败
                                return;
                            }
                            if (3 == img_text.size()) {
                                had_send.set(true);
                                return;
                            }

                            //凌晨0、1、2、3、4、5，6点 picLink = Utils.getSKUInfo(skuId, antappkey);
//                            if (Integer.parseInt(DateTime.now().toString("HH")) < 7 && Integer.parseInt(DateTime.now().toString("HH")) >= 0) {
                                //是否发送自助查券标志
                                String zzcq_flag = (String) redisTemplate.opsForValue().get("zzcq_flag"+accout.getName() );
                                if (!StringUtils.isEmpty(zzcq_flag)) {
                                    log.info("京东自助查券已发送,0-6点不再发送消息============>");
                                    return;
                                }
//                            }
                            if (img_text.size() == 2) {
                                hadSkuId.set(img_text.get(1));
                            }
                            //线报长度小 并且没有skuId 和数字
                            if (Utils.strLengh(s) && (StringUtils.isEmpty(hadSkuId.get()) || HasDigit(s))) {
                                return;
                            }

                            //将转链后的线报发送到 配置的群中
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, accout.getGroupId(), img_text.get(0), null, null, null);
                            String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            log.info("{}====>发送文字线报结果----->:{}", accout.getName(), s1);


                            //记录每一次发送消息的时间
                            redisTemplate.opsForValue().set("send_last_msg_time", DateTime.now().toString("HH"));


                            if (!StringUtils.isEmpty(hadSkuId.get()) && StringUtils.isEmpty(hadPic.get())) {
                                List<String> allUrl = Utils.getAllUrl(receiveMsgDto.getMsg());

                                //如果有多张图片 图片合并
                                String picLink = Utils.getSKUInfo2(allUrl, "5862cd52a87a1914", receiveMsgDto.getRid(), hadSkuId.get());
                                //如果有多张图片 图片不合并
//                            String picLink = Utils.getSKUInfo(hadSkuId.get(), accout.getAntappkey());

                                if (StringUtils.isEmpty(picLink)) {

                                    log.info("{}====>,图片为空,不发送----->", accout.getName(), picLink);

                                } else {
//                                log.info("开始添加水印,获取图片地址=======>{}", picLink);

//                                //为图片加水印
                                    try {
//                                    TextWatermarking.markImageBySingleText(picLink, Constants.BASE_URL, receiveMsgDto.getRid(), "jpeg", Color.black, "群已开启免单线报", null);
                                        TextWatermarking.markImageBySingleText(picLink, Constants.BASE_URL, receiveMsgDto.getRid(), "jpeg", Color.black, "", null);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
//                                hadPic.set(Constants.BASE_URL + receiveMsgDto.getRid() + ".jpeg");
                                    hadPic.set(picLink);
                                    //发送图片
                                    WechatSendMsgDto wechatSendMsgDto_img = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), robotId, accout.getGroupId(), hadPic.get(), null, null, null);
                                    String s2 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto_img);
                                    log.info("{}====>发送图片结果信息--------------->:{}", accout.getName(), s2);

                                }
                            } else {

                                if (!StringUtils.isEmpty(hadPic.get())) {

                                    //发送图片
                                    WechatSendMsgDto wechatSendMsgDto_img = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), robotId, accout.getGroupId(), hadPic.get(), null, null, null);
                                    String s2 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto_img);
                                    log.info("{}====>发送图片结果信息--------------->:{}", accout.getName(), s2);

                                    List<String> allUrl = Utils.getAllUrl(receiveMsgDto.getMsg());
                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    boolean delete1 = new File(Constants.BASE_URL + receiveMsgDto.getRid() + ".jpeg").delete();
                                    log.info("delete1===>{}", delete1);
                                    for (int i = 0; i < allUrl.size(); i++) {
                                        if (new File(Constants.BASE_URL + receiveMsgDto.getRid() + i + ".jpeg").exists()) {
                                            boolean delete = new File(Constants.BASE_URL + receiveMsgDto.getRid() + i + ".jpeg").delete();
                                            log.info("删除图片===>{}", delete);
                                        }
                                    }

                                } else {

                                    log.info("{}====>,图片为空,不发送----->", accout.getName());
                                }
                            }
                        }else{//京东生活40
                            if(!Objects.equals(receiveMsgDto.getFinal_from_wxid(),"wxid_0p28wr3n0uh822")){
                                return;
                            }
                            String s = removeTempateStr(receiveMsgDto.getMsg(), receiveMsgDto);
                            List<String> img_text = Utils.toLinkByDDX(s, configDo.getReminder(), configDo.getMsgKeyWords(), redisTemplate, receiveMsgDto, accout, false, false, true);

                            if (Objects.isNull(img_text) || (0 == img_text.size())) {
                                //转链失败
                                return;
                            }
                            //将转链后的线报发送到 配置的群中
                            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), robotId, accout.getGroupId(), img_text.get(0), null, null, null);
                            String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
                            log.info("{}====>发送文字线报结果----->:{}", accout.getName(), s1);

                                 String zzcq_str = (String) redisTemplate.opsForValue().get("zzcq_str");
                                //京东40群机器人发送了自助查券的线报
                                if(StringUtils.isNotBlank(zzcq_str)&& receiveMsgDto.getMsg().contains(zzcq_str)){
                                    redisTemplate.opsForValue().set("zzcq_flag"+accout.getName(),"flag");
                                    redisTemplate.expire("zzcq_flag"+accout.getName(),10,TimeUnit.HOURS);
                                }


                        }


                    });

                    //如果是test群 发送的是图片 zf发送
                } else if ((Objects.equals("22822365300@chatroom", it)||Objects.equals(receiveMsgDto.getFinal_from_wxid(),"wxid_0p28wr3n0uh822")) && AllEnums.wechatMsgType.IMAGE.getCode() == receiveMsgDto.getMsg_type()) {


                    Arrays.asList("17490589131@chatroom", "18949318188@chatroom").forEach(obj -> {
                        //发送图片
                        WechatSendMsgDto wechatSendMsgDto_img = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), robotId, obj, receiveMsgDto.getMsg(), null, null, null);
                        WechatUtils.sendWechatTextMsg(wechatSendMsgDto_img);
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

        // 1 好物线报 薅羊毛群 3不是特定人
        if (configDo.getOwnGroup().contains(receiveMsgDto.getFrom_wxid()) &&
                (!configDo.getWhitename().contains(receiveMsgDto.getFinal_from_wxid()))) {

            //发送的是图片并且包含二维码
            if (AllEnums.wechatMsgType.IMAGE.getCode() == receiveMsgDto.getMsg_type() && (Utils.isHaveQr(receiveMsgDto.getMsg()))) {

                deleteMember(receiveMsgDto.getFinal_from_wxid(), receiveMsgDto.getFrom_wxid(), robotId);
                log.info("包含二维码====>");

                return true;
            }

            //如果发送的是文字消息
            if (AllEnums.wechatMsgType.TEXT.getCode() == receiveMsgDto.getMsg_type()) {

                String msgContent = receiveMsgDto.getMsg();

                for (String keyWord : configDo.getKeyWords()) {
                    if (msgContent.contains(keyWord)) {
                        //包含关键字：
                        deleteMember(receiveMsgDto.getFinal_from_wxid(), receiveMsgDto.getFrom_wxid(), robotId);
                        log.info("包含违规关键字======>{}", keyWord);
                        return true;
                    }
                }
            }


            //发送的不是文字、完成群公告、图片、语音,动态表情 判定违规
            if ((!Arrays.asList(AllEnums.wechatMsgType.TEXT.getCode(), AllEnums.wechatMsgType.at_allPerson.getCode(), AllEnums.wechatMsgType.fabuqungonggao.getCode(), AllEnums.wechatMsgType.qungonggao.getCode(), AllEnums.wechatMsgType.IMAGE.getCode(), AllEnums.wechatMsgType.YY.getCode(), AllEnums.wechatMsgType.ADD_FRIEND.getCode(), AllEnums.wechatMsgType.Emoticon.getCode()).contains(receiveMsgDto.getMsg_type())) && !Objects.equals(receiveMsgDto.getFinal_from_wxid(), receiveMsgDto.getFrom_wxid()) && !Arrays.asList(AllEnums.loveCatMsgType.GROUP_MEMBER_UP.getCode(), AllEnums.loveCatMsgType.GROUP_MEMBER_DOWN.getCode()).contains(receiveMsgDto.getType())) {
                deleteMember(receiveMsgDto.getFinal_from_wxid(), receiveMsgDto.getFrom_wxid(), robotId);
                return true;
            }

        }

        //接收的消息群消息  但不是发送到我们自己管理的群中的,不违规
        if (!configDo.getOwnGroup().contains(receiveMsgDto.getFrom_wxid())) {
            return false;
        }


        //如果是自己人发送,则不违规
        if (configDo.getWhitename().contains(receiveMsgDto.getFinal_from_wxid())) {
            return false;
        }

        // 有人发送没有违规的消息通知群主
        tzQunZhu(receiveMsgDto, robotId);


        //当有群成员退出群时,通知群主
        sendGroupMasterMemberRelease(receiveMsgDto, configDo.getOwnGroup(), robotId, redisTemplate);

        return false;
    }

    /**
     * 消除多余字符串
     *
     * @return
     */
    public String removeTempateStr(String str, WechatReceiveMsgDto receiveMsgDto) {

        if (str.contains("删除:") && receiveMsgDto.getFrom_wxid().equals("22822365300@chatroom")) {
            log.info("set remove str------------------>{}", str.substring(3));

            redisTemplate.opsForList().leftPush("remove_str", str.substring(3));
        }


        int i1 = str.lastIndexOf("\n");

        if (i1 != -1) {
            String substring = str.substring(i1);

            if (Objects.equals(receiveMsgDto.getFrom_wxid(), "18172911411@chatroom")) {

                int index = findIndex(substring);

                if (index == -1) {
                    staticStr = str;
                } else if (index == 0) {
                    staticStr = str.substring(0, i1);
                } else {
                    staticStr = str.substring(0, i1 + index);
                }

            } else {
                staticStr = str;
            }

        } else {
            staticStr = str;
        }

        configDo.getRemoveStr().forEach(it -> staticStr = staticStr.replace(it, ""));

        List<Object> remove_str = redisTemplate.opsForList().range("remove_str", 0, -1);

        if (!CollectionUtils.isEmpty(remove_str)) {

            remove_str.forEach(it -> {
                int i2 = staticStr.lastIndexOf((String) it);

                if (i2 != -1) {
                    staticStr = staticStr.substring(0, i2);
                }
            });

        }
        if (staticStr.contains("coupon")) {
            return staticStr.trim();
        } else {
            return staticStr.replaceAll("\\??", "").trim();
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

            if (strNum(str, "u.jd.com") < strNum(str, "http")) {
                return indexFind(str);

            } else {

                return -1;
            }
        } else {
            return str.indexOf("http");
        }

    }

    /**
     * 删除成员
     *
     * @param member_wxid
     * @param group_wxid
     */
    public void deleteMember(String member_wxid, String group_wxid, String robotId) {
        try {
            WechatSendMsgDto wsm = new WechatSendMsgDto(AllEnums.loveCatMsgType.DELETE_GROUP_MEMBER.getCode(), robotId, null, null, null, null, null);
            wsm.setMember_wxid(member_wxid);
            wsm.setGroup_wxid(group_wxid);
            String s = WechatUtils.sendWechatTextMsg(wsm);
            log.info("违规将群成员踢出群聊结果----->:{}", s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 机器人通知群主
     *
     * @param receiveMsgDto
     * @param robotId
     */
    public void tzQunZhu(WechatReceiveMsgDto receiveMsgDto, String robotId) {
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
                        log.info("消息回调====>{}", receiveMsgDto.getMsg());

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
    }

    public static boolean qunzhuSendMsg(String groupId) {
        //群主 test发送的消息
        return Objects.equals("22822365300@chatroom", groupId);
    }

    //是否含有数字
    public static boolean HasDigit(String content) {
        boolean flag = false;
        Pattern p = Pattern.compile(".*\\d+.*");
        Matcher m = p.matcher(content);
        if (m.matches()) {
            flag = true;
        }
        return flag;
    }

    public static int indexFind(String str) {

        boolean flag = true;
        String strCopy = str;
        while (flag) {

            int i = strCopy.indexOf("jd.com");
            if (i != -1) {
                strCopy = strCopy.substring(strCopy.indexOf("jd.com") + 16);
            } else {
                flag = false;
            }
        }

        if (strCopy.contains("http")) {
            return str.indexOf(strCopy.substring(strCopy.indexOf("http")));
        } else {
            return -1;
        }
    }
}
