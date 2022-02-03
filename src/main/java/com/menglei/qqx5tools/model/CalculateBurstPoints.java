package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.SettingsAndUtils.MyThreadFactory;
import com.menglei.qqx5tools.SettingsAndUtils.OutputMode;
import com.menglei.qqx5tools.SettingsAndUtils.QQX5MapType;
import com.menglei.qqx5tools.bean.QQX5MapInfo;
import com.menglei.qqx5tools.controller.CalculateBurstPoints2Controller;
import javafx.application.Platform;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.menglei.qqx5tools.SettingsAndUtils.THREAD_NUM;
import static com.menglei.qqx5tools.SettingsAndUtils.logError;
import static com.menglei.qqx5tools.SettingsAndUtils.logInfo;
import static com.menglei.qqx5tools.SettingsAndUtils.nanoTime;

public class CalculateBurstPoints extends Thread {
    private final CalculateBurstPoints2Controller c;
    private final ConcurrentHashMap<File, QQX5MapType> xmlMap;
    private final OutputMode outputMode;
    private final int maxBurstPointsNum;
    private final int maxScoreDifference;

    public CalculateBurstPoints(CalculateBurstPoints2Controller c, ConcurrentHashMap<File, QQX5MapType> xmlMap,
                                OutputMode outputMode, int maxBurstPointsNum, int maxScoreDifference) {
        this.c = c;
        this.xmlMap = xmlMap;
        this.outputMode = outputMode;
        this.maxBurstPointsNum = maxBurstPointsNum;
        this.maxScoreDifference = maxScoreDifference;
    }

    @Override
    public void run() {
        long time = System.nanoTime();
        ExecutorService pool = new ThreadPoolExecutor(
                THREAD_NUM, THREAD_NUM,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(THREAD_NUM),
                new MyThreadFactory());
        CalculateBurstPointsThread.processedNum = 0;
        for (int i = 0; i < THREAD_NUM; i++) {
            pool.execute(new CalculateBurstPointsThread(i));
        }
        pool.shutdown();
        try {
            while (!pool.isTerminated()) {
                // 不在此处更新总体进度，而是每一次处理完文件后更新
                sleep(300);
            }
        } catch (InterruptedException e) {
            logError(e);
            Thread.currentThread().interrupt();
        }
        logInfo("处理完毕，共用时 " + nanoTime(time));
    }

    public class CalculateBurstPointsThread extends Thread {
        private final int threadNo;
        private static int processedNum;
        private final int fileNum;
        private static final WriteFireInfo writer = new WriteFireInfo();

        CalculateBurstPointsThread(int threadNo) {
            this.threadNo = threadNo;
            fileNum = xmlMap.size();
        }

        @Override
        public void run() {
            int index = 0;
            for (Map.Entry<File, QQX5MapType> entry : xmlMap.entrySet()) {
                if (index % THREAD_NUM == threadNo) {
                    File xml = entry.getKey();
                    QQX5MapType type = entry.getValue();
                    QQX5MapInfo mapInfo = new QQX5MapInfo(xml, type);
                    mapInfo.calculateAll(maxBurstPointsNum, maxScoreDifference);
                    mapInfo.writeAll(outputMode);
                    synchronized (CalculateBurstPointsThread.class) {
                        processedNum++;
                    }
                    Platform.runLater(() -> c.setProgress((double) processedNum / fileNum));
                    c.appendLine(type + " " + mapInfo.getTitle() + " 处理完毕");
                }
                index++;
            }
        }
    }
}