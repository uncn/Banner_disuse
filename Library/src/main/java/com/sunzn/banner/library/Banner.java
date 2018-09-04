package com.sunzn.banner.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sunzn on 2017/3/31.
 */

public class Banner<T> extends FrameLayout {

    private final String TAG = "Banner";

    public interface OnItemClickListener<T> {

        void onItemClick(int position, T item);

    }

    public interface OnItemBindListener<T> {

        void onItemBind(int position, T item, AppCompatImageView view);

    }

    private static final int DEFAULT_GAIN_COLOR = 0xffffffff;
    private static final int DEFAULT_MISS_COLOR = 0x50ffffff;

    private final Object mLock = new Object();

    private boolean mAttached;

    private RecyclerView mRecyclerView;
    private LinearLayout mLinearLayout;

    private BannerAdapter mBannerAdapter;

    private Handler mHandler = new Handler();

    private boolean isPlaying, mIsIndicatorShow;

    private List<T> mData = new ArrayList<>();

    private OnItemBindListener<T> mOnItemBindListener;

    private OnItemClickListener<T> mOnItemClickListener;

    private Drawable mIndicatorGainDrawable, mIndicatorMissDrawable;

    private int mInterval, mCurrentIndex, mIndicatorSize, mIndicatorSpace;

    private Runnable mBannerTask = new Runnable() {

        @Override
        public void run() {
            if (isPlaying) {
                int firstPos = ((BannerLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                if (firstPos >= mCurrentIndex) {
                    mRecyclerView.smoothScrollToPosition(++mCurrentIndex);
                    switchIndicator();
                    mHandler.postDelayed(this, mInterval);
                } else {
                    mHandler.postDelayed(this, mInterval * 2);
                }
            }
        }

    };

    public Banner(Context context) {
        this(context, null);
    }

    public Banner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Banner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.Banner);
        Drawable GainDrawable = attributes.getDrawable(R.styleable.Banner_indicator_gain);
        Drawable MissDrawable = attributes.getDrawable(R.styleable.Banner_indicator_miss);
        int IndicatorGravity = attributes.getInt(R.styleable.Banner_indicator_gravity, 1);
        mIsIndicatorShow = attributes.getBoolean(R.styleable.Banner_indicator_show, true);
        float mInch = attributes.getFloat(R.styleable.Banner_banner_inch, 100f);
        mInterval = attributes.getInt(R.styleable.Banner_banner_interval, 3000);
        mIndicatorSize = attributes.getDimensionPixelSize(R.styleable.Banner_indicator_size, 0);
        mIndicatorSpace = attributes.getDimensionPixelSize(R.styleable.Banner_indicator_space, dp2px(4));
        int IndicatorMargin = attributes.getDimensionPixelSize(R.styleable.Banner_indicator_margin, dp2px(8));

        if (GainDrawable == null) {
            mIndicatorGainDrawable = getDefaultDrawable(DEFAULT_GAIN_COLOR);
        } else {
            if (GainDrawable instanceof ColorDrawable) {
                mIndicatorGainDrawable = getDefaultDrawable(GainDrawable);
            } else {
                mIndicatorGainDrawable = GainDrawable;
            }
        }

        if (MissDrawable == null) {
            mIndicatorMissDrawable = getDefaultDrawable(DEFAULT_MISS_COLOR);
        } else {
            if (MissDrawable instanceof ColorDrawable) {
                mIndicatorMissDrawable = getDefaultDrawable(MissDrawable);
            } else {
                mIndicatorMissDrawable = MissDrawable;
            }
        }

        switch (IndicatorGravity) {
            case 0:
                IndicatorGravity = GravityCompat.START;
                break;
            case 1:
                IndicatorGravity = Gravity.CENTER;
                break;
            case 2:
                IndicatorGravity = GravityCompat.END;
                break;
        }

        attributes.recycle();

        mRecyclerView = new RecyclerView(context);
        mLinearLayout = new LinearLayout(context);

        new PagerSnapHelper().attachToRecyclerView(mRecyclerView);
        mBannerAdapter = new BannerAdapter();
        mRecyclerView.setAdapter(mBannerAdapter);
        mRecyclerView.setLayoutManager(new BannerLayoutManager(context, LinearLayoutManager.HORIZONTAL, false, mInch));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int sPos = ((BannerLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    int ePos = ((BannerLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                    if (sPos == ePos && mCurrentIndex != ePos) {
                        mCurrentIndex = ePos;
                        switchIndicator();
                    }
                }
            }
        });

        LayoutParams recyclerViewParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        LayoutParams linearLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayoutParams.gravity = Gravity.BOTTOM | IndicatorGravity;
        linearLayoutParams.setMargins(IndicatorMargin, IndicatorMargin, IndicatorMargin, IndicatorMargin);
        mLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        mLinearLayout.setGravity(Gravity.CENTER);

        addView(mRecyclerView, recyclerViewParams);
        addView(mLinearLayout, linearLayoutParams);
    }

    private Drawable getDefaultDrawable(Drawable drawable) {
        return getDefaultDrawable(((ColorDrawable) drawable).getColor());
    }

