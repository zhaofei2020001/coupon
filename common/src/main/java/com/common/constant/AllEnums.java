package com.common.constant;

import java.util.Objects;

/**
 * @author zf
 * since 2019/12/16
 */
public class AllEnums {

  /**
   * 是否京东自营
   */
  public enum ownerEnum {
    OWN("g", "【京东自营】"), NOT_OWN("p", "【京东】");

    ownerEnum(String code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private String code;

    private String desc;

    public String getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }

    public static String getStr(String code) {
      String value = "";
      for (ownerEnum ownerEnum : ownerEnum.values()) {
        if (Objects.equals(ownerEnum.getCode(), code)) {
          value = ownerEnum.getDesc();
        }
      }
      return value;
    }

  }

  /**
   * 频道id
   * 频道id：1-好券商品,2-超级大卖场,10-9.9专区,22-热销爆品,24-数码家电,
   * 25-超市,26-母婴玩具,27-家具日用,28-美妆穿搭,29-医药保健,
   * 30-图书文具,31-今日必推,32-王牌好货,33-秒杀商品,34-拼购商品
   */
  public enum eliteEnum {
    HQSP(1, "好券商品"),
    CJDMC(2, "超级大卖场"),
    NINENINEZQ(10, "9.9专区"), RXBP(22, "热销爆品"), SMJD(24, "数码家电"), CS(25, "超市"), MYWJ(26, "母婴玩具"), JDRY(27, "家具日用"),
    MZCD(28, "美妆穿搭"), YYBJ(29, "医药保健"), TSWJ(30, "图书文具"), JRBJ(31, "今日必推"), WPHH(32, "王牌好货"), MSSP(33, "秒杀商品"), PGSP(34, "拼购商品");

    eliteEnum(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private int code;

    private String desc;

    public int getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }
  }

  /**
   * 是否爆款
   */
  public enum isHotEnum {
    HOT(1, "是"), NOT_HOT(2, "否");

    isHotEnum(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private int code;

    private String desc;

    public int getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }
  }

  /**
   * 是否是最优优惠券
   */
  public enum isBestCouponEnum {
    YES(1, "是"), NO(0, "否");

    isBestCouponEnum(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private int code;

    private String desc;

    public int getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }
  }

  /**
   * 优惠券种类
   */
  public enum couponTypeEnum {
    QPQ(0, "全品类"),
    XPL(1, "限品类（自营商品"),
    XDP(2, "限店铺"),
    DPXSP(3, "店铺限商品券");

    couponTypeEnum(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private int code;

    private String desc;

    public int getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }

  }


  /**
   * Love Cat返回的微信  消息的事件类型
   */
  public enum loveCatMsgType {

    PRIVATE_MSG(100, "私聊消息"), GROUP_MSG(200, "群聊消息"), GROUP_MEMBER_UP(400, "群成员增加"),
    GROUP_MEMBER_DOWN(410, "群成员减少"),
    RECEIVE_FRIEND_REQUEST(500, "收到好友请求"), QR_RECEIVE_MONEY(600, "二维码收款"),
    RECEIVE_MONEY(700, "收到转账"), SOFT_START(800, "软件开始启动"),
    NEW_ACCOUNT_LOGIN(900, "新的账号登录完成"), ACCOUNT_LOGIN_OUT(910, "账号下线");

    loveCatMsgType(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private int code;

    private String desc;

    public int getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }
  }

  /**
   * Love Cat返回的微信  消息内容的类型
   */
  public enum wechatMsgType {

    TEXT(1, "文字和表情消息"), IMAGE(3, "图片消息"), VIDEO(43, "视频"), RED_MONEY(2001, "微信红包"),
    CARD(42, "名片"), POSITION(48, "位置信息"), Emoticon(47, "表情包图片"),
    TRANSFER_MONEY(0, "转账");

    wechatMsgType(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private int code;

    private String desc;

    public int getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }
  }

  /**
   * 请求图灵机器人类型
   */
  public enum tlRequestTypeEnum {

    TEXT(0, "文字"), IMAGE(1, "图片"), VIDEO(2, "音频");

    tlRequestTypeEnum(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private int code;

    private String desc;

    public int getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }
  }

  /**
   * 微信群名称
   */
  public enum wechatGroupEnum {
    XWW(0, "小窝窝"),
    DYN_JDNG(1, "京东内购优惠群"),
    TEST(2,"test群");

    wechatGroupEnum(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private int code;

    private String desc;

    public int getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }

    public static String getStr(String desc) {
      String value = "";
      for (wechatGroupEnum wechatGroup : wechatGroupEnum.values()) {
        if (desc.contains(wechatGroup.getDesc())) {
          return wechatGroup.getDesc();
        }
      }
      return null;
    }
  }

  /**
   * 微信群成员标识 （个人或群）
   */
  public enum wechatMemberFlag {

    GROUP(0, "群"), MEMBER(1, "成员"),ROBOT(2,"机器人");

    wechatMemberFlag(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    private int code;

    private String desc;

    public int getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }
  }


}
