package com.huangzhiwei.doubleslidinglayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.RelativeLayout;

/**
 * Created by huangzhiwei on 16/3/1.
 */
public class DoubleSlidiingLayout extends RelativeLayout implements View.OnTouchListener {

    /**
     * 设置滑动速度
     */
    private static final int SNAP_VELOCITY = 200;

    /**
     * 屏幕宽度
     */
    private int screenWidth;

    /**
     * 设置移动前手指可以移动的最大距离
     */
    private static int touchSlop;


    /**
     * 用于监听滑动事件的View。
     */
    private View mBindView;

    /**
     * 监听手部滑动的速度
     */
    private VelocityTracker mVelocityTracker;

    /**
     * 左侧菜单当前是显示还是隐藏。只有完全显示或隐藏时才会更改此值，滑动过程中此值无效。
     */
    private boolean isLeftMenuVisible;

    /**
     * 右侧菜单当前是显示还是隐藏。只有完全显示或隐藏时才会更改此值，滑动过程中此值无效。
     */
    private boolean isRightMenuVisible;

    /**
     * 是否正在滑动。
     */
    private boolean isSliding;

    /**
     * 左侧菜单布局对象。
     */
    private View leftMenuLayout;

    /**
     * 右侧菜单布局对象。
     */
    private View rightMenuLayout;

    /**
     * 内容布局对象。
     */
    private View contentLayout;

    /**
     * 左侧菜单布局的参数。
     */
    private MarginLayoutParams leftMenuLayoutParams;

    /**
     * 右侧菜单布局的参数。
     */
    private MarginLayoutParams rightMenuLayoutParams;

    /**
     * 内容布局的参数。
     */
    private RelativeLayout.LayoutParams contentLayoutParams;



    /**
     * 滑动状态的一种，表示未进行任何滑动。
     */
    public static final int DO_NOTHING = 0;

    /**
     * 滑动状态的一种，表示正在滑出左侧菜单。
     */
    public static final int SHOW_LEFT_MENU = 1;

    /**
     * 滑动状态的一种，表示正在滑出右侧菜单。
     */
    public static final int SHOW_RIGHT_MENU = 2;

    /**
     * 滑动状态的一种，表示正在隐藏左侧菜单。
     */
    public static final int HIDE_LEFT_MENU = 3;

    /**
     * 滑动状态的一种，表示正在隐藏右侧菜单。
     */
    public static final int HIDE_RIGHT_MENU = 4;

    /**
     * 记录当前的滑动状态
     */
    private int slideState;

    /**
     * 记录手指按下时的横坐标。
     */
    private float xDown;

    /**
     * 记录手指按下时的纵坐标。
     */
    private float yDown;

    /**
     * 记录手指移动时的横坐标。
     */
    private float xMove;

    /**
     * 记录手指移动时的纵坐标。
     */
    private float yMove;

    /**
     * 记录手机抬起时的横坐标。
     */
    private float xUp;

    /**
     * 初始化屏幕宽度，和最小移动距离
     * @param context
     * @param attrs
     */
    public DoubleSlidiingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        screenWidth = windowManager.getDefaultDisplay().getWidth();
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(changed)
        {
            leftMenuLayout = getChildAt(0);
            leftMenuLayoutParams = (MarginLayoutParams) leftMenuLayout.getLayoutParams();

            contentLayout = getChildAt(1);
            contentLayoutParams = (LayoutParams) leftMenuLayout.getLayoutParams();

            rightMenuLayout = getChildAt(2);
            rightMenuLayoutParams = (MarginLayoutParams) rightMenuLayout.getLayoutParams();

            contentLayoutParams.width = screenWidth;
            contentLayout.setLayoutParams(contentLayoutParams);
        }
    }

    public void setScrollEvent(View bindView)
    {
        mBindView = bindView;
        mBindView.setOnTouchListener(this);
    }




    @Override
    public boolean onTouch(View v, MotionEvent event) {
        createVelocity(event);
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                // 手指按下时，记录按下时的坐标
                xDown = event.getRawX();
                yDown = event.getRawY();
                // 将滑动状态初始化为DO_NOTHING
                slideState = DO_NOTHING;
                break;
            case MotionEvent.ACTION_MOVE:
                xMove = event.getRawX();
                yMove = event.getRawY();
                // 手指移动时，对比按下时的坐标，计算出移动的距离。
                int moveDistanceX = (int) (xMove - xDown);
                int moveDistanceY = (int) (yMove - yDown);

                // 检查当前的滑动状态  
                checkSlideState(moveDistanceX, moveDistanceY);
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    private void checkSlideState(int moveDistanceX, int moveDistanceY) {
        if(isLeftMenuVisible)
        {
            if(!isSliding && Math.abs(moveDistanceX)>= touchSlop && moveDistanceX<0)
            {
                isSliding = true;
                slideState = HIDE_LEFT_MENU;
            }
        }
        else if(isRightMenuVisible)
        {
            if(!isSliding && Math.abs(moveDistanceX)>=touchSlop && moveDistanceX>0)
            {
                isSliding = true;
                slideState = HIDE_RIGHT_MENU;
            }
        }
        else
        {
            if(!isSliding &&Math.abs(moveDistanceX)>=touchSlop && moveDistanceX>0)
            {
                isSliding = true;
                slideState = SHOW_LEFT_MENU;
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,LEFT_OF);
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                contentLayout.setLayoutParams(contentLayoutParams);

                leftMenuLayout.setVisibility(View.VISIBLE);
                rightMenuLayout.setVisibility(View.GONE);
            }
            else if(!isSliding && Math.abs(moveDistanceX)>=touchSlop && moveDistanceX <0)
            {
                isSliding = true;
                slideState = SHOW_RIGHT_MENU;
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,LEFT_OF);
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                contentLayout.setLayoutParams(contentLayoutParams);

                leftMenuLayout.setVisibility(GONE);
                rightMenuLayout.setVisibility(VISIBLE);
            }
        }
    }


    /**
     * 创建VelocityTracker对象，并将触摸事件加入到VelocityTracker当中。
     *
     * @param event
     *            右侧布局监听控件的滑动事件
     */
    private void createVelocity(MotionEvent event)
    {
        if(mVelocityTracker==null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    /**
     * 获取速度 根据每秒滑动的距离
     * @return
     */
    private int getVelocity()
    {
        mVelocityTracker.computeCurrentVelocity(1000);
        return (int) Math.abs(mVelocityTracker.getXVelocity());
    }

    /**
     * 回收速度监控
     */
    private void recycleVelocity()
    {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    /**
     * 使用可以获得焦点的控件在滑动的时候失去焦点。
     */
    public void unFocusBindView()
    {
        if(mBindView!=null)
        {
            mBindView.setEnabled(false);
            mBindView.setFocusable(false);
            mBindView.setFocusableInTouchMode(false);
        }
    }



}
