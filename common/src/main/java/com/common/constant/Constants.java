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
    //    public static final String LOVE_CAT_DOMAIN_NAME = "http://39.98.77.98:8073/";
    //调用lovely cat所在服务器的IP
    public static final String LOVE_CAT_DOMAIN_NAME = "http://127.0.0.1:8073/";

    /**
     * 蚂蚁星球appkey
     */
    public static final String ANT_SERVER_URL = "http://api-gw.haojingke.com/index.php/v1/api/jd/getunionurl";

    /**
     * 违规成员标志
     */
    public static final String wechat_msg_illegal = ":illegal:";

    /**
     * 订单侠获取京东商品skuid
     */
    public static final String DDX_GET_SKUID = "http://api.tbk.dingdanxia.com/jd/get_jd_skuid?apikey=%s&url=%s";

    /**
     * 订单侠apikey
     */
    public static final String DDX_APIKEY = "9emxXcz3OaIwpmv6bfvB9qpM0JiStMPW";


    public  static  String BASE_URL="C:\\Users\\Administrator\\Desktop\\cat\\";
    //================以下是为了获取淘口令图片==================

    /**
     * 喵有券 (重构版)万能高佣转链API接口
     */
    public static final String TKL_TO_SKU_INFO_REQUEST_URL = "http://api.web.21ds.cn/taoke/doItemHighCommissionPromotionLinkByAll?apkey=%s&tpwd=1&shorturl=1&tbname=%s&pid=%s&content=%s";

    /**
     * 喵有券apkey
     */
    public static final String MYB_APPKey = "3a3d9374-2698-321a-8b53-6d70804665a5";

    /**
     * 淘宝联盟中推广位的pid
     */
    public static final String TBLM_PID = "mm_812490050_1193400202_109884050450";

    /**
     * 淘宝用户名
     */
    public static final String tb_name = "赵志飞1990";

    //京东转链api
    public static final String ztk_tkl_jd_toLink = "https://api.zhetaoke.com:10001/api/open_gaoyongzhuanlian.ashx?appkey=a15e3d21c935400a8df0020eebd1ede3&sid=28024&pid=mm_812490050_1193400202_109884050450&num_iid=%s&signurl=5";

}
