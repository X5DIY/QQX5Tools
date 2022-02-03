package com.menglei.qqx5tools.model;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 使用的是基于JDK HttpURLConnection的同步下载，即按顺序下载
 * 如果同时下载多个任务，可以使用多线程
 */
public class DownloadNotRankSongs {
    public DownloadNotRankSongs(DownloadNotRankSongsController c) {

    }

    public void process() {
        // 下载列表
        ArrayList<String> downloadList = new ArrayList<>();
        // 添加下载地址
        for (int i = 100000; i < 103200; i++) {
            downloadList.add("http://x5music-40020.sh.gfp.tencent-cloud.com/all_" + i + ".zip");
        }
        download(downloadList);
    }

    /**
     * 下载
     */
    static void download(ArrayList<String> downloadList) {
        // 线程池
        ExecutorService pool = null;
        HttpURLConnection connection1 = null;
        try {
            for (String url : downloadList) {
                pool = Executors.newCachedThreadPool();
                String filename = getFilename(url);
                Future<HttpURLConnection> future = pool.submit(() -> {
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    // 下面两个值必须足够大（至少10s），以保证不会出现超时异常
                    connection.setConnectTimeout(20000);//连接超时时间
                    connection.setReadTimeout(20000);// 读取超时时间
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestMethod("GET");
                    connection.connect();
                    return connection;
                });
                connection1 = future.get();
                // 写入文件，只有响应码不为404时才有效
                if (connection1.getResponseCode() != 404) {
                    writeFile(new BufferedInputStream(connection1.getInputStream()),
                            URLDecoder.decode(filename, StandardCharsets.UTF_8));
                }
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            logError(e);
        } finally {
            if (null != connection1)
                connection1.disconnect();
            if (null != pool)
                pool.shutdown();
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
            System.out.println("下载完成：" + filename);
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
