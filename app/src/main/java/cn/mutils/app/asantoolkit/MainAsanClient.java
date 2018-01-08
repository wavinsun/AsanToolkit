package cn.mutils.app.asantoolkit;

import cn.mutils.app.asan.droid.AsanClient;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by wenhua.ywh on 2017/12/26.
 */

public class MainAsanClient extends AsanClient {

    private static final Executor ASAN_THREAD_POOL = Executors.newSingleThreadExecutor();
    private static MainAsanClient sInstance = new MainAsanClient();

    private MainAsanClient() {
        super();
    }

    public static MainAsanClient getInstance() {
        return sInstance;
    }

    @Override
    protected String getAppVersion() {
        return "1.0.0.0108";
    }

    @Override
    protected Executor getHttpPool() {
        return ASAN_THREAD_POOL;
    }

    @Override
    protected String getJenkinsJob() {
        return "app_1.0.0_asan";
    }
}
