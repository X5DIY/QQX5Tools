package com.menglei.qqx5tools.bean;

import com.menglei.qqx5tools.SettingsAndUtils.OutputMode;
import com.menglei.qqx5tools.SettingsAndUtils.QQX5MapType;
import com.menglei.qqx5tools.model.Calculate;
import com.menglei.qqx5tools.model.SetBasicInfo;
import lombok.Data;

import java.io.File;

/**
 * 该类存储谱面的所有相关信息，以及计算处理过程、输出至文件的方法.
 * <p>
 * -- 非押爆单排 --
 * 19.5拍爆气时长
 * 爆气开始或爆气结束的长条键都视作在爆气范围内
 * <p>
 * -- 押爆单排 --
 * 20拍爆气时长
 * 爆气开始的长条键而言，长条开头视作在爆气范围内，非长条开头视作在爆气范围外
 * 爆气结束的长条键都视作在爆气范围内
 * <p>
 * -- 超极限爆气 --
 * 20拍（无cool爆）至 21拍（有cool爆）爆气时长
 * 爆气开始或爆气结束的长条键都视作在爆气范围内
 * 技能为爆气时，爆气范围前后的单点也视作在爆气范围内，按照 cool 判计算
 * <p>
 * -- 非押爆双排 --
 * 38.5拍（存气状态）或 39拍（分开状态）爆气时长
 * 以非押爆单排数据为基础进行计算
 * 较为实用，一定程度上减小了延迟等因素的影响
 * <p>
 * -- 押爆双排 --
 * 39.5拍（存气状态）或 40拍（分开状态）爆气时长
 * 以押爆单排数据为基础进行计算
 * 理想状态，实际双排出现的可能性极低，仅做参考之用
 *
 * @author MengLeiFudge
 */
@Data
public
class QQX5MapInfo {
    private final File xml;
    private final QQX5MapType type;

    public QQX5MapInfo(File xml, QQX5MapType type) {
        this.xml = xml;
        this.type = type;
        new SetBasicInfo(this).set();
    }

    static int FireMaxNum;

    /* -- 基础信息 -- */

    public String getTypeStr() {
        return type.toString();
    }

    private double bpm;// 歌曲 bpm，用于判断是否能吃到 ab 段结尾按键，功能未指定——boolean bpmOver200？
    private String title;
    private String artist;
    private String firstLetter;
    private String level;
    private String bgmFilePath;
    private static final int BOX_PER_BAR = 32;// posNum = 64，boxPerBar 为一半
    // 1 bar = 4 拍 = 32 box = 64 pos
    int note1Bar;// a 段开始
    int st1Bar;// 中场 st 开始
    int note2Bar;// b 段开始
    int st2Bar;// 结尾 st 开始

    /**
     * 为了避免不必要的数组部分（第一个键之前和最后一个键之后），
     * 规定前后均只留 4 box 空白区，以便于后续计算 cool 爆
     *
     * @return note1 -> 4，其余 -> 原本 box 值 - 开头空白的长度 + 4
     */
    public int getNote1Box() {
        // return (note1Bar - note1Bar) * boxPerBar + 4;
        return 4;
    }

    public int getSt1Box() {
        return (st1Bar - note1Bar) * BOX_PER_BAR + 4;
    }

    public int getNote2Box() {
        return (note2Bar - note1Bar) * BOX_PER_BAR + 4;
    }

    public int getSt2Box() {
        return (st2Bar - note1Bar) * BOX_PER_BAR + 4;
    }

    int rowFireScore;// 无技能或爆气技能裸分
    // 注意：星动非排位/百人时，分数少50分

    int rowLimitScore;// 极限技能不爆气时的歌曲分数

    boolean combo20DiffScore = false;// 分数交界的不同分数按键标记
    boolean combo50DiffScore = false;
    boolean combo100DiffScore = false;

    /**
     * 在 20、50、100 combo 处会有分数增多，
     * 大致可分为 1.00、1.10、1.15、1.20 四个阶段。
     * 这些位置如果同时出现了不同分数的 note，
     * 必须先点击低分 note，让分数达到下一层次，再点击高分 note，以达到最大分数。
     *
     * @return 没有不同类型时，返回空字符串；有不同类型时，返回出现分差的所有位置
     */
    public String getScoreChange() {
        String scoreChange = "";
        if (this.combo20DiffScore) {
            scoreChange += "20、";
        }
        if (this.combo50DiffScore) {
            scoreChange += "50、";
        }
        if (this.combo100DiffScore) {
            scoreChange += "100、";
        }
        return scoreChange.equals("") ? "" : scoreChange.substring(0, scoreChange.length() - 1);
    }

