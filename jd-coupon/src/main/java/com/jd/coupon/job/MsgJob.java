package com.jd.coupon.job;

import com.common.constant.AllEnums;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.jd.Utf8Util;
import com.common.util.wechat.WechatUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author zf
 * since 2020/4/2
 */
@Slf4j
@Configuration
@EnableScheduling
public class MsgJob {

  @Scheduled(cron = "0 3 2 * * ?")
  public static void heNanRefundHandle() throws UnsupportedEncodingException {
    String msg = "京东自助查券:\nhttps://u.jd.com/BkjTcZ\n—\n" +
        "淘宝、唯品会、拼多多自助查券:\nhttps://w.url.cn/s/AG4Dntq\n—\n" + "需要什么产品，可以搜索领券，看看有没有活动!";
    WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), "wxid_8sofyhvoo4p322", "17490589131@chatroom", URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(msg), "UTF-8"), null, null, null);
    String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
    log.info("发送文字线报结果----->:{}", s1);
    System.out.println("s1-->" + s1);
  }
}
