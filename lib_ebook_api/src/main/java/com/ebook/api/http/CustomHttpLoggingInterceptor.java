package com.ebook.api.http;

import static okhttp3.internal.http.HttpHeaders.hasBody;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kotlin.Pair;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * http请求拦截以日志详情，方便开发中调试
 */
public final class CustomHttpLoggingInterceptor implements Interceptor {

    public static final String TAG = "HttpLoggingInterceptor";

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public enum Level {
        /**
         * No logs.
         */
        NONE,
        /**
         * Logs request and response lines.
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * }</pre>
         */
        BASIC,
        /**
         * Logs request and response lines and their respective headers.
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
         * }</pre>
         */
        HEADERS,
        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END GET
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * }</pre>
         */
        BODY
    }

    private final ExecutorService executorService = new ThreadPoolExecutor(2, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "net-request-thread-" + System.currentTimeMillis());
        }
    }, new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Log.i(TAG, "rejectedExecution: " + r.toString());
            executor.submit(r);
        }
    });

    private static class LogWorker implements Runnable {

        private static final String crlf = System.getProperty("line.separator");


        private Request request;

        /**
         * 请求部分
         */
        private long startTime;
        private String requestParams;

        /**
         * 响应部分
         */
        private long endTime;
        private String response;
        private int responseCode;

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        private boolean processSuccess = true;

        public void setProcessSuccess(boolean processSuccess) {
            this.processSuccess = processSuccess;
        }

        public LogWorker(long startTime, Request request) {
            this.startTime = startTime;
            this.request = request;
        }

        public void setRequestParams(String requestParams) {
            this.requestParams = requestParams;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            sb.append(crlf)
                    .append("   HTTP请求地址：")
                    .append(request.url().toString())
                    .append(crlf)
                    .append("   HTTP请求头：");

            Iterator<Pair<String, String>> iterator = request.headers().iterator();
            while (iterator.hasNext()) {
                Pair<String, String> next = iterator.next();
                if (next != null) {
                    sb.append(crlf);
                    sb.append("        ").append(next.getFirst()).append(":").append(next.getSecond());
                }
            }
            sb
                    .append(crlf)
                    .append("   HTTP请求方法：")
                    .append(request.method())
                    .append(crlf)
                    .append("   HTTP请求参数: ")
                    .append(requestParams)
                    .append(crlf)
                    .append("   HTTP请求响应码: ")
                    .append(responseCode)
                    .append(crlf)
                    .append("   HTTP请求结果: ")
                    .append(processSuccess)
                    .append(crlf)
                    .append("   HTTP请求响应: ")
                    .append(response)
                    .append(crlf)
                    .append("   HTTP请求耗时: ")
                    .append(TimeUnit.NANOSECONDS.toMillis(endTime - startTime))
                    .append("ms");
            Log.i(TAG, sb.toString());
        }
    }

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();

        long startNs = System.nanoTime();
        LogWorker logWorker = new LogWorker(startNs, request);

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        if (!hasRequestBody) {
            logWorker.setRequestParams("no request body");
        } else if (bodyEncoded(request.headers())) {
            logWorker.setRequestParams("encoded body omitted");
        } else {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);

            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }
            // 需要判断是否为上传
            if (requestBody instanceof MultipartBody) {
                logWorker.setRequestParams("binary " + requestBody.contentLength() + "-byte body omitted");
            } else {
                if (isPlaintext(buffer)) {
                    logWorker.setRequestParams(buffer.readString(charset));
                } else {
                    logWorker.setRequestParams("binary " + requestBody.contentLength() + "-byte body omitted");
                }
            }
        }

        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            Log.e(TAG, "intercept: " + request.url() + " 请求报错", e);
            logWorker.setEndTime(System.nanoTime());
            logWorker.setProcessSuccess(false);
            logWorker.setResponse(e.getLocalizedMessage());
            executorService.submit(logWorker);
            throw e;
        }
        ResponseBody responseBody = response.body();

        long contentLength = responseBody.contentLength();

        if (hasBody(response)) {
            logWorker.setEndTime(System.nanoTime());
            logWorker.setResponseCode(response.code());
            if (bodyEncoded(response.headers())) {
                logWorker.setResponse("encoded body omitted");
            } else {
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE); // Buffer the entire body.
                Buffer buffer = source.buffer();
                Charset charset = UTF8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    try {
                        charset = contentType.charset(UTF8);
                    } catch (UnsupportedCharsetException e) {
                        logWorker.setResponse("Couldn't decode the response body; charset is likely malformed.");
                        executorService.submit(logWorker);
                        return response;
                    }
                }

                if (!isPlaintext(buffer)) {
                    logWorker.setResponse("END HTTP (binary " + buffer.size() + "-byte body omitted)");
                    executorService.submit(logWorker);
                    return response;
                }

                if (contentLength != 0) {
                    logWorker.setResponse(buffer.clone().readString(charset));
                    executorService.submit(logWorker);
                }
            }
        } else {
            logWorker.setResponse("no response body");
            executorService.submit(logWorker);
        }
        return response;
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    static boolean isPlaintext(Buffer buffer) throws EOFException {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    private boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }
}