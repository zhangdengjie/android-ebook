package com.ebook.db.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.ebook.db.event.DBCode;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

/**
 * 书架item
 */

@Entity
public class BookShelf implements Parcelable, Cloneable {
    @Transient
    public static final long REFRESH_TIME = 5 * 60 * 1000;   //更新时间间隔 至少
    @Transient
    public static final String LOCAL_TAG = "loc_book";
    @Transient
    public static final Creator<BookShelf> CREATOR = new Creator<>() {
        @Override
        public BookShelf createFromParcel(Parcel in) {
            return new BookShelf(in);
        }

        @Override
        public BookShelf[] newArray(int size) {
            return new BookShelf[size];
        }
    };
    @Id
    private String noteUrl; //对应BookInfo noteUrl;
    private int durChapter;   //当前章节 （包括番外）
    private int durChapterPage = DBCode.BookContentView.DURPAGEINDEXBEGIN;
    private long finalDate;  //最后阅读时间
    private String tag;
    /**
     * 章节url根地址
     */
    private String chapterUrl;
    /**
     * 网页上总共的页数
     */
    private int pageCount;
    @Transient
    private BookInfo bookInfo = new BookInfo();

    protected BookShelf(Parcel in) {
        noteUrl = in.readString();
        durChapter = in.readInt();
        durChapterPage = in.readInt();
        finalDate = in.readLong();
        tag = in.readString();
        chapterUrl = in.readString();
        pageCount = in.readInt();
        bookInfo = in.readParcelable(BookInfo.class.getClassLoader());
    }

    @Generated(hash = 382077098)
    public BookShelf(String noteUrl, int durChapter, int durChapterPage,
            long finalDate, String tag, String chapterUrl, int pageCount) {
        this.noteUrl = noteUrl;
        this.durChapter = durChapter;
        this.durChapterPage = durChapterPage;
        this.finalDate = finalDate;
        this.tag = tag;
        this.chapterUrl = chapterUrl;
        this.pageCount = pageCount;
    }

    @Generated(hash = 547688644)
    public BookShelf() {
    }

    public static Creator<BookShelf> getCREATOR() {
        return CREATOR;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(noteUrl);
        dest.writeInt(durChapter);
        dest.writeInt(durChapterPage);
        dest.writeLong(finalDate);
        dest.writeString(tag);
        dest.writeString(chapterUrl);
        dest.writeInt(pageCount);
        dest.writeParcelable(bookInfo, flags);
    }

    public String getNoteUrl() {
        return this.noteUrl;
    }

    public void setNoteUrl(String noteUrl) {
        this.noteUrl = noteUrl;
    }

    public int getDurChapter() {
        return this.durChapter;
    }

    public void setDurChapter(int durChapter) {
        this.durChapter = durChapter;
    }

    public int getDurChapterPage() {
        return this.durChapterPage;
    }

    public void setDurChapterPage(int durChapterPage) {
        this.durChapterPage = durChapterPage;
    }

    public long getFinalDate() {
        return this.finalDate;
    }

    public void setFinalDate(long finalDate) {
        this.finalDate = finalDate;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public BookInfo getBookInfo() {
        return bookInfo;
    }

    public void setBookInfo(BookInfo bookInfo) {
        this.bookInfo = bookInfo;
    }

    @NonNull
    @Override
    public Object clone() throws CloneNotSupportedException {
        BookShelf bookShelf = (BookShelf) super.clone();
        bookShelf.noteUrl = noteUrl;
        bookShelf.tag = tag;
        bookShelf.bookInfo = (BookInfo) bookInfo.clone();
        return bookShelf;
    }

    public String getChapterUrl() {
        return this.chapterUrl;
    }

    public void setChapterUrl(String chapterUrl) {
        this.chapterUrl = chapterUrl;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
}