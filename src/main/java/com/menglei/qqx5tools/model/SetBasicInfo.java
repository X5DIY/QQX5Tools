package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.SettingsAndUtils.QQX5MapType;
import com.menglei.qqx5tools.bean.QQX5MapInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static com.menglei.qqx5tools.SettingsAndUtils.getInfo;
import static com.menglei.qqx5tools.SettingsAndUtils.logError;

/**
 * 该类主要有三个功能。
 * 1.存储每一个按键到给定的 XMLInfo a 中，长条也进行拆分；以及存储一些基础信息
 * 2.存储所有位置的爆点描述
 * 3.存储歌曲首字母和等级
 */
class SetBasicInfo {

    private SetBasicInfo(QQX5MapInfo mapInfo) {
        this.isFirstNote = true;
        this.isFirstST = true;
        this.mapInfo = mapInfo;
    }

    private final QQX5MapInfo mapInfo;
    private boolean isFirstNote;// 是否为 a段
    private boolean isFirstST;// 是否为 中场st
    private int note1Box;
    private int st1Box;
    private int note2Box;
    private int st2Box;
    private boolean[] resetBoxFlag;// 标记为 true，说明这个位置的按键数字重置为 1
    private int[] noteNum1;// 泡泡按键数字
    private int[][] noteNum2;// 泡泡按键数字
    private int[][] X;// 泡泡按键 x 坐标
    private int[][] Y;
    private int[][] angle;// 弦月按键角度

    private void process() {
        //setNote(mapInfo);
        //setDescribe(mapInfo);
    }


