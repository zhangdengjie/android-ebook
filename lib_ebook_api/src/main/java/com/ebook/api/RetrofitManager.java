package com.ebook.api;


import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.ebook.api.config.API;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@SuppressLint("StaticFieldLeak")
public class RetrofitManager {
    private volatile static RetrofitManager retrofitManager;
    public static Context mContext;
    private Retrofit mRetrofit;
    public String TOKEN;
    private OkHttpClient.Builder okHttpBuilder;

    private RetrofitManager() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.interceptors().add(logging);
        mRetrofit = new Retrofit.Builder()
                .client(okHttpBuilder.build())
                .baseUrl(API.URL_HOST_USER)
                //增加返回值为Oservable<T>的支持
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                //增加返回值为字符串的支持(以实体类返回)
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
    }

    public static void init(Application application) {
        mContext = application;
    }

    public static RetrofitManager getInstance() {
        if (retrofitManager == null) {
            synchronized (RetrofitManager.class) {
                if (retrofitManager == null) {
                    retrofitManager = new RetrofitManager();
                }
            }
        }
        return retrofitManager;
    }

    public CommonService getCommonService() {

        return mRetrofit.create(CommonService.class);
    }

    public NewsTypeService getNewsTypeService() {

        return mRetrofit.create(NewsTypeService.class);
    }

    public NewsDetailService getNewsDetailService() {

        return mRetrofit.create(NewsDetailService.class);
    }

    public void addToken(final String token) {
        if (okHttpBuilder != null)
            okHttpBuilder.addInterceptor(new Interceptor() {
                @NotNull
                @Override
                public Response intercept(@NotNull Chain chain) throws IOException {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .addHeader("Authorization", "Bearer " + token)
                            .build();
                    return chain.proceed(request);
                }
            });
    }

}