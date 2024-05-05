package com.ebook.basebook.mvp.model.impl;

import static com.github.promeg.pinyinhelper.Pinyin.toPinyin;

import android.util.Log;

import com.ebook.api.service.AwaBookService;
import com.ebook.api.service.ZeroBookService;
import com.ebook.basebook.base.impl.MBaseModelImpl;
import com.ebook.basebook.cache.ACache;
import com.ebook.basebook.mvp.model.StationBookModel;
import com.ebook.db.entity.BookContent;
import com.ebook.db.entity.BookInfo;
import com.ebook.db.entity.BookShelf;
import com.ebook.db.entity.ChapterList;
import com.ebook.db.entity.Library;
import com.ebook.db.entity.LibraryKindBookList;
import com.ebook.db.entity.LibraryNewBook;
import com.ebook.db.entity.SearchBook;
import com.ebook.db.entity.WebChapter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class AwaBookModelImpl extends MBaseModelImpl implements StationBookModel {

    private final String TAG = "AwaBookModelImpl";

    private AwaBookModelImpl() {

    }

    private static class Holder {
        private static final AwaBookModelImpl instance = new AwaBookModelImpl();
    }

    public static AwaBookModelImpl getInstance() {
        return AwaBookModelImpl.Holder.instance;
    }

    @Override
    public Observable<BookShelf> getBookInfo(BookShelf bookShelf) {
        // 根据book的链接,访问对应的网页,然后解析html
        return getRetrofitObject(AwaBookService.URL)
                .create(AwaBookService.class)
                .getBookInfo(bookShelf.getNoteUrl().replace(AwaBookService.URL, ""))
                .flatMap((Function<String, ObservableSource<BookShelf>>) s -> analyzeBookInfo(s, bookShelf));
    }

    private Observable<BookShelf> analyzeBookInfo(String s, BookShelf bookShelf) {
        return Observable.create(e -> {
            bookShelf.setTag(AwaBookService.URL);
            bookShelf.setBookInfo(analyzeBookInfo(s, bookShelf.getNoteUrl()));
            e.onNext(bookShelf);
            e.onComplete();
        });
    }

    private BookInfo analyzeBookInfo(String s, String novelUrl) {
        // 解析bookInfo
        BookInfo bookInfo = new BookInfo();
        bookInfo.setNoteUrl(novelUrl);   //id
        bookInfo.setTag(AwaBookService.URL);
        // 解析html
        Document doc = Jsoup.parse(s);
        bookInfo.setCoverUrl(doc.getElementsByClass("book_info").get(0).getElementsByClass("pic").get(0).child(0).attr("src"));
        bookInfo.setName(doc.getElementsByClass("book_info_box").get(0).getElementsByClass("book_r_box").get(0).child(0).child(0).text());
        bookInfo.setAuthor(doc.getElementsByClass("book_info_box").get(0).getElementsByClass("book_r_box").get(0).child(0).child(1).child(0).text());
        bookInfo.setIntroduce(doc.getElementById("jianjie").child(0).wholeText());
        if (bookInfo.getIntroduce().equals("\u3000\u3000")) {
            bookInfo.setIntroduce("暂无简介");
        }

        // 解析章节信息
        bookInfo.setChapterUrl(novelUrl);
        bookInfo.setOrigin(TAG);
        return bookInfo;
    }

    @Override
    public Observable<WebChapter<BookShelf>> getChapterList(BookShelf bookShelf) {
        return getRetrofitObject(AwaBookService.URL)
                .create(AwaBookService.class)
                .getChapterList(bookShelf.getBookInfo().getChapterUrl().replace(AwaBookService.URL, ""))
                .flatMap((Function<String, ObservableSource<WebChapter<BookShelf>>>) s -> analyzeChapterList(s, bookShelf))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<WebChapter<BookShelf>> analyzeChapterList(final String s, final BookShelf bookShelf) {
        return Observable.create(e -> {
            bookShelf.setTag(AwaBookService.URL);
            WebChapter<List<ChapterList>> temp = analyzeChapterList(s, bookShelf.getNoteUrl());
            bookShelf.getBookInfo().setChapterlist(temp.getData());
            e.onNext(new WebChapter<>(bookShelf, temp.getNext()));
            e.onComplete();
        });
    }

    /**
     * 解析章节信息
     * @param s
     * @param novelUrl
     * @return
     */
    private WebChapter<List<ChapterList>> analyzeChapterList(String s, String novelUrl) {

        Document doc = Jsoup.parse(s);
        Elements chapterList = doc.getElementsByClass("list").get(0).getElementsByTag("li");
        List<ChapterList> chapters = new ArrayList<>();
        for (int i = 0; i < chapterList.size(); i++) {
            ChapterList temp = new ChapterList();
            temp.setDurChapterUrl(AwaBookService.URL + chapterList.get(i).getElementsByTag("a").attr("href"));   //id
            Log.d(TAG, "analyzeChapterList: " + temp.getDurChapterUrl());
            temp.setDurChapterIndex(i);
            temp.setDurChapterName(chapterList.get(i).getElementsByTag("a").text());
            temp.setNoteUrl(novelUrl);
            temp.setTag(AwaBookService.URL);
            chapters.add(temp);
        }
        return new WebChapter<>(chapters, false);
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
        return getRetrofitObject(AwaBookService.URL).create(AwaBookService.class).getLibraryData("").flatMap((Function<String, ObservableSource<Library>>) s -> {
            if (s.length() > 0 && aCache != null) {
                aCache.put("cache_library", s);
            }
            return analyzeLibraryData(s);
        });
    }

    @Override
    public Observable<Library> analyzeLibraryData(String data) {
        return Observable.create(new ObservableOnSubscribe<Library>() {
            @Override
            public void subscribe(ObservableEmitter<Library> emitter) throws Exception {
                Library result = new Library();
                Document doc = Jsoup.parse(data);

                List<LibraryKindBookList> kindBooks = new ArrayList<>();
                result.setKindBooks(kindBooks);
                // 解析bodybox 分类
                Elements modules = doc
                        .getElementsByAttributeValue("class", "bodybox").get(0).getElementsByAttributeValue("class","module");
                for (Element element : modules) {
                    if (element.children().size() < 4) {
                        // 解决不是推荐分类的OOB异常
                        continue;
                    }
                    LibraryKindBookList bookList = new LibraryKindBookList();
                    kindBooks.add(bookList);
                    String kindName = element.getElementsByTag("h2").get(0).getElementsByTag("span").get(0).text();
                    // TODO: 2024/5/4 解析有没有更多
                    bookList.setKindUrl("");
                    bookList.setKindName(kindName);
                    // 添加第一部小说,包含封面作者简介等信息
                    SearchBook book = new SearchBook();
                    book.setTag(TAG);
                    Element bookInfo = element.getElementsByClass("book_info").get(0);
                    book.setCoverUrl(bookInfo.child(0).child(0).child(0).attr("src"));
                    book.setNoteUrl(bookInfo.child(1).child(0).attr("href"));
                    book.setAuthor(bookInfo.child(1).child(1).child(1).child(1).text());
                    book.setDesc(bookInfo.child(1).child(1).child(2).text());
                    book.setKind(kindName);
                    book.setOrigin(AwaBookService.URL);
                    book.setName(bookInfo.child(1).child(1).child(0).child(0).text());
                    bookList.getBooks().add(book);

                    // 解析列表 作者 + 小说名称 + url
                    Element toplist = element.getElementsByClass("list").get(0);
                    for (Element child : toplist.children()) {
                        SearchBook b = new SearchBook();
                        b.setOrigin(AwaBookService.URL);
                        b.setKind(kindName);
                        b.setName(child.child(0).attr("title"));
                        b.setNoteUrl(child.child(0).attr("href"));
                        b.setAuthor(child.child(0).text().split(" ")[0]);
                        b.setTag(TAG);
                        bookList.getBooks().add(b);
                    }

                }
                emitter.onNext(result);
                emitter.onComplete();
            }
        });
    }

    @Override
    public Observable<List<SearchBook>> searchBook(String content, int page) {
        return null;
    }
}
