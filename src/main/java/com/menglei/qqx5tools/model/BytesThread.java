package com.menglei.qqx5tools.model;

import com.menglei.qqx5tools.SettingsAndUtils.QQX5MapType;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static com.menglei.qqx5tools.SettingsAndUtils.PRINT_BYTES_INFO;
import static com.menglei.qqx5tools.SettingsAndUtils.THREAD_NUM;
import static com.menglei.qqx5tools.SettingsAndUtils.logError;

public class BytesThread extends Thread {
    private final int threadNo;
    private final ArrayList<File> bytesFileList;
    private byte[] bytesArray;
    /**
     * 指示 bytesArray 当前指针的位置.
     */
    private int index;

    BytesThread(int threadNo, ArrayList<File> bytesFileList) {
        this.threadNo = threadNo;
        this.bytesFileList = bytesFileList;
    }

    @Override
    public void run() {
        for (int i = 0; i < bytesFileList.size(); i++) {
            if (i % THREAD_NUM == threadNo) {
                process(bytesFileList.get(i));
            }
        }
    }

    private void process(File bytesFile) {
        // 原目录下生成xml
        String bytesFilePath;
        File xmlFile;
        try {
            bytesFilePath = bytesFile.getCanonicalPath();
            xmlFile = new File(bytesFilePath.substring(0, bytesFilePath.lastIndexOf(".")));
        } catch (IOException e) {
            logError(e);
            return;
        }
        // 用二进制流读取数据
        try (DataInputStream dis = new DataInputStream(new FileInputStream(bytesFile))) {
            bytesArray = new byte[dis.available()];
            dis.readFully(bytesArray);
        } catch (IOException e) {
            logError(e);
            return;
        }
        // 判断是否为炫舞的bytes文件
        index = 0;
        QQX5MapType type = switch (getString()) {
            case "XmlIdolExtend" -> QQX5MapType.IDOL;
            case "XmlPinballExtend" -> QQX5MapType.PINBALL;
            case "XmlBubbleExtend" -> QQX5MapType.BUBBLE;
            case "XmlClassicExtend" -> QQX5MapType.CLASSIC;
            case "XmlCrescentExtend" -> QQX5MapType.CRESCENT;
            default -> null;
        };
        if (type == null) {
            System.out.println(bytesFilePath + "不是炫舞谱面bytes文件！");
            return;
        }
        // 是否输出调试信息
        if (PRINT_BYTES_INFO) {
            for (int idx = 0; idx < bytesArray.length - 4; idx++) {
                // 以hex形式输出byte对应的值，为以其他形式输出打基础
                index = idx;
                System.out.printf("bytesArray[%5d] = ", idx);
                System.out.print(getHexStr(bytesArray[idx]));
                // 以int形式输出，以便于观察是否为整数
                index = idx;
                int intValue = getInt();
                if (intValue > 999999 || intValue < -99999) {
                    System.out.print(" int = ------");
                } else {
                    System.out.printf(" int = %6d", intValue);
                }
                // 以float形式输出，以便于观察是否为小数
                index = idx;
                float floatValue = getFloat();
                if (floatValue > 999999 || floatValue < -99999) {
                    System.out.print(" float = ------.--");
                } else {
                    System.out.printf(" float = %9.2f", floatValue);
                }
                // 以char形式输出，以便于观察是否为字符串
                index = idx;
                System.out.print(" char1 = " + new String(bytesArray, index, 1, StandardCharsets.UTF_8));
                System.out.println(" char3 = " + new String(bytesArray, index, 3, StandardCharsets.UTF_8));
            }
        }
        // 重置index并开始处理
        index = 0;
        writeXml(xmlFile, type);
        System.out.println(type + " " + bytesFile.getName() + " 转化完毕！");
    }