    int[] combo;// 各 box 的 combo

    public int getHalfCombo() {// 半场combo
        return combo[getSt1Box() + 1];
    }

    public int getSongCombo() {// 歌曲combo
        return combo[getSt2Box() + 1];
    }
    // 计算 combo 时，规定第一个 box 为 0，每个 box 增加的 combo 数都放到 box + 1
    // 适用于星弹，但泡泡输出 combo 时需 +1

    int[][] track;
    int[][] noteType;
    boolean[][] isLongNoteStart;
    boolean[][] isLongNoteEnd;

    // track0-4 存储按键分数类型，1代表一倍，2代表两倍，3代表 0.3 倍，4代表 0.4 倍
    // noteType0-4 存储具体的按键类型，依模式变化，具体参照下表
    // 星动 1 0 单点，3 0 长条，1 12 、1 13 等为滑键
    // 弹珠 1 0 单点，1 -1 滑键，2 0 白球，3 0 长条，1 i 为连点第 i 个
    // 泡泡 1 0 单点，1 1 蓝条，3 0 绿条
    // 弦月 1 0 单点，3 0 长条，3 1 滑条，4 0 滑点
    // isLongNoteStart/End 存储是否为第一个/最后一个 0.3 倍分数按键

    // private boolean[] resetBoxFlag;// 标记为 true，说明这个位置的按键数字重置为 1
    // private int[] noteNum1;// 泡泡按键数字
    // private int[][] noteNum2;// 泡泡按键数字
    // private int[][] X;// 泡泡按键 x 坐标
    // private int[][] Y;
    // private int[][] angle;// 弦月按键角度
    // 泡泡还有重置数字标记、按键数字，以及按键坐标
    // 弦月还有按键角度
    // 这些只在 SetBasicInfo 中使用，所以这里不定义


    /* -- 爆点描述 -- */

    String[][] boxDescribe;// 所有位置的 非cool爆开始/非cool爆结束/cool爆开始/cool爆结束 爆点描述

    void setBoxDescribe(boolean isLegendFireSkill, boolean isStart, int box, String boxDescribe) {
        if (isLegendFireSkill) {
            if (isStart) {
                this.boxDescribe[2][box] = boxDescribe;
            } else {
                this.boxDescribe[3][box] = boxDescribe;
            }
        } else {
            if (isStart) {
                this.boxDescribe[0][box] = boxDescribe;
            } else {
                this.boxDescribe[1][box] = boxDescribe;
            }
        }
    }

    public String getBoxDescribe(boolean isLegendFireSkill, boolean isStart, int box) {
        if (isLegendFireSkill) {
            if (isStart) {
                return this.boxDescribe[2][box];
            } else {
                return this.boxDescribe[3][box];
            }
        } else {
            if (isStart) {
                return this.boxDescribe[0][box];
            } else {
                return this.boxDescribe[1][box];
            }
        }
    }


    /* -- 爆点相关 -- */

    /**
     * 将 box 转为 bar box
     * 先去掉规定增加的 4 box，再取整，最后加 note1Bar
     *
     * @param box 爆点 box 值
     * @return 爆点 bar 值
     */
    public int getBar(int box) {
        return (box - 4) / BOX_PER_BAR + note1Bar;
    }

    /**
     * 将 box 转为 bar box
     * 同样去掉规定增加的 4 box，再取余
     * 谱面编辑器中，Frey 将最小的格子称为 box
     * 1box = 2pos，且按键时间戳以上方为准
     *
     * @param box 爆点 box 值
     * @return 爆点 bar 中的 box 值
     */
    public int getBarBox(int box) {
        return (box - 4) % BOX_PER_BAR;
    }

