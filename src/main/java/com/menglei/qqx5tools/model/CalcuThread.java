package com.menglei.qqx5tools.model;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CalcuThread extends Thread {

    private int threadNo;
    private int threadNum;
    static ReentrantReadWriteLock[] fileLock = new ReentrantReadWriteLock[4];

    private ProgressBar progressBar;
    private Text text;
    private TextArea textArea;

    static int FinishedFileNum;
    private File[] files;
    private int fileNum;
    private int[] fileMode;

    CalcuThread(int threadNo, int threadNum,
                ProgressBar progressBar, Text text, TextArea textArea,
                File[] files, int fileNum, int[] fileMode) {
        this.threadNo = threadNo;
        this.threadNum = threadNum;
        this.progressBar = progressBar;
        this.text = text;
        this.textArea = textArea;
        this.files = files;
        this.fileNum = fileNum;
        this.fileMode = fileMode;
    }

    /**
     * 为了较好地利用读写锁，将文件进行等分
     * 应分两种情况，即文件过少（不足线程数目），或过多
     * 如果都按照线程数等分，会造成文件少时
     */
    public void run() {
        if (fileNum < threadNum && threadNo < fileNum) {
            process(files[threadNo], fileMode[threadNo]);
        } else {
            for (int i = fileNum / threadNum * threadNo; i < fileNum / threadNum * (threadNo + 1); i++) {
                process(files[i], fileMode[i]);
            }
        }
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
        } catch (SetInfoException e) {
            e.warnMess();
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
        Platform.runLater(() -> {
            progressBar.setProgress(value);
            text.setText(new DecimalFormat("0.00").format(value * 100) + "%");
        });

        switch (mode) {
            case 1:
            case 2:
                println("星动 " + a.title + " 处理完毕");
                break;
            case 3:
                println("弹珠 " + a.title + " 处理完毕");
                break;
            case 4:
                println("泡泡 " + a.title + " 处理完毕");
                break;
            case 5:
                println("弦月 " + a.title + " 处理完毕");
                break;
        }
    }

    /**
     * 在 TextArea 最后追加 x 的内容
     * JavaFX 单线程刷新 ui，要用 runLater() 将要做的刷新加入 JavaFX 专用的刷新线程
     * PS：用我们自己的线程更新 ui，是“非线程安全”的
     * 同时，由于用了 lambda 表达式，这一部分必须要 JDK 1.8 及以上才能运行
     *
     * @param x 任意对象，先转成 String，再输出到 TextArea
     */
    private void print(Object x) {
        Platform.runLater(() -> textArea.appendText("" + x));
    }

    /**
     * 在 TextArea 最后新增一行，在新行中显示 x 的内容
     * System.out.println() 是先输出内容再新增一行，注意区别
     * JavaFX 单线程刷新 ui，要用 runLater() 将要做的刷新加入 JavaFX 专用的刷新线程
     * PS：用我们自己的线程更新 ui，是“非线程安全”的
     * 同时，由于用了 lambda 表达式，这一部分必须要 JDK 1.8 及以上才能运行
     *
     * @param x 任意对象，先转成 String，再输出到 TextArea
     */
    private void println(Object x) {
        Platform.runLater(() -> textArea.appendText("\n" + x));
    }

}
