package cn.mutils.app.asantoolkit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import cn.mutils.app.asan.AsanLogReader;
import cn.mutils.app.asan.droid.AsanClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private TextView mMsgView;
    private ScrollView mScrollView;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Thread mLogThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(new String[]{
                        "logcat", "-v", "time", "-e", "asan_logs(.*)", "-T",
                        AsanLogReader.LOG_DATE_FORMAT.format(new Date())});
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    final String lineText = line;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mMsgView.getText().length() > 500000) {
                                mMsgView.setText(lineText);
                            } else {
                                mMsgView.append(lineText);
                            }
                            mMsgView.append("\n");
                            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMsgView = (TextView) this.findViewById(R.id.message);
        mScrollView = (ScrollView) this.findViewById(R.id.scroll);
        mLogThread.start();
        findViewById(R.id.log_asan_info).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(
                            getAssets().open("asan.log"), "UTF-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        Log.d(AsanClient.TAG_PROCESS, line);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        mLogThread.interrupt();
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
