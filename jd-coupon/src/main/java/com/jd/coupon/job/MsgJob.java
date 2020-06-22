package com.jd.coupon.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author zf
 * since 2020/4/2
 */
@Slf4j
@Configuration
@EnableScheduling
public class MsgJob {

//  @Scheduled(cron = "0 3 2,9,11,12,13,14,16,18,20,22,23 * * ?")
//  public static void heNanRefundHandle() throws UnsupportedEncodingException {
//    String msg = "----------公告----------\n①618将近,本群新增免单线报\n②如果您需要线报留意某些商品,可以发送:买【关键字】,如想买手机可发送:买手机\n③如果您身边有朋友喜欢网购，也可以邀请加入，谢谢大家配合";
//    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), "wxid_8sofyhvoo4p322", "17490589131@chatroom", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(msg), "UTF-8"), null, null, null);
//    String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
//    log.info("发送文字线报结果----->:{},time--->{}", s1, DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
//  }
}
