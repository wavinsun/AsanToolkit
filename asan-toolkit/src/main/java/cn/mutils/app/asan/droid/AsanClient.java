package cn.mutils.app.asan.droid;

import android.util.Log;

import cn.mutils.app.asan.AsanFlushCallback;
import cn.mutils.app.asan.AsanLogReader;
import cn.mutils.app.asan.AsanStackTraceData;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asan安卓客户端监控工具
 * <p>
 * Created by wenhua.ywh on 2017/12/25.
 */
public abstract class AsanClient {

    private static final String ASAN_URL = "http://asan.app.com/server/asan/upload";

    private static final String TAG = AsanClient.class.getSimpleName();
    /**
     * 监控进程TAG
     */
    public static final String TAG_PROCESS = "cn.mutils.app";
    /**
     * 监控二进制SO
     */
    private static final String LIBRARY_DICE = "libXXX.so";
    /**
     * 最大同时进行的上传任务
     */
    private static final int MAX_FLUSHING_COUNT = 3;

    /**
     * 是否启动
     */
    private boolean mStarted;
    /**
     * 日志解析器
     */
    private AsanLogReader mLogReader = new AsanLogReader();
    /**
     * 日志冲洗计数器
     */
    private AtomicInteger mFlushingCounter = new AtomicInteger(0);
    /**
     * 最大同时进行日志冲洗限制
     */
    private int mMaxFlushingCount = MAX_FLUSHING_COUNT;

    public AsanClient() {
        super();
        mLogReader.setTagProcess(TAG_PROCESS);
    }

    public AsanClient(int maxFlushStacks) {
        this();
        mLogReader.setMaxFlushStackCount(maxFlushStacks);
    }

    public AsanClient(int maxFlushStacks, int maxFlushingCount) {
        this(maxFlushStacks);
        mMaxFlushingCount = maxFlushingCount;
    }

    /**
     * 获取执行网络上传线程池
     *
     * @return
     */
    protected abstract Executor getHttpPool();

    /**
     * 获取客户端版本号
     *
     * @return
     */
    protected abstract String getAppVersion();

    /**
     * 获取Jenkins打包任务名称
     *
     * @return
     */
    protected abstract String getJenkinsJob();

    /**
     * 启动
     */
    public void start() {
        if (mStarted) {
            return;
        }
        mStarted = true;
        if (mLogReader.isClosed()) {
            throw new RuntimeException("The client is stop running!");
        }
        new LogcatMonitor().start();
    }

    /**
     * 停止
     */
    public void stop() {
        mLogReader.close();
    }

    /**
     * 日志冲洗上传回调
     *
     * @param logCache
     */
    protected void onLogcatFlush(final String logCache) {
        if (mFlushingCounter.get() > mMaxFlushingCount) { //  超过限制直接抛弃
            return;
        }
        getHttpPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mFlushingCounter.getAndIncrement();
                    AsanHttpTask task = new AsanHttpTask(ASAN_URL);
                    task.addFileParam("crashFile", new TextFile("logcat.txt", logCache));
                    task.addTextParam("version", getAppVersion());
                    task.addTextParam("jobName", getJenkinsJob());
                    String response = task.send();
                    if (!new JSONObject(response).getBoolean("success")) {
                        throw new RuntimeException(response);
                    }
                    Log.d(TAG, "HTTP OK: " + response);
                } catch (Exception e) {
                    Log.d(TAG, "HTTP ERROR: " + ASAN_URL, e);
                } finally {
                    mFlushingCounter.getAndDecrement();
                }
            }
        });
    }

    /**
     * Logcat监控线程
     */
    private class LogcatMonitor extends Thread {

        public LogcatMonitor() {
            super("LogcatMonitor");
        }

        @Override
        public void run() {
            while (!mLogReader.isClosed()) {
                Log.d(TAG, "Logcat Monitor start ... ...");
                Process process = null;
                try {
                    process = Runtime.getRuntime().exec(new String[]{
                            "logcat", "-v", "time", "-e", "asan_logs(.*)", "-T",
                            AsanLogReader.LOG_DATE_FORMAT.format(new Date())});
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    mLogReader.read(br, new AsanFlushCallback() {
                        @Override
                        public void doFlush(CharSequence cache, List<AsanStackTraceData> errors) {
                            boolean hasLibrary = false;
                            for (AsanStackTraceData data : errors) {
                                if (data.isRunningLibrary(LIBRARY_DICE)) {
                                    hasLibrary = true;
                                    break;
                                }
                            }
                            if (!hasLibrary) {
                                return;
                            }
                            onLogcatFlush(cache.toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (process != null) {
                        process.destroy();
                    }
                }
                Log.d(TAG, "Logcat Monitor stop.");
            }
        }
    }

}
