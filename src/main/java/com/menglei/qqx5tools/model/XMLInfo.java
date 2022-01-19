package com.menglei.qqx5tools.model;

/**
 * 父类应在最大限度上，抽取出本质上共同的部分，比如按键分数。
 * 但是，分数还是有细微的误差。
 * 为此，我引入了 {@code mode}，并重写 {@code getScore} 方法，避免了误差。
 * <p>
 * 用 {@link SetBasicInfo} 录入信息时，就已经得到了模式值。
 * 通过模式值来选择不同的方法，以达到正确录入信息的目的。
 * 同时，也会生成爆点描述。
 * 这样的话，在 {@code XMLInfo} 类中，只需直接调用这些信息。
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
 */

class XMLInfo {

    XMLInfo(int mode) {
        this.mode = mode;
    }

    static int FireMaxNum;

    /* -- 基础信息 -- */

    private int mode;// 模式，以此去掉按键分数细微误差，同时确定长条类型（尤其是泡泡蓝条）

    String getStrMode(){
        switch (this.mode) {
            case 1:
                return "星动";
            case 2:
                return "弹珠";
            case 3:
                return "泡泡";
            case 4:
                return "弦月";
            default:
                return "";
        }
    }

    double bpm;// 歌曲 bpm，用于判断是否能吃到 ab 段结尾按键，功能未指定——boolean bpmOver200？
    String title;// 歌名
    String artist;// 歌手或作曲家（防止同名歌曲）
    String firstLetter;// 首字母
    String level;// 等级
    String bgmFilePath;// bgm 路径，官谱可确定对应 bgm 及谱面文件
    int boxPerBar = 32;// posNum = 64，boxPerBar 为一半
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
    int getNote1Box() {
        // return (note1Bar - note1Bar) * boxPerBar + 4;
        return 4;
    }

    int getSt1Box() {
        return (st1Bar - note1Bar) * boxPerBar + 4;
    }

    int getNote2Box() {
        return (note2Bar - note1Bar) * boxPerBar + 4;
    }

