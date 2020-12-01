package com.jd.coupon.Domain;

import lombok.Data;

@Data
public class YsPayReturnDto {
    private String  billBizType;
    private String  billDesc;
    private String   billStatus;
    private String   buyerPayAmount;
    private String   couponAmount;
    private String   netpayOrderId;
    private String   openId;
    private String  orderId;
    private String  payAmt;
    private String   payId;
    private String   payMethod;
    private String   paySeqId;
    private String   payTime;
    private String   randomKey;
    private String   refundAmount;
    private String   refundOrderId;
    private String    seqId;
    private String   signType;
    private String   srcReserve;
    private String   targetOrderId;
    private String   targetSys;
    private String   sign;
}