    /**
     * 在 Calculate 中，只需要调用一次该方法，即可完成某个爆点信息的写入。
     * 先将爆点后的（也就是分数低于该爆点的）所有爆点都后移一位，
     * 再对这个位置赋值。
     *
     * @param isLegend     是否为超极限
     * @param isCommon     是否为非押爆
     * @param insertNum    要插入的爆点的位置
     * @param isLimitSkill 是否为极限技能
     * @param fireBox      爆点位置
     * @param score        爆点分数
     * @param index        爆点指数
     */
    public void setSingle(boolean isLegend, boolean isCommon, boolean isLimitSkill,
                          int insertNum, int fireBox, int score, double index) {
        // 先将该位置后面的信息依次后移
        backSetSingleSkill(isLegend, isCommon, insertNum);
        backSetSingleFireBox(isLegend, isCommon, insertNum);
        backSetSingleScore(isLegend, isCommon, insertNum);
        backSetSingleIndex(isLegend, isCommon, insertNum);
        // 再将该位置赋值
        setSingleSkill(isLegend, isCommon, insertNum, isLimitSkill);
        setSingleFireBox(isLegend, isCommon, insertNum, fireBox);
        setSingleScore(isLegend, isCommon, insertNum, score);
        setSingleIndex(isLegend, isCommon, insertNum, index);
    }

    public void setDouble(boolean isSeparate, boolean isCommon, boolean isLimitSkill,
                          int insertNum, int fireBox1, int fireBox2, int score, double index) {
        // 先将该位置后面的信息依次后移
        backSetType(isCommon, insertNum);
        backSetDoubleSkill(isCommon, insertNum);
        backSetDoubleFireBox(isCommon, true, insertNum);
        backSetDoubleFireBox(isCommon, false, insertNum);
        backSetDoubleScore(isCommon, insertNum);
        backSetDoubleIndex(isCommon, insertNum);
        // 再将该位置赋值
        setType(isCommon, insertNum, isSeparate);
        setDoubleSkill(isCommon, insertNum, isLimitSkill);
        setDoubleFireBox(isCommon, true, insertNum, fireBox1);
        setDoubleFireBox(isCommon, false, insertNum, fireBox2);
        setDoubleScore(isCommon, insertNum, score);
        setDoubleIndex(isCommon, insertNum, index);
    }


    private final boolean[][] isSeparate = new boolean[2][FireMaxNum];// 双排类型（存气/分开）

    /**
     * backSet 用于将某个位置后面的所有内容都往后移一位，
     * 这样就可以将新的值插入进去了。
     * 移动时必然是从后往前，依次向后赋值。
     *
     * @param isCommon  是否为非押爆
     * @param insertNum 即将插入新数据的位置
     */
    private void backSetType(boolean isCommon, int insertNum) {
        for (int i = FireMaxNum - 1; i > insertNum; i--) {
            if (isCommon) {
                this.isSeparate[0][i] = this.isSeparate[0][i - 1];
            } else {
                this.isSeparate[1][i] = this.isSeparate[1][i - 1];
            }
        }
    }

    /**
     * set 用于将新的数据插入。
     * 由于数组赋值会覆盖掉数据，所以要先使用 backSet 后移该位置后面的数据。
     *
     * @param isCommon   是否为非押爆
     * @param num        插入新数据的位置
     * @param isSeparate 两次爆气是否为分开
     */
    private void setType(boolean isCommon, int num, boolean isSeparate) {
        if (isCommon) {
            this.isSeparate[0][num] = isSeparate;
        } else {
            this.isSeparate[1][num] = isSeparate;
        }
    }

    public boolean isSeparate(boolean isCommon, int num) {
        if (isCommon) {
            return this.isSeparate[0][num];
        } else {
            return this.isSeparate[1][num];
        }
    }

    public String getStrType(boolean isCommon, int num) {
        if (isSeparate(isCommon, num)) {
            return "分开";
        } else {
            return "存气";
        }
    }


    private final boolean[][] isLimitSkill = new boolean[5][FireMaxNum];// 技能

    private void backSetSingleSkill(boolean isLegend, boolean isCommon, int insertNum) {
        for (int i = FireMaxNum - 1; i > insertNum; i--) {
            if (isLegend) {
                this.isLimitSkill[2][i] = this.isLimitSkill[2][i - 1];
            } else if (isCommon) {
                this.isLimitSkill[0][i] = this.isLimitSkill[0][i - 1];
            } else {
                this.isLimitSkill[1][i] = this.isLimitSkill[1][i - 1];
            }
        }
    }

    private void setSingleSkill(boolean isLegend, boolean isCommon, int num, boolean isLimitSkill) {
        if (isLegend) {
            this.isLimitSkill[2][num] = isLimitSkill;
        } else if (isCommon) {
            this.isLimitSkill[0][num] = isLimitSkill;
        } else {
            this.isLimitSkill[1][num] = isLimitSkill;
        }
    }

