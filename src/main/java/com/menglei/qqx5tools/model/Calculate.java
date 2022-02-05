package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.bean.QQX5MapInfo;

import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.BASIC_SP_FOUR_TENTHS;
import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.BASIC_SP_ONCE;
import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.BASIC_SP_THREE_TENTHS;
import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.BASIC_SP_TWICE;
import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.COOL_ONCE;
import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.COOL_TWICE;
import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.SP_FOUR_TENTHS;
import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.SP_ONCE;
import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.SP_THREE_TENTHS;
import static com.menglei.qqx5tools.SettingsAndUtils.NoteScoreType.SP_TWICE;
import static com.menglei.qqx5tools.SettingsAndUtils.getNoteScore;

/**
 * {@code Calculate} 含有所有的计算方法，计算的中间变量都在此处。
 * 计算无爆气分数时，同时计算各种模式下每个 box 的爆气加分、指数等；
 * 计算一次爆气分数时，利用各种模式下每个 box 的爆气加分，算出所有爆点加分、指数，
 * 并存储各种模式下最高分所有爆点，并赋值给 a；
 * 计算两次爆气分数时，利用各种模式下所有爆点加分、指数，计算。
 */

public class Calculate {
    private final QQX5MapInfo a;

    public Calculate(QQX5MapInfo a) {
        this.a = a;
        // 以下所有成员的具体说明均在后面定义该成员的位置
        this.note1Box = a.getNote1Box();
        this.st1Box = a.getSt1Box();
        this.note2Box = a.getNote2Box();
        this.st2Box = a.getSt2Box();
        this.boxScore = new int[5][st2Box + 5];// 加5是为了便于计算cool爆
        this.boxIndex = new double[3][st2Box + 5];
        this.fireScore = new int[4][st2Box + 5];
        this.fireIndex = new double[2][st2Box + 5];
        this.partScore = new int[2][st2Box + 1][ExtremeFireLength];
        this.partIndex = new double[st2Box + 1][ExtremeFireLength];
    }

    static int CommonFireLength = 156;
    static int ExtremeFireLength = 160;
    static int LegendFireLength = 161;
    // legend 共有三种长度，160-162 均可。
    // 160 保留所有爆点，161 去掉长条开始长条结尾的爆点，
    // 162 去掉

    private final int note1Box;// a段开始的位置
    private final int st1Box;// a段结束
    private final int note2Box;// b段开始
    private final int st2Box;// b段结束

    public void calculate() {
        basic(a);// 保存每个瞬时位置分数、指数增加，以及计算一些基础信息
        calcuSingle(a, false, true);// 非押爆，保存每个爆点分数、指数增加
        calcuSingle(a, false, false);// 押爆，保存每个爆点分数、指数增加
        calcuSingle(a, true, false);// 超极限，不保存，因为双排不会用到
        part();// 计算歌曲b段任意长度（应低于1-160）的分数指数
        calcuDouble(a, true);// 非押爆，存气情况使用 part 简化计算
        calcuDouble(a, false);// 押爆，存气情况使用 part 简化计算
    }

    /* -- part1 裸分与box分、box指数 -- */

    private int rowLimitScore = 0;// 极限技能基础分（即极限技能且不爆气的分数）
    private int rowFireScore = 0;// 爆气技能（或不带技能）基础分


    private final int[][] boxScore;// 每个 box 的爆气加分
    // 所谓每个 box，指的是在某个瞬间爆气，这个瞬间的分数增加量

    /**
     * 给每个 box 增加分数。
     * 按键要按照分数从低到高依次检测，会在 basic(XMLInfo a) 中多次调用该方法，
     * 所以不能采用覆盖数据赋值的方法。
     * （尽管理论上可以先求和再依次赋值，但代码难免会出错，而且拆开也好维护）
     *
     * @param isLimitSkill 是否为极限技能
     * @param haveLongNote 是否有长条，这个属性出现的原因是，一个长条的开头与非开头是不同的。
     *                     我将“长条开头”定义为“可操控的”，即可以人为控制这个键判定生效的时机；
     *                     而“非长条开头”定义为“不可操控的”，即不可以人为控制这个键判定生效的时机。
     *                     所以在押爆或非押爆开始时，默认“能吃到长条开头按键”，但是“吃不到非长条开头按键”。
     *                     这样就产生了 haveLongNote 这个属性。
     *                     在后续算某一个爆点的分数时（押爆或非押爆情况），将会第一个 box 使用 false，
     *                     后续 box 使用 true；超极限则都为 true。
     * @param box          增加的分数的位置
     * @param noteScore    增加的分数
     */
    private void addBoxScore(boolean isLimitSkill, boolean haveLongNote, int box, int noteScore) {
        if (isLimitSkill) {
            if (!haveLongNote) {
                this.boxScore[0][box] += noteScore;
            } else {
                this.boxScore[1][box] += noteScore;
            }
        } else {
            if (!haveLongNote) {
                this.boxScore[2][box] += noteScore;
            } else {
                this.boxScore[3][box] += noteScore;
            }
        }
    }

