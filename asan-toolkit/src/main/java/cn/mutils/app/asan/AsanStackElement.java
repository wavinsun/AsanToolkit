package cn.mutils.app.asan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asan堆栈单元
 * <p>
 * Created by wenhua.ywh on 2017/12/21.
 */
public class AsanStackElement {

    /**
     * 堆栈正则表达式
     */
    public static final Pattern ASAN_STACK_PATTERN = Pattern.compile("    #\\d{1,} 0x(.*)  ((.*)+0x(.*))");

    /**
     * 堆栈索引
     */
    private int mIndex = -1;
    /**
     * 堆栈对应文件
     */
    private String mSoPath;
    /**
     * 堆栈地址偏移
     */
    private String mSoOffset;
    /**
     * 堆栈对应的SO名字
     */
    private String mSoName;

    public AsanStackElement() {

    }

    public AsanStackElement(String trace) {
        init(trace);
    }

    private void init(String trace) {
        Matcher m = ASAN_STACK_PATTERN.matcher(trace);
        if (!m.matches()) {
            return;
        }
        int indexOfAlarm = trace.indexOf('#');
        int indexOfSpace = trace.indexOf(' ', indexOfAlarm + 1);
        String index = trace.substring(indexOfAlarm + 1, indexOfSpace);
        try {
            mIndex = Integer.parseInt(index);
        } catch (Exception e) {
            return;
        }
        int indexOfLeftBracket = trace.indexOf('(', indexOfSpace + 1);
        int indexOfPlus = trace.lastIndexOf('+', trace.length() - 3);
        mSoPath = trace.substring(indexOfLeftBracket + 1, indexOfPlus);
        mSoOffset = trace.substring(indexOfPlus + 3, trace.length() - 1);
        int indexOfName = mSoPath.lastIndexOf('/');
        mSoName = mSoPath.substring(indexOfName + 1);
    }

    /**
     * 判断运行库是否是当前堆栈单元
     *
     * @param libName
     * @return
     */
    public boolean isRunningLibrary(String libName) {
        if (libName == null || libName.length() == 0) {
            return false;
        }
        if (libName.startsWith("lib")) {
            return mSoName.equals(libName);
        }
        return mSoName.equals("lib" + libName + ".so");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    ");
        if (mIndex == -1) {
            return sb.toString();
        }
        sb.append('#');
        if (mIndex < 10) {
            sb.append('0');
        }
        sb.append(Integer.valueOf(mIndex));
        sb.append(" pc ");
        for (int i = mSoOffset.length(); i < 8; i++) {
            sb.append('0');
        }
        sb.append(mSoOffset);
        sb.append(' ');
        sb.append(mSoPath);
        return sb.toString();
    }
}
