package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.controller.AdjustBpm2Controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static com.menglei.qqx5tools.SettingsAndUtils.getInfo;
import static com.menglei.qqx5tools.SettingsAndUtils.nanoTime;

public class AdjustBpm {

    private static String rootFilePath;

    public AdjustBpm(AdjustBpm2Controller c, ArrayList<File> fileList, double beishu) {
        AdjustBpm.rootFilePath = rootFilePath;
    }

    public void process() {
        long startTime = System.nanoTime();
        traversalFile(new File(rootFilePath));
        long endTime = System.nanoTime();
        System.out.println("全部文件处理完毕，共用时" + nanoTime(startTime, endTime));
    }

    /**
     * 遍历所有文件，如果是炫舞谱面文件，计算爆点
     * 确定谱面模式后，调用calculate函数，进行文件处理
     *
     * @param file 文件或文件夹
     */
    private static void traversalFile(File file) {
        if (!file.isDirectory()) {// 如果是文件
            if (file.getName().endsWith(".xml")) {// 如果后缀是xml
                process(file);
            }
        } else {// 如果是文件夹
            File[] listFiles = file.listFiles();// 为里面每个文件、目录创建对象
            if (listFiles == null) {// 如果文件夹为空，直接结束
                return;
            }
            for (File f : listFiles) {// 遍历每个文件和目录
                traversalFile(f);
            }
        }
    }

    private static void process(File fileIn) {
        try {
            File fileOut = new File(fileIn.getCanonicalPath().substring(0, fileIn.getCanonicalPath().length() - 4) + "_new.xml");
            BufferedReader br = new BufferedReader(new FileReader(fileIn));
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileOut));
            String s;
            while ((s = br.readLine()) != null) {// xml文件的任意一行已经放入s
                if (s.contains("PinballSingle")) {
                    String q = getInfo(s, "<", 1, "\"", 5, false, true);
                    int bar = Integer.parseInt(getInfo(s, "Bar=\"", "\" Pos=\""));
                    int pos = Integer.parseInt(getInfo(s, "\" Pos=\"", "\" Son"));
                    String h = getInfo(s, "\"", 8, ">", 1, false, true);
                } else if (s.contains("PinballSingle")) {
                    continue;
                } else if (s.contains("PinballSingle")) {
                    continue;
                } else if (s.contains("PinballSingle")) {
                    continue;
                } else {
                    bw.write(s);
                }
            }
            br.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {

    }
}
