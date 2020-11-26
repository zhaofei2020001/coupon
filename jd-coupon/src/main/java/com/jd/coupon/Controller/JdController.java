package com.jd.coupon.Controller;

import com.common.dto.wechat.WechatReceiveMsgDto;
import com.jd.coupon.service.JdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zf
 * since 2019/12/27
 */
@RestController
@RequestMapping("/jd")
@Slf4j
public class JdController {
  @Autowired
  private JdService jdService;

  /**
   * 从love cat上接收微信消息
   * @param wechatReceiveMsgDto
   */
  @PostMapping("/receive/wechat/msg")
  public void receiveWechatMsg(WechatReceiveMsgDto wechatReceiveMsgDto) {
    jdService.receiveWechatMsg(wechatReceiveMsgDto);
  }

//
//  @PostMapping("ys")
//  public void updateYsPayCallback(@RequestBody YsPayReturnDto ysPayReturnDto, HttpServletRequest request) {
//    log.info("ysPayReturnDto===>{}", JSONObject.toJSONString(ysPayReturnDto));
//    log.info("request===>", JSONObject.toJSONString(request));
//
//  }
}
