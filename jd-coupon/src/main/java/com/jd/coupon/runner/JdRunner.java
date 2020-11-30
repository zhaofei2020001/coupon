package com.jd.coupon.runner;

import com.alibaba.fastjson.JSONObject;
import com.common.dto.account.Account;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zf
 * since 2020/1/2
 */
@Slf4j
@Component//被spring容器管理
@Order(3)//如果多个自定义ApplicationRunner，用来标明执行顺序
public class JdRunner implements ApplicationRunner {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 发送线报使用哪个群中的机器人
     */
    @Value("${message.robot.group}")
    private String robotGroup;

    @Override
    public void run(ApplicationArguments args) {

//
//        String robotName = (String) redisTemplate.opsForHash().get("wechat_friends", "wxid_8sofyhvoo4p322");
//        if (StringUtils.isEmpty(robotName)) {
//
//            WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_FRIEND_MEMBER.getCode(), "wxid_8sofyhvoo4p322", null, null, null, null, null);
//            wechatSendMsgDto.setGroup_wxid("17490589131@chatroom");
//            wechatSendMsgDto.setIs_refresh("1");
//            String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
//
//            String str = JSONObject.parseObject(s1).getString("data");
//            String encode = URLUtil.decode(str, "UTF-8");
//            List<RobotFriend> robotFriends = JSONObject.parseArray(encode).toJavaList(RobotFriend.class);
//
//            robotFriends.forEach(item -> {
//                redisTemplate.opsForHash().put("wechat_friends", item.getWxid(), item.getNickname());
//            });
//
//
//            WechatSendMsgDto wechatSendMsgDtozzf = new WechatSendMsgDto(AllEnums.loveCatMsgType.GROUP_FRIEND_MEMBER.getCode(), "wxid_8sofyhvoo4p322", null, null, null, null, null);
//            wechatSendMsgDtozzf.setGroup_wxid("18949318188@chatroom");
//            wechatSendMsgDtozzf.setIs_refresh("1");
//            String s1zzf = WechatUtils.sendWechatTextMsg(wechatSendMsgDtozzf);
//
//            String strzzf = JSONObject.parseObject(s1zzf).getString("data");
//            String encodezzf = URLUtil.decode(strzzf, "UTF-8");
//            List<RobotFriend> robotFriendszzf = JSONObject.parseArray(encodezzf).toJavaList(RobotFriend.class);
//
//            robotFriendszzf.forEach(item -> {
//                redisTemplate.opsForHash().put("wechat_friends", item.getWxid(), item.getNickname());
//            });
//
//            log.info("-----------------------------加载完成-----------------------------");
//        }

        String accoutStr = (String) redisTemplate.opsForValue().get("account");
        if (StringUtils.isEmpty(accoutStr)) {
            List<Account> accountList = Lists.newArrayList();

            Account account1 = new Account();
            account1.setName("ddy");
            //将优惠线报发送给指定的人
            account1.setMsgToPersons(Arrays.asList("wxid_2r8n0q5v38h222", "du-yannan"));
            account1.setGroupId("17490589131@chatroom");
            account1.setJdtgwid("1987045755");
            account1.setAntappkey("872ea5798e8746d0");
            account1.setHbrk("https://u.jd.com/tLOfFeZ");

            Account account2 = new Account();
            account2.setMsgToPersons(new ArrayList<>());
            account2.setName("zzf");
            account2.setGroupId("18949318188@chatroom");
            account2.setJdtgwid("3002800583");
            account2.setAntappkey("5862cd52a87a1914");
            account2.setHbrk("https://u.jd.com/tk24yiI");

            accountList.add(account1);
            accountList.add(account2);
            String s = JSONObject.toJSONString(accountList);
            redisTemplate.opsForValue().set("account", s);
            log.info("-----------------------------加载账号完成-----------------------------");
        }

    }

}
