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


//  @PostMapping("ys")
//  public void updateYsPayCallback(  HttpServletRequest request) {
//    Map<String, String[]> parameterMap = request.getParameterMap();
//    log.info("paramMap==========>{}", JSONObject.toJSONString(parameterMap));
//    YsPayReturnDto ysPayReturnDto = new YsPayReturnDto();
//    ysPayReturnDto.setNetpayOrderId(Objects.isNull(parameterMap.get("netpayOrderId")) ? "" : parameterMap.get("netpayOrderId")[0]);
//    ysPayReturnDto.setOrderId(Objects.isNull(parameterMap.get("orderId")) ? "" : parameterMap.get("orderId")[0]);
//    ysPayReturnDto.setPayAmt(Objects.isNull(parameterMap.get("payAmt")) ? "" : parameterMap.get("payAmt")[0]);
//    ysPayReturnDto.setPayId(Objects.isNull(parameterMap.get("payId")) ? "" : parameterMap.get("payId")[0]);
//    ysPayReturnDto.setRefundAmount(Objects.isNull(parameterMap.get("refundAmount")) ? "" : parameterMap.get("refundAmount")[0]);
//    ysPayReturnDto.setRefundOrderId(Objects.isNull(parameterMap.get("refundOrderId")) ? "" : parameterMap.get("refundOrderId")[0]);
//
//
//    ysPayReturnDto.setBillStatus(Objects.isNull(parameterMap.get("billStatus")) ? "" : parameterMap.get("billStatus")[0]);
//    ysPayReturnDto.setRandomKey(Objects.isNull(parameterMap.get("randomKey")) ? "" : parameterMap.get("randomKey")[0]);
//    ysPayReturnDto.setSign(Objects.isNull(parameterMap.get("sign")) ? "" : parameterMap.get("sign")[0]);
//    ysPayReturnDto.setSignType(Objects.isNull(parameterMap.get("signType")) ? "" : parameterMap.get("signType")[0]);
//    ysPayReturnDto.setSeqId(Objects.isNull(parameterMap.get("seqId")) ? "" : parameterMap.get("seqId")[0]);
//    ysPayReturnDto.setBillDesc(Objects.isNull(parameterMap.get("billDesc")) ? "" : parameterMap.get("billDesc")[0]);
//    ysPayReturnDto.setSrcReserve(Objects.nonNull(parameterMap.get("srcReserve")) ? "" : parameterMap.get("srcReserve")[0]);
//    ysPayReturnDto.setBillBizType(Objects.nonNull(parameterMap.get("billBizType")) ? "" : parameterMap.get("billBizType")[0]);
//    ysPayReturnDto.setPaySeqId(Objects.isNull(parameterMap.get("paySeqId")) ? "" : parameterMap.get("paySeqId")[0]);
//    ysPayReturnDto.setCouponAmount(Objects.isNull(parameterMap.get("couponAmount")) ? "" : parameterMap.get("couponAmount")[0]);
//    ysPayReturnDto.setBuyerPayAmount(Objects.nonNull(parameterMap.get("buyerPayAmount")) ? "" : parameterMap.get("buyerPayAmount")[0]);
//    ysPayReturnDto.setPayTime(Objects.isNull(parameterMap.get("payTime")) ? "" : parameterMap.get("payTime")[0]);
//    ysPayReturnDto.setTargetOrderId(Objects.isNull(parameterMap.get("targetOrderId")) ? "" : parameterMap.get("targetOrderId")[0]);
//    ysPayReturnDto.setTargetSys(Objects.isNull(parameterMap.get("targetSys")) ? "" : parameterMap.get("targetSys")[0]);
//    ysPayReturnDto.setPayMethod(Objects.isNull(parameterMap.get("payMethod")) ? "" : parameterMap.get("payMethod")[0]);
//    ysPayReturnDto.setOpenId(Objects.isNull(parameterMap.get("openId")) ? "" : parameterMap.get("openId")[0]);
//
//
//    String s = JSONObject.toJSONString(ysPayReturnDto);
//
//  }
}
