package com.ebook.basebook.mvp.model.impl;

import static com.github.promeg.pinyinhelper.Pinyin.toPinyin;

import com.ebook.api.service.BookService;
import com.ebook.api.service.ZeroBookService;
import com.ebook.basebook.base.impl.MBaseModelImpl;
import com.ebook.basebook.cache.ACache;
import com.ebook.basebook.mvp.model.StationBookModel;
import com.ebook.db.entity.BookContent;
import com.ebook.db.entity.BookInfo;
import com.ebook.db.entity.BookShelf;
import com.ebook.db.entity.Library;
import com.ebook.db.entity.SearchBook;
import com.ebook.db.entity.WebChapter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MyBookModelImpl extends MBaseModelImpl implements StationBookModel {

    private final String TAG = "test";

    private MyBookModelImpl() {

    }

    private static class Holder {
        private static final MyBookModelImpl instance = new MyBookModelImpl();
    }

    public static MyBookModelImpl getInstance() {
        return Holder.instance;
    }

    @Override
    public Observable<BookShelf> getBookInfo(BookShelf bookShelf) {
        return getRetrofitString(ZeroBookService.URL,"utf8")
                .create(BookService.class)
                .testUrl()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<String, ObservableSource<BookShelf>>) s -> analyzeBookInfo(s, bookShelf));
    }

    private Observable<BookShelf> analyzeBookInfo(String s, BookShelf bookShelf) {
        return Observable.create(e -> {
            bookShelf.setTag(ZeroBookService.URL);
            bookShelf.setBookInfo(analyzeBookInfo(s, bookShelf.getNoteUrl()));
            e.onNext(bookShelf);
            e.onComplete();
        });
    }

    private BookInfo analyzeBookInfo(String s, String novelUrl) {
        BookInfo bookInfo = new BookInfo();
        if (true) {
            return bookInfo;
        }
        bookInfo.setNoteUrl(novelUrl);   //id
        bookInfo.setTag(ZeroBookService.URL);
        // TODO: 2024/4/29 zhangdengjie 解析 书本内容
        Document doc = Jsoup.parse(s);
        bookInfo.setName(doc.getElementsByClass("info").get(0).getElementsByTag("h1").get(0).text());
        bookInfo.setAuthor(doc.getElementsByClass("info").get(0).getElementsByTag("p").get(0).text().replace("作&nbsp;&nbsp;&nbsp;&nbsp;者：", ""));
        bookInfo.setIntroduce("\u3000\u3000" + doc.getElementsByAttributeValue("class", "desc xs-hidden").get(0).text());
        if (bookInfo.getIntroduce().equals("\u3000\u3000")) {
            bookInfo.setIntroduce("暂无简介");
        }
        bookInfo.setCoverUrl(ZeroBookService.COVER_URL + "/" + toPinyin(bookInfo.getName(), "").toLowerCase() + ".jpg");
        bookInfo.setChapterUrl(novelUrl);
        bookInfo.setOrigin(TAG);

        return bookInfo;
    }

    @Override
    public Observable<WebChapter<BookShelf>> getChapterList(BookShelf bookShelf) {
        return null;
    }

    @Override
    public Observable<BookContent> getBookContent(String durChapterUrl, int durChapterIndex) {
        return null;
    }

    @Override
    public Observable<List<SearchBook>> getKindBook(String url, int page) {
        return null;
    }

    @Override
    public Observable<Library> getLibraryData(ACache aCache) {
        return Observable.create(new ObservableOnSubscribe<Library>() {
            @Override
            public void subscribe(ObservableEmitter<Library> emitter) throws Exception {
                Library value = new Library();
                value.setKindBooks(new ArrayList<>());
                value.setLibraryNewBooks(new ArrayList<>());
                emitter.onNext(value);
                emitter.onComplete();
            }
        });
    }

    @Override
    public Observable<Library> analyzeLibraryData(String data) {
        return null;
    }

    @Override
    public Observable<List<SearchBook>> searchBook(String content, int page) {
        getBookInfo(new BookShelf()).subscribe();
        return Observable.create(new ObservableOnSubscribe<List<SearchBook>>() {
            @Override
            public void subscribe(ObservableEmitter<List<SearchBook>> emitter) throws Exception {
                emitter.onNext(new ArrayList<>());
                emitter.onComplete();
            }
        });
    }
}
