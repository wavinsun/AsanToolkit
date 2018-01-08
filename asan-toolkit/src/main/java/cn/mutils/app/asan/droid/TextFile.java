package cn.mutils.app.asan.droid;

import java.io.File;

/**
 * 虚拟文本文件
 * Created by wenhua.ywh on 2017/12/26.
 */
public class TextFile extends File {

    private String mText;

    public TextFile(String name, String text) {
        super(name);
        mText = text;
    }

    /**
     * 获取文件内容
     *
     * @return
     */
    public String getText() {
        return mText;
    }
}
