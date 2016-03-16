package com.sovnem.draganimlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 可拖动布局
 * <p/>
 * 里边有两个子view 宽度为match_parent 高度比例为8:1
 * <p/>
 * 手指move动作在较小的view上触发的时候 较小的view随手指移动而变大 移动的同时 另外一个view的透明度渐渐变大直至消失
 * <p/>
 * Created by wood on 2015/11/5.
 */
public class ForDragView extends LinearLayout {
    private View child1;
    private View child2;
    private int child2H;
    private boolean shouldExpress;//是不是应该展开
    private TextView textView;
    private View llButtons;
    private float llFactor;

    public ForDragView(Context context) {
        super(context);
        init();

    }

    public ForDragView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ForDragView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(Color.BLACK);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        child1 = getChildAt(0);
        child2 = getChildAt(1);
        child2.setClickable(true);
        child2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldExpress = true;
                animateChild2();
            }
        });

        llButtons = findViewById(R.id.ll);
        textView = (TextView) findViewById(R.id.textview);
        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "textview点击", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        child1.layout(l, t, r, t + child1.getHeight());
        child2.layout(l, b - child2.getMeasuredHeight(), r, b);
    }


    boolean measured;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!measured) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            ViewGroup.LayoutParams lp1 = child1.getLayoutParams();
            ViewGroup.LayoutParams lp2 = child2.getLayoutParams();
            lp1.width = width;
            lp1.height = height * 5 / 9;
            lp2.width = width;
            child2H = lp2.height = height * 4 / 9;
            child1.setLayoutParams(lp1);
            child2.setLayoutParams(lp2);

            measured = true;
        }
    }


    float downY;
    boolean draging;


    boolean downRight;


    /**
     * 分两种情况判断  向上滑动的时候滑动距离大于一定的值的时候记录这个时候的坐标
     * 这个时候满足拦截以后事件的条件 以后的过程就按照这个记录的坐标开始滑动操作。
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (0 == llFactor) {
            llFactor = getWidth() * 1.0f / llButtons.getWidth();
            Log.i("info", "ll需要放大的倍数:" + llFactor);
        }
        int action = event.getAction();
        float ex = event.getX();
        float ey = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downRight = isInBottom(ex, ey);
                super.dispatchTouchEvent(event);
                downY = ey;
                return true;
            case MotionEvent.ACTION_MOVE:
                //没有处于drag状态的时候判断

                if (draging) {//如果此时是处于拖动状态  计算此时的尺寸
                    float dy = ey - downY;
                    applyDy(dy);

                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                draging = false;
                handleUp();
                break;
        }
        return draging ? true : super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            Log.i("info", "ACTION_MOVE:");
            if (!draging && downRight) {
                float dy = ev.getY() - downY;
                Log.i("info", "ACTION_MOVE:" + dy);
                if (child2H < getHeight() / 2) {//处于底部，向上滑动才能判定
                    draging = dy < -30;
                } else {//处于占满全屏 向下滑动才能判定
                    draging = dy > 30;
                }
                if (draging) {//如果已经处于拖动状态 记录此时的坐标
                    downY = ev.getY();
                }
            }

            if (draging) {//如果此时是处于拖动状态  计算此时的尺寸
                float dy = ev.getY() - downY;
                applyDy(dy);
                return true;

            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        llFactor = getWidth() * 1.0f / llButtons.getWidth();
        Log.i("info", "ll需要放大的倍数:" + llFactor);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) return true;
        return super.onTouchEvent(event);
    }

    private void handleUp() {
        shouldExpress = child2.getHeight() >= getHeight() / 2;

        animateChild2();
    }

    public void animateChild2() {
        ValueAnimator va = ValueAnimator.ofInt(child2.getHeight(), shouldExpress ? getHeight() : getHeight() / 9);
        int duration = (int) (500 * (shouldExpress ? (getHeight() - child2.getHeight()) * 1.0f / getHeight() : (child2.getHeight() - getHeight() / 9) * 1.0f / getHeight()));
        Log.i("info", "计算出来的动画时间是:" + Math.abs(duration));
        va.setDuration(Math.abs(duration));
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = Integer.parseInt(animation.getAnimatedValue().toString());
                setChild2Height(value);
            }
        });
        va.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                child2H = child2.getHeight();

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        va.start();
    }

    private void applyDy(float dy) {
        setChild2Height(child2H - dy);
        requestLayout();
    }

    private void setChild2Height(float v) {
        if (v < getHeight() / 9 || v > getHeight()) return;
        float factor = (v - getHeight() / 9) / (getHeight() * 8 / 9);
        updateView(factor);
        ViewGroup.LayoutParams lp2 = child2.getLayoutParams();
        lp2.height = (int) (v);
        child2.setLayoutParams(lp2);
    }

    private void updateView(float factor) {
//        ViewCompat.setScaleX(textView,factor);
        ViewCompat.setTranslationX(textView, factor * textView.getWidth());
        ViewCompat.setScaleY(textView, factor + 1);
        ViewCompat.setScaleX(textView, factor + 1);
        ViewCompat.setTranslationY(textView, factor * 400);

//        ViewCompat.setRotation(textView,factor*360);
        child1.setAlpha(1 - factor);
    }


    private boolean isInBottom(float ex, float ey) {
        return (ex >= child2.getLeft() && ex <= child2.getRight()) && (ey >= child2.getTop() && ey <= child2.getBottom());
    }
}
