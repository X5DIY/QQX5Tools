package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.controller.CalculateBurstPoints2;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.menglei.qqx5tools.SettingsAndUtils.nanoTime;
import static com.menglei.qqx5tools.SettingsAndUtils.getInfo;

public class CalculateBurstPoints2Controller extends Thread {

    private ProgressBar progressBar;
    private Text text;
    private TextArea textArea;
    private Button finish;

    private File[] initialFiles;// 最初传进来的文件，不一定是炫舞谱面文件
    private File[] files;// 所有要处理的文件
    private int fileNum = 0;
    private int[] fileMode;
    private int threadNum = 24;// 线程数目
    private int outMode;
    private int maxDiff;

    public CalculateBurstPoints2Controller(CalculateBurstPoints2 calculateBurstPoints2, File[] initialFiles, int outMode, int fireMaxNum, int maxDiff) {
        this.progressBar = calculateBurstPoints2.progressBar;
        this.text = calculateBurstPoints2.text;
        this.textArea = calculateBurstPoints2.textArea;
        this.finish = calculateBurstPoints2.finish;
        files = new File[5000];// 所有要处理的文件，5000 是谱面文件最大数量
        fileMode = new int[5000];
        this.initialFiles = initialFiles;
        this.outMode = outMode;
        XMLInfo.FireMaxNum = fireMaxNum;
        this.maxDiff = maxDiff;
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();
        new WriteFireInfo(maxDiff, outMode, textArea).writeFirst();// 写入所有文件第一行
        println("正在查找谱面文件ing");
        for (File f : initialFiles) {
            traversalFile(f);// 记录所有符合的xml文件
        }
        println("查找完毕，共找到 " + fileNum + " 个谱面文件，开始处理！");

        CalcuThread[] calcuThreads = new CalcuThread[threadNum];
        CalcuThread.FinishedFileNum = 0;
        for (int i = 0; i < 4; i++) {
            CalcuThread.fileLock[i] = new ReentrantReadWriteLock();
        }
        for (int i = 0; i < threadNum; i++) {
            calcuThreads[i] = new CalcuThread(i, threadNum, progressBar, text, textArea, files, fileNum, fileMode);
            calcuThreads[i].start();
        }
        try {// 等待线程执行完毕
            for (int i = 0; i < threadNum; i++) {
                calcuThreads[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        println("全部文件处理完毕，共用时" + nanoTime(startTime));
        Platform.runLater(() -> {
            finish.setText("完成");
            finish.setDisable(false);
        });
    }

    /**
     * 遍历所有文件，如果是炫舞谱面文件，计算爆点
     * 确定谱面模式后，调用calculate函数，进行文件处理
     *
     * @param file 文件或文件夹
     */
    private void traversalFile(File file) {
        try {
            if (!file.isDirectory()) {// 如果是文件
                // 为了避免转化带来的问题，这里不进行bytes文件的处理
                // 如果需要转化，主界面运行转化操作即可
                if (file.getName().endsWith(".xml")) {// 如果后缀是xml
                    int mode = getMode(file);
                    if (mode != 0) {
                        files[fileNum] = file;
                        fileMode[fileNum] = mode;
                        fileNum++;
                    }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取文件内容，判断打开的文件是4k星动、5k星动、弹珠还是泡泡
     *
     * @param xml QQX5谱面文件
     * @return 1为4k星动，2为5k星动，3为弹珠，4为泡泡，5为弦月
     */
    private static int getMode(File xml) {
        int track = 0;
        boolean containsShort = false;
        try {
            BufferedReader br = new BufferedReader(new FileReader(xml));
            String s;
            while ((s = br.readLine()) != null) {
                if (s.contains("<Type>Crescent</Type>") || s.contains("note_type=\"light\"")) {
                    br.close();
                    return 5;
                }
                if (s.contains("TrackCount")) {
                    track = Integer.parseInt(getInfo(s, "<TrackCount>", "</TrackCount>"));
                }
                if (s.contains("note_type=\"short\"")) {
                    containsShort = true;
                }
                if (s.contains("Pinball")) {
                    br.close();
                    return 3;
                } else if (s.contains("ScreenPos")) {
                    br.close();
                    return 4;
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (containsShort) {
            if (track == 4) {
                return 1;
            } else {
                return 2;
            }
        }
        return 0;// 都不满足说明该xml不是炫舞谱面文件
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