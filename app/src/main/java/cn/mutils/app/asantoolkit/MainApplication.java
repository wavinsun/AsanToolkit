package cn.mutils.app.asantoolkit;

import android.app.Application;

/**
 * Created by wenhua.ywh on 2017/12/26.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MainAsanClient.getInstance().start();
    }

}
