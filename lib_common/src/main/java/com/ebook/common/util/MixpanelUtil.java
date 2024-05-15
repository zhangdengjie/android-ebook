package com.ebook.common.util;

import android.content.Context;
import android.util.Log;

import com.blankj.utilcode.util.ThreadUtils;
import com.ebook.db.entity.BookInfo;
import com.ebook.db.entity.SearchBook;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public enum MixpanelUtil {

    INSTANCE;

    private static final String TAG = "MixpanelUtil";

    private MixpanelAPI mixpanel;

    private String identifyId;

    public void init(Context context) {
        identifyId = DeviceIdUtil.getDeviceId(context);
        mixpanel = MixpanelAPI.getInstance(context, "855fd1c7c98dbb63031df025b2ac0852", true);
        mixpanel.setEnableLogging(true);
        mixpanel.identify(identifyId, false);
    }

    public void openHomePage() throws JSONException, IOException {
        ThreadUtil.executeTask(new Runnable() {
            @Override
            public void run() {
                mixpanel.track("打开首页");
            }
        });
    }

    public void launchApp() {
        ThreadUtil.executeTask(new Runnable() {
            @Override
            public void run() {
                mixpanel.track("启动应用");
            }
        });
    }

    public void retriveBook(SearchBook bookInfo) {
        ThreadUtil.executeTask(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("name", bookInfo.getName());
                } catch (JSONException e) {
                    Log.e(TAG, "埋点报错", e);
                }
                mixpanel.track("查看小说概览",jsonObject);
            }
        });
    }

    public void searchBook(String words) {
        ThreadUtil.executeTask(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("words", words);
                } catch (JSONException e) {
                    Log.e(TAG, "埋点报错", e);
                }
                mixpanel.track("搜索小说",jsonObject);
            }
        });
    }

    public void readBook(BookInfo bookInfo,String chapterTitle,int page) {
        ThreadUtil.executeTask(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("name", bookInfo.getName());
                    jsonObject.put("chapterTitle", chapterTitle);
                    jsonObject.put("pageIndex", page);
                } catch (JSONException e) {
                    Log.e(TAG, "埋点报错", e);
                }
                mixpanel.track("阅读小说",jsonObject);
            }
        });
    }
}
