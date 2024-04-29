package com.ebook;

import android.util.Log;

import com.ebook.api.RetrofitManager;
import com.ebook.common.BaseApplication;
import com.ebook.common.util.AppFrontBackHelper;
import com.ebook.db.GreenDaoManager;
import com.ebook.login.interceptor.LoginInterceptor;
import com.therouter.router.NavigatorKt;


public class MyApplication extends BaseApplication implements AppFrontBackHelper.OnAppStatusListener{

    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitManager.init(this);
        GreenDaoManager.init(this);
        // 登录拦截
        NavigatorKt.addRouterReplaceInterceptor(new LoginInterceptor());
        AppFrontBackHelper.getInstance().init(this);
        AppFrontBackHelper.getInstance().register(this);
    }

    @Override
    public void onBack() {
        AppFrontBackHelper.OnAppStatusListener.super.onBack();
        Log.i(TAG, "onBack: 应用退到后台");
    }

    @Override
    public void onFront() {
        AppFrontBackHelper.OnAppStatusListener.super.onFront();
        Log.i(TAG, "onFront: 应用进入前台");
    }
}
