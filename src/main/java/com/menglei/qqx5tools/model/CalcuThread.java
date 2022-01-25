package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.SettingsAndUtils;
import com.menglei.qqx5tools.controller.CalculateBurstPoints2Controller;
import javafx.application.Platform;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.menglei.qqx5tools.SettingsAndUtils.THREAD_NUM;

public class CalcuThread extends Thread {

    private final int threadNo;
    static ReentrantReadWriteLock[] fileLock = new ReentrantReadWriteLock[4];

    private final CalculateBurstPoints2Controller c;

    static int FinishedFileNum;
    private final ConcurrentHashMap<File, SettingsAndUtils.FileType> xmlMap;
    private final int fileNum;
    private final int threadNum;

    CalcuThread(int threadNo, CalculateBurstPoints2Controller c,
                ConcurrentHashMap<File, SettingsAndUtils.FileType> xmlMap) {
        this.c = c;
        this.threadNo = threadNo;
        this.xmlMap = xmlMap;
        fileNum = xmlMap.size();
        threadNum = Math.min(THREAD_NUM, fileNum);
    }

    @Override
    public void run() {
        //遍历。

//        if (fileNum < THREAD_NUM && threadNo < fileNum) {
//            process(xmlMap.[threadNo], fileMode[threadNo]);
//        } else {
//            for (int i = fileNum / THREAD_NUM * threadNo; i < fileNum / THREAD_NUM * (threadNo + 1); i++) {
//                process(files[i], fileMode[i]);
//            }
//        }
    }

    private void process(File xml, int mode) {
        XMLInfo a;
        switch (mode) {
            case 1:
            case 2:
                a = new XMLInfo(1);
                break;
            case 3:
                a = new XMLInfo(2);
                break;
            case 4:
                a = new XMLInfo(3);
                break;
            case 5:
                a = new XMLInfo(4);
                break;
            default:
                System.out.println("Mode in XML is not correct.");
                return;
        }

        try {
            new SetBasicInfo(xml, mode).set(a);// 读取 xml 并录入有用的信息
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        new Calculate(a).calculate(a);// 计算爆点

        // 每个模式分别对应一个文件锁，提高写的效率
        int type = mode > 1 ? (mode - 2) : (mode - 1);
        try {
            fileLock[type].writeLock().lock();
            new WriteFireInfo().write(a);// 输出信息
        } finally {
            fileLock[type].writeLock().unlock();
        }

        synchronized (this) {
            FinishedFileNum++;
        }
        double value = (double) FinishedFileNum / fileNum;
        Platform.runLater(() -> c.setProgress(value));

        switch (mode) {
            case 1:
            case 2:
                c.appendLine("星动 " + a.title + " 处理完毕");
                break;
            case 3:
                c.appendLine("弹珠 " + a.title + " 处理完毕");
                break;
            case 4:
                c.appendLine("泡泡 " + a.title + " 处理完毕");
                break;
            case 5:
                c.appendLine("弦月 " + a.title + " 处理完毕");
                break;
        }
    }
}
