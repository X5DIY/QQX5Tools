package com.menglei.qqx5tools.bean;

import com.menglei.qqx5tools.SettingsAndUtils.QQX5MapType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.Iterator;

import static com.menglei.qqx5tools.SettingsAndUtils.getInfo;
import static com.menglei.qqx5tools.SettingsAndUtils.logError;
import static com.menglei.qqx5tools.SettingsAndUtils.logInfo;

/**
 * @author MengLeiFudge
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class IdolMapInfo extends QQX5MapInfo {
    public IdolMapInfo(File xml) {
        super(xml, QQX5MapType.IDOL);
    }

    /* -- LevelInfo -- */

    private int trackCount;

    // 是否为 a段
    private boolean isFirstNote;
    // 是否为 中场st
    private boolean isFirstST;

    @Override
    public void setBasicInfo() {
        try {
            Element Level = new SAXReader().read(getXml()).getRootElement();

            Element LevelInfo = Level.element("LevelInfo");
            bpm = Double.parseDouble(LevelInfo.elementText("BPM"));
            assert "4".equals(LevelInfo.elementText("BeatPerBar"));
            assert "16".equals(LevelInfo.elementText("BeatLen"));
            enterTimeAdjust = Integer.parseInt(LevelInfo.elementText("EnterTimeAdjust"));
            notePreShow = (Double.parseDouble(LevelInfo.elementText("NotePreShow")));
            levelTime = Integer.parseInt(LevelInfo.elementText("LevelTime"));
            barAmount = Integer.parseInt(LevelInfo.elementText("BarAmount"));
            beginBarLen = Integer.parseInt(LevelInfo.elementText("BeginBarLen"));
            trackCount = Integer.parseInt(LevelInfo.elementText("TrackCount"));
            assert "4000".equals(LevelInfo.elementText("LevelPreTime"));
            if(LevelInfo.element("Star") != null){
                star = Integer.parseInt(LevelInfo.elementText("Star"));
            }

            Element MusicInfo = Level.element("MusicInfo");
            if (MusicInfo.element("Author") != null) {
                author = MusicInfo.elementText("Author");
            } else {
                author = "";
            }
            title = MusicInfo.elementText("Title");
            artist = MusicInfo.elementText("Artist");
            bgmFilePath = MusicInfo.elementText("FilePath");

            Element SectionSeq = Level.element("SectionSeq");
            Iterator<Element> SectionIterator = SectionSeq.elementIterator();
            while (SectionIterator.hasNext()) {
                Element Section = SectionIterator.next();
                String type = Section.attributeValue("type");
                String startBarStr = Section.attributeValue("startbar");
                int startBar = startBarStr == null ? 0 : Integer.parseInt(startBarStr);
                int endBar = Integer.parseInt(Section.attributeValue("endbar"));
                String mark = Section.attributeValue("mark");
                String param1 = Section.attributeValue("param1");
                Section section = new Section(type, startBar, endBar, mark, param1);
                switch (type) {
                    case "previous" -> {
                        assert endBar == 0;
                        previous = section;
                    }
                    case "begin" -> {
                        assert startBar == 1;
                        assert endBar == 4 || endBar == 8;
                        begin = section;
                    }
                    case "note" -> {
                        if (note1 == null) {
                            assert startBar == begin.endBar + 1;
                            note1 = section;
                        } else {
                            assert startBar == showtime1.endBar + 3;
                            note2 = section;
                        }
                    }
                    case "showtime" -> {
                        if (showtime1 == null) {
                            assert startBar == note1.endBar + 1;
                            assert endBar == startBar + 3;
                            showtime1 = section;
                        } else {
                            assert startBar == note2.endBar + 1;
                            assert endBar == startBar + 3;
                            showtime2 = section;
                        }
                    }
                    default -> throw new IllegalStateException("未知节点：SectionSeq -> " + type);
                }
            }
            assert previous != null;
            assert begin != null;
            assert note1 != null;
            assert showtime1 != null;
            assert (note2 == null && showtime2 == null) || (note2 != null && showtime2 != null);


            assert "64".equals(Level.element("IndicatorResetPos").attributeValue("PosNum"));

            Element NoteInfo = Level.element("NoteInfo").element("Normal");
            Iterator<Element> NoteIterator = NoteInfo.elementIterator();
            while (NoteIterator.hasNext()) {
                Element x = NoteIterator.next();
                if ("Note".equals(x.getName())) {
                    addNote(x);
                } else if ("CombineNote".equals(x.getName())) {
                    Iterator<Element> NoteIterator2 = x.elementIterator();
                    while (NoteIterator2.hasNext()) {
                        addNote(NoteIterator2.next());
                    }
                } else {
                    throw new IllegalStateException("未知节点：NoteInfo -> Normal -> " + x.getName());
                }
            }

            Element ActionSeq = Level.element("ActionSeq");
            assert "1".equals(ActionSeq.attributeValue("type"));
            Iterator<Element> ActionSeqIterator = ActionSeq.elementIterator();
            while (ActionSeqIterator.hasNext()) {
                Element Action = ActionSeqIterator.next();
                int start_bar = Integer.parseInt(Action.attributeValue("start_bar"));
                int dance_len = Integer.parseInt(Action.attributeValue("dance_len"));
                int seq_len = Integer.parseInt(Action.attributeValue("seq_len"));
                int level = Integer.parseInt(Action.attributeValue("level"));
                assert "".equals(Action.attributeValue("type"));
                Action action = new Action(start_bar, dance_len, seq_len, level);
                actionSeq.add(action);
            }

            Element CameraSeq = Level.element("CameraSeq");
            Iterator<Element> CameraSeqIterator = CameraSeq.elementIterator();
            while (CameraSeqIterator.hasNext()) {
                Element Camera = CameraSeqIterator.next();
                String name = Camera.attributeValue("name");
                int bar = Integer.parseInt(Camera.attributeValue("bar"));
                int pos = Integer.parseInt(Camera.attributeValue("pos"));
                int end_bar = Integer.parseInt(Camera.attributeValue("end_bar"));
                int end_pos = Integer.parseInt(Camera.attributeValue("end_pos"));
                Camera camera = new Camera(name, bar, pos, end_bar, end_pos);
                cameraSeq.add(camera);
            }

            Element DancerSort = Level.element("DancerSort");
            Iterator<Element> DancerSortIterator = DancerSort.elementIterator();
            while (DancerSortIterator.hasNext()) {
                dancerSort.add(Integer.parseInt(DancerSortIterator.next().getText()));
            }

            Element StageEffectSeq = Level.element("StageEffectSeq");
            Iterator<Element> StageEffectSeqIterator = StageEffectSeq.elementIterator();
            int index = 0;
            while (StageEffectSeqIterator.hasNext()) {
                Element effect = StageEffectSeqIterator.next();
                String name = effect.attributeValue("name");
                int bar = Integer.parseInt(effect.attributeValue("bar"));
                int length = Integer.parseInt(effect.attributeValue("length"));
                switch (index) {
                    case 0 -> {
                        assert "wutai_scene_global_script".equals(name);
                        assert bar == -1;
                        assert length == -1;
                    }
                    case 1 -> {
                        assert "wutai_scene_note_biaozhun_01".equals(name);
                        assert bar == note1.startBar;
                        assert length == note1.endBar - note1.startBar + 1;
                    }
                    case 2 -> {
                        assert "wutai_scene_note_biaozhun_01".equals(name);
                        if (note2 == null) {
                            assert bar == showtime1.endBar + 1;
                            assert length == -1;
                        } else {
                            assert bar == note2.startBar;
                            assert length == note2.endBar - note2.startBar + 1;
                        }
                    }
                    case 3 -> {
                        assert "wutai_scene_note_biaozhun_01".equals(name);
                        assert bar == showtime2.endBar + 1;
                        assert length == -1;
                    }
                    default ->  throw new IllegalStateException("未知节点：StageEffectSeq 含有超过四个effect");
                }
                index++;
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private void addNote(Element e) {
        switch (e.attributeValue("note_type")) {
            case "short" -> {
                logInfo("short");

            }
            // 滑键存到滑键结束位置的轨道，不与长条结尾的按键冲突
            case "slip" -> logInfo("slip");
            case "long" -> logInfo("long");
            default -> throw new IllegalStateException("未知键型：NoteInfo -> Normal -> type = " + e.attributeValue("note_type"));
        }
    }

//    IdolMapInfo mapInfo = null;
//
//    /**
//     * 从基础信息行录入信息
//     *
//     * @param a 谱面对象
//     * @param s 基础信息行
//     * @return 是否为基础信息行
//     */
//    private boolean basic(String s) {
//        boolean contains = false;
//        if (s.contains("Title")) {
//            mapInfo.title = getInfo(s, "<Title>", "</Title>");
//            contains = true;
//        } else if (s.contains("Artist")) {// 歌手、作曲家
//            mapInfo.artist = getInfo(s, "<Artist>", "</Artist>");
//            contains = true;
//        } else if (s.contains("FilePath")) {// bgm 路径，官谱将包含序号，方便后续找谱面文件
//            mapInfo.bgmFilePath = getInfo(s, "<FilePath>audio/bgm/", "</FilePath>");
//            // 自制谱面的这部分并不一定全为数字，所以不能转 int
//            contains = true;
//        } else if (s.contains("Section type=\"note\"")) {
//            if (isFirstNote) {// 歌曲 a 段开始 bar，默认 b 段开始也是这里
//                mapInfo.note1Bar = Integer.parseInt(getInfo(s, "startbar=\"", "\" endbar=\""));
//                this.note1Box = mapInfo.getNote1Box();
//                mapInfo.note2Bar = mapInfo.note1Bar;
//                this.note2Box = mapInfo.getNote2Box();
//                isFirstNote = false;
//            } else {// 歌曲 b 段开始 bar
//                mapInfo.note2Bar = Integer.parseInt(getInfo(s, "startbar=\"", "\" endbar=\""));
//                this.note2Box = mapInfo.getNote2Box();
//            }
//            contains = true;
//        } else if (s.contains("Section type=\"showtime\"")) {
//            if (isFirstST) {// 中场 st 开始 bar，默认结尾 st 开始也是这里
//                mapInfo.st1Bar = Integer.parseInt(getInfo(s, "startbar=\"", "\" endbar=\""));
//                this.st1Box = mapInfo.getSt1Box();
//                mapInfo.st2Bar = mapInfo.st1Bar;
//                this.st2Box = mapInfo.getSt2Box();
//                isFirstST = false;
//                newAllArray(mapInfo);// 初始化各个数组
//            } else {// 结尾 st 开始 bar
//                mapInfo.st2Bar = Integer.parseInt(getInfo(s, "startbar=\"", "\" endbar=\""));
//                this.st2Box = mapInfo.getSt2Box();
//                newAllArray(mapInfo);
//            }
//            contains = true;
//        }
//        return contains;
//    }
//
//    private void newAllArray(QQX5MapInfo mapInfo) {
//        if (mode == QQX5MapType.BUBBLE) {
//            this.resetBoxFlag = new boolean[st2Box + 5];
//            this.noteNum1 = new int[st2Box + 5];
//            this.noteNum1[note1Box] = 1;// 赋初值，防止开头没有数字重置标记
//            this.noteNum2 = new int[5][st2Box + 5];
//            this.X = new int[5][st2Box + 5];
//            this.Y = new int[5][st2Box + 5];
//        }
//        if (mode == QQX5MapType.CRESCENT) {
//            this.angle = new int[5][st2Box + 5];
//        }
//        mapInfo.combo = new int[st2Box + 5];
//        mapInfo.track = new int[5][st2Box + 5];
//        mapInfo.isLongNoteStart = new boolean[5][st2Box + 5];
//        mapInfo.isLongNoteEnd = new boolean[5][st2Box + 5];
//        mapInfo.noteType = new int[5][st2Box + 5];
//        mapInfo.boxDescribe = new String[4][st2Box + 5];
//    }
//
//    /**
//     * 传入谱面文件某行，返回按键对应的数组位置
//     * 由于截取得到的是 pos，所以最后还要 /2，才能转为 box
//     * 处理方式与定义 note1Box 等相同，同样去掉开头，再加 4 box
//     *
//     * @param s     谱面文件某行
//     * @param a     谱面对象
//     * @param state 截取类型
//     * @return 按键对应的数组位置
//     * @throws IllegalArgumentException 如果传入错误的截取方式
//     */
//    private int getBox(String s, int state) throws IllegalArgumentException {
//        int bar;
//        int box;
//        switch (state) {
//            case 11:// 星动单点/长条开始
//                bar = Integer.parseInt(getInfo(s, "<Note Bar=\"", "\" Pos=\""));
//                box = Integer.parseInt(getInfo(s, "\" Pos=\"", "\" from_track=\"")) / 2;
//                break;
//            case 12:// 星动长条结尾
//                bar = Integer.parseInt(getInfo(s, "EndBar=\"", "\" EndPos=\""));
//                box = Integer.parseInt(getInfo(s, "\" EndPos=\"", "\" />")) / 2;
//                break;
//            case 13:// 星动滑键
//                bar = Integer.parseInt(getInfo(s, "<Note Bar=\"", "\" Pos=\""));
//                box = Integer.parseInt(getInfo(s, "\" Pos=\"", "\" target_track=\"")) / 2;
//                break;
//            default:
//                throw new IllegalArgumentException("Can not find getBox type.");
//        }
//        return (bar - mapInfo.note1Bar) * mapInfo.boxPerBar + box + 4;
//    }
//
//    /**
//     * 将星动的字符串轨道与 track0-4 对应
//     *
//     * @param s    星动的字符串轨道
//     * @param is4k 是否为 4k星动
//     * @return 轨道数字
//     */
//    private int idol2Int(String s, boolean is4k) {
//        if (is4k) {
//            switch (s) {
//                case "Left2":
//                    return 0;
//                case "Left1":
//                    return 1;
//                case "Right1":
//                    return 2;
//                case "Right2":
//                    return 3;
//                default:
//                    throw new IllegalArgumentException("In idol_ConvertToInt, track is not correct.");
//            }
//        } else {
//            switch (s) {
//                case "Left2":
//                    return 0;
//                case "Left1":
//                    return 1;
//                case "Middle":
//                    return 2;
//                case "Right1":
//                    return 3;
//                case "Right2":
//                    return 4;
//                default:
//                    throw new IllegalArgumentException("In idol_ConvertToInt, track is not correct.");
//            }
//        }
//    }

    @Override
    public void setDescribe() {
//        for (int box = note1Box; box <= st2Box; box++) {
//            if (box > st1Box && box < note2Box) {
//                continue;
//            }
//            String basicStart = "^";// 正常操作，作为爆气开始位置的描述
//            String basicEnd = "^";
//            String coolStart = "^";
//            String coolEnd = "^";
//
//            // 首先将按键描述放上去
//            basicStart = basicIdolDescribe(mapInfo, box, basicStart, true);
//            basicEnd = basicIdolDescribe(mapInfo, box, basicEnd, false);
//            coolStart = coolIdolDescribe(mapInfo, box, basicStart, true);
//            coolEnd = coolIdolDescribe(mapInfo, box, basicEnd, false);
//
//            // 然后将AB段指示加到最前面
//            String part;
//            if (box <= st1Box) {
//                part = "A - ";
//            } else {
//                part = "B - ";
//            }
//
//            // 最后将得到的描述放入a中
//            mapInfo.setBoxDescribe(false, true, box, part + basicStart);
//            mapInfo.setBoxDescribe(false, false, box, part + basicEnd);
//            mapInfo.setBoxDescribe(true, true, box, part + coolStart);
//            mapInfo.setBoxDescribe(true, false, box, part + coolEnd);
//        }
    }
//
//    /**
//     * 如果某个位置没有按键，
//     * 则作为爆点开始描述时，应往后推，直至有描述；
//     * 反之则往前推，直至有描述
//     *
//     * @param isStart 是否为爆点开始
//     * @param box     要得到爆点描述的位置
//     * @return 移动方向及长度
//     */
//    private int boxChange(boolean isStart, int box) {
//        if (isStart && box <= st2Box) {
//            return 1;
//        } else if (!isStart && box >= note1Box) {
//            return -1;
//        }
//        return 0;
//    }
//
//    private String basicIdolDescribe(int box, String describe, boolean isStart) {
//        do {
//            StringBuilder s = new StringBuilder(describe);
//            for (int track = 0; track < 5; track++) {
//                if (mapInfo.track[track][box] == 1) {// 如果为单点或滑键
//                    if (mapInfo.noteType[track][box] == 0) {// 如果为单点
//                        s.append(idol2Str(track)).append("轨单点、");
//                    } else {// 如果为滑键
//                        int from_track = mapInfo.noteType[track][box] / 10;
//                        int target_track = mapInfo.noteType[track][box] % 10;
//                        boolean fromHaveLong = mapInfo.track[from_track][box] == 3;// 滑键开始位置是否有长条
//                        boolean targetHaveLong = mapInfo.track[target_track][box + 2] == 3;// 滑键结束位置后面是否有长条
//                        if (fromHaveLong) {
//                            s.append(idol2Str(from_track)).append("轨长条");
//                            if (target_track < from_track) {
//                                s.append("左滑、");
//                            } else {
//                                s.append("右滑、");
//                            }
//                        } else {
//                            if (targetHaveLong) {
//                                s.append(idol2Str(from_track)).append(idol2Str(target_track)).append("轨滑条、");
//                            } else {
//                                s.append(idol2Str(from_track)).append(idol2Str(target_track)).append("轨滑键、");
//                            }
//                        }
//                    }
//                } else if (mapInfo.track[track][box] == 3) {
//                    if (mapInfo.isLongNoteStart[track][box]) {
//                        // 因为前提是该位置为长条键，所以如果有滑键的话，这个位置必然不为3
//                        // 所以这样的情况必然是单长条起始，而非滑键长条起始
//                        s.append(idol2Str(track)).append("轨长条开始、");
//                    } else if (mapInfo.isLongNoteEnd[track][box]) {
//                        boolean isSingleLong = true;
//                        for (int i = 0; i < 5; i++) {
//                            if (mapInfo.noteType[i][box] != 0
//                                    && mapInfo.noteType[i][box] / 10 == track) {
//                                isSingleLong = false;
//                            }
//                        }
//                        if (isSingleLong) {
//                            s.append(idol2Str(track)).append("轨长条结尾、");
//                        }
//                    } else {
//                        s.append(idol2Str(track)).append("轨长条中间、");
//                    }
//                }
//            }
//            describe = s.toString();
//            if (boxChange(isStart, box) == 0) {
//                break;
//            } else {
//                box += boxChange(isStart, box);
//            }
//        } while (describe.equals("^"));
//        if (describe.equals("^")) {
//            return describe;
//        } else {
//            return describe.substring(1, describe.length() - 1);
//        }
//    }
//
//    private String idol2Str(int track) throws IllegalArgumentException {
//        switch (track) {
//            case 0:
//                return "一";
//            case 1:
//                return "二";
//            case 2:
//                return "三";
//            case 3:
//                return "四";
//            case 4:
//                return "五";
//            default:
//                throw new IllegalArgumentException("In idol_ConvertToString, track is not 0-4.");
//        }
//    }
//
//    private String coolIdolDescribe(int box, String describe, boolean isStart) {
//        StringBuilder s;
//        if (isStart) {
//            s = new StringBuilder();
//            for (int i = -4; i <= -1; i++) {
//                for (int track = 0; track < 5; track++) {
//                    if (mapInfo.track[track][box + i] == 1 && mapInfo.noteType[track][box + i] == 0) {
//                        s.append(idol2Str(track)).append("轨单点（cool）、");
//                    }
//                }
//            }
//            describe = s.append(describe).toString();
//        } else {
//            s = new StringBuilder(describe);
//            for (int i = 1; i <= 4; i++) {
//                for (int track = 0; track < 5; track++) {
//                    if (mapInfo.track[track][box + i] == 1 && mapInfo.noteType[track][box + i] == 0) {
//                        s.append("、").append(idol2Str(track)).append("轨单点（cool）");
//                    }
//                }
//            }
//            describe = s.toString();
//        }
//        return describe;
//    }

}
