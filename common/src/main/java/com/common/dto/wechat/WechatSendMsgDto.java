package com.common.dto.wechat;

import lombok.Data;

/**
 * @author zf
 * since 2019/12/27
 */
@Data
public class WechatSendMsgDto {

    private int type;
    /**
     * 机器人id
     */
    private String robot_wxid;
    /**
     * 接收方id
     */
    private String to_wxid;
    /**
     * 发送的文字消息（好友或群）
     */
    private String msg;
    /**
     * 发送图片绝对路径
     */
    private String path;
    /**
     * 被艾特某人的微信id
     */
    private String at_wxid;
    /**
     * 被艾特的群成员昵称
     */
    private String at_name;
    private String friend_wxid;


    /**
     * 是否刷新列表，0 从缓存获取  1 刷新并获取
     */
    private String is_refresh;
    /**
     * 获取群好友列表群id
     */
    private String group_wxid;

    /**
     * 被踢出群聊的群成员微信id
     */
    private String member_wxid;

    public WechatSendMsgDto(int type, String robot_wxid, String to_wxid, String msg, String path, String at_wxid, String at_name) {
        this.type = type;
        this.robot_wxid = robot_wxid;
        this.to_wxid = to_wxid;
        this.msg = msg;
        this.path = path;
        this.at_wxid = at_wxid;
        this.at_name = at_name;
    }


}
