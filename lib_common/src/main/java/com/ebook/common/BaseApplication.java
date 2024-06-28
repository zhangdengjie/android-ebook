package com.ebook.common;

import android.app.Application;
import android.text.TextUtils;

import com.ebook.common.util.log.KLog;
import com.facebook.stetho.Stetho;
import com.xixi.security.Monitor;

public class BaseApplication extends Application {
    private static BaseApplication mApplication;

    public static BaseApplication getInstance() {
        return mApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApplication = this;
        KLog.init(BuildConfig.IS_DEBUG);
        Stetho.initializeWithDefaults(this);
    }

    private String baseUrl;
    public synchronized String getBaseUrl() {
        if (TextUtils.isEmpty(baseUrl)) {
            baseUrl = new Monitor().baseUrl();
        }
        return baseUrl;
    }
}
