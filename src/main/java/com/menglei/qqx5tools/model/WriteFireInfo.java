package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.SettingsAndUtils.OutputMode;
import com.menglei.qqx5tools.bean.QQX5MapInfo;
import javafx.scene.control.TextArea;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.menglei.qqx5tools.model.Calculate.CommonFireLength;
import static com.menglei.qqx5tools.model.Calculate.ExtremeFireLength;
import static com.menglei.qqx5tools.model.Calculate.LegendFireLength;

class WriteFireInfo {

    private static int MaxDiff;// 允许的最大分差，超过的低分爆点将被舍去
    private static int OutMode;// 输出模式

    private static final File[] file1 = new File[12];
    private static final File[] file2 = new File[12];

    /**
     * 该构造函数在每次爆点计算完毕后调用
     * 用法为 new WriteFireInfo().write(XMLInfo a)
     * 作用为存储爆点信息
     */
    WriteFireInfo() {
    }

    /**
     * 该构造函数仅在计算爆点前使用一次
     * 用法为 new WriteFireInfo(0,1).writeFirst()
     * 作用为创建后续存储爆点信息的所有文件
     *
     * @param maxDiff    最大分差
     * @param outputMode 输出模式
     */
    WriteFireInfo(int maxDiff, OutputMode outputMode) {
        MaxDiff = maxDiff;
        OutMode = 1;
        createAll();
    }

