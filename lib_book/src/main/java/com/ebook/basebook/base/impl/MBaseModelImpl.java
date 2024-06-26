package com.ebook.basebook.base.impl;

import com.ebook.api.http.CustomHttpLoggingInterceptor;
import com.ebook.api.http.SSLSocketClient;
import com.ebook.basebook.cache.converter.EncodeConverter;

import java.net.Proxy;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MBaseModelImpl {
    protected OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS);

    public MBaseModelImpl() {
//        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
//        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
//        clientBuilder.interceptors().add(logging);
        clientBuilder.sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        });
        clientBuilder.proxy(Proxy.NO_PROXY);
        clientBuilder.hostnameVerifier(SSLSocketClient.getHostnameVerifier());
//        clientBuilder.addInterceptor(new EncodingInterceptor("UTF-8"));
        clientBuilder.addInterceptor(new EncodingInterceptor("GB2312"));
        clientBuilder.addInterceptor(new CustomHttpLoggingInterceptor());
    }

    protected Retrofit getRetrofitObject(String url) {
        return new Retrofit.Builder().baseUrl(url)
                //增加返回值为字符串的支持(以实体类返回)
                .addConverterFactory(ScalarsConverterFactory.create())
                //增加返回值为Observable<T>的支持
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(clientBuilder.build())
                .build();
    }

    protected Retrofit getRetrofitString(String url, String encode) {
        return new Retrofit.Builder().baseUrl(url)
                //增加返回值为字符串的支持(以实体类返回)
                .addConverterFactory(EncodeConverter.create(encode))
                //增加返回值为Observable<T>的支持
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(clientBuilder.build())
                .build();
    }

}