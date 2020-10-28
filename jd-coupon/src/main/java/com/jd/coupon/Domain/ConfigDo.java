package com.jd.coupon.Domain;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * @author zf
 * since 2020/2/17
 */
@Component
@Data
public class ConfigDo {
    /**
     * 线报采集群
     */
    @Value("#{'${message.from.group}'.split(',')}")
    private List<String> msgFromGroup;
    /**
     * 需要发送线报的群
     */
    @Value("#{'${message.own.group}'.split(',')}")
    private List<String> ownGroup;

    /**
     * 判定违规的关键字
     */
    @Value("#{'${message.key.word}'.split(',')}")
    private List<String> keyWords;

    /**
     * 判定违规后艾特某人的消息模板
     */
    @Value("${message.template}")
    private String template;

    /**
     * 发送线报使用哪个群中的机器人
     */
    @Value("${message.robot.group}")
    private String robotGroup;
    /**
     * 线报中提示语
     */
    @Value("${message.reminder}")
    private String reminderTemplate;
    /**
     * 消除接收线报中的指定字符串
     */
    @Value("#{'${message.remove.tempate}'.split(',')}")
    private List<String> removeStr;

    /**
     * 接收线报的关键字
     */
    @Value("#{'${message.keyWords}'.split(',')}")
    private List<String> msgKeyWords;

    /**
     * 发送给群主的关键字
     */
    @Value("#{'${message.owenkeywords}'.split(',')}")
    private List<String> owenkeywords;

    /**
     * 线报白名单
     */
    @Value("#{'${message.whitename}'.split(',')}")
    private List<String> whitename;
}
