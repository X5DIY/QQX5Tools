package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.SettingsAndUtils;
import com.menglei.qqx5tools.SettingsAndUtils.FileType;
import com.menglei.qqx5tools.SettingsAndUtils.OutputMode;
import com.menglei.qqx5tools.controller.CalculateBurstPoints2Controller;
import javafx.application.Platform;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.menglei.qqx5tools.SettingsAndUtils.THREAD_NUM;
import static com.menglei.qqx5tools.SettingsAndUtils.logInfo;
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
        long time = System.nanoTime();
        ExecutorService pool = new ThreadPoolExecutor(
                THREAD_NUM, THREAD_NUM,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(THREAD_NUM),
                new SettingsAndUtils.MyThreadFactory());
        CalculateBurstPointsThread.processedNum = 0;

        // 可能不需要了。
        for (int i = 0; i < 4; i++) {
            CalculateBurstPointsThread.fileLock[i] = new ReentrantReadWriteLock();
        }

        for (int i = 0; i < THREAD_NUM; i++) {
            pool.execute(new CalculateBurstPointsThread(i));
        }
        pool.shutdown();
        try {
            while (!pool.isTerminated()) {
                //logInfo("已处理 " + df.format((double) CalculateBurstPointsThread.processedNum / xmlMap.size()));
                sleep(300);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        logInfo("处理完毕，共用时 " + nanoTime(time));
    }

    public class CalculateBurstPointsThread extends Thread {
        static ReentrantReadWriteLock[] fileLock = new ReentrantReadWriteLock[4];

        private final int threadNo;
        static int processedNum;
        private final int fileNum;

        CalculateBurstPointsThread(int threadNo) {
            this.threadNo = threadNo;
            fileNum = xmlMap.size();
        }

        @Override
        public void run() {
            int index = 0;
            for (Map.Entry<File, FileType> entry : xmlMap.entrySet()) {
                if (index % THREAD_NUM == threadNo) {
                    process(entry.getKey(), entry.getValue());
                }
                index++;
            }
        }

        private void process(File xml, FileType type) {
            XMLInfo a = new XMLInfo(type);

            try {
                new SetBasicInfo(xml, type).set(a);// 读取 xml 并录入有用的信息
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            new Calculate(a).calculate(a);// 计算爆点

            // 每个模式分别对应一个文件锁，提高写的效率
            int type = type > 1 ? (type - 2) : (type - 1);
            try {
                fileLock[type].writeLock().lock();
                new WriteFireInfo().write(a);// 输出信息
            } finally {
                fileLock[type].writeLock().unlock();
            }

            synchronized (this) {
                processedNum++;
            }
            double value = (double) processedNum / fileNum;
            Platform.runLater(() -> c.setProgress(value));

            switch (type) {
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
}