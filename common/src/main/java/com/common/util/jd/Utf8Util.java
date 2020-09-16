package com.common.util.jd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zf
 * since 2019/12/31
 */
public class Utf8Util {
  public static Map<String, Integer> hexMap = new HashMap<String, Integer>();
  public static Map<String, Integer> byteMap = new HashMap<String, Integer>();

  static {
    hexMap.put("0", 2);
    hexMap.put("1", 2);
    hexMap.put("2", 2);
    hexMap.put("3", 2);
    hexMap.put("4", 2);
    hexMap.put("5", 2);
    hexMap.put("6", 2);
    hexMap.put("7", 2);
    hexMap.put("c", 4);
    hexMap.put("d", 4);
    hexMap.put("e", 6);
    hexMap.put("f", 8);

    byteMap.put("0", 1);
    byteMap.put("1", 1);
    byteMap.put("2", 1);
    byteMap.put("3", 1);
    byteMap.put("4", 1);
    byteMap.put("5", 1);
    byteMap.put("6", 1);
    byteMap.put("7", 1);
    byteMap.put("c", 2);
    byteMap.put("d", 2);
    byteMap.put("e", 3);
    byteMap.put("f", 4);
  }

  /**
   * 是否包含4字节UTF-8编码的字符
   * @param s 字符串
   * @return 是否包含4字节UTF-8编码的字符
   */
  public static boolean contains4BytesChar(String s) {
    if (s == null || s.trim().length() == 0) {
      return false;
    }

    byte[] bytes = s.getBytes();

    if (bytes == null || bytes.length == 0) {
      return false;
    }

    int index = 0;
    byte b;
    String hex = null;
    String firstChar = null;
    int step;
    while (index <= bytes.length - 1) {
      b = bytes[index];

      hex = byteToHex(b);
      if (hex == null || hex.length() < 2) {
        return false;
      }

      firstChar = hex.substring(0, 1);

      if (firstChar.equals("f")) {
        return true;
      }

      if (byteMap.get(firstChar) == null) {
        return false;
      }

      step = byteMap.get(firstChar);
      index = index + step;
    }

    return false;
  }

  /**
   * 去除4字节UTF-8编码的字符
   * @param s 字符串
   * @return 已去除4字节UTF-8编码的字符
   */
  public static String remove4BytesUTF8Char(String s) {
    byte[] bytes = s.getBytes();
    byte[] removedBytes = new byte[bytes.length];
    int index = 0;

    String hex = null;
    String firstChar = null;
    for (int i = 0; i < bytes.length; ) {
      hex = Utf8Util.byteToHex(bytes[i]);

      if (hex == null || hex.length() < 2) {
        return null;
      }

      firstChar = hex.substring(0, 1);

      if (byteMap.get(firstChar) == null) {
        return null;
      }

      if (firstChar.equals("f")) {
        for (int j = 0; j < byteMap.get(firstChar); j++) {
          i++;
        }
        continue;
      }

      for (int j = 0; j < byteMap.get(firstChar); j++) {
        removedBytes[index++] = bytes[i++];
      }
    }

    return new String(Arrays.copyOfRange(removedBytes, 0, index));
  }

  /**
   * 字节转十六进制
   * @param b 字节
   * @return 十六进制
   */
  public static String byteToHex(byte b) {
    int r = b & 0xFF;
    String hexResult = Integer.toHexString(r);

    StringBuilder sb = new StringBuilder();
    if (hexResult.length() < 2) {
      sb.append(0); // 前补0
    }
    sb.append(hexResult);
    return sb.toString();
  }

  public static void main(String[] args) {
    String str = "纯牛奶好价！！！\n" +
            "\n" +
            "先领取满229减40优惠券：https://w.url.cn/s/AUZ8yc2\n" +
            "\n" +
            "【京东自营】蒙牛 特仑苏 纯牛奶 250ml*16 礼盒装\n" +
            "\n" +
            "地址：https://u.jd.com/abYiVe\n" +
            "—\n" +
            "下3件，满2件7.5折，实付139.8元包邮！！！";
    String s = remove4BytesUTF8Char(str);
    System.out.println(s);
  }
}
