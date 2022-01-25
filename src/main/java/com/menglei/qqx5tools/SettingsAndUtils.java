package com.menglei.qqx5tools;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsAndUtils {
    public enum FileType {
        IDOL,
        IDOL3K,
        IDOL4K,
        IDOL5K,
        PINBALL,
        BUBBLE,
        CLASSIC,
        CRESCENT,
        RHYTHM;

        @Override
        public String toString() {
            return switch (this) {
                case IDOL, IDOL3K, IDOL4K, IDOL5K -> "星动";
                case PINBALL -> "弹珠";
                case BUBBLE -> "泡泡";
                case CLASSIC -> "传统";
                case CRESCENT -> "弦月";
                case RHYTHM -> "节奏";
            };
        }
    }

    public static FileType getFileType(File file) {
        if (file.isDirectory()) {
            return null;
        }
        if (!file.getName().endsWith(".xml")) {
            return null;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            int trackCount = 0;
            String s;
            while ((s = br.readLine()) != null) {
                s = s.trim();
                if (trackCount == 0 && s.startsWith("<TrackCount>")) {
                    trackCount = Integer.parseInt(getInfo(s, "<TrackCount>", "</TrackCount>"));
                } else if (s.startsWith("<Note ")) {
                    //这个判定必须放到判定target_track前，因为弦月也有target_track
                    if (s.contains("\" track=\"")) {
                        return FileType.CRESCENT;
                    }
                    if (s.contains("target_track")) {
                        return switch (trackCount) {
                            case 3 -> FileType.IDOL3K;
                            case 4 -> FileType.IDOL4K;
                            case 5 -> FileType.IDOL5K;
                            default -> null;
                        };
                    }
                    if (s.startsWith("<Note ID")) {
                        return FileType.PINBALL;
                    }
                    if (s.contains("Type")) {
                        return FileType.BUBBLE;
                    }
                    if (s.contains("length")) {
                        return FileType.CLASSIC;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public enum OutputMode {
        SIMPLE,
        FULL
    }

    public static final int THREAD_NUM = Runtime.getRuntime().availableProcessors();

    /*
    是否打印bytes文件本身的信息，便于调试
     */
    public static final boolean PRINT_BYTES_INFO = true;

    public static String milliTime(long startTime) {
        return milliTime(startTime, System.currentTimeMillis());
    }

    public static String milliTime(long startTime, long endTime) {
        long millisecond = endTime - startTime;
        StringBuilder time = new StringBuilder();
        long second;
        if (millisecond > 3600000L) {
            second = millisecond / 3600000L;
            millisecond %= 3600000L;
            time.append(second).append(" h ");
        }

        if (millisecond > 60000L) {
            second = millisecond / 60000L;
            millisecond %= 60000L;
            time.append(second).append(" min ");
        }

        if (millisecond > 1000L) {
            second = millisecond / 1000L;
            millisecond %= 1000L;
            time.append(second).append(" s ");
        }

        time.append(millisecond).append(" ms");
        return time.toString();
    }

    public static String nanoTime(long startTime) {
        return nanoTime(startTime, System.nanoTime());
    }

    public static String nanoTime(long startTime, long endTime) {
        long nanosecond = endTime - startTime;
        StringBuilder time = new StringBuilder();
        long microsecond;
        if (nanosecond > 60000000000L) {
            microsecond = nanosecond / 60000000000L;
            nanosecond %= 60000000000L;
            time.append(microsecond).append(" min ");
        }

        if (nanosecond > 1000000000L) {
            microsecond = nanosecond / 1000000000L;
            nanosecond %= 1000000000L;
            time.append(microsecond).append(" s ");
        }

        if (nanosecond > 1000000L) {
            microsecond = nanosecond / 1000000L;
            nanosecond = (long) ((double) nanosecond % 1000000.0D);
            time.append(microsecond).append(" ms ");
        }

        if (nanosecond > 1000L) {
            microsecond = nanosecond / 1000L;
            nanosecond %= 1000L;
            time.append(microsecond).append(" us ");
        }

        time.append(nanosecond).append(" ns");
        return time.toString();
    }

    public static String getInfo(String s, String s1, String s2) {
        return getInfo(s, s1, 1, s2, 1, false, false);
    }

    public static String getInfo(String s, String s1, int s1Num, String s2, int s2Num) {
        return getInfo(s, s1, s1Num, s2, s2Num, false, false);
    }

    public static String getInfo(String s, String s1, int s1Num, String s2, int s2Num, boolean overlap, boolean include) {
        String cut = "";

        try {
            int str1Index = getStrIndex(s, s1, s1Num, overlap);
            int str2Index = getStrIndex(s, s2, s2Num, overlap);
            if (include) {
                if (str1Index <= str2Index && str1Index + s1.length() >= str2Index + s2.length()) {
                    cut = s1;
                } else if (str1Index >= str2Index && str1Index + s1.length() <= str2Index + s2.length()) {
                    cut = s2;
                } else {
                    cut = str1Index < str2Index ? s.substring(str1Index, str2Index + s2.length()) : s.substring(str2Index, str1Index + s1.length());
                }
            } else if (str1Index + s1.length() <= str2Index) {
                cut = s.substring(str1Index + s1.length(), str2Index);
            } else {
                if (str2Index + s2.length() > str1Index) {
                    throw new IllegalStateException("两个目标字符串有重叠部分，无法截取！");
                }

                cut = s.substring(str2Index + s2.length(), str1Index);
            }
        } catch (IllegalStateException var10) {
            System.out.println("截取过程出现如下问题：");
            System.out.println(var10.getMessage());
        }

        return cut;
    }

    public static int getStrIndex(String str, String target, int num, boolean overlap) throws IllegalStateException {
        if (str != null && !str.equals("")) {
            if (target != null && !target.equals("")) {
                if (num < 1) {
                    throw new IllegalStateException("目标字符串序号至少为1！");
                } else {
                    int index = 0;

                    for (int i = 0; i < num - 1; ++i) {
                        if (overlap) {
                            index = str.indexOf(target, index) + 1;
                        } else {
                            index = str.indexOf(target, index) + target.length();
                        }
                    }

                    index = str.indexOf(target, index);
                    if (index == -1) {
                        throw new IllegalStateException("未从被查找字符串中找到目标字符串！");
                    } else {
                        return index;
                    }
                }
            } else {
                throw new IllegalStateException("目标字符串 为 null 或空，无法截取！");
            }
        } else {
            throw new IllegalStateException("被查找字符串 为 null 或空，无法截取！");
        }
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

    public static final DecimalFormat df = new DecimalFormat("0.00%");

    public static final Logger LOGGER = Logger.getLogger("qqx5");

    public static void logInfo(String msg) {
        LOGGER.log(Level.INFO, msg);
    }

    public static void logWarn(String msg) {
        LOGGER.log(Level.WARNING, msg);
    }

    public static void logError(String msg) {
        LOGGER.log(Level.SEVERE, msg);
    }

    public static void logError(Throwable thrown) {
        LOGGER.log(Level.SEVERE, null, thrown);
    }

    public static void logError(String msg, Throwable thrown) {
        LOGGER.log(Level.SEVERE, msg, thrown);
    }

    public static boolean showConfirmRetOK(String title, String msg) {
        return showAlert(AlertType.CONFIRMATION, title, msg) == ButtonType.OK;
    }

    public static void showInfo(String title, String msg) {
        showAlert(AlertType.INFORMATION, title, msg);
    }

    public static void showWarn(String title, String msg) {
        showAlert(AlertType.WARNING, title, msg);
    }

    public static void showError(String title, String msg) {
        showAlert(AlertType.ERROR, title, msg);
    }

    public static ButtonType showAlert(AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
        return alert.getResult();
    }

    public static void showException(Exception e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("出现异常");
        alert.setHeaderText(null);
        alert.setContentText(e.getClass().getName() + ": " + e.getMessage() + "\n展开以查看堆栈信息。");
        // 下方标题
        Label label = new Label("堆栈信息如下：");
        // 下方文本框
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        // 标题和文本框合到一起
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);
        // 作为可展开类型附加到alert上
        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    public static void showDialogAndWait(String info) {
        dialog(Modality.APPLICATION_MODAL, info);
    }

    public static void showDialog(String info) {
        dialog(Modality.NONE, info);
    }

    private static void dialog(Modality modality, String info) {
        Stage dialog = new Stage();
        dialog.initModality(modality);
        dialog.initOwner(QQX5ToolsApplication.getStage());
        VBox dialogVbox = new VBox(20);
        dialogVbox.getChildren().add(new Text(info));
        Scene dialogScene = new Scene(dialogVbox, 300, 200);
        dialog.setScene(dialogScene);
        dialog.show();
    }
}
