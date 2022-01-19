package com.menglei.qqx5tools;

public class SettingsAndUtils {
    public enum FileType {
        IDOL,
        IDOL3K,
        IDOL4K,
        IDOL5K,
        PINBALL,
        BUBBLE,
        CRESCENT;

        @Override
        public String toString() {
            return switch (this) {
                case IDOL, IDOL3K, IDOL4K, IDOL5K -> "星动";
                case PINBALL -> "弹珠";
                case BUBBLE -> "泡泡";
                case CRESCENT -> "弦月";
            };
        }
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
            nanosecond = (long)((double)nanosecond % 1000000.0D);
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

                    for(int i = 0; i < num - 1; ++i) {
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
}
