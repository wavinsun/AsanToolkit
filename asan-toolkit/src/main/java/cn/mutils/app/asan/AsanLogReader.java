package cn.mutils.app.asan;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Asan日志解析读取工具
 * Created by wenhua.ywh on 2017/12/21.
 */
public class AsanLogReader implements Closeable {

    private static final String TAG = "asan_logs";

    /**
     * 最大保持在内存中的个数
     */
    public static final int MAX_FLUSH_STACKS = 5;
    /**
     * 最大保持的时间间隔
     */
    private static final long MAX_FLUSH_DURATION = 5000L;

    private static final Pattern ASAN_START_PATTERN = Pattern.compile("==\\d{1,}==ERROR: AddressSanitizer: (.*) on address 0x(.*) at pc 0x(.*) bp 0x(.*) sp 0x(.*)");
    private static final Pattern ASAN_STACK_PATTERN = AsanStackElement.ASAN_STACK_PATTERN;
    public static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS"); // 04-23 03:40:00.011

    private int mMaxFlushStacks = MAX_FLUSH_STACKS;
    /**
     * 是否被关闭
     */
    private boolean mClosed;
    /**
     * 进程标签
     */
    private String mTagProcess;
    /**
     * 是否缓存所有asan日志，包括没有捕捉到堆栈之前的无效日志
     */
    private boolean mFlushAll;

    public boolean isFlushAll() {
        return mFlushAll;
    }

    public void setFlushAll(boolean flushAll) {
        mFlushAll = flushAll;
    }

    public String getTagProcess() {
        return mTagProcess;
    }

    public void setTagProcess(String tagProcess) {
        mTagProcess = tagProcess;
    }

    public boolean isClosed() {
        return mClosed;
    }

    public void close() {
        mClosed = true;
    }

    public int getMaxFlushStackCount() {
        return mMaxFlushStacks;
    }

    public void setMaxFlushStackCount(int maxFlushStacks) {
        mMaxFlushStacks = maxFlushStacks;
    }

    public void read(BufferedReader reader, List<AsanStackTraceData> stackTraceDataList) {
        read(reader, stackTraceDataList, null);
    }

    public void read(BufferedReader reader, AsanFlushCallback callback) {
        read(reader, new ArrayList<AsanStackTraceData>(), callback);
    }

