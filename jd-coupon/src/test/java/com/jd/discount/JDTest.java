package com.jd.discount;

import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.util.HttpUtils;
import com.common.util.jd.JdUtil;
import com.jd.coupon.JdApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author zf
 * since 2019/12/16
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {JdApplication.class})
public class JDTest {

  @Autowired
  RedisTemplate<String, Object> redisTemplate;

  /**
   * 将京东商品按不同类型放入缓存
   */
  @Test
  public void test() {
    JdUtil.test(redisTemplate);
  }


  /**
   * 从缓存中每种类型中取出一件商品并转为发消息的字符串
   */
  @Test
  public void getSmsStr() {
    //从缓存中每种类型中取出一件商品并转为发消息的字符串
    for (AllEnums.eliteEnum value : AllEnums.eliteEnum.values()) {
      System.out.println("*************************************************" + value.getDesc() + "************************************************");
      try {
        String str = JdUtil.getJFGoodsRespByType(value, redisTemplate);
        log.info("str------->{}",str );
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }




  @Test
  public void getToken(){
    String post = HttpUtils.post(Constants.requireAccessTokenUrl, null);
    System.out.println(post);
  }


}
