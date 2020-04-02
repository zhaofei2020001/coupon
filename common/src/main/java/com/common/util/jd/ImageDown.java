package com.common.util.jd;

import com.common.constant.Constants;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author zf
 * since 2020/4/2
 */
public class ImageDown {


  /**
   * 文件名,包含后缀（默认存放在/Users/mac/image/【文件名】）
   *
   * @param destUrl_copy
   */
  public static void saveToFile(String destUrl_copy) {
    String destUrl = Constants.LOVE_CAT_DOMAIN_NAME + "static/" + destUrl_copy;

    FileOutputStream fos = null;
    BufferedInputStream bis = null;
    HttpURLConnection httpUrl = null;
    URL url = null;
    int BUFFER_SIZE = 1024;
    byte[] buf = new byte[BUFFER_SIZE];
    int size = 0;
    try {
      url = new URL(destUrl);
      httpUrl = (HttpURLConnection) url.openConnection();
      httpUrl.connect();
      bis = new BufferedInputStream(httpUrl.getInputStream());
      fos = new FileOutputStream(Constants.PIC_SAVE_PATH + destUrl_copy);
      while ((size = bis.read(buf)) != -1) {
        fos.write(buf, 0, size);
      }
      fos.flush();
    } catch (IOException e) {
    } catch (ClassCastException e) {
    } finally {
      try {
        fos.close();
        bis.close();
        httpUrl.disconnect();
      } catch (IOException e) {
      } catch (NullPointerException e) {
      }
    }
  }
}
