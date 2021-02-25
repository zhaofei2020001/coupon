package com.common.util.jd;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

public class ReadFile {

    /**
     * 删除某个文件夹下所有文件（也删除子文件夹中的文件）
     *
     * @param delpath
     *            String
     * @throws FileNotFoundException
     * @throws IOException
     * @return boolean
     */
    public static boolean deletefile(String delpath)  {
        try {

            File file = new File(delpath);
            // 当且仅当此抽象路径名表示的文件存在且 是一个目录时，返回 true
            if (!file.isDirectory()) {
                file.delete();
            } else if (file.isDirectory()) {
                String[] filelist = file.list();
                for (int i = 0; i < filelist.length; i++) {
                    File delfile = new File(delpath + "\\" + filelist[i]);
                    if (!delfile.isDirectory()) {
                        delfile.delete();
                    } else if (delfile.isDirectory()) {
                        deletefile(delpath + "\\" + filelist[i]);
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
