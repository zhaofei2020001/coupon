package com.jd.coupon.Controller;

import com.common.dto.wechat.WechatReceiveMsgDto;
import com.jd.coupon.service.JdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/jd")
@Slf4j
public class JdController {
    @Autowired
    private JdService jdService;


    @PostMapping("/receive/wechat/msg")
    public void receiveWechatMsg(WechatReceiveMsgDto wechatReceiveMsgDto) {
        jdService.receiveWechatMsg(wechatReceiveMsgDto);
    }

}
