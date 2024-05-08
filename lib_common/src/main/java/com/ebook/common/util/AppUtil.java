package com.ebook.common.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public enum AppUtil {
    INSTANCE;

    private static final String TAG = "AppUtil";

    /**
     * 获取版本名称
     */
    public static int getVersionCode(Context context) throws PackageManager.NameNotFoundException {
        PackageInfo pi = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0);
        return pi.versionCode;
    }
}
