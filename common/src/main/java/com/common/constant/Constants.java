package com.common.constant;

/**
 * @author zf
 * since 2019/12/16
 */
public class Constants {

  /**
   * love cat外网调用api地址
   */

  public static final String LOVE_CAT_URL = Constants.LOVE_CAT_DOMAIN_NAME + "send";

  public static final String LOVE_CAT_DOMAIN_NAME = "http://172.16.118.128:8073/";
//  public static final String LOVE_CAT_DOMAIN_NAME = "http://127.0.0.1:8073/";;

  public static final String PIC_SAVE_PATH="/Users/mac/image/";

  /**
   * 蚂蚁星球appkey
   */
  public static final String ANT_SERVER_URL = "http://api-gw.haojingke.com/index.php/v1/api/jd/getunionurl";

  /**
   * 标记微信群中发送的消息
   */
  public static final String wechat_msg_send_flag = "message_send_flag";


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
   * **********************折淘客接口：http://www.zhetaoke.com/user/open/open_gaoyongzhuanlian_tkl.aspx **********************
   */
  //由淘口令直接高佣转链
  public static final String ztk_gy_zl = "https://api.zhetaoke.com:10001/api/open_gaoyongzhuanlian_tkl.ashx?appkey=a15e3d21c935400a8df0020eebd1ede3&sid=28024&pid=mm_812490050_1193400202_109884050450&tkl=%s&signurl=5";
  //淘口令转换为新浪链接
  public static final String ztk_tkl_toLink = "https://api.zhetaoke.com:10001/api/open_shorturl_sina_get.ashx?appkey=a15e3d21c935400a8df0020eebd1ede3&sid=28024&content=%s&type=1";

  //京东转链api
  public static final String ztk_tkl_jd_toLink = "https://api.zhetaoke.com:10001/api/open_gaoyongzhuanlian.ashx?appkey=a15e3d21c935400a8df0020eebd1ede3&sid=28024&pid=mm_812490050_1193400202_109884050450&num_iid=%s&signurl=5";

  /**
   * **********************淘口令网址 https://www.taokouling.com/api/tkljm/ **********************
   */
  //淘口令解析出网址
  public static final String tkl_toLink = "https://api.taokouling.com/tkl/tkljm?apikey=jOplIHwgOA&tkl=%s";

}
