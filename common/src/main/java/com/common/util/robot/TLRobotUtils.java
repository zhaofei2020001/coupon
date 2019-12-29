package com.common.util.robot;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.robot.InputText;
import com.common.dto.robot.Perception;
import com.common.dto.robot.TLRobotRequestDto;
import com.common.dto.robot.UserInfo;
import com.common.util.HttpUtils;

import java.util.Objects;

/**
 * @author zf
 * since 2019/12/27
 */
public class TLRobotUtils {


  public static String requestRobot(TLRobotRequestDto robot) {
    String post = HttpUtils.post(Constants.TL_ROBOT_URL, JSONObject.toJSONString(robot));
    System.out.println("机器人返回结果："+post);
    return post;
  }

  /**
   * 组装请求机器人的求请体
   *
   * @return
   */
  public static String assembleRobotDto(TLRobotRequestDto robot, String param) {
    if (Objects.equals(AllEnums.wechatMsgType.TEXT.getCode(), robot.getReqType())) {
      Perception perception = new Perception();
      perception.setInputText(new InputText(param));
      robot.setPerception(perception);

      UserInfo userInfo = new UserInfo(Constants.ROBOT_API_KEY, "abc123");
      robot.setUserInfo(userInfo);
      String s = requestRobot(robot);
      return s;
    } else {
      return null;
    }
  }
}
