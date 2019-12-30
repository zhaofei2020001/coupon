package com.common.util.robot;

import com.common.constant.AllEnums;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author zf
 * since 2019/12/27
 */
public class Utils {

  /**
   * 如果是@机器人则返@机器人的内容否则返回null(目前机器人只处理文字)
   *
   * @param str
   * @return
   */
  public static String jqStr(String str,int msg_type) {
    if(AllEnums.wechatMsgType.TEXT.getCode()!=msg_type){
      return null;
    }

    try {
      int strStartIndex = str.indexOf("[");
      int strEndIndex = str.indexOf("]");
      /* 开始截取 */
      List<String> list = CollectionUtils.arrayToList(str.substring(strStartIndex + 1, strEndIndex).split(","));
      if (Objects.equals("@at", list.get(0)) && Objects.equals("nickname=京东小助手", list.get(1))) {
        return str.substring(strEndIndex + 1).trim();
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }
}