    /**
     * 这是针对超极限中“cool爆”部分而设置的加分方法
     *
     * @param coolBox       cool爆按键位置
     * @param coolNoteScore cool爆加分
     */
    private void addBoxScore(int coolBox, int coolNoteScore) {
        this.boxScore[4][coolBox] += coolNoteScore;
    }

    private int getBoxScore(boolean isLimitSkill, boolean haveLongNote, int box) {
        if (isLimitSkill) {
            if (!haveLongNote) {
                return boxScore[0][box];
            } else {
                return boxScore[1][box];
            }
        } else {
            if (!haveLongNote) {
                return boxScore[2][box];
            } else {
                return boxScore[3][box];
            }
        }
    }

    private int getBoxScore(int coolBox) {
        return boxScore[4][coolBox];
    }


    private final double[][] boxIndex;// 每个 box 的爆气指数
    // 和上面的 boxScore 类似，也是一个瞬间的指数增加量

    private void addBoxIndex(boolean haveLongNote, int box, double noteIndex) {
        if (!haveLongNote) {
            this.boxIndex[0][box] += noteIndex;
        } else {
            this.boxIndex[1][box] += noteIndex;
        }
    }

    private void addBoxIndex(int coolBox, double coolNoteIndex) {
        this.boxIndex[2][coolBox] += coolNoteIndex;
    }

    private double getBoxIndex(boolean haveLongNote, int box) {
        if (!haveLongNote) {
            return boxIndex[0][box];
        } else {
            return boxIndex[1][box];
        }
    }

    private double getBoxIndex(int coolBox) {
        return boxIndex[2][coolBox];
    }


