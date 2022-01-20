package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.controller.BytesToXml2Controller;

import java.io.File;
import java.util.ArrayList;

import static com.menglei.qqx5tools.SettingsAndUtils.THREAD_NUM;
import static com.menglei.qqx5tools.SettingsAndUtils.nanoTime;

public class BytesToXml {
    private final File rootPath;
    private final ArrayList<File> bytesFileList;

    public BytesToXml(BytesToXml2Controller c, File rootPath) {
        this.rootPath = rootPath;
        bytesFileList = new ArrayList<>();
    }

    public void process() {
        long startTime = System.nanoTime();
        System.out.println("正在查找bytes文件...");
        findBytesFiles(rootPath);
        if (bytesFileList.isEmpty()) {
            System.out.println("未找到bytes文件！");
            return;
        }
        System.out.println("查找完毕，共找到 " + bytesFileList.size() + " 个bytes文件，开始处理！");
        BytesThread[] bytesThreads = new BytesThread[THREAD_NUM];
        for (int i = 0; i < THREAD_NUM; i++) {
            bytesThreads[i] = new BytesThread(i, bytesFileList);
            bytesThreads[i].start();
        }
        try {
            for (int i = 0; i < THREAD_NUM; i++) {
                bytesThreads[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        System.out.println("bytes文件已转换完毕，共用时" + nanoTime(startTime));
    }

    private void findBytesFiles(File file) {
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles == null) {
                return;
            }
            for (File f : listFiles) {
                findBytesFiles(f);
            }
        } else {
            if (file.getName().endsWith(".xml.bytes")) {
                bytesFileList.add(file);
            }
        }
    }
}