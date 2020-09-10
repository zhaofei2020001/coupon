package com.common.util.jd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * @author zf
 * since 2020/2/12
 */
@Slf4j
public class MapUtil {
    /**
     * 获取map中第一个非空数据值
     *
     * @param <>      Key的类型
     * @param <>      Value的类型
     * @param mapCopy 数据源
     * @return 返回的值
     */
    public static String getFirstNotNull(Map<String, String> mapCopy, RedisTemplate<String, Object> redisTemplate, String str, String name, String antappkey) {
        //按键有序输出
        TreeMap<String, String> map = new TreeMap<>();

        for (Map.Entry<String, String> entry : mapCopy.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }

        String result = null;
        int flag = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            flag++;
            String skuUrl = entry.getKey();
            String skuId = Utils.getSkuIdByUrl(skuUrl);
            //消息字符串
            String replace = str.replace(entry.getKey(), "");
            Boolean skuIdFlag;
            Boolean jd_skui_send;
            if (replace.length() > 11) {
                if (replace.substring(0, 10).contains("[@emoji=")) {
                    jd_skui_send = redisTemplate.opsForHash().putIfAbsent(replace + name, replace + name, "1");
                    redisTemplate.expire(replace, 20, TimeUnit.MINUTES);
                } else {
                    jd_skui_send = redisTemplate.opsForHash().putIfAbsent(replace.substring(0, 10) + name, replace.substring(0, 10) + name, "1");
                    redisTemplate.expire(replace.substring(0, 10), 8, TimeUnit.HOURS);
                }
            } else {
                jd_skui_send = true;
            }


            if (StringUtils.isEmpty(skuId)) {
                skuIdFlag = true;
            } else {
                skuIdFlag = redisTemplate.opsForHash().putIfAbsent(skuId + name, skuId + name, "1");
                redisTemplate.expire(skuId, 8, TimeUnit.HOURS);
            }

            if (jd_skui_send && skuIdFlag) {

            } else {
                log.info("京东商品的已经存在------>{},skuId-->{},jd_skui_send--->{},skuIdFlag--->{}", replace.substring(0, 10), skuId, jd_skui_send, skuIdFlag);
                return "HAD_SEND";
            }
            result = Utils.getSKUInfo(skuId, antappkey);
            if (!StringUtils.isEmpty(result)) {
                return result;
            } else {
                if (replace.length() > 11) {
                    redisTemplate.delete(replace.substring(0, 10));
                }
            }

            if (1 == flag) {
                if (replace.length() > 11) {
                redisTemplate.opsForHash().putIfAbsent(replace.substring(0, 10), replace.substring(0, 10), "1");
                redisTemplate.expire(replace.substring(0, 10), 20, TimeUnit.MINUTES);
                }
            }
        }
        return result;
    }
}
