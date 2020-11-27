package com.common.util.jd;

import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;


@Slf4j
public class FileSplitUtil {

    /**
     * 图片拼接
     *
     * @param files 要拼接的文件列表
     * @return
     */
    public static BufferedImage merge(String[] files) {

        int len = files.length;
        if (len < 1) {
            log.info("图片数量小于1");
            return null;
        }

        File[] src = new File[len];
        BufferedImage[] images = new BufferedImage[len];
        int[][] ImageArrays = new int[len][];
        for (int i = 0; i < len; i++) {
            try {
                src[i] = new File(files[i]);
                images[i] = ImageIO.read(src[i]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            int width = images[i].getWidth();
            int height = images[i].getHeight();
            ImageArrays[i] = new int[width * height];// 从图片中读取RGB
            ImageArrays[i] = images[i].getRGB(0, 0, width, height, ImageArrays[i], 0, width);
        }

        int newHeight = 0;
        int newWidth = 0;

        int newHeightMax = 0;
        int newWidthMax = 0;


        for (int i = 0; i < images.length; i++) {
            newHeightMax = newHeight > images[i].getHeight() ? newHeight : images[i].getHeight();
            newWidthMax = newWidth > images[i].getWidth() ? newWidth : images[i].getWidth();
        }

        newHeight = ((images.length / 2) + images.length % 2) * newHeightMax;
        newWidth = images.length / 2 < 1 ? newWidthMax : newWidthMax * 2;


        //生成新图片
        try {
            BufferedImage ImageNew = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_USHORT_555_RGB);


            Graphics2D g = ImageNew.createGraphics();

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, newWidth, newHeight);//填充整个屏幕
            g.setColor(Color.BLACK);
            BufferedImage qrCode = resizeImage(newWidth,newWidth,ImageIO.read(new File("/Users/mac/Desktop/aa/w4.png")));
            g.drawImage(qrCode, 0, 0, qrCode.getWidth(), qrCode.getHeight(), null);


            for (int i = 0; i < images.length; i++) {
                ImageNew.setRGB(images.length % 2 != 0 && i == images.length - 1&&images.length!=1 ?    (i % 2) * newWidthMax + newWidthMax / 2 : (i % 2) * newWidthMax, (i / 2) * newHeightMax, newWidthMax, newHeightMax, ImageArrays[i], 0, images[i].getWidth());
            }
            return ImageNew;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    static BASE64Encoder encoder = new sun.misc.BASE64Encoder();
    static BASE64Decoder decoder = new sun.misc.BASE64Decoder();


    static String getImageBinary(BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpg", baos);
            byte[] bytes = baos.toByteArray();

            return encoder.encodeBuffer(bytes).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void aabase64StringToImage(String base64String, String rid) {
        try {
            byte[] bytes1 = decoder.decodeBuffer(base64String);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes1);
            BufferedImage bi1 = ImageIO.read(bais);
            File f1 = new File("/Users/mac/Desktop/" + rid + ".jpeg");
            ImageIO.write(bi1, "jpeg", f1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //链接url下载图片
    public static void downloadPicture(String urlList, String picName) {
        URL url = null;
        int imageNumber = 0;

        try {
            url = new URL(urlList);
            DataInputStream dataInputStream = new DataInputStream(url.openStream());

            String imageName = "/Users/mac/" + picName + ".jpeg";

            FileOutputStream fileOutputStream = new FileOutputStream(new File(imageName));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;

            while ((length = dataInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            byte[] context = output.toByteArray();
            fileOutputStream.write(output.toByteArray());
            dataInputStream.close();
            fileOutputStream.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static BufferedImage resizeImage(int x, int y, BufferedImage bfi){
        BufferedImage bufferedImage = new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);
        bufferedImage.getGraphics().drawImage(
                bfi.getScaledInstance(x, y, BufferedImage.TYPE_INT_RGB), 0, 0, null);
        return bufferedImage;
    }

//    public static void main(String[] args) {
//        String[] str = {"/Users/mac/Desktop/aa/w1.jpeg","/Users/mac/Desktop/aa/w2.jpeg","/Users/mac/Desktop/aa/w3.jpeg","/Users/mac/Desktop/aa/w1.jpeg","/Users/mac/Desktop/aa/w2.jpeg"};
////        String[] str = {"https://img14.360buyimg.com/pop/jfs/t1/126458/36/6613/120867/5f0568c6Ebe3b9d39/e6043acef5550b2a.jpg", "https://img14.360buyimg.com/pop/jfs/t1/126458/36/6613/120867/5f0568c6Ebe3b9d39/e6043acef5550b2a.jpg"};
//        BufferedImage merge = merge(str);
//
//        aabase64StringToImage(getImageBinary(merge), "a2222");
//        System.out.println("==end==");
//
//    }

//        public static void main(String[] args) {
//        String url = "https://img14.360buyimg.com/pop/jfs/t1/126458/36/6613/120867/5f0568c6Ebe3b9d39/e6043acef5550b2a.jpg";
//        downloadPicture(url,"11");
//    }
}