package com.jd.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

/**
 * @author zf
 * since 2019/12/27
 */
@SpringCloudApplication
public class JdApplication {

  public static void main(String[] args) {
    SpringApplication.run(JdApplication.class, args);
    System.out.println("com.jd.coupon.JdApplication started successfully---------->");
  }
}
