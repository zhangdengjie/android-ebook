package com.ebook.find.mvvm.model;

import android.app.Application;

import com.ebook.basebook.cache.ACache;
import com.ebook.basebook.constant.Url;
import com.ebook.basebook.mvp.model.impl.WebBookModelImpl;
import com.ebook.common.mvvm.model.BaseModel;
import com.ebook.db.GreenDaoManager;
import com.ebook.db.entity.BookShelf;
import com.ebook.db.entity.Library;
import com.ebook.db.entity.SearchHistory;
import com.ebook.db.entity.SearchHistoryDao;
import com.ebook.find.entity.BookType;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


public class LibraryModel extends BaseModel {
    public final static String LIBRARY_CACHE_KEY = "cache_library";

    public LibraryModel(Application application) {
        super(application);
    }

    //获得书库信息
    public Observable<Library> getLibraryData(ACache mCache) {
        return Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> e) throws Exception {
                        String cache = mCache.getAsString(LIBRARY_CACHE_KEY);
                        e.onNext(cache);
                        e.onComplete();
                    }
                }).flatMap((Function<String, ObservableSource<Library>>) s -> WebBookModelImpl.getInstance().analyzeLibraryData(s))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    //获取书架书籍列表信息
    public Observable<List<BookShelf>> getBookShelfList() {
        return Observable.create((ObservableOnSubscribe<List<BookShelf>>) e -> {
                    List<BookShelf> temp = GreenDaoManager.getInstance().getmDaoSession().getBookShelfDao().queryBuilder().list();
                    if (temp == null)
                        temp = new ArrayList<>();
                    e.onNext(temp);
                    e.onComplete();
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    //将书籍信息存入书架书籍列表
    public Observable<BookShelf> saveBookToShelf(BookShelf bookShelf) {
        return Observable.create((ObservableOnSubscribe<BookShelf>) e -> {
                    GreenDaoManager.getInstance().getmDaoSession().getChapterListDao().insertOrReplaceInTx(bookShelf.getBookInfo().getChapterlist());
                    GreenDaoManager.getInstance().getmDaoSession().getBookInfoDao().insertOrReplace(bookShelf.getBookInfo());
                    //网络数据获取成功  存入BookShelf表数据库
                    GreenDaoManager.getInstance().getmDaoSession().getBookShelfDao().insertOrReplace(bookShelf);
                    e.onNext(bookShelf);
                    e.onComplete();
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    //保存查询记录
    public Observable<SearchHistory> insertSearchHistory(int type, String content) {
        return Observable.create(new ObservableOnSubscribe<SearchHistory>() {
                    @Override
                    public void subscribe(ObservableEmitter<SearchHistory> e) throws Exception {
                        List<SearchHistory> datas = GreenDaoManager.getInstance().getmDaoSession().getSearchHistoryDao()
                                .queryBuilder()
                                .where(SearchHistoryDao.Properties.Type.eq(type), SearchHistoryDao.Properties.Content.eq(content))
                                .limit(1)
                                .build().list();
                        SearchHistory searchHistory = null;
                        if (null != datas && datas.size() > 0) {
                            searchHistory = datas.get(0);
                            searchHistory.setDate(System.currentTimeMillis());
                            GreenDaoManager.getInstance().getmDaoSession().getSearchHistoryDao().update(searchHistory);
                        } else {
                            searchHistory = new SearchHistory(type, content, System.currentTimeMillis());
                            GreenDaoManager.getInstance().getmDaoSession().getSearchHistoryDao().insert(searchHistory);
                        }
                        e.onNext(searchHistory);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    //删除查询记录
    public Observable<Integer> cleanSearchHistory(int type, String content) {
        return Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                        int a = GreenDaoManager.getInstance().getDb().delete(SearchHistoryDao.TABLENAME, SearchHistoryDao.Properties.Type.columnName + "=? and " + SearchHistoryDao.Properties.Content.columnName + " like ?", new String[]{String.valueOf(type), "%" + content + "%"});
                        e.onNext(a);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    //获得查询记录
    public Observable<List<SearchHistory>> querySearchHistory(int type, String content) {
        return Observable.create(new ObservableOnSubscribe<List<SearchHistory>>() {
                    @Override
                    public void subscribe(ObservableEmitter<List<SearchHistory>> e) throws Exception {
                        List<SearchHistory> datas = GreenDaoManager.getInstance().getmDaoSession().getSearchHistoryDao()
                                .queryBuilder()
                                .where(SearchHistoryDao.Properties.Type.eq(type), SearchHistoryDao.Properties.Content.like("%" + content + "%"))
                                .orderDesc(SearchHistoryDao.Properties.Date)
                                .limit(20)
                                .build().list();
                        e.onNext(datas);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    //获取书籍类型信息，此处用本地数据。
    public List<BookType> getBookTypeList() {
        List<BookType> bookTypeList = new ArrayList<>();
        bookTypeList.add(new BookType("玄幻奇幻", Url.xh_qh));
        bookTypeList.add(new BookType("武侠仙侠", Url.wx_xx));
        bookTypeList.add(new BookType("都市言情", Url.ds_yq));
        bookTypeList.add(new BookType("历史军事", Url.ls_js));
        bookTypeList.add(new BookType("科幻灵异", Url.kh_ly));
        bookTypeList.add(new BookType("网游竞技", Url.wy_jj));
        bookTypeList.add(new BookType("女生频道", Url.np));
        return bookTypeList;
    }

    //获取书籍类型信息，此处用本地数据。
    public List<BookType> getBookTypeListAwa() {
        List<BookType> bookTypeList = new ArrayList<>();
        bookTypeList.add(new BookType("玄幻奇幻", "/wapsort1/"));
        bookTypeList.add(new BookType("武侠仙侠", "/wapsort2/"));
        bookTypeList.add(new BookType("都市言情", "/wapsort3/"));
        bookTypeList.add(new BookType("历史军事", "/wapsort4/"));
        bookTypeList.add(new BookType("科幻灵异", "/wapsort5/"));
        bookTypeList.add(new BookType("网游竞技", "/wapsort6/"));
        bookTypeList.add(new BookType("女生频道", "/wapfull/"));
        return bookTypeList;
    }

}
