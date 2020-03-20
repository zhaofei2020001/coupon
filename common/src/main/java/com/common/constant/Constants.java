package com.common.constant;

/**
 * @author zf
 * since 2019/12/16
 */
public class Constants {

  /**
   * love cat外网调用api地址
   */
  public static final String LOVE_CAT_URL = "http://172.16.135.178:8073/send";

  /**
   * 京东联盟appkey
   */
  public static final String JD_APP_KEY = "30ef42082f97c0c1839b241ad6f1ae6a";
  /**
   * 京东联盟appsecret
   */
  public static final String JD_APP_SECRET = "c516c5ae024e465d8e57ed31e68c2fd9";
  /**
   * 京东联盟推广位id
   */
  public static final String JD_TGW_ID = "1987045755";
  /**
   * 京东联盟的id(不是推广位的pid)
   */
  public static final String JDLM_ID = "1002127372";
  /**
   * 蚂蚁星球appkey
   */
  public static final String ANT_APP_KEY = "872ea5798e8746d0";
  public static final String ANT_SERVER_URL = "http://api-gw.haojingke.com/index.php/v1/api/jd/getunionurl";

  /**
   * 标记微信群中发送的消息
   */
  public static final String wechat_msg_send_flag = "message_send_flag";

  /**
   * 已经接收的消息
   */
  public static final String HAD_RECEIVE_MSG = "had_receive_msg";

  /**
   * 违规成员标志
   */
  public static final String wechat_msg_illegal = ":illegal:";

  /**
   * 群成员在群里发消息的标志
   */
  public static final String wechat_msg_send = ":send:";

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
  public static final String tb_name = "赵志飞1990";
  /**
   * 喵有券 (重构版)万能高佣转链API接口
   */
  public static final String TKL_TO_SKU_INFO_REQUEST_URL = "http://api.web.21ds.cn/taoke/doItemHighCommissionPromotionLinkByAll?apkey=%s&tpwd=1&shorturl=1&tbname=%s&pid=%s&content=%s";

  /**
   * 订单侠apikey
   */
  public static final String DDX_APIKEY = "9emxXcz3OaIwpmv6bfvB9qpM0JiStMPW";
  /**
   * 订单狭转链接口
   */
  public static final String DDX_TOLINK_URL = "http://api.tbk.dingdanxia.com/jd/by_unionid_promotion?apikey=%s&materialId=%s&unionId=%s";
  /**
   * 订单侠对淘宝优惠券链接
   */
  public static final String tb_coupon_tolink_ddx ="http://api.tbk.dingdanxia.com/tbk/tkl_privilege?apikey=%s&tkl=$s";
  /**
   * 订单侠获取商品信息接口
   */
  public static final String DDX_SKU_INFO = "http://api.tbk.dingdanxia.com/jd/query_goods_promotioninfo?apikey=%s&skuIds=%s";
  /**
   * 订单侠获取京东商品skuid
   */
  public static final String DDX_GET_SKUID = "http://api.tbk.dingdanxia.com/jd/get_jd_skuid?apikey=%s&url=%s";

  /**
   * 将淘口令放置某一页面模板中
   */
  public static final String TB_COPY_PAGE = "https://dl016.kuaizhan.com?";


  /**
   * **********************折淘客接口：http://www.zhetaoke.com/user/open/open_gaoyongzhuanlian_tkl.aspx **********************
   */
  public static final String ztk_appkey="a15e3d21c935400a8df0020eebd1ede3";
  public static final String ztk_sid="28024";
  public static final String ztk_tkl_create="https://api.zhetaoke.com:10001/api/open_tkl_create.ashx?appkey=a15e3d21c935400a8df0020eebd1ede3&sid=28024&text=你好啊&url=%s&signurl=0";
  //由淘口令直接高佣转链
  public static final String ztk_gy_zl="https://api.zhetaoke.com:10001/api/open_gaoyongzhuanlian_tkl.ashx?appkey=a15e3d21c935400a8df0020eebd1ede3&sid=28024&pid=mm_812490050_1193400202_109884050450&tkl=%s&signurl=5";
}
