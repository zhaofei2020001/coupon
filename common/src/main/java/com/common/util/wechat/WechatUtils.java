package com.common.util.wechat;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.Constants;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.HttpUtils;

/**
 * @author zf
 * since 2019/12/27
 */
public class WechatUtils {

  /**
   * 发送文字消息(好友或者群)
   *
   * @return json格式
   */
  public static String sendWechatTextMsg(WechatSendMsgDto wechatSendMsgDto) {
    String resultStr = HttpUtils.post(Constants.LOVE_CAT_URL, JSONObject.toJSONString(wechatSendMsgDto, true));
    return resultStr;
  }
}