    private void backSetDoubleSkill(boolean isCommon, int insertNum) {
        for (int i = FireMaxNum - 1; i > insertNum; i--) {
            if (isCommon) {
                this.isLimitSkill[3][i] = this.isLimitSkill[3][i - 1];
            } else {
                this.isLimitSkill[4][i] = this.isLimitSkill[4][i - 1];
            }
        }
    }

    private void setDoubleSkill(boolean isCommon, int num, boolean isLimitSkill) {
        if (isCommon) {
            this.isLimitSkill[3][num] = isLimitSkill;
        } else {
            this.isLimitSkill[4][num] = isLimitSkill;
        }
    }

    public boolean isLimitSkill(boolean isLegend, boolean isCommon, int num) {
        if (isLegend) {
            return this.isLimitSkill[2][num];
        } else if (isCommon) {
            return this.isLimitSkill[0][num];
        } else {
            return this.isLimitSkill[1][num];
        }
    }

    public String getStrSingleSkill(boolean isLegend, boolean isCommon, int num) {
        if (isLimitSkill(isLegend, isCommon, num)) {
            return "极限";
        } else {
            return "爆气";
        }
    }

    public boolean isLimitSkill(boolean isCommon, int num) {
        if (isCommon) {
            return this.isLimitSkill[3][num];
        } else {
            return this.isLimitSkill[4][num];
        }
    }

    public String getStrDoubleSkill(boolean isCommon, int num) {
        if (isLimitSkill(isCommon, num)) {
            return "极限";
        } else {
            return "爆气";
        }
    }


    private final int[][] fireBox = new int[7][FireMaxNum];// 爆点 box

    private void backSetSingleFireBox(boolean isLegend, boolean isCommon, int insertNum) {
        for (int i = FireMaxNum - 1; i > insertNum; i--) {
            if (isLegend) {
                this.fireBox[2][i] = this.fireBox[2][i - 1];
            } else if (isCommon) {
                this.fireBox[0][i] = this.fireBox[0][i - 1];
            } else {
                this.fireBox[1][i] = this.fireBox[1][i - 1];
            }
        }
    }

    private void setSingleFireBox(boolean isLegend, boolean isCommon, int num, int fireBox) {
        if (isLegend) {
            this.fireBox[2][num] = fireBox;
        } else if (isCommon) {
            this.fireBox[0][num] = fireBox;
        } else {
            this.fireBox[1][num] = fireBox;
        }
    }

    private void backSetDoubleFireBox(boolean isCommon, boolean isFirst, int insertNum) {
        for (int i = FireMaxNum - 1; i > insertNum; i--) {
            if (isCommon) {
                if (isFirst) {
                    this.fireBox[3][i] = this.fireBox[3][i - 1];
                } else {
                    this.fireBox[4][i] = this.fireBox[4][i - 1];
                }
            } else {
                if (isFirst) {
                    this.fireBox[5][i] = this.fireBox[5][i - 1];
                } else {
                    this.fireBox[6][i] = this.fireBox[6][i - 1];
                }
            }
        }
    }

    private void setDoubleFireBox(boolean isCommon, boolean isFirst, int num, int fireBox) {
        if (isCommon) {
            if (isFirst) {
                this.fireBox[3][num] = fireBox;
            } else {
                this.fireBox[4][num] = fireBox;
            }
        } else {
            if (isFirst) {
                this.fireBox[5][num] = fireBox;
            } else {
                this.fireBox[6][num] = fireBox;
            }
        }
    }

    public int getSingleFireBox(boolean isLegend, boolean isCommon, int num) {
        if (isLegend) {
            return this.fireBox[2][num];
        } else if (isCommon) {
            return this.fireBox[0][num];
        } else {
            return this.fireBox[1][num];
        }
    }

    public int getDoubleFireBox(boolean isCommon, boolean isFirst, int num) {
        if (isCommon) {
            if (isFirst) {
                return this.fireBox[3][num];
            } else {
                return this.fireBox[4][num];
            }
        } else {
            if (isFirst) {
                return this.fireBox[5][num];
            } else {
                return this.fireBox[6][num];
            }
        }
    }


    private final int[][] score = new int[7][FireMaxNum];// 分数

