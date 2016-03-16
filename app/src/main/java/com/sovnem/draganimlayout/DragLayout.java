package com.sovnem.draganimlayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 主要实现的功能：
 * 自定义的布局分上下 一大一小两部分，比例大概是7:1
 * 其中大的子view不动，底部的小的view可以随着手指滑动而变化，逐渐占满全屏
 * 在这个变化过程中另外一个大的子view的透明度和，小的子view内部的内容都可以发生变化
 * 最终实现一个滑动渐变动画的效果
 * <p/>
 * 功能详细：
 * 1、两个子view的比例是7:1分上下布局占满父控件
 * 2、手指落在底部的子view内并向上滑动的时候该子view的高度逐渐变化直至占满父控件
 * 3、底部view发生变化的时候大的子view的透明度和，该view的内部的一些内容根据变化的比率同时变化
 * 4、注意底部view内的控件的点击事件和滑动事件的冲突
 * <p/>
 * Created by wood on 2015/11/9.
 */
public class DragLayout extends LinearLayout {
    private static final int DURATION = 500;
    private final int SAFE_DISTANCE = 30;//手指移动的安全距离
    private View child1, child2;
    private boolean measured;
    private int min, max;

    public DragLayout(Context context) {
        super(context);
        init();
    }

    public DragLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        setBackgroundColor(Color.BLACK);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        child1.layout(l, t, r, t + child1.getHeight());
        child2.layout(l, b - child2.getHeight(), r, b);
    }

    /**
     * 测量子控件确定子控件的大小  比例为7:1
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int h1, h2;
        max = h1 = Math.round(height * 7 * 1.0f / 8);//为了去除因为小数造成的缝隙 两个高度向上取整
        min = h2 = Math.round(height * 1.0f / 8);
        measureChild(child1, widthMeasureSpec, MeasureSpec.makeMeasureSpec(h1, hMode));
        measureChild(child2, widthMeasureSpec, MeasureSpec.makeMeasureSpec(h2, hMode));
       if (0==tvW){
           tvW = tv.getWidth();
           tvH = tv.getHeight();
           tvSize = tv.getTextSize();
           isInitialed = true;
           Log.i("info", tvW + "," + tvH + "," + tvSize + "," + tvSizeMax);
       }
    }

    /**
     * 设置cv的高度为height
     *
     * @param cv
     * @param height
     */
    private void setChildSize(View cv, float height) {
        if (height < getHeight() / 8 || height > getHeight()) return;//小于最小高度和大于最大高度 不处理
        ViewGroup.LayoutParams lp = cv.getLayoutParams();
        lp.height = (int) height;
        cv.setLayoutParams(lp);

        float factor = (height - min) / max;
        setSthWithFactor(factor);
    }


    boolean isTouchFromChild2;//手指的down事件落点是不是在child2里边
    float downY;//记录起始的y坐标
    boolean isDraged;//是不是正在拖拉变化中
    float child2H;//child2 未拖拽时候的高度

    /**
     * 事件的派发  如果这个方法内手动return 则事件不会派发给onTouchEvent方法
     * 区别：
     * 如果是在ACTION_DOWN的时候手动return 则以后的move和up事件都只在该层级和其父控件之间传递，区别是true为包含本层级，false为不包含本层级
     * 如果是在ACTION_MOVE和ACTION_UP发生的时候手动return 则只会影响当前的move或者up事件
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        float eX = ev.getX();
        float eY = ev.getY();


        switch (action) {
            case MotionEvent.ACTION_DOWN://监听down事件 判断是不是落在child2里边
                child2H = child2.getHeight();//每次手指落下重新记录child2的高度
                if (isTouchFromChild2 = isDownInChild2(eX, eY)) {//如果是落在了child2里边 则记录当前的y坐标
                    downY = eY;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isTouchFromChild2) {//如果down的时候落点在child2里边则在此对move事件做处理
                    float dy = eY - downY;//得到y方向的偏移值 有正负之分
                    //要根据child2的状态来区分，全屏和非全屏， 移动的方向也不一样
                    if (!isDraged) {
                        if ((child2.getHeight() > getHeight() / 2 && dy > SAFE_DISTANCE)//展开状态 并且下拉的距离高于设定的阈值
                                || (child2.getHeight() < getHeight() / 2 && dy < -SAFE_DISTANCE)//处于底部的状态  并且手指移动的距离大于阈值
                                ) {
                            downY = eY;//重新记录起始的位置
                            isDraged = true;//标记为正在拖拽
                        }
                    }

                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 事件的处理ACTION_DOWN的时候判断落点是不是在child2中  此为前提
     * 如果满足该前提，在ACTION_MOVE中判断移动的y方向偏移是否大于 SAFE_DISTANCE
     * 如果以上条件都满足 则子view2的高度就会随着手指的移动而变化 此时onInteruptTouchEvent方法应该返回true 来保证 事件不会被子布局中的控件处理掉
     *
     */

    /**
     * 这个方法默认是返回false的
     * 一旦在一次手势操作的过程中这个方法手动返回了true
     * 则以后的所有事件都不会向子控件传递了，就是说一旦这个方法返回了true  以后的动作就可以把这个容器控件当做一个非容器控件来看待
     * 这个状态一直到手势结束的时候停止
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_MOVE && isTouchFromChild2 && isDraged) {//如果满足move的条件 阻断后续事件的传递
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 处理由本层级的dispatchtouchevent方法派发来的事件
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float ey = event.getY();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (isDraged) {//如果是正在拖拽 根据拖拽距离变化child2的高度
                    float dY = ey - downY;
                    setChildSize(child2, child2H - dY);//改变child2的高度
                    return true;//消耗该事件
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP://初始化两个标记参数
                isDraged = false;
                isTouchFromChild2 = false;
                handleUp();
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 处理手指抬起时候的动画
     * 根据手指抬起时候child2的高度 如果大于总高度的二分之一则松手之后渐渐变为最大高度，否则渐渐变小。
     */
    private void handleUp() {
        int h = child2.getHeight();
        if (h <= getHeight() / 2) {//渐渐变小
            int duration = (int) (DURATION * (h - min) * 1.0f / (max));
            doAnimate(h, min, duration);
        } else {//渐渐变大
            int duration = (int) (DURATION * (getHeight() - h) * 1.0f / (max));
            doAnimate(h, getHeight(), duration);
        }

    }

    /**
     * child2高度变化动画 从h到 max用时duration毫秒
     *
     * @param h
     * @param max
     * @param duration
     */
    private void doAnimate(int h, int max, int duration) {
        final ValueAnimator va = ValueAnimator.ofInt(h, max);
        va.setDuration(duration);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = Integer.parseInt(animation.getAnimatedValue().toString());
                setChildSize(child2, value);
            }
        });
        va.start();
    }

    /**
     * 点(x,y)是不是落在了child2上
     *
     * @param x
     * @param y
     */
    private boolean isDownInChild2(float x, float y) {
        return x >= child2.getLeft() && x <= child2.getRight() && y <= child2.getBottom() && y >= child2.getTop();
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        child1 = getChildAt(0);
        child2 = getChildAt(1);
        tv = (TextView) findViewById(R.id.tv);
        child2.setClickable(true);
        tv.setClickable(true);
        tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "textview点击", Toast.LENGTH_SHORT).show();
            }
        });
        child2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Math.abs(child2H - min) < 5) {
                    doAnimate(min, getHeight(), DURATION);

                }
            }
        });
    }

    boolean isInitialed;
    TextView tv;
    int tvW;
    int tvH;
    float tvSize;
    float tvSizeMax;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

    }

    /**
     * 利用变化的比率做你想做的事情
     *
     * @param factor child2的高度为最小值时候为0 全屏时为1
     */
    private void setSthWithFactor(float factor) {

        child1.setAlpha(1 - factor);
        tv.setTextSize(20 + 40 * factor);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tv.getLayoutParams();
        lp.width = (int) (tvW+10 + (getWidth() - tvW) * factor);
        lp.topMargin = (int) (factor*50);
        tv.setLayoutParams(lp);
    }
}