    /**
     * 创建所有文件夹和文件
     */
    private void createAll() {
        int haveCreated = 0;
        String[] mode = new String[4];
        mode[0] = "星动";
        mode[1] = "弹珠";
        mode[2] = "泡泡";
        mode[3] = "弦月";
        String date = new SimpleDateFormat("_MMdd_HHmmss",
                new Locale("zh", "CN")).format(new Date());
        if (OutMode == 1) {
            for (int i = 0; i < 4; i++) {
                if (createFile(new File("burstPointsInfo/全信息模式/" + mode[i]), true)) {
                    haveCreated++;
                }
            }
            if (haveCreated == 4) {
                print("创建目录：成功");
            } else {
                print("创建目录：失败，请删除\"burstPointsInfo\"文件夹后再试");
                // todo: 弹出错误框，强制退到主界面
            }
            String[] kinds = new String[3];
            kinds[0] = "基础信息";
            kinds[1] = "一次爆气";
            kinds[2] = "两次爆气";
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) {
                    file1[i * 3 + j] = new File("burstPointsInfo/全信息模式/"
                            + mode[i] + "/" + kinds[j] + date + ".csv");
                    if (createFile(file1[i * 3 + j], false)) {
                        haveCreated++;
                    }
                }
            }
            if (haveCreated == 16) {
                c.ap("创建文件：成功");
            } else {
                println("创建文件：失败，请删除\"burstPointsInfo\"文件夹后再试");
                // todo: 弹出错误框，强制退到主界面
            }
        } else if (OutMode == 2) {
            String[] kinds = new String[3];
            kinds[0] = "非押爆";
            kinds[1] = "押爆";
            kinds[2] = "超极限";
            for (int i = 0; i < 3; i++) {
                if (createFile(new File("burstPointsInfo/爆气表模式/" + kinds[i]), true, textArea)) {
                    haveCreated++;
                }
            }
            if (haveCreated == 3) {
                print("创建目录：成功");
            } else {
                print("创建目录：失败，请删除\"burstPointsInfo\"文件夹后再试");
                // todo: 弹出错误框，强制退到主界面
            }
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) {
                    file2[i * 3 + j] = new File("burstPointsInfo/爆气表模式/"
                            + kinds[j] + "/" + mode[i] + kinds[j] + date + ".csv");
                    if (createFile(file2[i * 3 + j], false, textArea)) {
                        haveCreated++;
                    }
                }
            }
            if (haveCreated == 15) {
                println("创建文件：成功");
            } else {
                println("创建文件：失败，请删除\"burstPointsInfo\"文件夹后再试");
                // todo: 弹出错误框，强制退到主界面
            }
        }
    }

    private static boolean createFile(File file, boolean isDir, TextArea textArea) {
        boolean isCreated = false;
        if (file.exists()) {
            if (file.isDirectory() == isDir) {
                isCreated = true;
            } else {
                textArea.appendText("创建文件失败，请删除\"burstPointsInfo\"后再试");
            }
        } else {
            if (isDir) {
                if (file.mkdirs()) {
                    isCreated = true;
                }
            } else {
                try {
                    if (file.createNewFile()) {
                        isCreated = true;
                    }
                } catch (IOException e) {
                    logError(e);
                }
            }
        }
        return isCreated;
    }


    private static final String div = ",";// Excel 表格导入文本时所用的分隔符

    /**
     * i 代表 文件的下标，只根据下标即可决定要写入的内容
     */
    void writeFirst() {
        for (int i = 0; i <= 11; i++) {
            if (OutMode == 1) {
                firstLine1(i);
            } else if (OutMode == 2) {
                firstLine2(i);
            }
        }
    }

    private void firstLine1(int i) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file1[i]));
            if (i % 3 == 0) {
                bw.write("首字母" + div + "等级" + div
                        + "歌名" + div + "歌手" + div + "bgm路径" + div
                        + "无技能裸分" + div + "极限技能裸分" + div
                        + "半cb" + div + "全cb" + div + "分数变化");
            } else if (i % 3 == 1) {
                bw.write("首字母" + div + "等级" + div
                        + "歌名" + div + "歌手" + div
                        + "类型" + div + "技能" + div
                        + "爆点" + div + "bar" + div + "box" + div
                        + "开头描述" + div + "结尾描述" + div
                        + "分数" + div + "指数");
            } else {
                bw.write("首字母" + div + "等级" + div
                        + "歌名" + div + "歌手" + div
                        + "类型" + div + "技能" + div
                        + "状态" + div + "先后" + div
                        + "爆点" + div + "bar" + div + "box" + div
                        + "开头描述" + div + "结尾描述" + div
                        + "分数" + div + "指数");
            }
            bw.close();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void firstLine2(int i) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file2[i]));
            bw.write("字母" + div + "等级" + div + "歌名" + div + "歌手" + div
                    + "突变" + div + "技能" + div);
            if (i % 3 != 2) {
                bw.write("类型" + div + "双排爆点" + div
                        + "开头描述" + div + "结尾描述" + div
                        + "半cb" + div + "全cb" + div + "双指数" + div);
            }
            bw.write("单排爆点" + div
                    + "开头描述" + div + "结尾描述" + div
                    + "半cb" + div + "全cb" + div + "分数" + div + "单指数");
            bw.close();
        } catch (IOException e) {
            logError(e);
        }
    }


    void write(QQX5MapInfo a) {
        if (OutMode == 1) {
            switch (a.getTypeStr()) {
                case "星动":
                    writeBasic(a, 0);
                    writeSingle(a, 1, false, true);
                    writeSingle(a, 1, false, false);
                    writeSingle(a, 1, true, false);
                    writeDouble(a, 2, true);
                    writeDouble(a, 2, false);
                    break;
                case "弹珠":
                    writeBasic(a, 3);
                    writeSingle(a, 4, false, true);
                    writeSingle(a, 4, false, false);
                    writeSingle(a, 4, true, false);
                    writeDouble(a, 5, true);
                    writeDouble(a, 5, false);
                    break;
                case "泡泡":
                    writeBasic(a, 6);
                    writeSingle(a, 7, false, true);
                    writeSingle(a, 7, false, false);
                    writeSingle(a, 7, true, false);
                    writeDouble(a, 8, true);
                    writeDouble(a, 8, false);
                    break;
                case "弦月":
                    writeBasic(a, 9);
                    writeSingle(a, 10, false, true);
                    writeSingle(a, 10, false, false);
                    writeSingle(a, 10, true, false);
                    writeDouble(a, 11, true);
                    writeDouble(a, 11, false);
                    break;
            }
        } else if (OutMode == 2) {
            switch (a.getTypeStr()) {
                case "星动":
                    writeAll(a, 0);
                    writeAll(a, 1);
                    writeAll(a, 2);
                    break;
                case "弹珠":
                    writeAll(a, 3);
                    writeAll(a, 4);
                    writeAll(a, 5);
                    break;
                case "泡泡":
                    writeAll(a, 6);
                    writeAll(a, 7);
                    writeAll(a, 8);
                    break;
                case "弦月":
                    writeAll(a, 9);
                    writeAll(a, 10);
                    writeAll(a, 11);
                    break;
            }
        }
    }

    /**
     * -- 以下为 模式1 输出方法 --
     **/
    private void writeBasic(QQX5MapInfo a, int i) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file1[i], true));
            bw.newLine();// 先换行再写内容
            bw.write(a.firstLetter + div + a.level + div
                    + "\"" + a.title + "\"" + div + "\"" + a.artist + "\"" + div
                    // 引号防止歌曲名中有英文逗号，从而造成其余信息位置错误
                    + a.bgmFilePath + div);
            if (a.getTypeStr().equals("星动")) {
                bw.write((a.rowFireScore - 50) + div);
            } else {
                bw.write(a.rowFireScore + div);
            }
            bw.write(a.rowLimitScore + div
                    + a.getHalfCombo() + div + a.getSongCombo() + div + a.getScoreChange());
            bw.close();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void writeSingle(QQX5MapInfo a, int i, boolean isLegend, boolean isCommon) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file1[i], true));
            for (int num = 0; num < QQX5MapInfo.FireMaxNum; num++) {
                int score = a.getSingleScore(isLegend, isCommon, num);
                if (score < a.getSingleScore(isLegend, isCommon, 0) - MaxDiff) {
                    break;// 与最高分爆点的分数相差过多，将该爆点之后的点都舍去
                }

                bw.newLine();

                bw.write(a.firstLetter + div + a.level + div
                        + "\"" + a.title + "\"" + div + "\"" + a.artist + "\"" + div);

                if (isLegend) {
                    bw.write("超极限" + div);
                } else if (isCommon) {
                    bw.write("非押爆" + div);
                } else {
                    bw.write("押爆" + div);
                }
                bw.write(a.getStrSingleSkill(isLegend, isCommon, num) + div);

                int fireBox = a.getSingleFireBox(isLegend, isCommon, num);
                if (a.getTypeStr().equals("泡泡")) {
                    bw.write((a.combo[fireBox] + 1) + div);
                } else {
                    bw.write(a.combo[fireBox] + div);
                }

                boolean isLegendFireSkill = isLegend && !a.isLimitSkill(true, false, num);
                int fireLength;
                if (isLegend) {
                    fireLength = LegendFireLength;
                } else if (isCommon) {
                    fireLength = CommonFireLength;
                } else {
                    fireLength = ExtremeFireLength;
                }
                bw.write(a.getBar(fireBox) + div + a.getBarBox(fireBox) + div
                        + a.getBoxDescribe(isLegendFireSkill, true, fireBox) + div
                        + a.getBoxDescribe(isLegendFireSkill, false, fireBox + fireLength) + div
                        + score + div + a.getStrSingleIndex(isLegend, isCommon, num));
            }
            bw.close();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void writeDouble(QQX5MapInfo a, int i, boolean isCommon) {
        for (int num = 0; num < QQX5MapInfo.FireMaxNum; num++) {
            writeDouble(a, i, isCommon, true, num);
            writeDouble(a, i, isCommon, false, num);
        }
    }

    private void writeDouble(QQX5MapInfo a, int i, boolean isCommon, boolean isFirst, int num) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file1[i], true));
            int score = a.getDoubleScore(isCommon, num);
            if (score < a.getDoubleScore(isCommon, 0) - MaxDiff) {
                return;// 与最高分爆点的分数相差过多，将该爆点之后的点都舍去
            }

            bw.newLine();

            bw.write(a.firstLetter + div + a.level + div
                    + "\"" + a.title + "\"" + div + "\"" + a.artist + "\"" + div);

            if (isCommon) {
                bw.write("非押爆" + div);
            } else {
                bw.write("押爆" + div);
            }
            bw.write(a.getStrDoubleSkill(isCommon, num) + div + a.getStrType(isCommon, num) + div);

            if (isFirst) {
                bw.write("一爆" + div);
            } else {
                bw.write("二爆" + div);
            }
            int fireBox = a.getDoubleFireBox(isCommon, isFirst, num);

            if (a.getTypeStr().equals("泡泡")) {
                bw.write((a.combo[fireBox] + 1) + div);
            } else {
                bw.write(a.combo[fireBox] + div);
            }
            bw.write(a.getBar(fireBox) + div + a.getBarBox(fireBox) + div
                    + a.getBoxDescribe(false, true, fireBox) + div);

            int fireLength;
            if (isCommon) {
                fireLength = CommonFireLength;
            } else {
                fireLength = ExtremeFireLength;
            }
            if (a.isSeparate(isCommon, num)) {// 分开
                bw.write(a.getBoxDescribe(false, false, fireBox + fireLength) + div
                        + score + div + a.getStrDoubleIndex(isCommon, num));
            } else {// 存气
                if (isFirst) {
                    bw.write(a.getBoxDescribe(false, false, a.getSt1Box()) + div);
                } else {
                    int fireLength2 = 2 * fireLength - (a.getSt1Box() - a.getDoubleFireBox(isCommon, true, num)) - 4;
                    bw.write(a.getBoxDescribe(false, false, fireBox + fireLength2) + div);
                }
                bw.write(score + div + a.getStrDoubleIndex(isCommon, num));
            }
            bw.close();
        } catch (IOException e) {
            logError(e);
        }
    }

    /**
     * -- 以下为 模式2 输出方法 --
     **/
    private void writeAll(QQX5MapInfo a, int i) {
        boolean isLegend = false, isCommon = true;
        if (i % 3 != 0) {
            isCommon = false;
        }
        if (i % 3 == 2) {
            isLegend = true;
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file2[i], true));
            bw.newLine();

            bw.write(a.firstLetter + div + a.level + div
                    + "\"" + a.title + "\"" + div + "\"" + a.artist + "\"" + div);
            // 引号防止歌曲名中有英文逗号，从而造成其余信息位置错误

            bw.write(a.getScoreChange() + div);

            if (!a.isLimitSkill(isLegend, isCommon, 0)) {
                bw.write("红色" + div);
            } else if (a.isLimitSkill(isCommon, 0)) {
                bw.write("蓝色" + div);
            } else {
                bw.write(div);
            }

            int fireLength;
            if (isLegend) {
                fireLength = LegendFireLength;
            } else if (isCommon) {
                fireLength = CommonFireLength;
            } else {
                fireLength = ExtremeFireLength;
            }

            if (!isLegend) {
                bw.write(a.getStrType(isCommon, 0) + div);

                StringBuilder doubleCombo = new StringBuilder();
                for (int num = 0; num < QQX5MapInfo.FireMaxNum; num++) {
                    if (a.getDoubleScore(isCommon, num) < a.getDoubleScore(isCommon, 0)) {
                        break;
                    }
                    if (a.getTypeStr().equals("泡泡")) {
                        doubleCombo.append(a.combo[a.getDoubleFireBox(isCommon, true, num)] + 1).append(" + ")
                                .append(a.combo[a.getDoubleFireBox(isCommon, false, num)] + 1).append("、");
                    } else {
                        doubleCombo.append(a.combo[a.getDoubleFireBox(isCommon, true, num)]).append(" + ")
                                .append(a.combo[a.getDoubleFireBox(isCommon, false, num)]).append("、");
                    }
                }
                bw.write(doubleCombo.substring(0, doubleCombo.toString().length() - 1) + div);

                int fireBox1 = a.getDoubleFireBox(isCommon, true, 0);
                int fireBox2 = a.getDoubleFireBox(isCommon, false, 0);
                bw.write(a.getBoxDescribe(false, true, fireBox1) + "^^^"
                        + a.getBoxDescribe(false, true, fireBox2) + div);
                // WPS 中利用替换功能，可将 ^^^ 替换为换行符，从而达到分割目的

                if (a.isSeparate(isCommon, 0)) {// 分开
                    bw.write(a.getBoxDescribe(false, false, fireBox1 + fireLength) + "^^^"
                            + a.getBoxDescribe(false, false, fireBox2 + fireLength) + div);
                } else {// 存气
                    int fireLength2 = 2 * fireLength - (a.getSt1Box() - fireBox1) - 4;
                    bw.write(a.getBoxDescribe(false, false, a.getSt1Box()) + "^^^"
                            + a.getBoxDescribe(false, false, fireBox2 + fireLength2) + div);
                }

                bw.write(a.getHalfCombo() + div
                        + a.getSongCombo() + div);

                bw.write(a.getStrDoubleIndex(isCommon, 0) + div);
            }

            int score = a.getSingleScore(isLegend, isCommon, 0);
            StringBuilder singleCombo = new StringBuilder();
            for (int num = 0; num < QQX5MapInfo.FireMaxNum; num++) {
                if (a.getSingleScore(isLegend, isCommon, num) < score) {
                    break;
                }
                if (a.getTypeStr().equals("泡泡")) {
                    singleCombo.append(a.combo[a.getSingleFireBox(isLegend, isCommon, num)] + 1).append("、");
                } else {
                    singleCombo.append(a.combo[a.getSingleFireBox(isLegend, isCommon, num)]).append("、");
                }
            }
            bw.write(singleCombo.substring(0, singleCombo.toString().length() - 1) + div);

            boolean isLegendFireSkill = isLegend && !a.isLimitSkill(true, isCommon, 0);
            int fireBox = a.getSingleFireBox(isLegend, isCommon, 0);
            bw.write(a.getBoxDescribe(isLegendFireSkill, true, fireBox) + div
                    + a.getBoxDescribe(isLegendFireSkill, false, fireBox + fireLength) + div);

            bw.write(a.getHalfCombo() + div + a.getSongCombo() + div);

            bw.write(score + div + a.getStrSingleIndex(isLegend, isCommon, 0));

            bw.close();
        } catch (IOException e) {
            logError(e);
        }
    }
}