    /**
     * 返回某个byte对应的十六进制字符串.
     * <p>
     * 在处理过程中 &0xFF 的原因是，java的byte具有正负，负数以补码形式存储。
     * <p>
     * 例如byte类型的负数-12，值为1111 0100；
     * 如果执行Integer.toHexString(b)，参数b会从byte隐式转换为int，
     * 即补符号位扩展为int，所以参数的值相当于11111111 11111111 11111111 11110100；
     * 转为16进制字符串，结果为fffffff4，这显然不是需要的结果。
     * <p>
     * 如果先&0xFF在作为参数传递，则参数变为32位，且值为00000000 00000000 00000000 11110100；
     * 再转为16进制字符串，结果为f4，这是需要的。
     *
     * @param b 一个byte
     * @return 对应的十六进制字符串，不足两字符的补足至两字符
     */
    private static String getHexStr(byte b) {
        StringBuilder stringBuilder = new StringBuilder();
        String hexStr = Integer.toHexString(b & 0xFF);
        if (hexStr.length() < 2) {
            stringBuilder.append(0);
        }
        stringBuilder.append(hexStr);
        return stringBuilder.toString();
    }

    /**
     * 四字节转整型.
     * <p>
     * 不使用DataInputStream的readInt()方法有两个原因，一是{@link #getHexStr(byte)}中
     * 提到的byte正负问题，二是炫舞bytes文件本身大小端与java不一致的问题。
     * <p>
     * 炫舞bytes文件为小端在前大端在后，但java默认大端在前小端在后。
     * <p>
     * 比如整数4，小端在前为04 00 00 00，大端在前为00 00 00 04，所以顺序需要调一下。
     *
     * @return 转化后的整型数据
     */
    private int getInt() {
        int ch1 = bytesArray[index] & 0xff;
        int ch2 = bytesArray[index + 1] & 0xff;
        int ch3 = bytesArray[index + 2] & 0xff;
        int ch4 = bytesArray[index + 3] & 0xff;
        // 读完数据后自动移动指针
        index += 4;
        return (ch1 + (ch2 << 8) + (ch3 << 16) + (ch4 << 24));
    }

    /**
     * 四字节转浮点型.
     *
     * @return 转化后的浮点型数据
     */
    private float getFloat() {
        int kk = getInt();
        index -= 4;

        int l;
        l = bytesArray[index];
        l &= 0xff;
        l |= ((long) bytesArray[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) bytesArray[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) bytesArray[index + 3] << 24);


        // 读完数据后自动移动指针
        index += 4;

        System.out.println("zzzz " + kk + " " + l);

        return Float.intBitsToFloat(l);
    }

    /**
     * 单字节转布尔型.
     * <p>
     * 炫舞bytes文件中，以0x00表示False，以0x01表示True。
     * <p>
     * 该方法将0x00转为False，将非0x00转为True。
     *
     * @return 转化后的布尔型数据
     */
    private boolean getBoolean() {
        boolean bool = (bytesArray[index] != 0);
        // 读完数据后自动移动指针
        index++;
        return bool;
    }

    /**
     * 单字节/多字节转字符串
     * <p>
     * 字符串在炫舞bytes文件中存储为连续的两部分，其一为字符串长度，用一个int值（假设该值为len）表示；
     * 其二为字符串本身，用len个byte以及一个0x00表示。
     *
     * @return 转化后的字符串
     */
    private String getString() {
        // 获取字符串的长度
        int length = getInt();
        String s = new String(bytesArray, index, length, StandardCharsets.UTF_8);
        // 读完数据后自动移动指针，注意要跳过字符串结尾的0x00
        index += (length + 1);
        return s;
    }

