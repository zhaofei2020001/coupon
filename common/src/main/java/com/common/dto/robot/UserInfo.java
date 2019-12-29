package com.common.dto.robot;

import lombok.Data;

/**
 * @author zf
 * since 2019/12/27
 */
@Data
public class UserInfo {
  /**
   * 机器人标识
   */
  private String apiKey;
  /**
   * 用户唯一标识
   */
  private String userId;
  /**
   * 群聊唯一标识
   */
  private String groupId;
  /**
   * 群内用户昵称
   */
  private String userIdName;

  public UserInfo(String apiKey,String userId){
    this.apiKey=apiKey;
    this.userId=userId;
  }
}
