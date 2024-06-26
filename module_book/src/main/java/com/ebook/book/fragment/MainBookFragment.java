package com.ebook.book.fragment;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.databinding.ObservableList;
import androidx.lifecycle.ViewModelProvider;

import com.ebook.basebook.base.manager.BitIntentDataManager;
import com.ebook.basebook.mvp.presenter.impl.BookDetailPresenterImpl;
import com.ebook.basebook.mvp.presenter.impl.ReadBookPresenterImpl;
import com.ebook.basebook.mvp.view.impl.BookDetailActivity;
import com.ebook.basebook.mvp.view.impl.ImportBookActivity;
import com.ebook.basebook.mvp.view.impl.ReadBookActivity;
import com.ebook.basebook.view.popupwindow.DownloadListPop;
import com.ebook.book.BR;
import com.ebook.book.R;
import com.ebook.book.adapter.BookListAdapter;
import com.ebook.book.databinding.FragmentBookMainBinding;
import com.ebook.book.mvvm.factory.BookViewModelFactory;
import com.ebook.book.mvvm.viewmodel.BookListViewModel;
import com.ebook.book.service.DownloadService;
import com.ebook.common.event.RxBusTag;
import com.ebook.common.mvvm.BaseMvvmRefreshFragment;
import com.ebook.common.util.ObservableListUtil;
import com.ebook.db.entity.BookShelf;
import com.ebook.db.entity.DownloadChapterList;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;
import com.refresh.lib.DaisyRefreshLayout;

import java.util.Timer;
import java.util.TimerTask;

public class MainBookFragment extends BaseMvvmRefreshFragment<FragmentBookMainBinding, BookListViewModel> {
    private BookListAdapter mBookListAdatper;
    private ImageButton ibAdd;
    private ImageButton ibDownload;
    private DownloadListPop downloadListPop;

    public static MainBookFragment newInstance() {
        return new MainBookFragment();
    }

    @Override
    public Class<BookListViewModel> onBindViewModel() {
        return BookListViewModel.class;
    }

    @Override
    public ViewModelProvider.Factory onBindViewModelFactory() {
        return BookViewModelFactory.getInstance(mActivity.getApplication());
    }

    @Override
    public void initViewObservable() {

    }

    @Override
    public int onBindVariableId() {
        return BR.viewModel;
    }

    @Override
    public int onBindLayout() {
        return R.layout.fragment_book_main;
    }

    @Override
    public void initView(View view) {
        downloadListPop = new DownloadListPop(mActivity);
        ibAdd = view.findViewById(R.id.ib_add);
        ibDownload = view.findViewById(R.id.ib_download);
        mBookListAdatper = new BookListAdapter(mActivity, mViewModel.getList());
        mViewModel.getList().addOnListChangedCallback(ObservableListUtil.getListChangedCallback(mBookListAdatper));
        mViewModel.getList().addOnListChangedCallback(new ObservableList.OnListChangedCallback() {
            @Override
            public void onChanged(ObservableList sender) {
                mBookListAdatper.notifyDataSetChanged();
                showEmpty();
            }

            @Override
            public void onItemRangeChanged(ObservableList sender, int positionStart, int itemCount) {
                mBookListAdatper.notifyItemRangeChanged(positionStart, itemCount);
            }

            @Override
            public void onItemRangeInserted(ObservableList sender, int positionStart, int itemCount) {
                mBookListAdatper.notifyItemRangeInserted(positionStart, itemCount);
                showEmpty();
            }

            @Override
            public void onItemRangeMoved(ObservableList sender, int fromPosition, int toPosition, int itemCount) {
                if (itemCount == 1) {
                    mBookListAdatper.notifyItemMoved(fromPosition, toPosition);
                } else {
                    mBookListAdatper.notifyDataSetChanged();
                }
                showEmpty();
            }

            @Override
            public void onItemRangeRemoved(ObservableList sender, int positionStart, int itemCount) {
                mBookListAdatper.notifyItemRangeRemoved(positionStart, itemCount);
                showEmpty();
            }
        });
        mBinding.recview.setAdapter(mBookListAdatper);
    }

    private void showEmpty() {
        if (mBookListAdatper.getItemCount() == 0) {
            mBinding.recview.setVisibility(View.GONE);
            mBinding.tvNoData.setVisibility(View.VISIBLE);
        } else {
            mBinding.recview.setVisibility(View.VISIBLE);
            mBinding.tvNoData.setVisibility(View.GONE);
        }
    }

    @Override
    public void initData() {
        mViewModel.refreshData();

    }

    @Override
    public void initListener() {
        super.initListener();
        ibDownload.setOnClickListener(v -> downloadListPop.showAsDropDown(ibDownload));

        ibAdd.setOnClickListener(v -> {
            //点击更多
            startActivity(new Intent(mActivity, ImportBookActivity.class));
        });
        mBookListAdatper.setItemClickListener((bookShelf, position) -> {
            Intent intent = new Intent(mActivity, ReadBookActivity.class);
            intent.putExtra("from", ReadBookPresenterImpl.OPEN_FROM_APP);
            String key = String.valueOf(System.currentTimeMillis());
            intent.putExtra("data_key", key);
            try {
                BitIntentDataManager.getInstance().putData(key, bookShelf.clone());
            } catch (CloneNotSupportedException e) {
                BitIntentDataManager.getInstance().putData(key, bookShelf);
                e.printStackTrace();
            }
            startActivity(intent);
        });
        mBookListAdatper.setOnItemLongClickListener((bookShelf, postion) -> {
            Intent intent = new Intent(mActivity, BookDetailActivity.class);
            intent.putExtra("from", BookDetailPresenterImpl.FROM_BOOKSHELF);
            String key = String.valueOf(System.currentTimeMillis());
            intent.putExtra("data_key", key);
            BitIntentDataManager.getInstance().putData(key, bookShelf);
            startActivity(intent);
            return true;
        });

    }

    @Override
    public String getToolbarTitle() {
        return "我的书架";
    }

    @Override
    public DaisyRefreshLayout getRefreshLayout() {
        return mBinding.refviewBookList;
    }


    @Subscribe(thread = EventThread.MAIN_THREAD,
            tags = {
                    @Tag(RxBusTag.HAD_ADD_BOOK),
                    @Tag(RxBusTag.HAD_REMOVE_BOOK),
                    @Tag(RxBusTag.UPDATE_BOOK_PROGRESS)
            }
    )
    public void hadAddOrRemoveBook(BookShelf bookShelf) {
        mViewModel.refreshData();
        //autoLoadData();
    }

    @Subscribe(thread = EventThread.NEW_THREAD,
            tags = {
                    @Tag(RxBusTag.START_DOWNLOAD_SERVICE)
            }
    )
    public void startDownloadService(DownloadChapterList result) {
        Log.e(TAG, "startDownloadService: 开启下载服务");
        mActivity.startService(new Intent(mActivity, DownloadService.class));
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // 逻辑处理
                RxBus.get().post(RxBusTag.ADD_DOWNLOAD_TASK, result);
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 500); // 延迟0.5秒，执行一次task
    }

}
