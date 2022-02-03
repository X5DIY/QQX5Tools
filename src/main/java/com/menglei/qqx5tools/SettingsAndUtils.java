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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author MengLeiFudge
 */
public class SettingsAndUtils {
    /* 炫舞相关 */

    public enum QQX5MapType {
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

    public static QQX5MapType getQQX5MapType(File file) {
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
                        return QQX5MapType.CRESCENT;
                    }
                    if (s.contains("target_track")) {
                        return switch (trackCount) {
                            case 3 -> QQX5MapType.IDOL3K;
                            case 4 -> QQX5MapType.IDOL4K;
                            case 5 -> QQX5MapType.IDOL5K;
                            default -> null;
                        };
                    }
                    if (s.startsWith("<Note ID")) {
                        return QQX5MapType.PINBALL;
                    }
                    if (s.contains("Type")) {
                        return QQX5MapType.BUBBLE;
                    }
                    if (s.contains("length")) {
                        return QQX5MapType.CLASSIC;
                    }
                }
            }
        } catch (IOException e) {
            logError(e);
        }
        return null;
    }

    public enum OutputMode {
        SIMPLE,
        FULL
    }

    public enum ScoreType {
        SP_ONCE,
        SP_TWICE,
        SP_THREE_TENTHS,
        SP_FOUR_TENTHS,
        COOL_ONCE,
        COOL_TWICE,
        SSP_ONCE,
        SSP_TWICE,
        SSP_THREE_TENTHS,
        SSP_FOUR_TENTHS,
    }

    /**
     * 返回不同技能、是否爆气对数组位置的差别.
     *
     * @param isLimitSkill true 表示 极限Lv3，false 表示 爆气Lv3
     * @param isBursting   是否处于爆气状态
     * @return 指定情况对应的index
     */
    private int getNoteState(boolean isLimitSkill, boolean isBursting) {
        if (!isBursting) {
            return !isLimitSkill ? 0 : 1;
        } else {
            return !isLimitSkill ? 2 : 3;
        }
    }

    private int getComboState(int combo) {
        if (combo < 19) {
            return 0;
        } else if (combo < 49) {
            return 1;
        } else if (combo < 99) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * 根据按键类型、技能类型、是否处于爆气状态、当前combo数，返回指定按键的基础分或爆气加分.
     * <p>
     * 按键类型：不同按键有不同分数，以单点为1，则长条为0.3，白球为2，滑点为0.4
     * <p>
     * 技能类型：只考虑 极限增强Lv3 和 爆气加成Lv3
     * <ul>
     *     <li>极限增强Lv3：SSP得分提高12%，SP得分提高11%，perfect得分提高9%，对局中自动触发</li>
     *     <li>爆气加成Lv3：爆气技能分数加成额外提高240%，对局中使用爆气时自动生效</li>
     * </ul>
     * <p>
     * 爆气状态：爆气状态下，分数有0.5倍加成
     * <p>
     * 计算公式为 (1+基础分数加成)*(1+0.5*(1+爆气加成))，下面以SP判定为例
     * <ul>
     *     <li>极限技能+爆气状态：(1+0.11)*(1+0.5*(1+0))=1.665</li>
     *     <li>爆气技能+爆气状态：(1+0)*(1+0.5*(1+2.4))=2.7</li>
     * </ul>
     * combo数：0-18为1倍，19-48为1.1倍，49-98为1.15倍，99及以上为1.2倍
     * <p>
     * 需要注意的是，实际分数并不是简单的全部相乘再取整，而是中间步骤就取整了。
     * 这部分并不需要细致研究，使用数组可以避免问题，且读取速度更快。
     * <p>
     * 由于cool爆只在爆气技能+爆气状态才有意义，所以只使用一维数组。
     *
     * @param type         分数类型
     * @param isLimitSkill 是否为极限技能
     * @param isBursting   是否处于爆气状态
     * @param combo        未点击该按键前的combo值
     * @return 非爆气状态返回按键基础分数，爆气状态返回按键加分
     */
    int getNoteScore(ScoreType type, boolean isLimitSkill, boolean isBursting, int combo) {
        int noteState = getNoteState(isLimitSkill, isBursting);
        int comboState = getComboState(combo);
        return switch (type) {
            case SP_ONCE -> onceSpNoteScore[noteState][comboState];
            case SP_TWICE -> twiceSpNoteScore[noteState][comboState];
            case SP_THREE_TENTHS -> threeTenthsSpNoteScore[noteState][comboState];
            case SP_FOUR_TENTHS -> fourTenthsSpNoteScore[noteState][comboState];
            case COOL_ONCE -> onceCoolNoteScore[comboState];
            case COOL_TWICE -> twiceCoolNoteScore[comboState];
            case SSP_ONCE -> onceSspNoteScore[noteState][comboState];
            case SSP_TWICE -> twiceSspNoteScore[noteState][comboState];
            case SSP_THREE_TENTHS -> threeTenthsSspNoteScore[noteState][comboState];
            case SSP_FOUR_TENTHS -> fourTenthsSspNoteScore[noteState][comboState];
            ///test
        };
    }

    private static final int[][] onceSpNoteScore = {
            {2600, 2886, 4420, 1443,},
            {2860, 3174, 4862, 1587,},
            {2990, 3318, 5083, 1659,},
            {3120, 3463, 5304, 1731,},
    };

    private static final int[][] twiceSpNoteScore = {
            {5200, 5772, 8840, 2886,},
            {5720, 6349, 9724, 3174,},
            {5980, 6637, 10166, 3318,},
            {6240, 6926, 10608, 3463,},
    };

    private static final int[][] threeTenthsSpNoteScore = {
            {780, 865, 1326, 432,},
            {858, 952, 1458, 476,},
            {897, 995, 1524, 497,},
            {936, 1038, 1591, 519,},
    };

    private static final int[][] fourTenthsSpNoteScore = {
            {1040, 1154, 1768, 577,},
            {1144, 1269, 1944, 634,},
            {1196, 1327, 2033, 663,},
            {1248, 1385, 2121, 692,},
    };

    private static final int[] onceCoolNoteScore = {100, 110, 115, 120,};

    private static final int[] twiceCoolNoteScore = {200, 220, 230, 240,};

    /* 基础设置、默认目录选择 */

    /**
     * 是否打印bytes文件本身的信息，便于调试.
     */
    public static final boolean PRINT_BYTES_INFO = true;


    /* 程序通用设置 */

    public static final int THREAD_NUM = Runtime.getRuntime().availableProcessors();

    public static final DecimalFormat df = new DecimalFormat("0.00%");


    /* 时间转换 */

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


    /* 字符串截取 */

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


    /* 自定义线程池 */

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


    /* 日志记录 */

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


    /* 弹窗，基于Alert */

    public static boolean showConfirmRetOk(String title, String msg) {
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

    private static ButtonType showAlert(AlertType type, String title, String msg) {
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


    /* 弹窗，基于Stage */

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


    /* 文件锁 */

    private static final ConcurrentHashMap<File, ReentrantReadWriteLock> lockMap = new ConcurrentHashMap<>();

    public static ReentrantReadWriteLock getLock(File file) {
        synchronized (file.getPath()) {
            if (lockMap.containsKey(file)) {
                return lockMap.get(file);
            }
            ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
            lockMap.put(file, lock);
            return lock;
        }


//        ConcurrentHashMap<File, ReentrantReadWriteLock> lockMapTemp = new ConcurrentHashMap<>(lockMap);
//        for (Map.Entry<File, ReentrantReadWriteLock> entry : lockMapTemp.entrySet()) {
//            if (entry.getKey().equals(file)) {
//                return entry.getValue();
//            }
//        }

    }

    private static synchronized ReentrantReadWriteLock addLock(File file) {
        return null;
    }
}
