package cn.mutils.app.asan.droid;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 网络协议实现
 * <p>
 * Created by wenhua.ywh on 2017/12/26.
 */
public class AsanHttpTask {

    /**
     * 分界符号
     */
    private final String TAG_BOUNDARY = "--------wavinsun";

    /**
     * 请求地址
     */
    private String mUrl;
    /**
     * 文本参数
     */
    private Map<String, String> mTextParams = new HashMap<String, String>();
    /**
     * 上传文件参数
     */
    private Map<String, File> mFileParams = new HashMap<String, File>();

    public AsanHttpTask(String url) {
        mUrl = url;
    }

    /**
     * 获取请求地址
     *
     * @return
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * 设置请求
     *
     * @param url
     */
    public void setUrl(String url) {
        mUrl = url;
    }

    /**
     * 添加文本参数
     *
     * @param name
     * @param value
     * @return
     */
    public String addTextParam(String name, String value) {
        return mTextParams.put(name, value);
    }

    /**
     * 添加文件参数
     *
     * @param name
     * @param value
     * @return
     */
    public File addFileParam(String name, File value) {
        return mFileParams.put(name, value);
    }

    /**
     * 移除文本参数
     *
     * @param name
     * @return
     */
    public String removeTextParam(String name) {
        return mTextParams.remove(name);
    }

    /**
     * 移除文件参数
     *
     * @param name
     * @return
     */
    public File removeFileParam(String name) {
        return mFileParams.remove(name);
    }

    /**
     * 清空所有参数
     */
    public void clearAllParameters() {
        mTextParams.clear();
        mFileParams.clear();
    }

    /**
     * 发送请求
     * @param url
     * @return
     * @throws Exception
     */
    public String send(String url) throws Exception {
        mUrl = url;
        return send();
    }

    /**
     * 发送请求
     * @return
     * @throws Exception
     */
    public String send() throws Exception {
        HttpURLConnection conn = null;
        ByteArrayOutputStream out = null;
        try {
            conn = createConnection();
            conn.connect();
            DataOutputStream ds = new DataOutputStream(conn.getOutputStream());
            writeFileParams(ds);
            writeTextParams(ds);
            writeEndParams(ds);
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("HTTP-" + code);
            }
            InputStream is = conn.getInputStream();
            byte[] buffer = new byte[4096];
            int bufferCount = -1;
            out = new ByteArrayOutputStream();
            while ((bufferCount = is.read(buffer)) != -1) {
                out.write(buffer, 0, bufferCount);
            }
            return out.toString("UTF-8");
        } finally {
            close(out);
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private HttpURLConnection createConnection() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(mUrl).openConnection();
        conn.setUseCaches(false);
        conn.setConnectTimeout(100000);
        conn.setReadTimeout(300000);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("Accept-Charset", "UTF-8");
        conn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + TAG_BOUNDARY);
        return conn;
    }

    private void writeFileParams(DataOutputStream os) throws Exception {
        byte[] buffer = null;
        int bufferCount = -1;
        for (Map.Entry<String, File> entry : mFileParams.entrySet()) {
            String name = entry.getKey();
            File file = entry.getValue();
            os.writeBytes("--" + TAG_BOUNDARY + "\r\n");
            os.writeBytes("Content-Disposition: form-data; name=\"" + name
                    + "\"; filename=\"" + encode(file.getName()) + "\"\r\n");
            os.writeBytes("Content-Type: application/octet-stream\r\n");
            os.writeBytes("\r\n");
            if (file instanceof TextFile) {
                os.write(((TextFile) file).getText().getBytes("UTF-8"));
            } else {
                if (buffer == null) {
                    buffer = new byte[4096];
                }
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    while ((bufferCount = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bufferCount);
                    }
                } finally {
                    close(fis);
                }
            }
            os.writeBytes("\r\n");
        }
    }

    private void writeTextParams(DataOutputStream os) throws Exception {
        for (Map.Entry<String, String> entry : mTextParams.entrySet()) {
            String name = entry.getKey();
            String text = entry.getValue();
            os.writeBytes("--" + TAG_BOUNDARY + "\r\n");
            os.writeBytes("Content-Disposition: form-data; name=\"" + name
                    + "\"\r\n");
            os.writeBytes("\r\n");
            os.writeBytes(encode(text));
            os.writeBytes("\r\n");
        }
    }

    private void writeEndParams(DataOutputStream os) throws Exception {
        os.writeBytes("--" + TAG_BOUNDARY + "--");
        os.writeBytes("\r\n");
        os.writeBytes("\r\n");
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
