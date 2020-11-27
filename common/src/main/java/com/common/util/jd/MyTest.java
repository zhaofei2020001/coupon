package com.common.util.jd;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class MyTest {

    public static void main(String[] args) {
        //背景图片地址
        String backgroundPath = "/Users/mac/Desktop/aa/w1.jpeg";
        //添加的图片地址
        String qrCodePath = "/Users/mac/Desktop/aa/w2.jpeg";
        String bottomPath = "/Users/mac/Desktop/aa/w3.jpeg";

        String merchandDetail ="扫描下方二家萨达撒多萨USB达萨达as必备，省钱购物专属小专属小秘啥事大萨达萨达萨达";
        String price = "原价:¥1000";
        String priceHeng = "——————";
        String currText = "当前价:";
        String currRMB = "¥";
        String currPrice = "153";
        //输出目录
        String outPutPath="/Users/mac/Desktop/aa/test.jpeg";
        overlapImage(backgroundPath,qrCodePath,bottomPath,merchandDetail,price,priceHeng,currText,currRMB,currPrice,outPutPath);
    }

    public static String overlapImage(String backgroundPath,String qrCodePath,String bottomPath,
                                      String merchandDetail,String price,String priceHeng,String currText,String currRMB,String currPrice,String outPutPath){
        try {
            //设置图片大小
            BufferedImage background = resizeImage(750,1334, ImageIO.read(new File(backgroundPath)));
            BufferedImage qrCode = resizeImage(591,533,ImageIO.read(new File(qrCodePath)));
            BufferedImage bottom = resizeImage(202,202,ImageIO.read(new File(bottomPath)));

            Graphics2D g = background.createGraphics();

            g.setColor(Color.black);
            g.setFont(new Font("宋体",Font.PLAIN,33));
            //商品描述自动换行------------------------------
            if(merchandDetail.length()<=18){
                g.drawString(merchandDetail, 83, 760);
            }else{
                String merchandOne =  merchandDetail.substring(0,18);
                String merchandTwo =  merchandDetail.substring(18);
                g.drawString(merchandOne, 83, 760);
                g.drawString(merchandTwo, 83, 800);
            }
            g.drawString(currText,83 ,910);

            g.setColor(Color.gray);
            g.drawString(price,83 ,850);
            g.drawString(priceHeng,83 ,850);

            g.setColor(Color.red);
            g.drawString(currRMB,200 ,910);

            g.setFont(new Font("宋体",Font.PLAIN,50));
            g.drawString(currPrice,215 ,910);
            //在背景图片上添加图片
            g.drawImage(qrCode, 83, 158, qrCode.getWidth(), qrCode.getHeight(), null);
            g.drawImage(bottom, 83, 1011, bottom.getWidth(), bottom.getHeight(), null);
            g.dispose();
            ImageIO.write(background, "jpg", new File(outPutPath));
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static BufferedImage resizeImage(int x, int y, BufferedImage bfi){
        BufferedImage bufferedImage = new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);
        bufferedImage.getGraphics().drawImage(
                bfi.getScaledInstance(x, y, Image.SCALE_SMOOTH), 0, 0, null);
        return bufferedImage;
    }


}
