package com.ebook.api;


import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.ebook.api.config.API;
import com.ebook.api.http.CustomHttpLoggingInterceptor;
import com.ebook.api.service.AwaBookService;
import com.ebook.api.service.BookService;
import com.ebook.api.service.CommentService;
import com.ebook.api.service.UserService;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@SuppressLint("StaticFieldLeak")
public class RetrofitManager {
    public static Context mContext;
    private volatile static RetrofitManager retrofitManager;
    private final Retrofit mRetrofit;
    public String TOKEN;
    private OkHttpClient.Builder okHttpBuilder;

    private RetrofitManager() {
        okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.addInterceptor(new EncodingInterceptor("GB2312"));
        okHttpBuilder.addInterceptor(new CustomHttpLoggingInterceptor());
        mRetrofit = new Retrofit.Builder()
                .client(okHttpBuilder.build())
                .baseUrl(API.BASE_URL)
                //增加返回值为Observable<T>的支持
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                //增加返回值为字符串的支持(以实体类返回)
//                .addConverterFactory(GsonConverterFactory.create())
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

    public UserService getUserService() {
        return mRetrofit.create(UserService.class);
    }

    public CommentService getCommentService() {
        return mRetrofit.create(CommentService.class);
    }

    public AwaBookService getAwaBookService() {
        return mRetrofit.create(AwaBookService.class);
    }
}