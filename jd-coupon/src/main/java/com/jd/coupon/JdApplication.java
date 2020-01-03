package com.jd.coupon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

/**
 * @author zf
 * since 2019/12/27
 */
@Slf4j
@SpringCloudApplication
public class JdApplication {

  public static void main(String[] args) {
    SpringApplication.run(JdApplication.class, args);
    log.info("---------------com.jd.coupon.JdApplication started successfully--------------->");
  }
}
