package com.jd.coupon.Controller;

import com.common.dto.wechat.WechatReceiveMsgDto;
import com.jd.coupon.service.JdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

/**
 * @author zf
 * since 2019/12/27
 */
@RestController
@RequestMapping("/jd")
public class JdController {
  @Autowired
  private JdService jdService;

  /**
   * 从love cat上接收微信消息
   * @param wechatReceiveMsgDto
   */
  @PostMapping("/receive/wechat/msg")
  public void receiveWechatMsg(WechatReceiveMsgDto wechatReceiveMsgDto) throws UnsupportedEncodingException {
    jdService.receiveWechatMsg(wechatReceiveMsgDto);
  }

  /**
   * 将消息模板存至缓存，等待机器人调用发送到群里
   * @param content
   * @param imgName
   * @return
   */
  @PostMapping("/set/robot/msg")
  public boolean setMsgToRedis(@RequestParam(value = "content") String content,@RequestParam(value = "groupName")String groupName,@RequestParam(value = "imgName",required = false) String imgName) {
    return jdService.setMsgToRedis(content,imgName,groupName);
  }
  @PostMapping("/code")
  public void getCode(HttpServletRequest request){
    System.out.println(request);
  }
}
