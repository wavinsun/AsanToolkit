package cn.mutils.app.asan;

import java.util.ArrayList;

/**
 * Asan堆栈列表跟踪列表
 * <p>
 * Created by wenhua.ywh on 2017/12/19.
 */
public class AsanStackTraceData extends ArrayList<String> {

    /**
     * 调用堆栈单元组
     */
    private AsanStackElement[] mStackTrace = new AsanStackElement[0];

    /**
     * 输出Asan日志原文
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, size = this.size(); i < size; i++) {
            if (i != 0) {
                sb.append('\n');
            }
            sb.append(this.get(i));
        }
        return sb.toString();
    }

    /**
     * 格式化输出安卓标准堆栈
     *
     * @return
     */
    public String printStackTrace() {
        StringBuilder sb = new StringBuilder();
        sb.append("backtrace:");
        for (AsanStackElement element : mStackTrace) {
            sb.append("\n");
            sb.append(element.toString());
        }
        return sb.toString();
    }

    public AsanStackElement[] getStackTrace() {
        return mStackTrace;
    }

    /**
     * 判断运行库是否是当前堆栈
     *
     * @param libName
     * @return
     */
    public boolean isRunningLibrary(String libName) {
        for (AsanStackElement element : mStackTrace) {
            if (element.isRunningLibrary(libName)) {
                return true;
            }
        }
        return false;
    }

    protected void initEnd() {
        AsanStackElement[] stackTrace = null;
        for (int i = 0, j = 0, size = this.size(); i < size; i++) {
            String s = this.get(i);
            if (!s.startsWith("    #")) {
                continue;
            }
            if (stackTrace == null) {
                stackTrace = new AsanStackElement[size - i];
                j = i;
            }
            stackTrace[i - j] = new AsanStackElement(s);
        }
        if (stackTrace != null) {
            mStackTrace = stackTrace;
        }
    }

}