    protected void read(BufferedReader reader, List<AsanStackTraceData> errors, AsanFlushCallback callback) {
        String l, line = null;
        AsanStackTraceData data = null;
        StringBuilder cache = callback != null ? new StringBuilder() : null;
        Long t = null;
        boolean hasCatchAsanError = false; // 是否捕捉到错误
        boolean isInAsanLogMode = false; // 是否开始进行log解析
        boolean isInStackMode = false; // 是否开始堆栈解析
        boolean checkTime = false; // 是否检测时间
        try {
            while ((l = reader.readLine()) != null) {
                if (mClosed) {
                    return;
                }
                line = l;
                if (line.length() == 0) {
                    continue;
                }
                Long logTime = null;
                if (checkTime) { // 捕捉到错误，防止长时间没有asan日志导致原先捕捉到的无法上报
                    logTime = obtainTime(l);
                    if (t != null && (logTime - t) > MAX_FLUSH_DURATION) {
                        if (callback != null) {
                            callback.doFlush(cache, errors);
                            cache = new StringBuilder();
                            errors.clear();
                        }
                    }
                }
                line = obtainLogContent(line);
                if (line == null || line.length() == 0) {
                    continue;
                }
                if (!isAsanTag(line)) {
                    checkTime = callback != null && errors.size() > 0;
                    continue;
                }
                checkTime = false;
                line = obtainAsanLogContent(line);
                if (line == null || line.length() == 0) {
                    if (callback != null && cache != null) {
                        if (mFlushAll || hasCatchAsanError) {
                            cache.append(l);
                            cache.append("\n");
                        }
                    }
                    continue;
                }
                if (!isInAsanLogMode) {
                    isInAsanLogMode = isAsanLogStart(line);
                    if (isInAsanLogMode) {
                        if (data != null) {
                            data.initEnd();
                            errors.add(data);
                            if (callback != null && cache != null) {
                                if (errors.size() >= mMaxFlushStacks) {
                                    callback.doFlush(cache, errors);
                                    cache = new StringBuilder();
                                    errors.clear();
                                }
                            }
                        }
                        data = new AsanStackTraceData();
                        data.add(line);
                    }
                } else {
                    if (isInStackMode) {
                        isInStackMode = isAsanStackStart(line);
                        if (!isInStackMode) {
                            isInAsanLogMode = false;
                        }
                        if (isInAsanLogMode) {
                            data.add(line);
                        }
                    } else {
                        isInStackMode = isAsanStackStart(line);
                        data.add(line);
                    }
                }
                if (isInAsanLogMode) {
                    hasCatchAsanError = true;
                }
                if (callback != null && cache != null) {
                    if (mFlushAll || hasCatchAsanError) {
                        cache.append(l);
                        cache.append("\n");
                    }
                }
                t = callback != null ? (logTime != null ? logTime : obtainTime(l)) : null;
            }
            if (data != null) {
                data.initEnd();
                errors.add(data);
                if (callback != null) {
                    callback.doFlush(cache, errors);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 去除头部时间戳
     *
     * @param line
     * @return
     */
    private String obtainLogContent(String line) {
        int length = line.length();
        if (length < 18) { // 04-23 03:40:00.011
            return null;
        }
        int contentIndex = line.indexOf(':', 21);// 04-23 03:40:00.011 F/libc
        if (contentIndex == -1) {
            return null;
        }
        if (contentIndex + 2 >= length) {
            return null;
        }
        if (!isAsanProcess(line.substring(21, contentIndex))) {
            return null;
        }
        return line.substring(contentIndex + 2);
    }

    /**
     * 判断TAG是否有进程标记
     *
     * @param processPart
     * @return
     */
    private boolean isAsanProcess(String processPart) {
        if (mTagProcess == null) {
            return true;
        }
        if (processPart.startsWith(mTagProcess)) {
            return true;
        }
        return false;
    }

    /**
     * 判断内容是否是asan内容关键字开始
     *
     * @param line
     * @return
     */
    private boolean isAsanTag(String line) {
        if (line.startsWith(TAG)) {
            return true;
        }
        return false;
    }

    /**
     * 获取日志内容，并且去除开始关键字
     *
     * @param line
     * @return
     */
    private String obtainAsanLogContent(String line) {
        int length = line.length();
        if (length < 9) { // asan_logs
            return null;
        }
        int contentIndex = line.indexOf(':', 9);
        if (contentIndex == -1) {
            return null;
        }
        if (contentIndex + 3 >= length) { // asan_logs32:  ==2328==ERROR:
            return null;
        }
        return line.substring(contentIndex + 3);
    }

    /**
     * 判断asan日志是否是开始asan错误
     *
     * @param line
     * @return
     */
    protected boolean isAsanLogStart(String line) {
        if (ASAN_START_PATTERN.matcher(line).matches()) {
            return true;
        }
        return false;
    }

    /**
     * 判断当前asan错误是否开始记录堆栈
     *
     * @param line
     * @return
     */
    protected boolean isAsanStackStart(String line) {
        if (ASAN_STACK_PATTERN.matcher(line).matches()) {
            return true;
        }
        return false;
    }

    /**
     * 获取当前日志的纪录时间戳
     *
     * @param line
     * @return
     */
    protected Long obtainTime(String line) {
        if (line.length() < 18) { // 04-23 03:40:00.011
            return null;
        }
        try {
            return LOG_DATE_FORMAT.parse(line.substring(0, 18)).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
