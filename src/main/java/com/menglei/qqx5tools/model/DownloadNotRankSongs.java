package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.SettingsAndUtils;
import com.menglei.qqx5tools.controller.DownloadNotRankSongsController;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.menglei.qqx5tools.SettingsAndUtils.THREAD_NUM;
import static com.menglei.qqx5tools.SettingsAndUtils.logError;
import static com.menglei.qqx5tools.SettingsAndUtils.logInfo;
import static java.lang.Thread.sleep;

/**
 * 使用的是基于JDK HttpURLConnection的同步下载，即按顺序下载
 * 如果同时下载多个任务，可以使用多线程
 */
public class DownloadNotRankSongs {
    public DownloadNotRankSongs(DownloadNotRankSongsController c) {

    }

    public static final class MyThreadFactory implements ThreadFactory {
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        public MyThreadFactory() {
            group = Thread.currentThread().getThreadGroup();
            namePrefix = "thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    public void process() {
        // 下载列表
        ArrayList<String> downloadList = new ArrayList<>();
        // 添加下载地址
        for (int i = 100000; i < 105000; i++) {
            downloadList.add("http://x5music-40020.sh.gfp.tencent-cloud.com/all_" + i + ".zip");
        }
        download(downloadList);
    }

    /**
     * 下载
     */
    void download(ArrayList<String> downloadList) {
        ExecutorService pool = new ThreadPoolExecutor(
                THREAD_NUM, THREAD_NUM,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(THREAD_NUM),
                new SettingsAndUtils.MyThreadFactory());
        for (int i = 0; i < THREAD_NUM; i++) {
            pool.execute(new Download(downloadList, i));
        }
        pool.shutdown();
        try {
            while (!pool.isTerminated()) {
                sleep(300);
            }
        } catch (InterruptedException e) {
            logError(e);
            Thread.currentThread().interrupt();
        }
        logInfo("处理完毕");
    }

    class Download extends Thread {
        ArrayList<String> downloadList;
        int threadNo;

        Download(ArrayList<String> downloadList, int threadNo) {
            this.downloadList = downloadList;
            this.threadNo = threadNo;
        }

        @Override
        public void run() {
            for (int i = 0; i < downloadList.size(); i++) {
                if (i % THREAD_NUM == threadNo) {
                    HttpURLConnection connection = null;
                    try {
                        for (String url : downloadList) {
                            String filename = getFilename(url);
                            connection = (HttpURLConnection) new URL(url).openConnection();
                            // 下面两个值必须足够大（至少10s），以保证不会出现超时异常
                            connection.setConnectTimeout(20000);//连接超时时间
                            connection.setReadTimeout(20000);// 读取超时时间
                            connection.setDoInput(true);
                            connection.setDoOutput(true);
                            connection.setRequestMethod("GET");
                            connection.connect();
                            // 写入文件，只有响应码不为404时才有效
                            if (connection.getResponseCode() != 404) {
                                writeFile(new BufferedInputStream(connection.getInputStream()),
                                        URLDecoder.decode(filename, StandardCharsets.UTF_8));
                                System.out.println("下载完成：" + filename);
                            } else {
                                System.out.println("下载失败：" + filename);
                            }
                        }
                    } catch (IOException e) {
                        logError(e);
                    } finally {
                        if (null != connection)
                            connection.disconnect();
                    }
                }
            }
        }
    }

    /**
     * 通过截取URL地址获得文件名
     * 注意：还有一种下载地址是没有文件后缀的，这个需要通过响应头中的
     * Content-Disposition字段 获得filename，一般格式为："attachment; filename=\xxx.exe\"
     *
     * @param url URL地址
     * @return 文件名
     */
    private static String getFilename(String url) {
        return ("".equals(url) || null == url) ? "" : url.substring(url.lastIndexOf("/") + 1);
    }

    /**
     * 写入文件
     *
     * @param bis      写入文件的流
     * @param filename 文件名
     */
    private static void writeFile(BufferedInputStream bis, String filename) {
        //创建本地文件
        File file = new File("x5Files/download/" + filename);
        if (file.exists()) {
            file.delete();
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }
        FileOutputStream fos = null;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            byte[] b = new byte[1024];
            int len;
            // 写入文件
            while ((len = bis.read(b, 0, b.length)) != -1) {
                fos.write(b, 0, len);
            }
        } catch (IOException e) {
            logError(e);
        } finally {
            try {
                if (null != fos) {
                    fos.flush();
                    fos.close();
                }
                if (null != bis)
                    bis.close();
            } catch (IOException e) {
                logError(e);
            }
        }
    }
}
