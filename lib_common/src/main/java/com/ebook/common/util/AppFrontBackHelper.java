package com.senseu.baby.utils.platform;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.MainThread;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppFrontBackHelper {
    private static final String TAG = "AppFrontBackHelper";
    private final List<WeakReference<OnAppStatusListener>> listeners = new ArrayList<>();

    private boolean isFront = false;

    /**
     * 默认值 -1  表示app打开过（进程起来了）
     */
    private int activityStartCount = -1;

    public boolean isFront() {
        return isFront;
    }

    /**
     * app是否打开过
     */
    public boolean isOpened() {
        return activityStartCount != -1;
    }

    private AppFrontBackHelper() {
    }

    private static class Holder {
        private static final AppFrontBackHelper instance = new AppFrontBackHelper();
    }

    public static AppFrontBackHelper getInstance() {
        return Holder.instance;
    }

    public void init(Application application) {
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    @MainThread
    public void register(OnAppStatusListener listener) {
        for (WeakReference<OnAppStatusListener> weakReference : listeners) {
            if (weakReference.get() == listener) {
                return;
            }
        }
        listeners.add(new WeakReference<>(listener));
    }

    @MainThread
    public void unRegister(OnAppStatusListener listener) {
        for (WeakReference<OnAppStatusListener> weakReferenceListener : listeners) {
            if (weakReferenceListener.get() == listener) {
                weakReferenceListener.clear();
                listeners.remove(weakReferenceListener);
                break;
            }
        }
    }

    private final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            Log.d(TAG, "onActivityCreated: " + activity.getClass().getSimpleName() + " 创建");

            //去除涂鸦SDK中云存储购买页面的Activity自定义转场效果
            if (activity.getClass().getName().contains("com.thingclips.smart.jsbridge.base.webview.WebViewActivity")) {
                Class webActivity = activity.getClass();
                try {
                    Field field = webActivity.getSuperclass().getSuperclass().getDeclaredField("mNeedDefaultAni");
                    field.setAccessible(true);
                    field.set(activity,false);
                } catch (Exception e) {
                    Log.e(TAG, "onActivityCreated: ", e);
                }
            }
            try {
                Log.d(TAG,"进入 "+ activity.getClass().getSimpleName() + " intent中参数如下");
                Intent intent = activity.getIntent();
                if (intent != null) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        Set<String> keys = extras.keySet();
                        for (String key : keys) {
                            Object value = extras.get(key);
                            if(value instanceof Parcelable) {
                                Log.d(TAG,key + ": parcel对象，不进行打印，防止干扰系统的序列化");
                            } else {
                                Log.d(TAG,key + ":" + value);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG,"打印activity跳转参数报错",e);
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            Log.d(TAG, "onActivityStarted: " + activity.getClass().getSimpleName() + " 启动");
            if(activityStartCount == -1){   //默认值是-1, 从0开始累加, 目前主要用于isOpen()方法判断
                activityStartCount = 0;
            }
            activityStartCount++;
            if (activityStartCount == 1) {
                Log.i(TAG, "onFront: 应用进入前台");
                isFront = true;
                for (WeakReference<OnAppStatusListener> listener : listeners) {
                    if (listener.get() != null) {
                        listener.get().onFront();
                    }
                }
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
            Log.d(TAG, "onActivityResumed: " + activity.getClass().getSimpleName() + " 可见");
        }

        @Override
        public void onActivityPaused(Activity activity) {
            Log.d(TAG, "onActivityPaused: " + activity.getClass().getSimpleName() + " 暂停");
        }

        @Override
        public void onActivityStopped(Activity activity) {
            Log.d(TAG, "onActivityStopped: " + activity.getClass().getSimpleName() + " 不可见");
            activityStartCount--;
            if (activityStartCount == 0) {
                Log.i(TAG, "onBack: 应用进入后台");
                isFront = false;
                for (WeakReference<OnAppStatusListener> listener : listeners) {
                    if (listener.get() != null) {
                        listener.get().onBack();
                    }
                }
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            Log.d(TAG, "onActivityDestroyed: " + activity.getClass().getSimpleName() + " 销毁");
        }
    };

    public interface OnAppStatusListener {
        default void onFront(){};

        /**
         * 与画中画 相互独立
         */
        default void onBack(){};
    }
}