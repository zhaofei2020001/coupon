package com.common.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;


@Slf4j
public class HttpUtils {

  /**
   * 发送HttpPost请求
   *
   * @param strURL 服务地址
   * @param params json字符串,例如: "{ \"id\":\"12345\" }" ;其中属性名必须带双引号<br/>
   * @return 成功:返回json字符串<br/>
   */
  public static String post(String strURL, String params) {
    log.info("\n************* url: {} \n params: {} \n", strURL, params);
    BufferedReader reader = null;
    try {
      URL url = new URL(strURL);// 创建连接
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.setUseCaches(false);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestMethod("POST"); // 设置请求方式
      // connection.setRequestProperty("Accept", "application/json"); // 设置接收数据的格式
      connection.setRequestProperty("Content-Type", "application/json"); // 设置发送数据的格式
      connection.connect();
      //一定要用BufferedReader 来接收响应， 使用字节来接收响应的方法是接收不到内容的
      OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8"); // utf-8编码
      out.append(params);
      out.flush();
      out.close();
      // 读取响应
      reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
      String line;
      String res = "";
      while ((line = reader.readLine()) != null) {
        res += line;
      }
      reader.close();
      return res;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return "error"; // 自定义错误信息
  }


  /**
   * 模拟发送url Get 请求
   * @param url
   * @return
   */
  public static String getRequest(String url) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    StringBuilder entityStringBuilder = null;
    try {
      HttpGet get = new HttpGet(url);
      CloseableHttpResponse httpResponse = null;
      httpResponse = httpClient.execute(get);
      try {
        HttpEntity entity = httpResponse.getEntity();
        entityStringBuilder = new StringBuilder();
        if (null != entity) {
          BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"), 8 * 1024);
          String line = null;
          while ((line = bufferedReader.readLine()) != null) {
            entityStringBuilder.append(line + "/n");
          }
        }
      } finally {
        httpResponse.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (httpClient != null) {
          httpClient.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return entityStringBuilder.toString();
  }

}