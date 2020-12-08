package com.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;

/**
 *https://www.yuque.com/simonalong/jishu/gepe8v
 */
@Slf4j
@UtilityClass
public class UrlToPicTransfer {

    public String PHANTOM_JS_BIN_PATH="/Users/mac/Desktop/phantomjs-2.1.1-windows/bin/phantomjs.exe";

    public static String trans(String url, String picFileNamePre) throws IOException {
        try {
            //设置必要参数
            DesiredCapabilities capabilities = new DesiredCapabilities();
            //ssl证书支持
            capabilities.setCapability("acceptSslCerts", true);
            //截屏支持
            capabilities.setCapability("takesScreenshot", true);
            //css搜索支持
            capabilities.setCapability("cssSelectorsEnabled", true);
            //js支持
            capabilities.setJavascriptEnabled(true);
            //驱动支持（第二参数表明的是你的phantomjs引擎所在的路径）
            capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, PHANTOM_JS_BIN_PATH);

            //创建无界面浏览器对象
            PhantomJSDriver driver = new PhantomJSDriver(capabilities);
            //设置隐性等待（作用于全局）
            // driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

            //打开页面
            driver.get(url);
            Thread.sleep(1000);

            File srcFile = driver.getScreenshotAs(OutputType.FILE);

            String picFileName = picFileNamePre + ".png";
            FileCopyUtils.copy(srcFile, new File(picFileName));
            return picFileName;
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            return picFileNamePre;
        } catch (Throwable e) {
            log.error("转换异常", e);
            throw e;
        }
    }

//    public static void main(String[] args) throws IOException {
//        trans("https://u.jd.com/tlyfd1s","C:\\Users\\Mac\\test");
//    }
}