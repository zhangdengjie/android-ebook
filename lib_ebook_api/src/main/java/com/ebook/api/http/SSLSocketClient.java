package com.ebook.api.http;

import android.util.Log;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * <p>
 * 忽略https证书验证
 */

public class SSLSocketClient {

    private static final String TAG = "SSLSocketClient";

    //获取这个SSLSocketFactory
    public static SSLSocketFactory getSSLSocketFactory() {
        try {
//            SSLContext sslContext = SSLContext.getInstance("SSL");
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, getTrustManager(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //获取TrustManager
    private static TrustManager[] getTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                //do nothing
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                //do nothing
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        }};
        return trustAllCerts;
    }

    //获取HostnameVerifier
    public static HostnameVerifier getHostnameVerifier() {
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession sslSession) {
                // 只有 https 请求才会走进来验证， http 不进入该方法
//                if (TextUtils.isEmpty(hostname)) {
//                    return false;
//                }
//                return hostname.contains("sense-u.com");
                Log.i(TAG, "verify: " + hostname);
                return true;
            }
        };
        return hostnameVerifier;
    }
}