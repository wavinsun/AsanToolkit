package cn.mutils.app.asan;

import java.util.List;

/**
 * 日志数据冲洗回调
 * <p>
 * Created by wenhua.ywh on 2017/12/25.
 */
public interface AsanFlushCallback {

    /**
     * 发生日志冲洗回调
     *
     * @param cache  日志缓冲区
     * @param errors 错误堆栈
     */
    public void doFlush(CharSequence cache, List<AsanStackTraceData> errors);

}
