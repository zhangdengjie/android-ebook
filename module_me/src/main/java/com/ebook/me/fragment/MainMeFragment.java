package com.ebook.me.fragment;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.SPUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.ebook.api.config.API;
import com.ebook.common.event.KeyCode;
import com.ebook.common.event.RxBusTag;
import com.ebook.common.mvvm.BaseFragment;
import com.ebook.common.view.SettingBarView;
import com.ebook.common.view.profilePhoto.CircleImageView;
import com.ebook.me.R;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;
import com.therouter.TheRouter;


public class MainMeFragment extends BaseFragment {

    private SettingBarView mSetting;
    private Button mButton;
    private SettingBarView mSetComment;
    private SettingBarView mSetInform;
    private CircleImageView mCircleImageView;
    private TextView mTextView;

    public static MainMeFragment newInstance() {
        return new MainMeFragment();
    }


    @Override
    public int onBindLayout() {
        return R.layout.fragment_me_main;
    }

    @Override
    public void initView(View view) {
        mSetComment = view.findViewById(R.id.view_my_comment);
        mSetInform = view.findViewById(R.id.view_my_inform);
        mButton = view.findViewById(R.id.btn_login);
        mSetting = view.findViewById(R.id.view_setting);
        mCircleImageView = view.findViewById(R.id.view_user_image);
        mTextView = view.findViewById(R.id.view_user_name);

    }

    @Override
    public void initListener() {
        mSetComment.setOnClickSettingBarViewListener(() -> TheRouter.build(KeyCode.Me.COMMENT_PATH)
                .navigation(getActivity()));
        mSetInform.setOnClickSettingBarViewListener(() -> TheRouter.build(KeyCode.Me.MODIFY_PATH)
                .navigation(getActivity()));
        mButton.setOnClickListener(v -> TheRouter.build(KeyCode.Login.LOGIN_PATH)
                .navigation());
        mSetting.setOnClickSettingBarViewListener(() -> TheRouter.build(KeyCode.Me.SETTING_PATH)
                .navigation(getActivity()));
    }

    @Override
    public void initData() {
        updateView(new Object());
    }

    @Override
    public String getToolbarTitle() {
        return "我的";
    }

    @Subscribe(thread = EventThread.MAIN_THREAD,
            tags = {
                    @Tag(RxBusTag.MODIFY_PROFIE_PICTURE)
            }
    )
    public void setProfilePicture(String path) {
        Glide.with(mActivity)
                .load(API.URL_HOST_USER + "user/image/" + path)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fitCenter()
                .dontAnimate()
                .placeholder(ContextCompat.getDrawable(mActivity, R.drawable.image_default))
                .into(mCircleImageView);
    }

    @Subscribe(thread = EventThread.MAIN_THREAD,
            tags = {
                    @Tag(RxBusTag.SET_PROFIE_PICTURE_AND_NICKNAME)
            }
    )
    public void updateView(Object o) {
        if (!SPUtils.getInstance().getBoolean(KeyCode.Login.SP_IS_LOGIN)) {
            //未登录，显示按钮
            mTextView.setVisibility(View.GONE);
            mButton.setVisibility(View.VISIBLE);
            mCircleImageView.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.image_default));
        } else {
            //已登录，显示昵称
            mTextView.setVisibility(View.VISIBLE);
            mButton.setVisibility(View.GONE);
            setProfilePicture(SPUtils.getInstance().getString(KeyCode.Login.SP_IMAGE));
            mTextView.setText(SPUtils.getInstance().getString(KeyCode.Login.SP_NICKNAME));
        }
    }
}
