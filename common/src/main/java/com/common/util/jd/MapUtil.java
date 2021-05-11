package com.common.util.jd;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zf
 * since 2020/2/12
 */
@Slf4j
public class MapUtil {
    /**
     * 获取map中第一个非空数据值
     *
     * @param <>  Key的类型
     * @param <>  Value的类型
     * @param map 数据源
     * @param rid 同一条消息标志
     * @return 返回的值
     */
    public static String getFirstNotNull(LinkedHashMap<String, String> map, RedisTemplate<String, Object> redisTemplate, String str, String antappkey, String rid) {
        Boolean sku_str_flag;
        int num = 0;
        boolean picFlag = true;
        String picLink = "";


        for (Map.Entry<String, String> entry : map.entrySet()) {
            num++;

            //消息为第一次发送标志 boolean默认为false
            boolean oneSendFlag;
            String skuUrl = entry.getKey();
            String skuId = Utils.getSkuIdByUrl(skuUrl);

            if (StringUtils.isEmpty(skuId)) {
                log.info("京东skuId获取失败====>{}", skuUrl);
                oneSendFlag = true;
            } else {
                oneSendFlag = redisTemplate.opsForHash().putIfAbsent(skuId, skuId, rid);
                redisTemplate.expire(skuId, DateTime.now().plusDays(1).toLocalDate().toDate().getTime() + (3600000 * 7) - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }

            if (!oneSendFlag && (!Objects.equals(skuId, "202010120001"))) {
                String redisRid = (String) redisTemplate.opsForHash().get(skuId, skuId);

                if (!redisRid.equals(rid)) {
                    log.info("京东商品skuId的已经存在------>{}", skuId);
                    return "HAD_SEND";
                }
            }


            if (1 == num || picFlag) {

                picLink = Utils.getSKUInfo(skuId, antappkey);
                if (!StringUtils.isEmpty(picLink)) {
                    picFlag = false;
                    //凌晨0、1、2、3、4、5，6点 picLink = Utils.getSKUInfo(skuId, antappkey);
                    if (Integer.parseInt(DateTime.now().toString("HH")) < 7 && Integer.parseInt(DateTime.now().toString("HH")) >= 0) {
                        //是否发送自助查券标志
                        String zzcq_flag = (String) redisTemplate.opsForValue().get("zzcq" + DateTime.now().toString("yyyy-MM-dd"));
                        if (!StringUtils.isEmpty(zzcq_flag)) {
                            log.info("京东自助查券已发送,0-6点不再发送消息============>");
                            return "HAD_SEND";
                        }
                    }
                    //记录每一次发送消息的时间
                    redisTemplate.opsForValue().set("send_last_msg_time", DateTime.now().toString("HH"));
                } else {
                    log.info("skuId,或者获取图片地址失败,skuId=======>{}", skuId);
                }
            }
        }

        if (!StringUtils.isEmpty(picLink)) {
            return picLink;
        }


        String compare_str = str;
        String pattern = "https://u.jd.com/[0-9A-Za-z]{7}";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(compare_str);

        while (m.find()) {
            compare_str = compare_str.replace(m.group(), "");
        }

        sku_str_flag = redisTemplate.opsForHash().putIfAbsent(compare_str, compare_str, rid);
        redisTemplate.expire(compare_str, DateTime.now().plusDays(1).toLocalDate().toDate().getTime() + (3600000 * 7) - System.currentTimeMillis(), TimeUnit.MILLISECONDS);


        if (!sku_str_flag && (!compare_str.contains("虹包")) && (!compare_str.contains("红包"))) {
            String msgRid = (String) redisTemplate.opsForHash().get(compare_str, compare_str);

            if (!msgRid.equals(rid)) {
                log.info("京东商品已经存在------>{}", compare_str);
                return "HAD_SEND";
            }
        }

        return "";
    }


    /**
     * 获取第一个不为空的skuId
     *
     * @param list
     * @return skuId null "" "HAD_SEND"
     */
    public static String getFirstSkuId(List<String> list, RedisTemplate<String, Object> redisTemplate,String sendMsgRobotId) {


        String skuId;
        for (String url : list) {
            boolean oneSendFlag;

            skuId = Utils.getSkuIdByUrl2(url);

            if (StringUtils.isEmpty(skuId)) {
                log.info("第一次获取skuId失败========>{}", url);
                skuId = Utils.getSkuIdByUrl(url);
            }

            if (!StringUtils.isEmpty(skuId)) {

                oneSendFlag = redisTemplate.opsForHash().putIfAbsent(skuId, skuId, new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
                    //排除京东生活40群
                if (!oneSendFlag &&!Objects.equals(sendMsgRobotId,"wxid_0p28wr3n0uh822")) {

                    log.info("skuId的已经存在------>{}", skuId);
                    return "HAD_SEND";
                } else {
                    redisTemplate.expire(skuId, 4, TimeUnit.HOURS);
                    log.info("skuId=====>{}", skuId);
                    return skuId;
                }
            }
        }

        return "";
    }


    /**
     * 没有skuId的url是否重复
     *
     * @param list
     * @param content
     * @param redisTemplate
     * @return
     */
    public static boolean hadSendStr(List<String> list, String content, RedisTemplate<String, Object> redisTemplate, String name) {
        String str = content;
        for (String it : list) {
            str = str.replace(it, "");
        }

        boolean sku_str_flag = redisTemplate.opsForHash().putIfAbsent(str + name, str, new DateTime().toString("yyyy-MM-dd HH:mm:ss"));

        if (!sku_str_flag) {

            log.info("已经存在------>{}", str);
            return true;
        }

        redisTemplate.expire(str + name, 4, TimeUnit.HOURS);
        return false;
    }


//    //redis为空补救
//    public static void main(String[] args) {
//        Jedis jedis = new Jedis("39.98.77.98");
//        System.out.println("连接本地的 Redis 服务成功！");
//        // 查看服务是否运行
//        System.out.println("服务 正在运行: " + jedis.ping());
//
//        jedis.set("msg_group","22822365300@chatroom,21874856168@chatroom,19933485573@chatroom,17490589131@chatroom,18949318188@chatroom,5013506060@chatroom,23765777130@chatroom,23336882997@chatroom,23216907002@chatroom,18172911411@chatroom");
//        jedis.set("account","[{\"antappkey\":\"872ea5798e8746d0\",\"groupId\":\"17490589131@chatroom\",\"jdtgwid\":\"1987045755\",\"msgToPersons\":[\"wxid_2r8n0q5v38h222\",\"du-yannan\",\"wxid_pdigq6tu27ag21\"],\"name\":\"ddy\"},{\"antappkey\":\"5862cd52a87a1914\",\"groupId\":\"18949318188@chatroom\",\"jdtgwid\":\"3002800583\",\"msgToPersons\":[],\"name\":\"zzf\"}]");
//        jedis.lpush("tbmd","wxid_qj37xlvrt9t422:A02【小文】线报冕単分享??","wxid_zlhgrhsx42sb22:淘礼金免单②群","wxid_j2h1kopuoqlc12:阿涛福利社-04A15","wxid_qj37xlvrt9t422:【小K】捡漏??B16群");
//
//    }
}