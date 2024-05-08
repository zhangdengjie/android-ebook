package com.ebook.main

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.ebook.api.RetrofitManager
import com.ebook.common.mvvm.BaseActivity
import com.ebook.common.provider.IBookProvider
import com.ebook.common.provider.IFindProvider
import com.ebook.common.provider.IMeProvider
import com.ebook.common.util.ToastUtil
import com.ebook.main.entity.MainChannel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.therouter.TheRouter
import com.trello.rxlifecycle3.android.ActivityEvent
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.annotations.Async.Schedule
import org.jsoup.Jsoup


class MainActivity : BaseActivity() {
    private var mBookFragment: Fragment? = null
    private var mFindFragment: Fragment? = null
    private var mMeFragment: Fragment? = null
    private var mCurrFragment: Fragment? = null
    private var exitTime: Long = 0
    override fun onBindLayout(): Int {
        return R.layout.activity_main
    }

    override fun enableToolbar(): Boolean {
        return false
    }

    override fun initView() {
        val navigation = findViewById<BottomNavigationView>(R.id.navigation_main)
        navigation.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            val i = menuItem.itemId
            if (i == R.id.navigation_trip) {
                switchContent(mCurrFragment, mBookFragment, MainChannel.BOOKSHELF.name)
                mCurrFragment = mBookFragment
                return@setOnNavigationItemSelectedListener true
            } else if (i == R.id.navigation_discover) {
                switchContent(mCurrFragment, mFindFragment, MainChannel.BOOKSTORE.name)
                mCurrFragment = mFindFragment
                return@setOnNavigationItemSelectedListener true
            } else if (i == R.id.navigation_me) {
                switchContent(mCurrFragment, mMeFragment, MainChannel.ME.name)
                mCurrFragment = mMeFragment
                return@setOnNavigationItemSelectedListener true
            }
            false
        }
        mBookFragment = TheRouter.get(IBookProvider::class.java)?.getMainBookFragment()
        mFindFragment = TheRouter.get(IFindProvider::class.java)?.getMainFindFragment()
        mMeFragment = TheRouter.get(IMeProvider::class.java)?.getMainMeFragment()
        mCurrFragment = mBookFragment
        if (mBookFragment != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_content, mBookFragment!!, MainChannel.BOOKSHELF.name).commit()
        }
    }

    override fun initData() {
        checkAppUpdate()
    }
    fun switchContent(from: Fragment?, to: Fragment?, tag: String?) {
        if (from == null || to == null) {
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        if (!to.isAdded) {
            transaction.hide(from).add(R.id.frame_content, to, tag).commit()
        } else {
            transaction.hide(from).show(to).commit()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun exit() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            ToastUtil.showToast("再按一次退出程序")
            exitTime = System.currentTimeMillis()
        } else {
            finish()
            System.exit(0)
        }
    }

    @SuppressLint("CheckResult")
    private fun checkAppUpdate() {
        RetrofitManager.getInstance().awaBookService.checkAppUpdate()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindUntilEvent(ActivityEvent.DESTROY))
            .subscribe(object : Observer<String>{
                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: 检测更新报错", e)
                }

                override fun onComplete() {
                }

                override fun onNext(t: String) {
                    // 解析html
                    val doc = Jsoup.parse(t)
                    val version = doc.getElementsByTag("version")[0].text()
                    val info = doc.getElementsByTag("info")[0].text()
                    val d = AlertDialog.Builder(this@MainActivity)
                        .setTitle("APP有新版本")
                        .setMessage(info)
                        .setNegativeButton("取消",object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                dialog?.dismiss()
                            }
                        })
                        .setPositiveButton("更新",object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                Log.i(TAG, "onClick: 去下载新版本的包,并且安装")
                            }
                        })
                        .create()
                    d.show()
                }
            })
    }
}
