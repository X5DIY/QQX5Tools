package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.SettingsAndUtils.FileType;
import com.menglei.qqx5tools.SettingsAndUtils.OutputMode;
import com.menglei.qqx5tools.controller.CalculateBurstPoints2Controller;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.menglei.qqx5tools.SettingsAndUtils.THREAD_NUM;
import static com.menglei.qqx5tools.SettingsAndUtils.nanoTime;

public class CalculateBurstPoints extends Thread {
    private final CalculateBurstPoints2Controller c;

    private final ConcurrentHashMap<File, FileType> xmlMap;
    private final OutputMode outputMode;
    private final int maxDiff;

    public CalculateBurstPoints(CalculateBurstPoints2Controller c, ConcurrentHashMap<File, FileType> xmlMap,
                                OutputMode outputMode, int maxBurstPointsNum, int maxScoreDifference) {
        this.c = c;
        this.xmlMap = xmlMap;
        this.outputMode = outputMode;
        XMLInfo.FireMaxNum = maxBurstPointsNum;
        this.maxDiff = maxScoreDifference;
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();
        new WriteFireInfo(maxDiff, outputMode).writeFirst();// 写入所有文件第一行

        CalcuThread[] calcuThreads = new CalcuThread[THREAD_NUM];
        CalcuThread.FinishedFileNum = 0;
        for (int i = 0; i < 4; i++) {
            CalcuThread.fileLock[i] = new ReentrantReadWriteLock();
        }
        for (int i = 0; i < THREAD_NUM; i++) {
            calcuThreads[i] = new CalcuThread(i, c, xmlMap);
            calcuThreads[i].start();
        }
        try {// 等待线程执行完毕
            for (int i = 0; i < THREAD_NUM; i++) {
                calcuThreads[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        c.appendLine("全部文件处理完毕，共用时" + nanoTime(startTime));
        c.finished();
    }
}