    /**
     * 在计算基础信息的同时，存储每个瞬间的分数、指数增加量，并且检验分数突变
     * 检测方式为依照时间顺序，每一瞬间依次检测。
     * 每次检测都为从左往右循环检测，循环按照分数从低到高排列。
     * 目前有四种分数的键型，每个瞬间也就会循环检测四次。
     *
     * @param a 星弹泡弦任意对象
     */
    private void basic(QQX5MapInfo a) {
        int[] comboBox = new int[6];// comboBox[0] = combo18Box，comboBox[1] = combo19Box……
        int combo = 0;// 从0开始，位于按键前
        for (int box = note1Box; box <= st2Box; box++) {
            if (box > st1Box && box < note2Box) {
                continue;
            }

            for (int track = 0; track < 5; track++) {// 先读取 0.3 倍分数
                if (a.getTrack()[track][box] == 3) {
                    if (combo < 100 && combo > 17) {
                        checkCombo(combo, comboBox, box);
                    }

                    addBoxScore(true, true, box,
                            getNoteScore(SP_THREE_TENTHS, true, combo));
                    addBoxScore(false, true, box,
                            getNoteScore(SP_THREE_TENTHS, false, combo));
                    rowLimitScore += getNoteScore(BASIC_SP_THREE_TENTHS, true, combo);
                    rowFireScore += getNoteScore(BASIC_SP_THREE_TENTHS, false, combo);

                    addBoxIndex(true, box, 0.3);

                    if (a.getIsLongNoteStart()[track][box]) {
                        // 长条开头 note 默认总能吃到
                        addBoxScore(true, false, box,
                                getNoteScore(SP_THREE_TENTHS, true, combo));
                        addBoxScore(false, false, box,
                                getNoteScore(SP_THREE_TENTHS, false, combo));
                        addBoxIndex(false, box, 0.3);
                    }

                    combo++;
                }
            }

            for (int track = 0; track < 5; track++) {// 再读取 0.4 倍分数
                if (a.getTrack()[track][box] == 4) {
                    if (combo < 100 && combo > 17) {
                        checkCombo(combo, comboBox, box);
                    }

                    addBoxScore(true, true, box,
                            getNoteScore(SP_FOUR_TENTHS, true, combo));
                    addBoxScore(true, false, box,
                            getNoteScore(SP_FOUR_TENTHS, true, combo));
                    addBoxScore(false, true, box,
                            getNoteScore(SP_FOUR_TENTHS, false, combo));
                    addBoxScore(false, false, box,
                            getNoteScore(SP_FOUR_TENTHS, false, combo));
                    rowLimitScore += getNoteScore(BASIC_SP_FOUR_TENTHS, true, combo);
                    rowFireScore += getNoteScore(BASIC_SP_FOUR_TENTHS, false, combo);

                    addBoxIndex(true, box, 0.4);
                    addBoxIndex(false, box, 0.4);

                    combo++;
                }
            }

            for (int track = 0; track < 5; track++) {// 再读取 1 倍分数
                if (a.getTrack()[track][box] == 1) {
                    if (combo < 100 && combo > 17) {
                        checkCombo(combo, comboBox, box);
                    }

                    addBoxScore(true, true, box,
                            getNoteScore(SP_ONCE, true, combo));
                    addBoxScore(true, false, box,
                            getNoteScore(SP_ONCE, true, combo));
                    addBoxScore(false, true, box,
                            getNoteScore(SP_ONCE, false, combo));
                    addBoxScore(false, false, box,
                            getNoteScore(SP_ONCE, false, combo));
                    addBoxScore(box, getNoteScore(COOL_ONCE, false, combo));
                    rowLimitScore += getNoteScore(BASIC_SP_ONCE, true, combo);
                    rowFireScore += getNoteScore(BASIC_SP_ONCE, false, combo);

                    addBoxIndex(true, box, 1);
                    addBoxIndex(false, box, 1);
                    addBoxIndex(box, 1);// cool part

                    combo++;
                }
            }

            for (int track = 0; track < 5; track++) {// 最后读取 2 倍分数
                if (a.getTrack()[track][box] == 2) {
                    if (combo < 100 && combo > 17) {
                        checkCombo(combo, comboBox, box);
                    }

                    addBoxScore(true, true, box,
                            getNoteScore(SP_TWICE, true, combo));
                    addBoxScore(true, false, box,
                            getNoteScore(SP_TWICE, true, combo));
                    addBoxScore(false, true, box,
                            getNoteScore(SP_TWICE, false, combo));
                    addBoxScore(false, false, box,
                            getNoteScore(SP_TWICE, false, combo));
                    addBoxScore(box, getNoteScore(COOL_TWICE, false, combo));
                    rowLimitScore += getNoteScore(BASIC_SP_TWICE, true, combo);
                    rowFireScore += getNoteScore(BASIC_SP_TWICE, false, combo);

                    addBoxIndex(true, box, 2);
                    addBoxIndex(false, box, 2);
                    addBoxIndex(box, 2);// cool part

                    combo++;
                }
            }

            a.getCombo()[box + 1] = combo;
        }
        a.getCombo()[note2Box] = a.getCombo()[st1Box + 1];
        if (comboBox[0] == comboBox[1]) {
            a.setCombo20DiffScore(haveDifferent(a, comboBox[0]));
        }
        if (comboBox[2] == comboBox[3]) {
            a.setCombo50DiffScore(haveDifferent(a, comboBox[2]));
        }
        if (comboBox[4] == comboBox[5]) {
            a.setCombo100DiffScore(haveDifferent(a, comboBox[4]));
        }
        a.setRowLimitScore(rowLimitScore);
        a.setRowFireScore(rowFireScore);
    }

    private void checkCombo(int combo, int[] comboBox, int box) {
        if (combo == 18) {
            comboBox[0] = box;
        } else if (combo == 19) {
            comboBox[1] = box;
        } else if (combo == 48) {
            comboBox[2] = box;
        } else if (combo == 49) {
            comboBox[3] = box;
        } else if (combo == 98) {
            comboBox[4] = box;
        } else if (combo == 99) {
            comboBox[5] = box;
        }
    }

