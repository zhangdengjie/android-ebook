package com.ebook.basebook.mvp.model.impl;

import static com.github.promeg.pinyinhelper.Pinyin.toPinyin;

import android.util.Log;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ebook.api.entity.BookEntity;
import com.ebook.api.service.AwaBookService;
import com.ebook.api.service.ZeroBookService;
import com.ebook.basebook.base.impl.MBaseModelImpl;
import com.ebook.basebook.base.manager.ErrorAnalyContentManager;
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
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
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
        // 根据book详情的链接,访问对应的网页,然后解析html
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
        bookInfo.setTag(TAG);
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
        bookInfo.setChapterUrl(doc.getElementsByClass("book_action").get(0).child(0).attr("href").substring(2));
        bookInfo.setOrigin(AwaBookService.URL);
        return bookInfo;
    }

    @Override
    public Observable<WebChapter<BookShelf>> getChapterList(BookShelf bookShelf) {
        String noteUrl = bookShelf.getNoteUrl();
        String aid = noteUrl.split("_")[1];
        String chapterListUrl = "http://www.ttawa.com/list.php?aid=" + aid.substring(0,aid.length() - 1);
        return getRetrofitObject(AwaBookService.URL)
                .create(AwaBookService.class)
                .getChapterList(chapterListUrl.replace(AwaBookService.URL, ""))
                .flatMap(new Function<String, ObservableSource<BookShelf>>() {
                    @Override
                    public ObservableSource<BookShelf> apply(String s) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<BookShelf>() {
                            @Override
                            public void subscribe(ObservableEmitter<BookShelf> emitter) throws Exception {
                                bookShelf.getBookInfo().getChapterlist().addAll(analyzeChapterListNew(s, bookShelf));
                                emitter.onNext(bookShelf);
                                emitter.onComplete();
                            }
                        });
                    }
                }).flatMap(new Function<BookShelf, ObservableSource<WebChapter<BookShelf>>>() {
                    @Override
                    public ObservableSource<WebChapter<BookShelf>> apply(BookShelf bookShelfWebChapter) throws Exception {
                        return analyzeChapterListForObject(bookShelfWebChapter);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        // 先获取第一页,然后拿到总共的页数,并发请求
//        return getRetrofitObject(AwaBookService.URL)
//                .create(AwaBookService.class)
//                .getChapterList(bookShelf.getBookInfo().getChapterUrl().replace(AwaBookService.URL, ""))
//                .flatMap(new Function<String, ObservableSource<BookShelf>>() {
//                    @Override
//                    public ObservableSource<BookShelf> apply(String s) throws Exception {
//                        return analyzeChapterCount(s,bookShelf);
//                    }
//                })
//                .flatMap(new Function<BookShelf, ObservableSource<BookShelf>>() {
//                    @Override
//                    public ObservableSource<BookShelf> apply(BookShelf bookShelf) throws Exception {
//                        List<Observable<List<ChapterList>>> os = new ArrayList<>();
//                        for (int i = 0; i <= bookShelf.getPageCount(); i++) {
//                            os.add(getChapterList(i, bookShelf));
//                        }
//                        return Observable.zipIterable(os, new Function<Object[], BookShelf>() {
//                            @Override
//                            public BookShelf apply(Object[] objects) throws Exception {
//                                for (Object object : objects) {
//                                    bookShelf.getBookInfo().getChapterlist().addAll(((List<ChapterList>) object));
//                                }
//                                return bookShelf;
//                            }
//                        }, false, 10);
//                    }
//                })
//                .flatMap((Function<BookShelf, ObservableSource<WebChapter<BookShelf>>>) s -> analyzeChapterListForObject(s))
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread());
    }
    private List<ChapterList> analyzeChapterListNew(String s,BookShelf bookShelf) {
        Document doc = Jsoup.parse(s);
        Elements chapterList = doc.getElementsByTag("li");
        List<ChapterList> chapters = new ArrayList<>();
        for (int i = 1; i < chapterList.size(); i++) {
            ChapterList temp = new ChapterList();
            temp.setDurChapterUrl(AwaBookService.URL + chapterList.get(i).getElementsByTag("a").attr("href"));   //id
            Log.d(TAG, "analyzeChapterList: " + temp.getDurChapterUrl());
            temp.setDurChapterIndex(i);
            temp.setDurChapterName(chapterList.get(i).getElementsByTag("a").text());
            temp.setNoteUrl(bookShelf.getNoteUrl());
            temp.setTag(AwaBookService.URL);
            chapters.add(temp);
        }
        return chapters;
    }

    private Observable<List<ChapterList>> getChapterList(int pageIndex,BookShelf bookShelf) {
        return getRetrofitObject(AwaBookService.URL)
                .create(AwaBookService.class)
                .getChapterList(bookShelf.getBookInfo().getChapterUrl().replace(AwaBookService.URL, "") + pageIndex)
                .flatMap(new Function<String, ObservableSource<List<ChapterList>>>() {
                    @Override
                    public ObservableSource<List<ChapterList>> apply(String s) throws Exception {
                        return new Observable<List<ChapterList>>() {
                            @Override
                            protected void subscribeActual(Observer<? super List<ChapterList>> observer) {
                                observer.onNext(analyzeChapterList(s,bookShelf));
                                observer.onComplete();
                            }
                        };
                    }
                });
    }

    /**
     * @param s 每个page的章节目录的html
     * @param bookShelf
     * @return
     */
    private List<ChapterList> analyzeChapterList(String s,BookShelf bookShelf) {

        Document doc = Jsoup.parse(s);
        Elements chapterList = doc.getElementsByClass("list").get(0).getElementsByTag("li");
        List<ChapterList> chapters = new ArrayList<>();
        for (int i = 0; i < chapterList.size(); i++) {
            ChapterList temp = new ChapterList();
            temp.setDurChapterUrl(AwaBookService.URL + chapterList.get(i).getElementsByTag("a").attr("href"));   //id
            Log.d(TAG, "analyzeChapterList: " + temp.getDurChapterUrl());
            temp.setDurChapterIndex(i);
            temp.setDurChapterName(chapterList.get(i).getElementsByTag("a").text());
            temp.setNoteUrl(bookShelf.getNoteUrl());
            temp.setTag(AwaBookService.URL);
            chapters.add(temp);
        }
        return chapters;
    }

    private Observable<BookShelf> analyzeChapterCount(final String s, final BookShelf bookShelf) {
        return Observable.create(e -> {
            Document doc = Jsoup.parse(s);
            bookShelf.setPageCount(Integer.parseInt(doc.getElementsByClass("pageRemark").get(0).child(0).text()));
            e.onNext(bookShelf);
            e.onComplete();
        });
    }

    private Observable<WebChapter<BookShelf>> analyzeChapterListForObject(final BookShelf bookShelf) {
        return Observable.create(e -> {
            bookShelf.setTag(AwaBookService.URL);
            WebChapter<List<ChapterList>> temp = new WebChapter<>(bookShelf.getBookInfo().getChapterlist(),false);
            bookShelf.getBookInfo().setChapterlist(temp.getData());
            e.onNext(new WebChapter<>(bookShelf, temp.getNext()));
            e.onComplete();
        });
    }

    @Override
    public Observable<BookContent> getBookContent(String durChapterUrl, int durChapterIndex) {
        return getRetrofitObject(AwaBookService.URL)
                .create(AwaBookService.class)
                .getBookContent(durChapterUrl.replace(AwaBookService.URL, ""))
                .flatMap((Function<String, ObservableSource<BookContent>>) s -> analyzeBookContent(s, durChapterUrl, durChapterIndex));
    }

    /**
     * 解析章节内容
     * @param s
     * @param durChapterUrl
     * @param durChapterIndex
     * @return
     */
    private Observable<BookContent> analyzeBookContent(final String s, final String durChapterUrl, final int durChapterIndex) {
        return Observable.create(e -> {
            BookContent bookContent = new BookContent();
            bookContent.setDurChapterIndex(durChapterIndex);
            bookContent.setDurChapterUrl(durChapterUrl);
            bookContent.setTag(AwaBookService.URL);
            try {
                Document doc = Jsoup.parse(s);
                List<TextNode> contentEs = doc.getElementById("article").textNodes();
                StringBuilder content = new StringBuilder();
                for (int i = 0; i < contentEs.size(); i++) {
                    String temp = contentEs.get(i).text().trim();
                    temp = temp.replaceAll(" ", "").replaceAll(" ", "").replaceAll("\\s*", "");
                    if (temp.length() > 0) {
                        content.append("\u3000\u3000").append(temp);
                        if (i < contentEs.size() - 1) {
                            content.append("\r\n");
                        }
                    }
                }
                bookContent.setDurCapterContent(content.toString());
                bookContent.setRight(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                ErrorAnalyContentManager.getInstance().writeNewErrorUrl(durChapterUrl);
                bookContent.setDurCapterContent(durChapterUrl.substring(0, durChapterUrl.indexOf('/', 8)) + "站点暂时不支持解析");
                bookContent.setRight(false);
            }
            e.onNext(bookContent);
            e.onComplete();
        });
    }

    /**
     * 按分类搜索书籍
     * @param url
     * @param page
     * @return
     */
    @Override
    public Observable<List<SearchBook>> getKindBook(String url, int page) {
        return getRetrofitObject(AwaBookService.URL)
                .create(AwaBookService.class)
                .getKindBooks(url + page)
                .flatMap((Function<String, ObservableSource<List<SearchBook>>>) this::analyzeKindBook);
    }

    private Observable<List<SearchBook>> analyzeKindBook(String s) {
        return Observable.create(e -> {
            Document doc = Jsoup.parse(s);
            // 解析分类书籍列表
            Elements kindBookEs = doc.getElementsByClass("book_info2");
            List<SearchBook> books = new ArrayList<>();
            for (int i = 0; i < kindBookEs.size(); i++) {
                SearchBook book = new SearchBook();
                book.setTag(TAG);
                book.setCoverUrl(kindBookEs.get(i).child(0).child(0).child(0).attr("src").replaceAll("\\.\\.",""));
                book.setNoteUrl(kindBookEs.get(i).child(1).child(1).child(0).attr("href"));
                book.setAuthor(kindBookEs.get(i).child(1).child(1).child(1).child(1).text());
                book.setDesc(kindBookEs.get(i).child(1).child(1).child(2).text());
                book.setOrigin(AwaBookService.URL);
                book.setName(kindBookEs.get(i).child(1).child(1).child(0).child(0).text());

//                item.setAuthor(kindBookEs.get(i).getElementsByClass("s4").text());
//                item.setLastChapter(kindBookEs.get(i).getElementsByTag("a").get(1).text());
//                item.setOrigin(TAG);
//                item.setName(kindBookEs.get(i).getElementsByTag("a").get(0).text());
//                item.setNoteUrl(kindBookEs.get(i).getElementsByTag("a").get(0).attr("href"));
//                item.setCoverUrl(ZeroBookService.COVER_URL + "/" + toPinyin(item.getName(), "").toLowerCase() + ".jpg");
                books.add(book);
            }
            e.onNext(books);
            e.onComplete();
        });
    }

    /**
     * 获取书城书籍列表
     * @param aCache
     * @return
     */
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
                // 解析bodybox 分类列表
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
                    // TODO: 2024/5/4 解析有没有更多 通过kindUrl是否为空来判断是否显示
//                    bookList.setKindUrl("");
                    bookList.setKindName(kindName);

                    Elements bookInfos = element.getElementsByClass("book_info");
                    for (Element bookInfo : bookInfos) {
                        // 添加第一部小说,包含封面作者简介等信息
                        SearchBook book = new SearchBook();
                        book.setTag(TAG);
                        book.setCoverUrl(bookInfo.child(0).child(0).child(0).attr("src"));
                        book.setNoteUrl(bookInfo.child(1).child(0).attr("href"));
                        book.setAuthor(bookInfo.child(1).child(1).child(1).child(1).text());
                        book.setDesc(bookInfo.child(1).child(1).child(2).text());
                        book.setKind(kindName);
                        book.setOrigin(AwaBookService.URL);
                        book.setName(bookInfo.child(1).child(1).child(0).child(0).text());
                        bookList.getBooks().add(book);
                    }
//
//
//                    // 解析列表 作者 + 小说名称 + url
//                    Element toplist = element.getElementsByClass("list").get(0);
//                    for (Element child : toplist.children()) {
//                        SearchBook b = new SearchBook();
//                        b.setOrigin(AwaBookService.URL);
//                        b.setKind(kindName);
//                        b.setName(child.child(0).attr("title"));
//                        b.setNoteUrl(child.child(0).attr("href"));
//                        b.setAuthor(child.child(0).text().split(" ")[0]);
//                        b.setTag(TAG);
//                        bookList.getBooks().add(b);
//                    }

                    // 只显示3个书籍
                    bookList.setBooks(bookList.getBooks().subList(0, 3));

                }
                emitter.onNext(result);
                emitter.onComplete();
            }
        });
    }

    @Override
    public Observable<List<SearchBook>> searchBook(String content, int page) {
        try {
            String str = URLEncoder.encode(content, "GB2312");
            return getRetrofitObject(AwaBookService.URL)
                    .create(AwaBookService.class)
                    .searchBook(str)
                    .flatMap((Function<String, ObservableSource<List<SearchBook>>>) this::analyzeSearchBook);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public Observable<List<SearchBook>> analyzeSearchBook(final String s) {
        return Observable.create(e -> {
            List<SearchBook> result = new ArrayList<>();
            Document doc = Jsoup.parse(s);
            Elements elements = doc.getElementsByClass("book_info");
            for (Element bookInfo : elements) {
                if (bookInfo.getElementsByClass("pic").isEmpty()) {
                    continue;
                }
                SearchBook book = new SearchBook();
                book.setTag(TAG);
                book.setOrigin(AwaBookService.URL);
                book.setCoverUrl(bookInfo.child(0).child(0).child(0).attr("src"));
                book.setNoteUrl(bookInfo.child(1).child(0).attr("href"));
                book.setName(bookInfo.child(1).child(0).child(0).child(0).html());
                book.setAuthor(bookInfo.child(1).child(0).child(0).child(1).html());
                book.setDesc(bookInfo.child(1).child(0).child(0).child(2).text());
                result.add(book);
            }
            e.onNext(result);
            e.onComplete();
        });
    }
}