    private void setPinball(QQX5MapInfo mapInfo) {
        int[] seriesNum = new int[5000];// 存储连点是第几个
        try {
            BufferedReader br = new BufferedReader(new FileReader(xml));
            String s;
            while ((s = br.readLine()) != null) {// xml文件的任意一行已经放入s
                if (basic(mapInfo, s)) {
                    continue;
                }
                if (s.contains("note_type=\"PinballSingle") || s.contains("note_type=\"PinballSeries")) {// 如果是单点或连点
                    int EndArea = pinball2Int(getInfo(s, "EndArea=\"", "\" MoveTime=\""));
                    int boxNum = getBox(s, mapInfo, 21);
                    mapInfo.track[EndArea][boxNum] = 1;
                    mapInfo.isLongNoteStart[EndArea][boxNum] = false;
                    mapInfo.isLongNoteEnd[EndArea][boxNum] = false;
                    if (s.contains("PinballSeries")) {// 如果是连点
                        int noteID = Integer.parseInt(getInfo(s, "Note ID=\"", "\" note_type=\""));
                        mapInfo.noteType[EndArea][boxNum] = seriesNum[noteID] + 1;
                        String Son = getInfo(s, "\" Son=\"", "\" EndArea=\"");
                        if (!Son.equals("")) {
                            seriesNum[Integer.parseInt(Son)] = seriesNum[noteID] + 1;
                        }
                    }
                } else if (s.contains("note_type=\"PinballSlip\"")) {// 如果是滑键或白球
                    int EndArea = pinball2Int(getInfo(s, "EndArea=\"", "\" MoveTime=\""));
                    int boxNum = getBox(s, mapInfo, 21);
                    String Son = getInfo(s, "\" Son=\"", "\" EndArea=\"");
                    // Son 有可能为空，不能直接转 int
                    if (!Son.equals("")) {// Son 中有数据说明是滑键
                        mapInfo.track[EndArea][boxNum] = 1;
                        mapInfo.isLongNoteStart[EndArea][boxNum] = false;
                        mapInfo.isLongNoteEnd[EndArea][boxNum] = false;
                        mapInfo.noteType[EndArea][boxNum] = -1;
                    } else {// 无数据是白球
                        mapInfo.track[EndArea][boxNum] = 2;
                        mapInfo.isLongNoteStart[EndArea][boxNum] = false;
                        mapInfo.isLongNoteEnd[EndArea][boxNum] = false;
                    }
                } else if (s.contains("note_type=\"PinballLong\"")) {// 如果是长条
                    int EndArea = pinball2Int(getInfo(s, "EndArea=\"", "\" MoveTime=\""));
                    int boxNum = getBox(s, mapInfo, 22);
                    int endBoxNum = getBox(s, mapInfo, 23);
                    setCommonLong(mapInfo.track[EndArea], boxNum, endBoxNum);
                    mapInfo.isLongNoteStart[EndArea][boxNum] = true;
                    mapInfo.isLongNoteEnd[EndArea][endBoxNum] = true;
                }
            }
            br.close();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void setBubble(QQX5MapInfo mapInfo) {
        int type = -1;
        int boxNum = 0;
        int endBoxNum = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(xml));
            String s;
            while ((s = br.readLine()) != null) {// xml文件的任意一行已经放入s
                if (basic(mapInfo, s)) {
                    continue;
                }
                if (s.contains("<Pos Bar=\"")) {// 如果是数字重置
                    boxNum = getBox(s, mapInfo, 31);
                    if (boxNum >= 0 && boxNum < this.resetBoxFlag.length) {
                        this.resetBoxFlag[boxNum] = true;
                    }
                } else if (s.contains("Type=\"0\"")) {// 如果是单点
                    type = 0;
                    boxNum = getBox(s, mapInfo, 32);
                } else if (s.contains("Type=\"1\"")) {// 如果是绿条
                    type = 1;
                    boxNum = getBox(s, mapInfo, 32);
                    endBoxNum = getBox(s, mapInfo, 33);
                } else if (s.contains("Type=\"2\"")) {// 如果是蓝条
                    type = 2;
                    boxNum = getBox(s, mapInfo, 32);
                    endBoxNum = getBox(s, mapInfo, 33);
                } else if (s.contains("<ScreenPos")) {// 如果是按键位置信息
                    int x = Integer.parseInt(getInfo(s, "<ScreenPos x=\"", "\" y=\""));
                    int y;
                    if (this.resetBoxFlag[boxNum]) {
                        this.noteNum1[boxNum] = 1;
                    } else {
                        for (int box = boxNum - 1; box >= note1Box; box--) {
                            if (this.noteNum1[box] != 0) {
                                this.noteNum1[boxNum] = this.noteNum1[box] + 1;
                                break;
                            } else if (this.resetBoxFlag[box]) {
                                this.noteNum1[boxNum] = 1;
                                break;
                            }
                        }
                    }
                    switch (type) {
                        case 0:
                            y = Integer.parseInt(getInfo(s, "\" y=\"", "\">"));
                            setBubbleSingle(mapInfo, boxNum, x, y);
                            break;
                        case 1:
                            y = Integer.parseInt(getInfo(s, "\" y=\"", "\" />"));
                            setBubbleLong(mapInfo, boxNum, endBoxNum, x, y, false);
                            break;
                        case 2:
                            y = Integer.parseInt(getInfo(s, "\" y=\"", "\" />"));
                            setBubbleLong(mapInfo, boxNum, endBoxNum, x, y, true);
                            break;
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void setCrescent(QQX5MapInfo mapInfo) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(xml));
            String s;
            while ((s = br.readLine()) != null) {// xml文件的任意一行已经放入s
                if (basic(mapInfo, s)) {
                    continue;
                }
                if (s.contains("note_type=\"short\"")) {// 如果是单点
                    int boxNum = getBox(s, mapInfo, 41);
                    int angle = Integer.parseInt(getInfo(s, "track=\"", "\" note_type=\""));
                    setCrescentSingle(mapInfo, boxNum, angle, false);
                } else if (s.contains("note_type=\"pair\"")) {// 如果是双点
                    // 实际上，这个完全可以写成两个单点
                    // 没办法，毕竟官谱有这玩意，只能加上了
                    int boxNum = getBox(s, mapInfo, 41);
                    int angle1 = Integer.parseInt(getInfo(s, "track=\"", "\" target_track=\""));
                    int angle2 = Integer.parseInt(getInfo(s, "\" target_track=\"", "\" note_type=\""));
                    setCrescentSingle(mapInfo, boxNum, angle1, false);
                    setCrescentSingle(mapInfo, boxNum, angle2, false);
                } else if (s.contains("note_type=\"light\"")) {// 如果是滑点
                    int boxNum = getBox(s, mapInfo, 41);
                    int angle = Integer.parseInt(getInfo(s, "track=\"", "\" note_type=\""));
                    setCrescentSingle(mapInfo, boxNum, angle, true);
                } else if (s.contains("note_type=\"long\"")) {// 如果是长条
                    int boxNum = getBox(s, mapInfo, 41);
                    int angle = Integer.parseInt(getInfo(s, "track=\"", "\" note_type=\""));
                    int length = Integer.parseInt(getInfo(s, "\" length=\"", "\" />")) / 2;
                    // length 以 pos 为单位，所以要除以2，转为以 box 为单位
                    setCrescentLong(mapInfo, boxNum, boxNum + length, angle);
                } else if (s.contains("note_type=\"slip\"")) {// 如果是滑条
                    int boxNum = getBox(s, mapInfo, 41);
                    int angle1 = Integer.parseInt(getInfo(s, "track=\"", "\" target_track=\""));
                    String Angle2 = getInfo(s, "\" target_track=\"", "\" note_type=\"");
                    // Angle2 可能包含"," 需要拆分成多个
                    String Length = getInfo(s, "\" length=\"", "\" />");
                    // Length 同样需要拆分
                    setCrescentSlip(mapInfo, boxNum, angle1, Angle2, Length, 0);
                }
            }
            br.close();
        } catch (IOException e) {
            logError(e);
        }
    }


    /**
     * 从基础信息行录入信息
     *
     * @param a 谱面对象
     * @param s 基础信息行
     * @return 是否为基础信息行
     */
    private boolean basic(String s) {
        boolean contains = false;
        if (s.contains("Title")) {
            mapInfo.title = getInfo(s, "<Title>", "</Title>");
            contains = true;
        } else if (s.contains("Artist")) {// 歌手、作曲家
            mapInfo.artist = getInfo(s, "<Artist>", "</Artist>");
            contains = true;
        } else if (s.contains("FilePath")) {// bgm 路径，官谱将包含序号，方便后续找谱面文件
            mapInfo.bgmFilePath = getInfo(s, "<FilePath>audio/bgm/", "</FilePath>");
            // 自制谱面的这部分并不一定全为数字，所以不能转 int
            contains = true;
        } else if (s.contains("Section type=\"note\"")) {
            if (isFirstNote) {// 歌曲 a 段开始 bar，默认 b 段开始也是这里
                mapInfo.note1Bar = Integer.parseInt(getInfo(s, "startbar=\"", "\" endbar=\""));
                this.note1Box = mapInfo.getNote1StartBox();
                mapInfo.note2Bar = mapInfo.note1Bar;
                this.note2Box = mapInfo.getNote2StartBox();
                isFirstNote = false;
            } else {// 歌曲 b 段开始 bar
                mapInfo.note2Bar = Integer.parseInt(getInfo(s, "startbar=\"", "\" endbar=\""));
                this.note2Box = mapInfo.getNote2StartBox();
            }
            contains = true;
        } else if (s.contains("Section type=\"showtime\"")) {
            if (isFirstST) {// 中场 st 开始 bar，默认结尾 st 开始也是这里
                mapInfo.st1Bar = Integer.parseInt(getInfo(s, "startbar=\"", "\" endbar=\""));
                this.st1Box = mapInfo.getShowtime1StartBox();
                mapInfo.st2Bar = mapInfo.st1Bar;
                this.st2Box = mapInfo.getShowtime2StartBox();
                isFirstST = false;
                newAllArray(mapInfo);// 初始化各个数组
            } else {// 结尾 st 开始 bar
                mapInfo.st2Bar = Integer.parseInt(getInfo(s, "startbar=\"", "\" endbar=\""));
                this.st2Box = mapInfo.getShowtime2StartBox();
                newAllArray(mapInfo);
            }
            contains = true;
        }
        return contains;
    }

    private void newAllArray(QQX5MapInfo mapInfo) {
        if (mode == QQX5MapType.BUBBLE) {
            this.resetBoxFlag = new boolean[st2Box + 5];
            this.noteNum1 = new int[st2Box + 5];
            this.noteNum1[note1Box] = 1;// 赋初值，防止开头没有数字重置标记
            this.noteNum2 = new int[5][st2Box + 5];
            this.X = new int[5][st2Box + 5];
            this.Y = new int[5][st2Box + 5];
        }
        if (mode == QQX5MapType.CRESCENT) {
            this.angle = new int[5][st2Box + 5];
        }
        mapInfo.combo = new int[st2Box + 5];
        mapInfo.track = new int[5][st2Box + 5];
        mapInfo.isLongNoteStart = new boolean[5][st2Box + 5];
        mapInfo.isLongNoteEnd = new boolean[5][st2Box + 5];
        mapInfo.noteType = new int[5][st2Box + 5];
        mapInfo.boxDescribe = new String[4][st2Box + 5];
    }

    /**
     * 传入谱面文件某行，返回按键对应的数组位置
     * 由于截取得到的是 pos，所以最后还要 /2，才能转为 box
     * 处理方式与定义 note1Box 等相同，同样去掉开头，再加 4 box
     *
     * @param s     谱面文件某行
     * @param a     谱面对象
     * @param state 截取类型
     * @return 按键对应的数组位置
     * @throws IllegalArgumentException 如果传入错误的截取方式
     */
    private int getBox(String s, int state) throws IllegalArgumentException {
        int bar;
        int box;
        switch (state) {
            case 11:// 星动单点/长条开始
                bar = Integer.parseInt(getInfo(s, "<Note Bar=\"", "\" Pos=\""));
                box = Integer.parseInt(getInfo(s, "\" Pos=\"", "\" from_track=\"")) / 2;
                break;
            case 12:// 星动长条结尾
                bar = Integer.parseInt(getInfo(s, "EndBar=\"", "\" EndPos=\""));
                box = Integer.parseInt(getInfo(s, "\" EndPos=\"", "\" />")) / 2;
                break;
            case 13:// 星动滑键
                bar = Integer.parseInt(getInfo(s, "<Note Bar=\"", "\" Pos=\""));
                box = Integer.parseInt(getInfo(s, "\" Pos=\"", "\" target_track=\"")) / 2;
                break;
            case 21:// 弹珠单点/滑键/白球/连点
                bar = Integer.parseInt(getInfo(s, "Bar=\"", "\" Pos=\""));
                box = Integer.parseInt(getInfo(s, "\" Pos=\"", "\" Son=\"")) / 2;
                break;
            case 22:// 弹珠长条开始
                bar = Integer.parseInt(getInfo(s, "Bar=\"", "\" Pos=\""));
                box = Integer.parseInt(getInfo(s, "\" Pos=\"", "\" EndBar=\"")) / 2;
                break;
            case 23:// 弹珠长条结尾
                bar = Integer.parseInt(getInfo(s, "\" EndBar=\"", "\" EndPos=\""));
                box = Integer.parseInt(getInfo(s, "\" EndPos=\"", "\" Son=\"")) / 2;
                break;
            case 31:// 泡泡数字重置
                bar = Integer.parseInt(getInfo(s, "<Pos Bar=\"", "\" BeatPos=\""));
                box = Integer.parseInt(getInfo(s, "\" BeatPos=\"", "\" />")) / 2;
                break;
            case 32:// 泡泡单点/绿条开始/蓝条开始
                bar = Integer.parseInt(getInfo(s, "<Note Bar=\"", "\" BeatPos=\""));
                box = Integer.parseInt(getInfo(s, "\" BeatPos=\"", "\" Track=\"")) / 2;
                break;
            case 33:// 泡泡绿条结尾/蓝条结尾
                bar = Integer.parseInt(getInfo(s, "EndBar=\"", "\" EndPos=\""));
                box = Integer.parseInt(getInfo(s, "\" EndPos=\"", "\" ID=\"")) / 2;
                break;
            case 41:// 弦月
                bar = Integer.parseInt(getInfo(s, "<Note Bar=\"", "\" Pos=\""));
                box = Integer.parseInt(getInfo(s, "\" Pos=\"", "\" track=\"")) / 2;
                break;
            default:
                throw new IllegalArgumentException("Can not find getBox type.");
        }
        return (bar - mapInfo.note1Bar) * mapInfo.boxPerBar + box + 4;
    }

    /**
     * 将星动的字符串轨道与 track0-4 对应
     *
     * @param s    星动的字符串轨道
     * @param is4k 是否为 4k星动
     * @return 轨道数字
     */
    private int idol2Int(String s, boolean is4k) {
        if (is4k) {
            switch (s) {
                case "Left2":
                    return 0;
                case "Left1":
                    return 1;
                case "Right1":
                    return 2;
                case "Right2":
                    return 3;
                default:
                    throw new IllegalArgumentException("In idol_ConvertToInt, track is not correct.");
            }
        } else {
            switch (s) {
                case "Left2":
                    return 0;
                case "Left1":
                    return 1;
                case "Middle":
                    return 2;
                case "Right1":
                    return 3;
                case "Right2":
                    return 4;
                default:
                    throw new IllegalArgumentException("In idol_ConvertToInt, track is not correct.");
            }
        }
    }

    private int pinball2Int(String s) throws IllegalArgumentException {
        switch (s) {
            case "1":
            case "2":
            case "1|2":
            case "2|1":
            case "3|1":
                return 0;
            case "":
                return 1;
            case "3":
            case "4":
            case "3|4":
            case "4|3":
            case "3|3":
                return 2;
            default:
                throw new IllegalArgumentException("In pinball_ConvertToInt, track is not correct.");
        }
    }

    /**
     * 检查长条起始是否为滑键结尾
     * 为0 说明非滑键结尾，该格子有长条分数；反之无长条分数
     * 长条结尾必然有一个长条键
     *
     * @param track     长条所在的轨道
     * @param boxNum    长条开始 box
     * @param endBoxNum 长条结尾 box
     */
    private void setCommonLong(int[] track, int boxNum, int endBoxNum) {
        for (int box = boxNum; box < endBoxNum; box += 2) {
            if (track[box] == 0) {
                track[box] = 3;
            }
        }
        if (track[endBoxNum] == 0) {
            track[endBoxNum] = 3;
        }
    }

    /**
     * 泡泡单点
     * 泡泡可能存在按键重叠的问题
     *
     * @param a      泡泡对象
     * @param boxNum 单点所在 box
     * @param x      X 坐标
     * @param y      Y 坐标
     */
    private void setBubbleSingle(int boxNum, int x, int y) throws IllegalArgumentException {
        for (int track = 0; track < 5; track++) {
            if (mapInfo.track[track][boxNum] == 0) {
                mapInfo.track[track][boxNum] = 1;
                this.X[track][boxNum] = x;
                this.Y[track][boxNum] = y;
                this.noteNum2[track][boxNum] = this.noteNum1[boxNum];
                break;
            } else if (track == 4) {
                throw new IllegalArgumentException("6 or over notes in bubble at one time.");
            }
        }
    }

    /**
     * 泡泡绿条/蓝条
     * 泡泡可能存在按键重叠的问题
     * 泡泡长条只能是 8box 的倍数，否则无法选择长条类型
     *
     * @param a         泡泡对象
     * @param boxNum    开始 box
     * @param endBoxNum 结尾 box
     * @param x         X 坐标
     * @param y         Y 坐标
     * @param isBlue    是否为蓝条
     */
    private void setBubbleLong(int boxNum, int endBoxNum,
                               int x, int y, boolean isBlue) throws IllegalArgumentException {
        for (int box = boxNum; box <= endBoxNum; box += 4) {
            for (int track = 0; track < 5; track++) {
                if (mapInfo.track[track][box] == 0) {
                    if (isBlue) {
                        mapInfo.track[track][box] = 1;
                        mapInfo.noteType[track][box] = 1;
                    } else {
                        mapInfo.track[track][box] = 3;
                    }
                    if (box == boxNum) {
                        mapInfo.isLongNoteStart[track][box] = true;
                    } else if (box == endBoxNum) {
                        mapInfo.isLongNoteEnd[track][box] = true;
                    }
                    this.X[track][box] = x;
                    this.Y[track][box] = y;
                    this.noteNum2[track][box] = this.noteNum1[boxNum];
                    break;
                } else if (track == 4) {
                    throw new IllegalArgumentException("6 or over notes in bubble at one time.");
                }
            }
        }
    }

    /**
     * 弦月单点/滑点
     * 弦月轨道过多，也仿照泡泡进行处理
     *
     * @param a       弦月对象
     * @param boxNum  单点/滑点所在 box
     * @param angle   按键角度
     * @param isLight true 表示滑点，false 表示单点
     */
    private void setCrescentSingle(int boxNum, int angle, boolean isLight) throws IllegalArgumentException {
        for (int track = 0; track < 5; track++) {
            if (mapInfo.track[track][boxNum] == 0) {
                if (isLight) {
                    mapInfo.track[track][boxNum] = 4;
                } else {
                    mapInfo.track[track][boxNum] = 1;
                }
                this.angle[track][boxNum] = angle;
                break;
            } else if (track == 4) {
                throw new IllegalArgumentException("6 or over notes in crescent at one time.");
            }
        }
    }

    /**
     * 弦月长条
     *
     * @param a         弦月对象
     * @param boxNum    开始 box
     * @param endBoxNum 结束 box
     * @param angle     按键角度
     */
    private void setCrescentLong(int boxNum, int endBoxNum, int angle) {
        for (int box = boxNum; box < endBoxNum; box += 2) {
            for (int track = 0; track < 5; track++) {
                if (mapInfo.track[track][box] == 0) {
                    mapInfo.track[track][box] = 3;
                    this.angle[track][box] = angle;
                    if (box == boxNum) {
                        mapInfo.isLongNoteStart[track][box] = true;
                    }
                    break;
                } else if (track == 4) {
                    throw new IllegalArgumentException("6 or over notes in crescent at one time.");
                }
            }
        }
        for (int track = 0; track < 5; track++) {
            if (mapInfo.track[track][endBoxNum] == 0) {
                mapInfo.track[track][endBoxNum] = 3;
                this.angle[track][endBoxNum] = angle;
                mapInfo.isLongNoteEnd[track][endBoxNum] = true;
                break;
            } else if (track == 4) {
                throw new IllegalArgumentException("6 or over notes in crescent at one time.");
            }
        }
    }

    /**
     * 弦月滑条
     *
     * @param a             弦月对象
     * @param boxNum        开始 box
     * @param angle1        滑条起始角度
     * @param Angle2        滑条终止角度合集
     * @param Length        滑条长度合集
     * @param pastDirection 之前的滑条方向，2右滑，-2左滑，第一次传入0
     */
    private void setCrescentSlip(int boxNum, int angle1,
                                 String Angle2, String Length, int pastDirection) {
        int angle2;
        int length;
        boolean isFirst = pastDirection == 0;
        boolean isEnd = false;// isEnd 等价于 Angle2.contains(",") 或 Length.contains(",")
        if (Angle2.contains(",")) {
            angle2 = Integer.parseInt(Angle2.substring(0, Angle2.indexOf(",")));
            length = Integer.parseInt(Length.substring(0, Length.indexOf(","))) / 2;
        } else {
            angle2 = Integer.parseInt(Angle2);
            length = Integer.parseInt(Length) / 2;
            isEnd = true;
        }
        // 处理非整个滑条结尾的部分
        int endBoxNum = boxNum + length;
        for (int box = boxNum; box < endBoxNum; box += 2) {
            for (int track = 0; track < 5; track++) {
                if (mapInfo.track[track][box] == 0) {
                    mapInfo.track[track][box] = 3;
                    if (box == boxNum && !isFirst) {// 如果是滑条转折点
                        if (angle1 < angle2) {// 如果现在是右滑
                            mapInfo.noteType[track][box] = pastDirection + 1;
                        } else {
                            mapInfo.noteType[track][box] = pastDirection - 1;
                        }
                    } else {// 如果是滑条中间，或者是整个滑条的开头
                        if (angle1 < angle2) {
                            mapInfo.noteType[track][box] = 2;
                        } else {
                            mapInfo.noteType[track][box] = -2;
                        }
                    }
                    this.angle[track][box] = angle1;
                    if (isFirst && box == boxNum) {
                        mapInfo.isLongNoteStart[track][box] = true;
                    }
                    break;
                } else if (track == 4) {
                    throw new IllegalArgumentException("6 or over notes in crescent at one time.");
                }
            }
        }
        // 处理整个滑条结尾
        if (isEnd) {
            for (int track = 0; track < 5; track++) {
                if (mapInfo.track[track][endBoxNum] == 0) {
                    mapInfo.track[track][endBoxNum] = 3;
                    if (angle1 < angle2) {
                        mapInfo.noteType[track][endBoxNum] = 2;
                    } else {// 因为是滑条，所以 angle1 和 angle2 必不相等
                        mapInfo.noteType[track][endBoxNum] = -2;
                    }
                    this.angle[track][endBoxNum] = angle1;
                    mapInfo.isLongNoteEnd[track][endBoxNum] = true;
                    break;
                } else if (track == 4) {
                    throw new IllegalArgumentException("6 or over notes in crescent at one time.");
                }
            }
        } else {
            setCrescentSlip(mapInfo, endBoxNum, angle2,
                    Angle2.substring(Angle2.indexOf(",") + 1),
                    Length.substring(Length.indexOf(",") + 1),
                    angle1 < angle2 ? 2 : -2);
        }
    }





    /* -- part2  -- */

    private void setDescribe(QQX5MapInfo mapInfo) {
        for (int box = note1Box; box <= st2Box; box++) {
            if (box > st1Box && box < note2Box) {
                continue;
            }
            String basicStart = "^";// 正常操作，作为爆气开始位置的描述
            String basicEnd = "^";
            String coolStart = "^";
            String coolEnd = "^";

            // 首先将按键描述放上去
            switch (mapInfo.getType().toString()) {
                case "星动":
                    basicStart = basicIdolDescribe(mapInfo, box, basicStart, true);
                    basicEnd = basicIdolDescribe(mapInfo, box, basicEnd, false);
                    coolStart = coolIdolDescribe(mapInfo, box, basicStart, true);
                    coolEnd = coolIdolDescribe(mapInfo, box, basicEnd, false);
                    break;
                case "弹珠":
                    basicStart = basicPinballDescribe(mapInfo, box, basicStart, true);
                    basicEnd = basicPinballDescribe(mapInfo, box, basicEnd, false);
                    coolStart = coolPinballDescribe(mapInfo, box, basicStart, true);
                    coolEnd = coolPinballDescribe(mapInfo, box, basicEnd, false);
                    break;
                case "泡泡":
                    basicStart = basicBubbleDescribe(mapInfo, box, basicStart, true);
                    basicEnd = basicBubbleDescribe(mapInfo, box, basicEnd, false);
                    coolStart = coolBubbleDescribe(mapInfo, box, basicStart, true);
                    coolEnd = coolBubbleDescribe(mapInfo, box, basicEnd, false);
                    break;
                case "弦月":
                    basicStart = basicCrescentDescribe(mapInfo, box, basicStart, true);
                    basicEnd = basicCrescentDescribe(mapInfo, box, basicEnd, false);
                    coolStart = coolCrescentDescribe(mapInfo, box, basicStart, true);
                    coolEnd = coolCrescentDescribe(mapInfo, box, basicEnd, false);
                    break;
            }

            // 然后将AB段指示加到最前面
            String part;
            if (box <= st1Box) {
                part = "A - ";
            } else {
                part = "B - ";
            }

            // 最后将得到的描述放入a中
            mapInfo.setBoxDescribe(false, true, box, part + basicStart);
            mapInfo.setBoxDescribe(false, false, box, part + basicEnd);
            mapInfo.setBoxDescribe(true, true, box, part + coolStart);
            mapInfo.setBoxDescribe(true, false, box, part + coolEnd);
        }
    }

    /**
     * 如果某个位置没有按键，
     * 则作为爆点开始描述时，应往后推，直至有描述；
     * 反之则往前推，直至有描述
     *
     * @param isStart 是否为爆点开始
     * @param box     要得到爆点描述的位置
     * @return 移动方向及长度
     */
    private int boxChange(boolean isStart, int box) {
        if (isStart && box <= st2Box) {
            return 1;
        } else if (!isStart && box >= note1Box) {
            return -1;
        }
        return 0;
    }

    private String basicPinballDescribe(int box, String describe, boolean isStart) {
        do {
            StringBuilder s = new StringBuilder(describe);
            for (int track = 0; track < 3; track++) {
                if (mapInfo.track[track][box] == 1) {
                    switch (mapInfo.noteType[track][box]) {
                        case 0:
                            s.append(pinball2Str(track)).append("单点、");
                            break;
                        case -1:
                            s.append(pinball2Str(track)).append("滑键、");
                            break;
                        default:
                            s.append(pinball2Str(track))
                                    .append("连点第 ").append(mapInfo.noteType[track][box]).append(" 个、");
                    }
                } else if (mapInfo.track[track][box] == 2) {
                    s.append(pinball2Str(track)).append("白球、");
                } else if (mapInfo.track[track][box] == 3) {
                    if (mapInfo.isLongNoteStart[track][box]) {
                        s.append(pinball2Str(track)).append("长条开始、");
                    } else if (mapInfo.isLongNoteEnd[track][box]) {
                        s.append(pinball2Str(track)).append("长条结尾、");
                    } else {
                        s.append(pinball2Str(track)).append("长条中间、");
                    }
                }
            }
            describe = s.toString();
            if (boxChange(isStart, box) == 0) {
                break;
            } else {
                box += boxChange(isStart, box);
            }
        } while (describe.equals("^"));
        if (describe.equals("^")) {
            return describe;
        } else {
            return describe.substring(1, describe.length() - 1);
        }
    }

    private String pinball2Str(int track) throws IllegalArgumentException {
        switch (track) {
            case 0:
                return "左边";
            case 1:
                return "随机位置";
            case 2:
                return "右边";
            default:
                throw new IllegalArgumentException("In pinball_ConvertToString, track is not 0-2.");
        }
    }

    private String coolPinballDescribe(int box, String describe, boolean isStart) {
        StringBuilder s;
        if (isStart) {
            s = new StringBuilder();
            for (int i = -4; i <= -1; i++) {
                for (int track = 0; track < 3; track++) {
                    if (mapInfo.track[track][box + i] == 1) {
                        switch (mapInfo.noteType[track][box + i]) {
                            case 0:
                                s.append(pinball2Str(track)).append("单点（cool）、");
                                break;
                            case -1:
                                s.append(pinball2Str(track)).append("滑键（cool）、");
                                break;
                            default:
                                s.append(pinball2Str(track))
                                        .append("连点第 ").append(mapInfo.noteType[track][box + i]).append(" 个（cool）、");
                        }
                    } else if (mapInfo.track[track][box + i] == 2) {
                        s.append(pinball2Str(track)).append("白球（cool）、");
                    }
                }
            }
            describe = s.append(describe).toString();
        } else {
            s = new StringBuilder(describe);
            for (int i = 1; i <= 4; i++) {
                for (int track = 0; track < 3; track++) {
                    if (mapInfo.track[track][box + i] == 1) {
                        switch (mapInfo.noteType[track][box + i]) {
                            case 0:
                                s.append("、").append(pinball2Str(track)).append("单点（cool）");
                                break;
                            case -1:
                                s.append("、").append(pinball2Str(track)).append("滑键（cool）");
                                break;
                            default:
                                s.append("、").append(pinball2Str(track))
                                        .append("连点第 ").append(mapInfo.noteType[track][box + i]).append(" 个（cool）");
                        }
                    } else if (mapInfo.track[track][box + i] == 2) {
                        s.append("、").append(pinball2Str(track)).append("白球（cool）");
                    }
                }
            }
            describe = s.toString();
        }
        return describe;
    }


    private String basicBubbleDescribe(int box, String describe, boolean isStart) {
        do {
            StringBuilder s = new StringBuilder(describe);
            for (int track = 0; track < 5; track++) {
                if (mapInfo.track[track][box] == 1) {
                    if (mapInfo.noteType[track][box] == 0) {
                        s.append(bubbleDirection(track, box))
                                .append("数字 ").append(this.noteNum2[track][box]).append(" 单点、");
                    } else {
                        s.append(bubbleDirection(track, box)).append("数字 ").append(this.noteNum2[track][box]);
                        if (mapInfo.isLongNoteStart[track][box]) {
                            s.append(" 蓝条开始、");
                        } else if (mapInfo.isLongNoteEnd[track][box]) {
                            s.append(" 蓝条结尾、");
                        } else {
                            s.append(" 蓝条中间、");
                        }
                    }
                } else if (mapInfo.track[track][box] == 3) {
                    s.append(bubbleDirection(track, box)).append("数字 ").append(this.noteNum2[track][box]);
                    if (mapInfo.isLongNoteStart[track][box]) {
                        s.append(" 绿条开始、");
                    } else if (mapInfo.isLongNoteEnd[track][box]) {
                        s.append(" 绿条结尾、");
                    } else {
                        s.append(" 绿条中间、");
                    }
                }
            }
            describe = s.toString();
            if (boxChange(isStart, box) == 0) {
                break;
            } else {
                box += boxChange(isStart, box);
            }
        } while (describe.equals("^"));
        if (describe.equals("^")) {
            return describe;
        } else {
            return describe.substring(1, describe.length() - 1);
        }
    }

    private String bubbleDirection(int track, int box) {
        int basicX = 180;
        int basicY = 80;
        int x = this.X[track][box];
        int y = this.Y[track][box];
        if (x >= basicX) {
            if (y >= basicY) {
                return "右上角";
            } else if (y <= -basicY) {
                return "右下角";
            } else {
                return "右边";
            }
        } else if (x <= -basicX) {
            if (y >= basicY) {
                return "左上角";
            } else if (y <= -basicY) {
                return "左下角";
            } else {
                return "左边";
            }
        } else {
            if (y >= basicY) {
                return "上面";
            } else if (y <= -basicY) {
                return "下面";
            } else {
                return "中间";
            }
        }
    }

    private String coolBubbleDescribe(int box, String describe, boolean isStart) {
        StringBuilder s;
        if (isStart) {
            s = new StringBuilder();
            for (int i = -4; i <= -1; i++) {
                for (int track = 0; track < 5; track++) {
                    if (mapInfo.track[track][box + i] == 1 && mapInfo.noteType[track][box + i] == 0) {
                        s.append(bubbleDirection(track, box))
                                .append("数字 ").append(this.noteNum2[track][box + i]).append(" 单点（cool）、");
                    }
                }
            }
            describe = s.append(describe).toString();
        } else {
            s = new StringBuilder(describe);
            for (int i = 1; i <= 4; i++) {
                for (int track = 0; track < 5; track++) {
                    if (mapInfo.track[track][box + i] == 1 && mapInfo.noteType[track][box + i] == 0) {
                        s.append("、").append(bubbleDirection(track, box))
                                .append("数字 ").append(this.noteNum2[track][box + i]).append(" 单点（cool）");
                    }
                }
            }
            describe = s.toString();
        }
        return describe;
    }


    private String basicCrescentDescribe(int box, String describe, boolean isStart) {
        do {
            StringBuilder s = new StringBuilder(describe);
            for (int track = 0; track < 5; track++) {
                if (mapInfo.track[track][box] == 1) {// 如果为单点
                    s.append(crescent2Str(track, box)).append("单点、");
                } else if (mapInfo.track[track][box] == 4) {// 如果为滑点
                    s.append(crescent2Str(track, box)).append("滑点、");
                } else if (mapInfo.track[track][box] == 3) {// 如果为长条或滑条
                    if (mapInfo.noteType[track][box] == 0) {// 长条三种
                        if (mapInfo.isLongNoteStart[track][box]) {
                            s.append(crescent2Str(track, box)).append("长条开始、");
                        } else if (mapInfo.isLongNoteEnd[track][box]) {
                            s.append(crescent2Str(track, box)).append("长条结尾、");
                        } else {
                            s.append(crescent2Str(track, box)).append("长条中间、");
                        }
                    } else {// 滑条四种，开头中间结尾拐
                        s.append(crescent2Str(track, box));
                        int type = mapInfo.noteType[track][box];
                        if (type < 0) {
                            s.append("左滑条");
                        } else {
                            s.append("右滑条");
                        }
                        if (type == -3) {
                            s.append("左拐、");
                        } else if (type == -1) {
                            s.append("右拐、");
                        } else if (type == 1) {
                            s.append("左拐、");
                        } else if (type == 3) {
                            s.append("右拐、");
                        } else {
                            if (mapInfo.isLongNoteStart[track][box]) {
                                s.append("开始、");
                            } else if (mapInfo.isLongNoteEnd[track][box]) {
                                s.append("结尾、");
                            } else {
                                s.append("中间、");
                            }
                        }
                    }
                }
            }
            describe = s.toString();
            if (boxChange(isStart, box) == 0) {
                break;
            } else {
                box += boxChange(isStart, box);
            }
        }
        while (describe.equals("^"));
        if (describe.equals("^")) {
            return describe;
        } else {
            return describe.substring(1, describe.length() - 1);
        }
    }

    private String crescent2Str(int track, int box) {
        int angle = this.angle[track][box];
        if (angle < -25) {
            return "左边";
        } else if (angle < 0) {
            return "左下";
        } else if (angle == 0) {
            return "中间";
        } else if (angle <= 25) {
            return "右下";
        } else {
            return "右边";
        }
    }

    private String coolCrescentDescribe(int box, String describe, boolean isStart) {
        StringBuilder s;
        if (isStart) {
            s = new StringBuilder();
            for (int i = -4; i <= -1; i++) {
                for (int track = 0; track < 5; track++) {
                    if (mapInfo.track[track][box + i] == 1) {
                        s.append(idol2Str(track)).append("单点（cool）、");
                    }
                }
            }
            describe = s.append(describe).toString();
        } else {
            s = new StringBuilder(describe);
            for (int i = 1; i <= 4; i++) {
                for (int track = 0; track < 5; track++) {
                    if (mapInfo.track[track][box + i] == 1) {
                        s.append("、").append(idol2Str(track)).append("单点（cool）");
                    }
                }
            }
            describe = s.toString();
        }
        return describe;
    }


}