    /**
     * 分数突变是指在20、50、100combo这三个位置，存在按键基础分增加的情况。
     * 这时候必须先点击分数少的按键，再点击分数多的按键，以达到分数最大化。
     * <p>
     * 但是，星动是有特例的。
     * 单长条滑动时，由于同一根长条的按键有先后顺序，所以不应显示分数突变。
     *
     * @param a   星弹泡弦任意对象
     * @param box 刚好combo变化时，有两个以上的按键的位置
     * @return 如果键型不同，那么返回 true；键型相同则无影响，返回 false
     */
    private boolean haveDifferent(QQX5MapInfo a, int box) {
        if (a.getTypeStr().equals("星动")) {
            for (int i = 0; i < 4; i++) {
                for (int j = i + 1; j < 5; j++) {
                    if (a.getTrack()[i][box] != 0 && a.getTrack()[j][box] != 0 && a.getTrack()[i][box] != a.getTrack()[j][box]) {
                        // 如果是同一根长条，并且其余位置没有键，则略过
                        if ((a.getTrack()[i][box] == 1 && a.getNoteType()[i][box] > 0 && a.getTrack()[j][box] == 3)
                                || (a.getTrack()[i][box] == 3 && a.getTrack()[j][box] == 1 && a.getNoteType()[j][box] > 0)) {
                            int slip = a.getTrack()[i][box] == 1 ? i : j;
                            int longNote = a.getTrack()[i][box] == 3 ? i : j;
                            if (a.getNoteType()[slip][box] / 10 == longNote) {
                                continue;
                            }
                        }
                        return true;
                    }
                }
            }
        } else {
            int type = 0;
            for (int i = 0; i < 5; i++) {
                if (a.getTrack()[i][box] != 0) {
                    if (type == 0) {
                        type = a.getTrack()[i][box];
                    } else if (a.getTrack()[i][box] != type) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /* -- part2 爆点分、爆点指数以及一次爆气的计算与处理 -- */

    private final int[][] fireScore;// 所有爆点爆气加分
    // 指的是在某个瞬间爆气，在爆气长度内（依据爆气模式而定）的所有 box 的分数增加量和

    /**
     * 设置某个爆点爆气，在爆气时间内，相比基础分增加的分数
     * 特别注意：由于双排计算只会用到非押爆/押爆，所以不需要存超极限
     *
     * @param isCommon     是否为非押爆
     * @param isLimitSkill 是否为极限技能
     * @param box          爆点位置
     * @param fireScore    爆点加分
     */
    private void setFireScore(boolean isCommon, boolean isLimitSkill, int box, int fireScore) {
        if (isCommon) {
            if (isLimitSkill) {
                this.fireScore[0][box] = fireScore;
            } else {
                this.fireScore[1][box] = fireScore;
            }
        } else {
            if (isLimitSkill) {
                this.fireScore[2][box] = fireScore;
            } else {
                this.fireScore[3][box] = fireScore;
            }
        }
    }

    private int getFireScore(boolean isCommon, boolean isLimitSkill, int box) {
        if (isCommon) {
            if (isLimitSkill) {
                return this.fireScore[0][box];
            } else {
                return this.fireScore[1][box];
            }
        } else {
            if (isLimitSkill) {
                return this.fireScore[2][box];
            } else {
                return this.fireScore[3][box];
            }
        }
    }


    private final double[][] fireIndex;// 所有爆点爆气指数

    private void setFireIndex(boolean isCommon, int box, double fireIndex) {
        if (isCommon) {
            this.fireIndex[0][box] = fireIndex;
        } else {
            this.fireIndex[1][box] = fireIndex;
        }
    }

    private double getFireIndex(boolean isCommon, int box) {
        if (isCommon) {
            return this.fireIndex[0][box];
        } else {
            return this.fireIndex[1][box];
        }
    }


    /**
     * 该方法将被调用三次，分别对应非押爆、押爆、超极限三种情况。
     * 每次调用，都会同时处理极限技能和爆气技能。
     * 计算完成后，除了将结果赋值给 a，还要将非押爆、押爆的信息
     * 存到本类的 fireScore 和 fireIndex 中，供后续双排计算之用。
     *
     * @param a        星弹泡弦任意对象
     * @param isLegend 是否为超极限
     * @param isCommon 是否为非押爆
     */
    private void calcuSingle(QQX5MapInfo a, boolean isLegend, boolean isCommon) {
        int fireLength;
        if (isLegend) {
            fireLength = LegendFireLength;
        } else if (isCommon) {
            fireLength = CommonFireLength;
        } else {
            fireLength = ExtremeFireLength;
        }

        for (int fireBox = note1Box; fireBox <= st2Box - fireLength; fireBox++) {
            if (fireBox > st1Box && fireBox < note2Box) {
                continue;// 限定 fireBox 范围
            }
            // 先计算分数
            int limitScore = 0;
            int fireScore = 0;
            double commonIndex = 0;// 通常情况下，爆气指数不受技能影响
            double legendAddIndex = 0;// 但是legend情况下，爆气指数受技能影响，这里存储legend多加的指数
            for (int box = fireBox - 4; box <= fireBox + fireLength + 4; box++) {
                if (box < fireBox) {// 如果是爆气前
                    if (isLegend) {
                        fireScore += getBoxScore(box);
                        legendAddIndex += getBoxIndex(box);
                    }
                } else if (box == fireBox) {// 如果是爆气第一个位置
                    // 规定押爆、非押爆情况下，爆气开始吃不到非长条开头的长条键
                    boolean haveLongNote = isLegend;
                    // 规定超极限可以吃到任意长条键
                    limitScore += getBoxScore(true, haveLongNote, box);
                    fireScore += getBoxScore(false, haveLongNote, box);
                    commonIndex += getBoxIndex(haveLongNote, box);
                } else if (box <= fireBox + fireLength) {// 如果是爆气其他位置
                    limitScore += getBoxScore(true, true, box);
                    fireScore += getBoxScore(false, true, box);
                    commonIndex += getBoxIndex(true, box);
                } else {// 如果是爆气后
                    if (isLegend) {
                        fireScore += getBoxScore(box);
                        legendAddIndex += getBoxIndex(box);
                    }
                }
            }
            // 再存储非押爆/押爆情况下的信息，便于双排计算
            if (!isLegend) {
                setFireScore(isCommon, true, fireBox, limitScore);
                setFireScore(isCommon, false, fireBox, fireScore);
                setFireIndex(isCommon, fireBox, commonIndex);
            }
            // 最后判断分数的位置并赋值
            limitScore += this.rowLimitScore;
            fireScore += this.rowFireScore;
            setSingle(a, isLegend, isCommon, true,
                    fireBox, limitScore, commonIndex, legendAddIndex);
            setSingle(a, isLegend, isCommon, false,
                    fireBox, fireScore, commonIndex, legendAddIndex);
        }
    }

    static final int FireMaxNum = 5000;

    private void setSingle(QQX5MapInfo a, boolean isLegend, boolean isCommon, boolean isLimitSkill,
                           int fireBox, int score, double commonIndex, double legendAddIndex) {
        int insertNum = FireMaxNum - 1;
        // 确定爆点插入的位置
        while (insertNum >= 0 && score > a.getSingleScore(isLegend, isCommon, insertNum)) {
            insertNum--;
        }
        insertNum++;
        // 如果分数未超过最低分，insertNum 应等于 FireMaxNum
        if (insertNum < FireMaxNum) {
            boolean isLegendFireSkill = isLegend && !isLimitSkill;
            if (insertNum != 0 && a.getBoxDescribe(isLegendFireSkill, true,
                            a.getSingleFireBox(isLegend, isCommon, insertNum - 1))
                    .equals(a.getBoxDescribe(false, true, fireBox))) {
                return;// 爆点描述与上一个相同，舍去
            }
            double index = commonIndex;
            if (isLegendFireSkill) {
                index += legendAddIndex;
            }
            a.setSingle(isLegend, isCommon, isLimitSkill, insertNum, fireBox, score, index);
        }
    }


    /* -- part3 b段任意长度分数和、指数和 -- */

    private final int[][][] partScore;// b段任意长度的分数增加和，供双排存气计算用

    private int getPartScore(boolean isLimitSkill, int start, int end) {
        if (isLimitSkill) {
            return this.partScore[0][start][end - start];
        } else {
            return this.partScore[1][start][end - start];
        }
    }

    private final double[][] partIndex;// b段任意长度的指数增加和，供双排存气计算用

    private double getPartIndex(int start, int end) {
        return this.partIndex[start][end - start];
    }

    /**
     * 根据已有的每个瞬时位置的分数、指数增加量，
     * 计算出歌曲b段任意长度的分数、指数增加量，供双排存气计算之用
     */
    private void part() {
        if (note1Box != note2Box) {// 有中场 st 说明两次爆气需计算存气，有必要计算 part
            for (int box = note2Box; box < st2Box + 1; box++) {
                int limitScore = getBoxScore(true, true, box);
                int fireScore = getBoxScore(false, true, box);
                double index = getBoxIndex(true, box);
                for (int i = 1; i < ExtremeFireLength; i++) {
                    this.partScore[0][box - i][i] = this.partScore[0][box - i][i - 1] + limitScore;
                    this.partScore[1][box - i][i] = this.partScore[1][box - i][i - 1] + fireScore;
                    this.partIndex[box - i][i] = this.partIndex[box - i][i - 1] + index;
                }
            }
        }
    }


    /* -- part4 两次爆气的计算与处理 -- */

    private void calcuDouble(QQX5MapInfo a, boolean isCommon) {
        int fireLength;
        if (isCommon) {
            fireLength = CommonFireLength;
        } else {
            fireLength = ExtremeFireLength;
        }

        // 计算两次分开的完整爆气的最高分
        for (int fireBox1 = note1Box; fireBox1 < st2Box - 2 * fireLength; fireBox1++) {
            if (fireBox1 > st1Box - fireLength && fireBox1 < note2Box) {
                continue; // 限定 fireBox1 在有意义的爆气区
            }
            for (int fireBox2 = fireBox1 + fireLength + 1; fireBox2 <= st2Box - fireLength; fireBox2++) {
                if (fireBox2 > st1Box - fireLength && fireBox2 < note2Box) {
                    continue; // 限定 fireBox2 在有意义的爆气区
                }
                // 判断分数的位置并赋值
                int limitScore = getFireScore(isCommon, true, fireBox1)
                        + getFireScore(isCommon, true, fireBox2)
                        + this.rowLimitScore;
                int fireScore = getFireScore(isCommon, false, fireBox1)
                        + getFireScore(isCommon, false, fireBox2)
                        + this.rowFireScore;
                double index = getFireIndex(isCommon, fireBox1) + getFireIndex(isCommon, fireBox2);
                setDouble(a, true, isCommon, true,
                        fireBox1, fireBox2, limitScore, index);
                setDouble(a, true, isCommon, false,
                        fireBox1, fireBox2, fireScore, index);
            }
        }

        // 计算存气最高分
        if (note1Box != note2Box) {// 有中场 st 时，才计算存气
            for (int fireBox1 = st1Box - fireLength + 5; fireBox1 <= a.getSt1Box() - 8; fireBox1++) {
                // 规定存气时，a段长度大于等于一拍，小于19拍半
                // 19拍半没有意义，因为此时二爆长度等于 fireLength，分开爆的分数一定高于存气
                int fireLength2 = 2 * fireLength - (st1Box - fireBox1) - 4;// 二爆爆气范围，默认少半拍
                for (int fireBox2 = note2Box; fireBox2 <= st2Box - fireLength2; fireBox2++) {
                    // 判断分数的位置并赋值
                    int limitScore = getFireScore(isCommon, true, fireBox1)
                            + getFireScore(isCommon, true, fireBox2)
                            + this.rowLimitScore
                            + getPartScore(true, fireBox2 + fireLength + 1, fireBox2 + fireLength2);
                    int fireScore = getFireScore(isCommon, false, fireBox1)
                            + getFireScore(isCommon, false, fireBox2)
                            + this.rowFireScore
                            + getPartScore(false, fireBox2 + fireLength + 1, fireBox2 + fireLength2);
                    double index = getFireIndex(isCommon, fireBox1) + getFireIndex(isCommon, fireBox2)
                            + getPartIndex(fireBox2 + fireLength + 1, fireBox2 + fireLength2);
                    setDouble(a, false, isCommon, true,
                            fireBox1, fireBox2, limitScore, index);
                    setDouble(a, false, isCommon, false,
                            fireBox1, fireBox2, fireScore, index);
                }
            }
        }
    }

    private void setDouble(QQX5MapInfo a, boolean isSeparate, boolean isCommon, boolean isLimitSkill,
                           int fireBox1, int fireBox2, int score, double index) {
        int insertNum = FireMaxNum - 1;
        // 确定爆点插入的位置
        while (insertNum >= 0 && score > a.getDoubleScore(isCommon, insertNum)) {
            insertNum--;
        }
        insertNum++;
        // 如果分数未超过最低分，insertNum 应等于 FireMaxNum
        if (insertNum < FireMaxNum) {
            if (insertNum != 0 && a.getBoxDescribe(false, true,
                            a.getDoubleFireBox(isCommon, true, insertNum - 1))
                    .equals(a.getBoxDescribe(false, true, fireBox1))
                    && a.getBoxDescribe(false, true,
                            a.getDoubleFireBox(isCommon, false, insertNum - 1))
                    .equals(a.getBoxDescribe(false, true, fireBox2))) {
                return;// 爆点描述与上一个相同，舍去
            }
            a.setDouble(isSeparate, isCommon, isLimitSkill, insertNum, fireBox1, fireBox2, score, index);
        }
    }

}
