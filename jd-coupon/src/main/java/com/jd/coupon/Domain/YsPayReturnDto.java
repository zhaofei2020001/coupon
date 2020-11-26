package com.jd.coupon.Domain;

import lombok.Data;

@Data
public class YsPayReturnDto {
    private String mid;//商户号
    private String tid;//终端号
    /**
     * NEW_ORDER：新订单；
     * UNKNOW:不明确的交易状态；
     * TRADE_CLOSED：
     * 在指定时间段内未支付时关闭的交易；在交易完成撤销成功时关闭的交易；支付失败的交易。
     * TRADE_SUCCESS：支付成功；
     * TRADE_REFUND:订单转入退货流程
     */
    private String billStatus ;//交易状态
    private String randomKey;//随机数
    private String signType;//签名算法
    private String seqId;//订单id 福彩方下单时上送的orderId 原样返回
    private String sign;//签名
    private String billDesc;//账单描述
    private String srcReserve;//预留字段
    private String totalAmount;//账单总金额
    private String merOrderId;//商户订单号
    private String billBizType;//账单业务类型
    private String paySeqId;//交易参考号
    private String couponAmount;//网付计算的优惠金额
    private String buyerPayAmount;//实付金额
    private String payTime;//支付时间
    private String targetOrderId;//第三方订单号
    private String targetSys;//目标平台代码
    private String payMethod;//支付类型
    /**
     * 微信渠道值为微信openId；
     * 支付宝渠道值为：支付宝id;
     * 云闪付渠道值为：银联openid
     */
    private String openId;//微信openId/支付宝id/银联openid
}
