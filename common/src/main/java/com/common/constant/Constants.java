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
  public static final String LOVE_CAT_URL = "http://172.16.135.174:8073/send";

  /**
   * 图灵机器人地址
   */
  public static final String TL_ROBOT_URL = "http://openapi.tuling123.com/openapi/api/v2";
  /**
   * 图灵机器人apikey
   */
  public static final String ROBOT_API_KEY = "4a70138a1f864f14ae18f581c396c8e6";

  //京东联盟appkey
  public static final String JD_APP_KEY = "30ef42082f97c0c1839b241ad6f1ae6a";
  //京东联盟appsecret
  public static final String JD_APP_SECRET = "c516c5ae024e465d8e57ed31e68c2fd9";
  //京东联盟推广位id
  public static final String JD_TGW_ID = "1987045755";
  /**
   * 京东联盟的id(不是推广位的pid)
   */
  public static final String JDLM_ID="1002127372";

  //蚂蚁星球appkey
  public static final String ANT_APP_KEY = "872ea5798e8746d0";
  public static final String ANT_SERVER_URL = "http://api-gw.haojingke.com/index.php/v1/api/jd/getunionurl";

  //标记微信群中发送的消息
  public static final String wechat_msg_send_flag = "message_send_flag";

  //违规成员标志
  public static final String wechat_msg_illegal = ":illegal:";

  //群成员在群里发消息的标志
  public static final String wechat_msg_send = ":send:";


  public static final int pageIndex = 1;
  public static final int pageSize = 50;
  public static final int eliteI = 10;
  public static final String sort = "desc";
  public static final String sort_name = "inOrderCount30DaysSku";

  /**
   * 请求京东获取授权的服务器地址
   */
  public static final String requireAccessTokenUrl = "https://auth.360buy.com/oauth/authorize?response_type=code&client_id=125993&redirect_uri=http://qxkvk3.natappfree.cc/jd/test";


  /**
   * 淘宝联盟中推广位的pid
   */
  public static final String TBLM_PID = "mm_812490050_1193400202_109884050450";
  /**
   * 喵有券apkey
   */
  public static final String MYB_APPKey = "3a3d9374-2698-321a-8b53-6d70804665a5";
  /**
   * 淘宝用户名
   */
  public static final String tb_name="赵志飞1990";
  /**
   * 喵有券 (重构版)万能高佣转链API接口
   */
  public static final String TKL_TO_SKU_INFO_REQUEST_URL = "http://api.web.21ds.cn/taoke/doItemHighCommissionPromotionLinkByAll?apkey=%s&tpwd=1&shorturl=1&tbname=%s&pid=%s&content=%s";


  /**
   * 订单侠apikey
   */
  public static final String DDX_APIKEY="9emxXcz3OaIwpmv6bfvB9qpM0JiStMPW";
  /**
   * 订单狭转链接口
   */
  public static final String DDX_TOLINK_URL="http://api.tbk.dingdanxia.com/jd/by_unionid_promotion?apikey=%s&materialId=%s&unionId=%s";
  /**
   * 订单侠获取商品信息接口
   */
  public static final String DDX_SKU_INFO="http://api.tbk.dingdanxia.com/jd/query_goods_promotioninfo?apikey=%s&skuIds=%s";
  /**
   * 订单侠获取京东商品skuid
   */
  public static final String DDX_GET_SKUID="http://api.tbk.dingdanxia.com/jd/get_jd_skuid?apikey=%s&url=%s";
}