    private void writeXml(File xmlFile, QQX5MapType type) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(xmlFile))) {
            getTopInfo(bw, type);
            switch (type) {
                case IDOL -> getIdolNotesInfo(bw);
                case PINBALL -> getPinballNotesInfo(bw);
                case BUBBLE -> getBubbleNotesInfo(bw);
                case CRESCENT -> getCrescentNotesInfo(bw);
            }
            getBottomInfo(bw, type);
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getTopInfo(BufferedWriter bw, QQX5MapType type) {
        try {
            bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            bw.newLine();
            bw.write("<Level>");
            bw.newLine();
            bw.write("  <LevelInfo>");
            bw.newLine();
            bw.write("    <BPM>" + getFloat() + "</BPM>");
            bw.newLine();
            bw.write("    <BeatPerBar>" + getInt() + "</BeatPerBar>");
            bw.newLine();
            bw.write("    <BeatLen>" + getInt() + "</BeatLen>");
            bw.newLine();
            bw.write("    <EnterTimeAdjust>" + getInt() + "</EnterTimeAdjust>");
            bw.newLine();
            bw.write("    <NotePreShow>" + getFloat() + "</NotePreShow>");
            bw.newLine();
            bw.write("    <LevelTime>" + getInt() + "</LevelTime>");
            bw.newLine();
            bw.write("    <BarAmount>" + getInt() + "</BarAmount>");
            bw.newLine();
            bw.write("    <BeginBarLen>" + getInt() + "</BeginBarLen>");
            bw.newLine();
            boolean isFourTrack = getBoolean();
            int trackCount = getInt();
            int levelPreTime = getInt();
            if (trackCount != 0) {// 星弹泡
                bw.write("    <IsFourTrack>" + (isFourTrack ? "True" : "False") + "</IsFourTrack>");
                bw.newLine();
                bw.write("    <TrackCount>" + trackCount + "</TrackCount>");
                bw.newLine();
                bw.write("    <LevelPreTime>" + levelPreTime + "</LevelPreTime>");
            } else {// 弦月
                bw.write("    <LevelPreTime>" + levelPreTime + "</LevelPreTime>");
                bw.newLine();
                bw.write("    <Type>Crescent</Type>");
            }
            bw.newLine();
            // int -1
            index += 4;
            bw.write("  </LevelInfo>");
            bw.newLine();
            bw.write("  <MusicInfo>");
            bw.newLine();
            bw.write("    <Title>" + getString() + "</Title>");
            bw.newLine();
            bw.write("    <Artist>" + getString() + "</Artist>");
            bw.newLine();
            bw.write("    <FilePath>" + getString() + "</FilePath>");
            // 5个0x00
            index += 5;
            bw.newLine();
            bw.write("  </MusicInfo>");
            bw.newLine();
            bw.write("  <SectionSeq>");
            bw.newLine();
            int sectionTypeNum = getInt();
            for (int i = 0; i < sectionTypeNum; i++) {
                bw.write("    <Section type=\"" + getString());
                if (i == 0) {
                    index += 4;// Section type="previous" 时，不需要 startbar
                } else {
                    bw.write("\" startbar=\"" + getInt());
                }
                bw.write("\" endbar=\"" + getInt());
                bw.write("\" mark=\"" + getString());
                bw.write("\" param1=\"" + getString());
                // 10个0x00
                index += 10;
                bw.write("\" />");
                bw.newLine();
            }
            bw.write("  </SectionSeq>");
            bw.newLine();
            if (type != QQX5MapType.CRESCENT) {
                bw.write("  <IndicatorResetPos PosNum=\"" + getInt());
                int resetNum = getInt();
                if (resetNum == 0) {
                    bw.write("\" />");
                    bw.newLine();
                } else {
                    bw.write("\">");
                    bw.newLine();
                    for (int i = 0; i < resetNum; i++) {
                        bw.write("    <Pos Bar=\"" + getInt() + "\" BeatPos=\"" + getInt() + "\" />");
                        bw.newLine();
                    }
                    bw.write("  </IndicatorResetPos>");
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getIdolNotesInfo(BufferedWriter bw) {
        try {
            bw.write("  <NoteInfo>");
            bw.newLine();
            bw.write("    <Normal>");
            bw.newLine();
            int noteNum = getInt();
            int combineNote;// 一定存在，为 int 0（表示没有CombineNote标签）或 int 1
            int combineNum;// combineNote 为 1 时存在，表示CombineNote标签的长度
            for (int i = 0; i < noteNum; i++) {
                combineNote = getInt();
                if (combineNote == 0) {
                    getOneIdolNote(bw);
                } else if (combineNote == 1) {
                    combineNum = getInt();
                    bw.write("      <CombineNote>");
                    bw.newLine();
                    for (int j = 0; j < combineNum; j++) {
                        bw.write("  ");
                        getOneIdolNote(bw);
                    }
                    bw.write("      </CombineNote>");
                    bw.newLine();
                }
            }
            bw.write("    </Normal>");
            bw.newLine();
            bw.write("  </NoteInfo>");
            bw.newLine();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getOneIdolNote(BufferedWriter bw) {
        try {
            bw.write("      <Note Bar=\"" + getInt() + "\" Pos=\"" + getInt());
            String s1 = getString();
            String s2 = getString();
            String s3 = getString();
            String type = getString();
            int endBar = getInt();
            int endPos = getInt();
            switch (type) {
                case "short" -> bw.write("\" from_track=\"" + s1 + "\" target_track=\"" + s3
                        + "\" note_type=\"" + type + "\" />");
                case "slip" -> bw.write("\" target_track=\"" + s3 + "\" end_track=\"" + s2
                        + "\" note_type=\"" + type + "\" />");
                case "long" -> bw.write("\" from_track=\"" + s1 + "\" target_track=\"" + s3
                        + "\" note_type=\"" + type + "\" EndBar=\"" + endBar
                        + "\" EndPos=\"" + endPos + "\" />");
                default -> System.out.println("发现星动未知类型按键：" + type);
            }
            bw.newLine();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getPinballNotesInfo(BufferedWriter bw) {
        try {
            bw.write("  <NoteInfo>");
            bw.newLine();
            bw.write("    <Normal>");
            bw.newLine();
            int noteNum = getInt();
            for (int i = 0; i < noteNum; i++) {
                getOnePinballNote(bw);
            }
            bw.write("    </Normal>");
            bw.newLine();
            bw.write("  </NoteInfo>");
            bw.newLine();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getOnePinballNote(BufferedWriter bw) {
        try {
            bw.write("      <Note ID=\"" + getInt() + "\" note_type=\"" + getString()
                    + "\" Bar=\"" + getInt() + "\" Pos=\"" + getInt());
            int endBar = getInt();
            if (endBar != 0) {
                bw.write("\" EndBar=\"" + endBar + "\" EndPos=\"" + getInt());
            } else {
                index += 4;
            }
            bw.write("\" Son=\"");
            int sonNum = getInt();// Son状态标识，0表示Son为空，1表示后面存储Son的值
            if (sonNum != 0) {
                bw.write("" + getInt());
            }
            bw.write("\" EndArea=\"");
            int endAreaNum = getInt();// EndArea参数数目，0表示无参数（即按键在random区）
            if (endAreaNum == 1) {
                bw.write("" + (getInt() + 1));
            } else if (endAreaNum == 2) {
                bw.write((getInt() + 1) + "|" + (getInt() + 1));
            }
            bw.write("\" MoveTime=\"" + (int) getFloat() + "\" />");
            bw.newLine();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getBubbleNotesInfo(BufferedWriter bw) {
        try {
            bw.write("  <NoteInfo>");
            bw.newLine();
            bw.write("    <Normal PosNum=\"" + getInt() + "\">");
            bw.newLine();
            int noteNum = getInt();
            for (int i = 0; i < noteNum; i++) {
                getOneBubbleNote(bw);
            }
            bw.write("    </Normal>");
            bw.newLine();
            bw.write("  </NoteInfo>");
            bw.newLine();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getOneBubbleNote(BufferedWriter bw) {
        try {
            bw.write("      <Note Bar=\"" + getInt()
                    + "\" BeatPos=\"" + getInt() + "\" Track=\"" + getInt());
            int type = getInt();
            if (type == 0) {
                bw.write("\" Type=\"" + type + "\">");
                // 单点不需要 EndBar, EndPos, ID
                index += 12;
            } else {
                bw.write("\" Type=\"" + type + "\" EndBar=\"" + getInt()
                        + "\" EndPos=\"" + getInt() + "\" ID=\"" + getInt() + "\">");
            }
            bw.newLine();
            // 最开始写这段代码时，需要列出所有数据，再拼出正确结构
            // 单点有16个数据，非单点有18个数据，前八个数据类型相同，后面数据根据第八个而变
            // type是a4，这个分界点是a8，就不改名了，当个纪念
            int a8 = getInt();
            if (a8 == 0) {
                // int 0, int 1
                index += 8;
                bw.write("        <ScreenPos x=\"" + getInt() + "\" y=\"" + getInt() + "\">");
                bw.newLine();
                // int 0, int 1
                index += 8;
                bw.write("          <FlyTrack name=\"" + getString()
                        + "\" degree=\"" + getFloat() + "\" />");
                bw.newLine();
                bw.write("        </ScreenPos>");
                bw.newLine();
                bw.write("      </Note>");
                bw.newLine();
            } else if (a8 == 1) {
                bw.write("        <MoveTrack name=\"" + getString()
                        + "\" degree=\"" + getFloat() + "\" />");
                bw.newLine();
                // int 1
                index += 4;
                bw.write("        <FlyTrack name=\"" + getString()
                        + "\" degree=\"" + getFloat() + "\" />");
                bw.newLine();
                // int 1
                index += 4;
                bw.write("        <ScreenPos x=\"" + getInt() + "\" y=\"" + getInt() + "\" />");
                bw.newLine();
                // int 0, int 0
                index += 8;
                bw.write("      </Note>");
                bw.newLine();
            }
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getCrescentNotesInfo(BufferedWriter bw) {
        try {
            bw.write("  <NoteInfo>");
            bw.newLine();
            bw.write("    <Normal>");
            bw.newLine();
            // int 0, int 0
            index += 8;
            int noteNum = getInt();
            for (int i = 0; i < noteNum; i++) {
                getOneCrescentNote(bw);
            }
            bw.write("    </Normal>");
            bw.newLine();
            bw.write("  </NoteInfo>");
            bw.newLine();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getOneCrescentNote(BufferedWriter bw) {
        try {
            bw.write("      <Note Bar=\"" + getInt() + "\" Pos=\"" + getInt());
            String type = getString();
            int track = getInt();
            switch (type) {
                case "short", "light", "long" -> {
                    // int 0
                    index += 4;
                    bw.write("\" track=\"" + track + "\" note_type=\"" + type);
                    if (type.equals("long")) {
                        bw.write("\" length=\"" + getInt());
                    } else {
                        // int 0
                        index += 4;
                    }
                    // int 0
                    index += 16;
                }
                case "pair" -> {
                    bw.write("\" track=\"" + track
                            + "\" target_track=\"" + getInt() + "\" note_type=\"" + type);
                    // 5个int 0
                    index += 20;
                }
                case "slip" -> {
                    int a = getInt();// 可能是target_track，要看后面的值
                    int b = getInt();// 非零为length
                    // 2个int 0
                    index += 8;
                    if (b != 0) {// 因为target_track可能为0，所以不用a的值判断
                        bw.write("\" track=\"" + track + "\" target_track=\"" + a
                                + "\" note_type=\"" + type + "\" length=\"" + b);
                        // 2个int 0
                        index += 8;
                    } else {// a == 0 && b == 0
                        bw.write("\" track=\"" + track + "\" target_track=\"");
                        int target_trackNum = getInt();
                        for (int i = 0; i < target_trackNum; i++) {
                            bw.write(((i == 0) ? "" : ",") + getInt());
                        }
                        bw.write("\" note_type=\"" + type + "\" length=\"");
                        int lengthNum = getInt();
                        for (int i = 0; i < lengthNum; i++) {
                            bw.write(((i == 0) ? "" : ",") + getInt());
                        }
                    }
                }
                default -> System.out.println("发现弦月未知类型按键：" + type);
            }
            bw.write("\" />");
            bw.newLine();
        } catch (IOException e) {
            logError(e);
        }
    }

    private void getBottomInfo(BufferedWriter bw, QQX5MapType type) {
        try {
            int actionSeqType = getInt();// ActionSeq 类型，有两种，结构不同
            bw.write("  <ActionSeq type=\"" + actionSeqType + "\">");
            bw.newLine();
            int actionSeqNum = getInt();// ActionSeq 个数
            if (actionSeqType == 0) {
                for (int i = 0; i < actionSeqNum; i++) {
                    bw.write("    <ActionList start_bar=\"" + getInt());
                    // 全是0x00
                    index += 17;
                    bw.write("\" id=\"" + getString() + "\" />");
                    // 全是0x00
                    index += 5;
                    bw.newLine();
                }
                if (type == QQX5MapType.IDOL) {
                    index += 4;
                }
                bw.write("  </ActionSeq>");
                bw.newLine();
                bw.write("  <CoupleActionSeq type=\"" + getInt() + "\">");
                bw.newLine();
                int coupleActionSeq = getInt();// CoupleActionSeq 个数
                for (int i = 0; i < coupleActionSeq; i++) {
                    bw.write("    <ActionList start_bar=\"" + getInt()
                            + "\" dance_len=\"" + getInt() + "\" seq_len=\"" + getInt()
                            + "\" level=\"" + getInt() + "\" type=\"" + getString() + "\" />");
                    index += 10;
                    bw.newLine();
                }
                bw.write("  </CoupleActionSeq>");
                bw.newLine();
            } else if (actionSeqType == 1) {
                for (int i = 0; i < actionSeqNum; i++) {
                    bw.write("    <ActionList start_bar=\"" + getInt()
                            + "\" dance_len=\"" + getInt() + "\" seq_len=\"" + getInt()
                            + "\" level=\"" + getInt() + "\" type=\"" + getString() + "\" />");
                    index += 10;
                    bw.newLine();
                }
                // 都是0x00
                if (type == QQX5MapType.IDOL) {
                    index += 4;
                } else {
                    index += 8;
                }
                bw.write("  </ActionSeq>");
                bw.newLine();
            }
            bw.write("  <CameraSeq>");
            bw.newLine();
            int cameraSeqNum = getInt();
            for (int i = 0; i < cameraSeqNum; i++) {
                bw.write("    <Camera name=\"" + getString()
                        + "\" bar=\"" + getInt() + "\" pos=\"" + getInt()
                        + "\" end_bar=\"" + getInt() + "\" end_pos=\"" + getInt() + "\" />");
                bw.newLine();
            }
            bw.write("  </CameraSeq>");
            bw.newLine();
            bw.write("  <DancerSort>");
            bw.newLine();
            int dancerSortNum = getInt();
            for (int i = 0; i < dancerSortNum; i++) {
                bw.write("    <Bar>" + getInt() + "</Bar>");
                bw.newLine();
            }
            bw.write("  </DancerSort>");
            bw.newLine();
            bw.write("  <StageEffectSeq>");
            bw.newLine();
            int stageEffectSeqNum = getInt();
            for (int i = 0; i < stageEffectSeqNum; i++) {
                bw.write("    <effect name=\"" + getString()
                        + "\" bar=\"" + getInt()
                        + "\" length=\"" + getInt() + "\" />");
                // 5个0x00
                index += 5;
                bw.newLine();
            }
            bw.write("  </StageEffectSeq>");
            bw.newLine();
            if (type == QQX5MapType.CRESCENT) {
                bw.write("  <IndicatorResetPos PosNum=\"64\" />");// bytes文件没有这个值
                // 最后int32应该是MD5之类的东西，是个str
                bw.newLine();
            }
            bw.write("</Level>");
        } catch (IOException e) {
            logError(e);
        }
    }
}