    int getSt2Box() {
        return (st2Bar - note1Bar) * boxPerBar + 4;
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
    String getScoreChange() {
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

    int getHalfCombo() {// 半场combo
        return combo[getSt1Box() + 1];
    }

    int getSongCombo() {// 歌曲combo
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

    String getBoxDescribe(boolean isLegendFireSkill, boolean isStart, int box) {
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


    /* -- 单个按键分数 -- */

    /**
     * 前面在 getScoreChange 中提到过，基础分是逐渐增多的。
     * 在 20、50、100 combo 处会有分数增多，
     * 大致可分为 1.00、1.10、1.15、1.20 四个阶段。
     * 之所以说大致，有两个原因。
     * 一是技能对分数有一定影响（极限技能 1.11 倍分数），会出现小数的分数。
     * 一般情况下，舍去小数部分即可。但是这样仍得不到准确值，这就涉及第二个原因。
     * 二是模式也对分数有一定影响。
     * 根据测量结果，星动爆气技能的排位/百人、爆气状态的 50-100 combo 单个按键
     * 比弹泡要少 2-3 分；同时，音乐实验室也比排位/百人少 1 分。
     * 这个方法则是用于解决星弹泡之间的按键分数细微影响。
     * 至于音乐实验室的影响，输出裸分时，星动模式将减去 50。
     *
     * @param scoreTimes   分数倍数，1 为一倍，2 为两倍，3 为 0.3 倍，4 为 0.4 倍
     * @param isLimitSkill 是否为极限技能
     * @param isInFire     是否处于爆气状态
     * @param combo        combo
     * @return 一个按键的分数
     */
    int getNoteScore(int scoreTimes, boolean isLimitSkill, boolean isInFire, int combo) {
        switch (scoreTimes) {
            case 1:
                return this.onceNoteScore(getState(isLimitSkill, isInFire), combo);
            case 2:
                return this.twiceNoteScore(getState(isLimitSkill, isInFire), combo);
            case 3:
                return this.threeTenthsNoteScore(getState(isLimitSkill, isInFire), combo);
            case 4:
                return this.fourTenthsNoteScore(getState(isLimitSkill, isInFire), combo);
        }
        return 0;
    }

    int getNoteScore(int scoreTimes, int combo) {
        switch (scoreTimes) {
            case 1:
                return this.onceCoolNoteScore(combo);
            case 2:
                return this.twiceCoolNoteScore(combo);
        }
        return 0;
    }


    /**
     * 为了减少 {@code getNoteScore} 内容而加入的状态数字，分数由低到高排序
     *
     * @param isLimitSkill 是否为极限技能
     * @param isFire       是否处于爆气状态
     * @return 爆气/极限技能的非爆气/爆气状态
     */
    private int getState(boolean isLimitSkill, boolean isFire) {
        if (isLimitSkill) {
            if (!isFire) {
                return 1;
            } else {
                return 2;
            }
        } else {
            if (!isFire) {
                return 0;
            } else {
                return 3;
            }
        }
    }

    /**
     * 返回不考虑模式带来的细微误差的按键分数
     *
     * @param state 爆气/极限技能的非爆气/爆气状态
     * @param combo combo
     * @return 非爆气状态为按键基础分，爆气状态为爆气加分
     */
    private int onceNoteScore(int state, int combo) {
        if (combo < 19) {
            switch (state) {
                case 0:
                    return 2600;
                case 1:
                    return 2886;
                case 2:
                    return 1443;
                case 3:
                    return 4420;
            }
        } else if (combo < 49) {
            switch (state) {
                case 0:
                    return 2860;
                case 1:
                    return 3174;
                case 2:
                    return 1587;
                case 3:
                    return 4862;
            }
        } else if (combo < 99) {
            switch (state) {
                case 0:
                    return 2990;
                case 1:
                    return 3318;
                case 2:
                    return 1659;
                case 3:
                    return 5083;
            }
        } else {
            switch (state) {
                case 0:
                    return 3120;
                case 1:
                    return 3463;
                case 2:
                    return 1731;
                case 3:
                    return 5304;
            }
        }
        return 0;
    }

    private int twiceNoteScore(int state, int combo) {
        if (combo < 19) {
            switch (state) {
                case 0:
                    return 5200;
                case 1:
                    return 5772;
                case 2:
                    return 2886;
                case 3:
                    return 8840;
            }
        } else if (combo < 49) {
            switch (state) {
                case 0:
                    return 5720;
                case 1:
                    return 6349;
                case 2:
                    return 3174;
                case 3:
                    return 9724;
            }
        } else if (combo < 99) {
            switch (state) {
                case 0:
                    return 5980;
                case 1:
                    return 6637;
                case 2:
                    return 3318;
                case 3:
                    return 10166;
            }
        } else {
            switch (state) {
                case 0:
                    return 6240;
                case 1:
                    return 6926;
                case 2:
                    return 3463;
                case 3:
                    return 10608;
            }
        }
        return 0;
    }

    private int threeTenthsNoteScore(int state, int combo) {
        if (combo < 19) {
            switch (state) {
                case 0:
                    return 780;
                case 1:
                    return 865;
                case 2:
                    return 432;
                case 3:
                    return 1326;
            }
        } else if (combo < 49) {
            switch (state) {
                case 0:
                    return 858;
                case 1:
                    return 952;
                case 2:
                    return 476;
                case 3:
                    return 1458;
            }
        } else if (combo < 99) {
            switch (state) {
                case 0:
                    return 897;
                case 1:
                    return 995;
                case 2:
                    return 497;
                case 3:
                    return 1524;
            }
        } else {
            switch (state) {
                case 0:
                    return 936;
                case 1:
                    return 1038;
                case 2:
                    return 519;
                case 3:
                    return 1591;
            }
        }
        return 0;
    }

    private int fourTenthsNoteScore(int state, int combo) {
        if (combo < 19) {
            switch (state) {
                case 0:
                    return 1040;
                case 1:
                    return 1154;
                case 2:
                    return 577;
                case 3:
                    return 1768;
            }
        } else if (combo < 49) {
            switch (state) {
                case 0:
                    return 1144;
                case 1:
                    return 1269;
                case 2:
                    return 634;
                case 3:
                    return 1944;
            }
        } else if (combo < 99) {
            switch (state) {
                case 0:
                    return 1196;
                case 1:
                    return 1327;
                case 2:
                    return 663;
                case 3:
                    return 2033;
            }
        } else {
            switch (state) {
                case 0:
                    return 1248;
                case 1:
                    return 1385;
                case 2:
                    return 692;
                case 3:
                    return 2121;
            }
        }
        return 0;
    }

    private int onceCoolNoteScore(int combo) {
        if (combo < 19) {
            return 100;
        } else if (combo < 49) {
            return 110;
        } else if (combo < 99) {
            return 115;
        } else {
            return 120;
        }
    }

    private int twiceCoolNoteScore(int combo) {
        if (combo < 19) {
            return 200;
        } else if (combo < 49) {
            return 220;
        } else if (combo < 99) {
            return 230;
        } else {
            return 240;
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
    int getBar(int box) {
        return (box - 4) / boxPerBar + note1Bar;
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
    int getBarBox(int box) {
        return (box - 4) % boxPerBar;
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
    void setSingle(boolean isLegend, boolean isCommon, boolean isLimitSkill,
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

    void setDouble(boolean isSeparate, boolean isCommon, boolean isLimitSkill,
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


    private boolean[][] isSeparate = new boolean[2][FireMaxNum];// 双排类型（存气/分开）

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

    boolean isSeparate(boolean isCommon, int num) {
        if (isCommon) {
            return this.isSeparate[0][num];
        } else {
            return this.isSeparate[1][num];
        }
    }

    String getStrType(boolean isCommon, int num) {
        if (isSeparate(isCommon, num)) {
            return "分开";
        } else {
            return "存气";
        }
    }


    private boolean[][] isLimitSkill = new boolean[5][FireMaxNum];// 技能

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

    boolean isLimitSkill(boolean isLegend, boolean isCommon, int num) {
        if (isLegend) {
            return this.isLimitSkill[2][num];
        } else if (isCommon) {
            return this.isLimitSkill[0][num];
        } else {
            return this.isLimitSkill[1][num];
        }
    }

    String getStrSingleSkill(boolean isLegend, boolean isCommon, int num) {
        if (isLimitSkill(isLegend, isCommon, num)) {
            return "极限";
        } else {
            return "爆气";
        }
    }

    boolean isLimitSkill(boolean isCommon, int num) {
        if (isCommon) {
            return this.isLimitSkill[3][num];
        } else {
            return this.isLimitSkill[4][num];
        }
    }

    String getStrDoubleSkill(boolean isCommon, int num) {
        if (isLimitSkill(isCommon, num)) {
            return "极限";
        } else {
            return "爆气";
        }
    }


    private int[][] fireBox = new int[7][FireMaxNum];// 爆点 box

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

    int getSingleFireBox(boolean isLegend, boolean isCommon, int num) {
        if (isLegend) {
            return this.fireBox[2][num];
        } else if (isCommon) {
            return this.fireBox[0][num];
        } else {
            return this.fireBox[1][num];
        }
    }

    int getDoubleFireBox(boolean isCommon, boolean isFirst, int num) {
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


    private int[][] score = new int[7][FireMaxNum];// 分数

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

    int getSingleScore(boolean isLegend, boolean isCommon, int num) {
        if (isLegend) {
            return this.score[2][num];
        } else if (isCommon) {
            return this.score[0][num];
        } else {
            return this.score[1][num];
        }
    }

    int getDoubleScore(boolean isCommon, int num) {
        if (isCommon) {
            return this.score[3][num];
        } else {
            return this.score[4][num];
        }
    }


    private double[][] index = new double[7][FireMaxNum];// 指数

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

    String getStrSingleIndex(boolean isLegend, boolean isCommon, int num) {
        return index2Str(getSingleIndex(isLegend, isCommon, num));
    }

    private double getDoubleIndex(boolean isCommon, int num) {
        if (isCommon) {
            return this.index[3][num];
        } else {
            return this.index[4][num];
        }
    }

    String getStrDoubleIndex(boolean isCommon, int num) {
        return index2Str(getDoubleIndex(isCommon, num));
    }

    /**
     * 指数只有一位小数，但是要注意存放的数字。
     * 存储的 double 并非准确值（40 可能存储为 39.999999964），将造成错误的结果。
     * 运算之前先加 0.01，确保小数点后一位是准确值，再进行强制转换类型处理。
     *
     * @param index 指数，一位小数有效
     * @return 字符串类型的指数，40.3 返回 40 + 0.3，39.7 返回 40 - 0.3
     */
    private String index2Str(double index) {
        index += 0.01;
        int integerPart = (int) index;
        int decimalPart = ((int) (index * 10)) % 10;
        if (decimalPart < 5) {
            return integerPart + " + 0." + decimalPart;
        } else {
            integerPart++;
            decimalPart = 10 - decimalPart;
            return integerPart + " - 0." + decimalPart;
        }
    }

}