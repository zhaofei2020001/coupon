package com.common.constant;

/**
 * @author zf
 * since 2019/12/16
 */
public class AllEnums {

    /**
     * 微信消息的 事件类型
     */
    public enum loveCatMsgType {

        PRIVATE_MSG(100, "私聊消息"), GROUP_MSG(200, "群聊消息"), GROUP_MEMBER_UP(400, "群成员增加"), GROUP_AT_MSG(102, "发送群消息并艾特某人"),
        SKU_PICTURE(103, "发送图片"), FRIEND_MEMBER(204, "获取好友列表"), GROUP_FRIEND_MEMBER(206, "获取群成员列表"), DELETE_GROUP_MEMBER(306, "踢出群成员"),
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
     * 微信消息的 内容的类型
     */
    public enum wechatMsgType {

        TEXT(1, "文本信息"), IMAGE(3, "图片消息"), YY(34, "语音"), VIDEO(43, "视频"), RED_MONEY(2001, "微信红包"),
        CARD(42, "名片"), POSITION(48, "位置信息"), Emoticon(47, "表情包图片"), LINK(49, "分享"),
        TRANSFER_MONEY(0, "转账"), xcx(2002, "小程序"), at_allPerson(2006, "艾特所有人"), ADD_FRIEND(10000, "添加好友请求");

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

        public static String getStr(int code) {
            for (wechatMsgType wechatMsgType : wechatMsgType.values()) {
                if (wechatMsgType.getCode() == code) {
                    return wechatMsgType.getDesc();
                }
            }
            return "消息代码类型[" + code + "]";
        }


    }
}
