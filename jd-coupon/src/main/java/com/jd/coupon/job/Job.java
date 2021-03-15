package com.jd.coupon.job;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.account.Account;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.jd.ReadFile;
import com.common.util.jd.Utils;
import com.common.util.wechat.WechatUtils;
import com.jd.coupon.Domain.ConfigDo;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@Slf4j
public class Job {


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    ConfigDo configDo;

//    /**
//     * 发送自助查券链接
//     */
//    @Scheduled(cron = "0 0/30 1-5 * * ?")
//    public void zzcq() {
//        String hour = (String) redisTemplate.opsForValue().get("send_last_msg_time");
//        String flag = (String) redisTemplate.opsForValue().get("zzcq" + DateTime.now().toString("yyyy-MM-dd"));
//
//        if (!StringUtils.isEmpty(hour) && StringUtils.isEmpty(flag) && (Integer.parseInt(DateTime.now().toString("HH")) - Integer.parseInt(hour) >= 2)) {
//            //京东优惠券查询入口地址
//            String jdCheckList = (String) redisTemplate.opsForValue().get("JD_CHECK_LIST");
//            if (StringUtils.isEmpty(jdCheckList)) {
//                log.info("京东优惠券查询入口地址为空,请添加===================================>");
//                return;
//            }
//            //当日京东优惠券查询入口已发送
//            redisTemplate.opsForValue().set("zzcq" + DateTime.now().toString("yyyy-MM-dd"), new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
//            redisTemplate.expire("zzcq" + DateTime.now().toString("yyyy-MM-dd"), 8, TimeUnit.HOURS);
//
//            //获取不同账号京东转链参数
//            String accoutStr = (String) redisTemplate.opsForValue().get("account");
//            List<Account> accounts = JSONObject.parseArray(accoutStr, Account.class);
//
//
//            Account account = accounts.stream().filter(it -> Objects.equals("ddy", it.getName())).findFirst().get();
//
//            List<String> allUrl = Utils.getAllUrl(jdCheckList);
//            String shortUrl = Utils.zlStr(jdCheckList, account, allUrl,"");
//
//
//            if (StringUtils.isEmpty(shortUrl)) {
//                log.info("京东优惠券查询入口更新失败,请尽快查询原因避免接口地址过期===================================>");
//            } else {
//                //更新京东优惠券查询入口地址
//                redisTemplate.opsForValue().set("JD_CHECK_LIST", shortUrl);
//            }
//
//            accounts.forEach(it -> {
//
//                if (Objects.equals(it.getName(), "ddy")) {
//                    WechatSendMsgDto wechatSendMsgDto = null;
//                    try {
//                        wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), configDo.getRobotGroup(), it.getGroupId(), URLEncoder.encode(shortUrl, "UTF-8"), null, null, null);
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }
////                    String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
////                    log.info("发送文字线报结果----->:{}", s1);
//
//                } else {
//                    WechatSendMsgDto wechatSendMsgDto = null;
//
//                    try {
//                        wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), configDo.getRobotGroup(), it.getGroupId(), URLEncoder.encode(Utils.zlStr(jdCheckList, it, allUrl,""), "UTF-8"), null, null, null);
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }
////                    String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
////                    log.info("发送文字线报结果----->:{}", s1);
//                }
//
//
//                //发送图片
//                WechatSendMsgDto wechatSendMsgDto_img = new WechatSendMsgDto(AllEnums.loveCatMsgType.SKU_PICTURE.getCode(), configDo.getRobotGroup(), it.getGroupId(), Constants.BASE_URL + "pic.jpeg", null, null, null);
////                String s2 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto_img);
////                log.info("图片群{}====>结果--------------->:{}", it.getName(), s2);
//
//            });
//        }
//    }

    /**
     * 删除缓存图片
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void deleteCachePic() {
        try {
            boolean deletefile = ReadFile.deletefile("C:\\Users\\Administrator\\Desktop\\cat\\app\\https.cat\\http\\网页\\static");
            if(deletefile){
                log.info("删除缓存图片成功=========================================================>");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
