package com.common.util.jd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
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
     * @param map 数据源
     * @return 返回的值
     */
    public static String getFirstNotNull(LinkedHashMap<String, String> map, RedisTemplate<String, Object> redisTemplate, String str, String name, String antappkey) {

        String result = null;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String skuUrl = entry.getKey();
            String skuId = Utils.getSkuIdByUrl(skuUrl);
            //消息字符串
            String replace = str.replace(entry.getKey(), "");
            Boolean skuIdFlag;
            Boolean jd_skui_send;
            if (replace.length() > 11) {
                    jd_skui_send = redisTemplate.opsForHash().putIfAbsent(replace + name, replace + name, "1");
                    redisTemplate.expire(replace+name, 8, TimeUnit.HOURS);
            } else {
                jd_skui_send = true;
            }


            if (StringUtils.isEmpty(skuId)) {
                skuIdFlag = true;
            } else {
                skuIdFlag = redisTemplate.opsForHash().putIfAbsent(skuId + name, skuId + name, "1");
                redisTemplate.expire(skuId+name, 8, TimeUnit.HOURS);
            }

            if (jd_skui_send && skuIdFlag) {

            } else {
                log.info("京东商品的已经存在------>{},skuId-->{},jd_skui_send--->{},skuIdFlag--->{}", replace+name, skuId, jd_skui_send, skuIdFlag);
                return "HAD_SEND";
            }
            result = Utils.getSKUInfo(skuId, antappkey);
            if (!StringUtils.isEmpty(result)) {
                return result;
            } else {
                if (replace.length() > 11) {
                    redisTemplate.delete(replace+name);
                }
            }

        }
        return result;
    }
}
