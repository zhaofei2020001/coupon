package com.common.constant;

/**
 * @author zf
 * since 2019/12/16
 */
public class Constants {
  /**
   * 京东联盟正式调用环境
   */
  public static final String JD_SERVER_URL = "https://router.jd.com/api";
  /**
   * love cat外网调用api地址
   */
  public static final String LOVE_CAT_URL = "http://172.16.135.159:8073/send";

  /**
   * 图灵机器人地址
   */
  public static final String TL_ROBOT_URL = "http://openapi.tuling123.com/openapi/api/v2";
  /**
   * 图灵机器人apikey
   */
  public static final String ROBOT_API_KEY = "4a70138a1f864f14ae18f581c396c8e6";

  //京东联盟appkey
  public static final String JD_APP_KEY = "0be52de336de95ef24e4555d0c7255b2";
  //京东联盟appsecret
  public static final String JD_APP_SECRET = "c77e85eb40034360a70980687ad501e2";
  //京东联盟推广位id
  public static  final  String JD_TGW_ID="1966400171";

  //蚂蚁星球appkey
  public static final String ANT_APP_KEY = "509fbeedfba4d419";
  public  static  final  String ANT_SERVER_URL="http://api-gw.haojingke.com/index.php/v1/api/jd/getunionurl";
  /**
   * 京粉精选商品查询接口
   */
  public static final String JD_JF_QUERY = "jd.union.open.goods.jingfen.query";


  public static final String SPLIT_FLAG = "::::";


  public static final int pageIndex = 1;
  public static final int pageSize = 50;
  public static final int eliteI = 10;
  public static final String sort = "desc";
  public static final String sort_name = "inOrderCount30DaysSku";

  /**
   * 请求京东获取授权的服务器地址
   */
  public static final String requireAccessTokenUrl = "https://auth.360buy.com/oauth/authorize?response_type=code&client_id=125993&redirect_uri=http://qxkvk3.natappfree.cc/jd/test";

  public static final String getCodeUrl = "https://open-oauth.jd.com/oauth2/to_login?app_key=0be52de336de95ef24e4555d0c7255b2&response_type=code&redirect_uri=http://qxkvk3.natappfree.cc/jd/code&state=20180416&scope=snsapi_base";

  public static final String HAD_SEND_SKU = "HAD_SEND_SKU";
}
