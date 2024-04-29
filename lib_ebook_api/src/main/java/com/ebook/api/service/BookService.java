package com.ebook.api.service;

import io.reactivex.Observable;
import retrofit2.http.GET;

public interface BookService {
    @GET("https://www.hitxt.cc/app/login.php?aid=1")
    Observable<String> testUrl();
}
