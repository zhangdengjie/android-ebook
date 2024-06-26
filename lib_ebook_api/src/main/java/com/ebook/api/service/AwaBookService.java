package com.ebook.api.service;

import com.ebook.api.config.API;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface AwaBookService {
    String URL = API.BASE_URL;
//    String COVER_URL = "http://www.sundung.com";
//    String SEARCH_URL = "http://sou.bbzayy.com";

    @GET
    @Headers({"Accept:text/html,application/xhtml+xml,application/xml",
            "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36 Edg/91.0.864.48",
            "Accept-Charset:*",
            "Content-Type:text/html; charset=UTF-8",
            "Content-Type:text/html; charset=GB2312",
            "Connection:close",
            "Cache-Control:no-cache"})
    Observable<String> getLibraryData(@Url String url);

    @GET
    @Headers({"Accept:text/html,application/xhtml+xml,application/xml",
            "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36 Edg/91.0.864.48",
            "Accept-Charset:*",
            "Content-Type:text/html; charset=UTF-8",
            "Content-Type:text/html; charset=GB2312",
            "Connection:close",
            "Cache-Control:no-cache"})
    Observable<String> getBookInfo(@Url String url);

    @GET("/application/search.php")
    @Headers({"Accept:text/html,application/xhtml+xml,application/xml",
            "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36 Edg/91.0.864.48",
            "Content-Type:application/x-www-form-urlencoded; charset=UTF-8",
            "Accept-Charset:*",
            "Content-Type:text/html; charset=UTF-8",
            "Content-Type:text/html; charset=GB2312",
            "Connection:close",
            "Cache-Control:no-cache"})
    Observable<String> searchBook(@Query(value = "words", encoded = true) String words);

    @GET
    @Headers({"Accept:text/html,application/xhtml+xml,application/xml",
            "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36 Edg/91.0.864.48",
            "Accept-Charset:*",
            "Content-Type:text/html; charset=UTF-8",
            "Content-Type:text/html; charset=GB2312",
            "Connection:close",
            "Cache-Control:no-cache"})
    Observable<String> getBookContent(@Url String url);

    @GET
    @Headers({"Accept:text/html,application/xhtml+xml,application/xml",
            "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36 Edg/91.0.864.48",
            "Accept-Charset:*",
            "Content-Type:text/html; charset=UTF-8",
            "Content-Type:text/html; charset=GB2312",
            "Connection:close",
            "Cache-Control:no-cache"})
    Observable<String> getChapterList(@Url String url);

    @GET
    @Headers({"Accept:text/html,application/xhtml+xml,application/xml",
            "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36 Edg/91.0.864.48",
            "Accept-Charset:*",
            "Content-Type:text/html; charset=UTF-8",
            "Content-Type:text/html; charset=GB2312",
            "Connection:close",
            "Cache-Control:no-cache"})
    Observable<String> getKindBooks(@Url String url);

    @GET("/version/version.php")
    @Headers({"Accept:text/html,application/xhtml+xml,application/xml",
            "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36 Edg/91.0.864.48",
            "Accept-Charset:*",
            "Content-Type:text/html; charset=UTF-8",
            "Content-Type:text/html; charset=GB2312",
            "Connection:close",
            "Cache-Control:no-cache"})
    Observable<String> checkAppUpdate();
}
