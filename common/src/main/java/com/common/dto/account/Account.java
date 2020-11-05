package com.common.dto.account;

import lombok.Data;

import java.util.List;

@Data
public class Account {

    /**
     * 管理人
     */
    private String name;
    /**
     * 特惠消息推送人id
     */
    private List<String> msgToPersons;

    /**
     * 消息发送的群id
     */
    private String groupId;
    /**
     * 京东联盟推广位id
     */
    private String jdtgwid;
    /**
     * 蚂蚁星球appkey
     */
    private String antappkey;
    /**
     * 消息发送的群id
     */
    private List<String> msgToGroupId;
    //双十一领红包入口
    private String hbrk;
}