    private Drawable getDefaultDrawable(int color) {
        GradientDrawable gradient = new GradientDrawable();
        gradient.setSize(dp2px(6), dp2px(6));
        gradient.setCornerRadius(dp2px(6));
        gradient.setColor(color);
        return gradient;
    }

    private void createIndicators() {
        if (mIsIndicatorShow && mLinearLayout != null) {
            mLinearLayout.removeAllViews();
            for (int i = 0; i < (mData == null ? 0 : mData.size()); i++) {
                AppCompatImageView img = new AppCompatImageView(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.leftMargin = mIndicatorSpace / 2;
                lp.rightMargin = mIndicatorSpace / 2;
                if (mIndicatorSize >= dp2px(4)) {
                    lp.width = lp.height = mIndicatorSize;
                } else {
                    img.setMinimumWidth(dp2px(2));
                    img.setMinimumHeight(dp2px(2));
                }
                img.setImageDrawable(i == 0 ? mIndicatorGainDrawable : mIndicatorMissDrawable);
                mLinearLayout.addView(img, lp);
            }
        }
    }

    private void switchIndicator() {
        if (mIsIndicatorShow && mLinearLayout != null && mLinearLayout.getChildCount() > 0) {
            for (int i = 0; i < mLinearLayout.getChildCount(); i++) {
                if (mData != null && mData.size() > 0) {
                    ((AppCompatImageView) mLinearLayout.getChildAt(i)).setImageDrawable(i == mCurrentIndex % mData.size() ? mIndicatorGainDrawable : mIndicatorMissDrawable);
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPlaying(false);
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                setPlaying(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setPlaying(true);
        regReceiver();
        Log.e(TAG, "Banner onAttachedToWindow");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setPlaying(false);
        unrReceiver();
        Log.e(TAG, "Banner onDetachedFromWindow");
    }

    @Override
    public void onStartTemporaryDetach() {
        super.onStartTemporaryDetach();
//        setPlaying(false);
    }

    @Override
    public void onFinishTemporaryDetach() {
        super.onFinishTemporaryDetach();
//        setPlaying(true);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
//        setPlaying(visibility == VISIBLE);
    }

    public void setBannerData(List<T> data) {
        setPlaying(false);
        mData.clear();
        if (data != null && data.size() > 0) {
            if (data.size() > 1) {
                mData.addAll(data);
                mCurrentIndex = mData.size() * 100000;
                mBannerAdapter.notifyDataSetChanged();
                mRecyclerView.scrollToPosition(mCurrentIndex);
                createIndicators();
                setPlaying(true);
            } else {
                mCurrentIndex = 0;
                mData.addAll(data);
                mBannerAdapter.notifyDataSetChanged();
            }
        }
    }

    private class BannerAdapter extends RecyclerView.Adapter<BannerViewHolder> {

        @NonNull
        @Override
        public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            AppCompatImageView imageView = new AppCompatImageView(parent.getContext());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            imageView.setScaleType(AppCompatImageView.ScaleType.CENTER_CROP);
            imageView.setId(R.id.banner_image_view_id);
            imageView.setLayoutParams(params);
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null)
                        mOnItemClickListener.onItemClick(mCurrentIndex % mData.size(), mData.get(mCurrentIndex % mData.size()));
                }
            });
            return new BannerViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
            if (mOnItemBindListener != null)
                mOnItemBindListener.onItemBind(position % mData.size(), mData.get(position % mData.size()), holder.mImageView);
        }

        @Override
        public int getItemCount() {
            return mData == null ? 0 : mData.size() < 2 ? mData.size() : Integer.MAX_VALUE;
        }

    }

    private static class BannerViewHolder extends RecyclerView.ViewHolder {

        AppCompatImageView mImageView;

        BannerViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.banner_image_view_id);
        }

    }

    public void setPlaying(boolean playing) {
        synchronized (mLock) {
            if (playing) {
                playBanner();
            } else {
                stopBanner();
            }
        }
    }

    public void playBanner() {
        if (mHandler != null && !isPlaying && mBannerAdapter.getItemCount() > 1) {
            isPlaying = true;
            mHandler.removeCallbacks(mBannerTask);
            mHandler.postDelayed(mBannerTask, mInterval);
            Log.e(TAG, "Play Banner");
        }
    }

    public void stopBanner() {
        if (mHandler != null) {
            isPlaying = false;
            mHandler.removeCallbacks(mBannerTask);
            Log.e(TAG, "Stop Banner");
        }
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        mOnItemClickListener = listener;
    }

    public void setOnItemBindListener(OnItemBindListener<T> listener) {
        mOnItemBindListener = listener;
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                setPlaying(true);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                setPlaying(false);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                setPlaying(true);
            }
        }
    };

    private void regReceiver() {
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            getContext().registerReceiver(mReceiver, filter);
        }
    }

    private void unrReceiver() {
        if (mAttached) {
            getContext().unregisterReceiver(mReceiver);
            mAttached = false;
        }
    }

}