    private void backSetSingleScore(boolean isLegend, boolean isCommon, int insertNum) {
        for (int i = FireMaxNum - 1; i > insertNum; i--) {
            if (isLegend) {
                this.score[2][i] = this.score[2][i - 1];
            } else if (isCommon) {
                this.score[0][i] = this.score[0][i - 1];
            } else {
                this.score[1][i] = this.score[1][i - 1];
            }
        }
    }

    private void setSingleScore(boolean isLegend, boolean isCommon, int num, int score) {
        if (isLegend) {
            this.score[2][num] = score;
        } else if (isCommon) {
            this.score[0][num] = score;
        } else {
            this.score[1][num] = score;
        }
    }

    private void backSetDoubleScore(boolean isCommon, int insertNum) {
        for (int i = FireMaxNum - 1; i > insertNum; i--) {
            if (isCommon) {
                this.score[3][i] = this.score[3][i - 1];
            } else {
                this.score[4][i] = this.score[4][i - 1];
            }
        }
    }

    private void setDoubleScore(boolean isCommon, int num, int score) {
        if (isCommon) {
            this.score[3][num] = score;
        } else {
            this.score[4][num] = score;
        }
    }

    public int getSingleScore(boolean isLegend, boolean isCommon, int num) {
        if (isLegend) {
            return this.score[2][num];
        } else if (isCommon) {
            return this.score[0][num];
        } else {
            return this.score[1][num];
        }
    }

    public int getDoubleScore(boolean isCommon, int num) {
        if (isCommon) {
            return this.score[3][num];
        } else {
            return this.score[4][num];
        }
    }


    private final double[][] index = new double[7][FireMaxNum];// 指数

    private void backSetSingleIndex(boolean isLegend, boolean isCommon, int insertNum) {
        for (int i = FireMaxNum - 1; i > insertNum; i--) {
            if (isLegend) {
                this.index[2][i] = this.index[2][i - 1];
            } else if (isCommon) {
                this.index[0][i] = this.index[0][i - 1];
            } else {
                this.index[1][i] = this.index[1][i - 1];
            }
        }
    }

    private void setSingleIndex(boolean isLegend, boolean isCommon, int num, double index) {
        if (isLegend) {
            this.index[2][num] = index;
        } else if (isCommon) {
            this.index[0][num] = index;
        } else {
            this.index[1][num] = index;
        }
    }

    private void backSetDoubleIndex(boolean isCommon, int insertNum) {
        for (int i = FireMaxNum - 1; i > insertNum; i--) {
            if (isCommon) {
                this.index[3][i] = this.index[3][i - 1];
            } else {
                this.index[4][i] = this.index[4][i - 1];
            }
        }
    }

    private void setDoubleIndex(boolean isCommon, int num, double index) {
        if (isCommon) {
            this.index[3][num] = index;
        } else {
            this.index[4][num] = index;
        }
    }

    private double getSingleIndex(boolean isLegend, boolean isCommon, int num) {
        if (isLegend) {
            return this.index[2][num];
        } else if (isCommon) {
            return this.index[0][num];
        } else {
            return this.index[1][num];
        }
    }

    public String getStrSingleIndex(boolean isLegend, boolean isCommon, int num) {
        return index2Str(getSingleIndex(isLegend, isCommon, num));
    }

    private double getDoubleIndex(boolean isCommon, int num) {
        if (isCommon) {
            return this.index[3][num];
        } else {
            return this.index[4][num];
        }
    }

    public String getStrDoubleIndex(boolean isCommon, int num) {
        return index2Str(getDoubleIndex(isCommon, num));
    }

    /**
     * 转为指数提示字符串.
     * <p>
     * 由于游戏中只显示四舍五入的整数，所以
     *
     * @param burstValue 爆气指数，一位小数
     * @return 爆气指数的字符串表示，40.3 返回 40 + 0.3，39.7 返回 40 - 0.3
     */
    private String index2Str(double burstValue) {
        long integerPart = Math.round(burstValue);
        long decimalPart = Math.round((burstValue - integerPart) * 10);
        if (decimalPart >= 0) {
            return integerPart + " + 0." + decimalPart;
        } else {
            integerPart++;
            decimalPart = 10 - decimalPart;
            return integerPart + " - 0." + decimalPart;
        }
    }


    public void calculateAll(int a, int b) {
        new Calculate(this).calculate();
    }

    public void writeAll(OutputMode outputMode) {

    }

}