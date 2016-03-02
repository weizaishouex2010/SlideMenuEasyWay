package com.huangzhiwei.doubleslidinglayout;

import android.content.Context;
import android.os.AsyncTask;
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


    public void setScrollEvent(View bindView)
    {
        mBindView = bindView;
        mBindView.setOnTouchListener(this);
    }
    /**
     * 将界面从右侧菜单滚动到内容界面，滚动速度设定为30.
     */
    public void scrollToContentFromRightMenu() {
        new RightMenuScrollTask().execute(30);
    }

    /**
     * 将界面滚动到右侧菜单界面，滚动速度设定为-30.
     */
    public void scrollToRightMenu() {
        new RightMenuScrollTask().execute(-30);
    }

    /**
     * 将界面从左侧菜单滚动到内容界面，滚动速度设定为30.
     */
    private void scrollToContentFromLeftMenu() {
        new LeftMenuScrollTask().execute(30);
    }

    /**
     * 将界面滚动到左侧菜单界面，滚动速度设定为-30.
     */
    public void scrollToLeftMenu() {
        new LeftMenuScrollTask().execute(-30);
    }



    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(changed)
        {
            leftMenuLayout = getChildAt(0);
            leftMenuLayoutParams = (MarginLayoutParams) leftMenuLayout.getLayoutParams();

            rightMenuLayout = getChildAt(1);
            rightMenuLayoutParams = (MarginLayoutParams) rightMenuLayout.getLayoutParams();

            contentLayout = getChildAt(2);
            contentLayoutParams = (LayoutParams) contentLayout.getLayoutParams();

            contentLayoutParams.width = screenWidth;
            contentLayout.setLayoutParams(contentLayoutParams);
        }
    }






    @Override
    public boolean onTouch(View v, MotionEvent event) {
        createVelocity(event);
        switch (event.getAction()) {
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
                // 根据当前滑动状态决定如何偏移内容布局
                switch (slideState) {
                    case SHOW_LEFT_MENU:
                        contentLayoutParams.rightMargin = -moveDistanceX;
                        checkLeftMenuBorder();
                        contentLayout.setLayoutParams(contentLayoutParams);
                        break;
                    case HIDE_LEFT_MENU:
                        contentLayoutParams.rightMargin = -leftMenuLayoutParams.width - moveDistanceX;
                        checkLeftMenuBorder();
                        contentLayout.setLayoutParams(contentLayoutParams);
                    case SHOW_RIGHT_MENU:
                        contentLayoutParams.leftMargin = moveDistanceX;
                        checkRightMenuBorder();
                        contentLayout.setLayoutParams(contentLayoutParams);
                        break;
                    case HIDE_RIGHT_MENU:
                        contentLayoutParams.leftMargin = -rightMenuLayoutParams.width + moveDistanceX;
                        checkRightMenuBorder();
                        contentLayout.setLayoutParams(contentLayoutParams);
                    default:
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                xUp = event.getRawX();
                int upDistanceX = (int) (xUp - xDown);
                if (isSliding) {
                    // 手指抬起时，进行判断当前手势的意图
                    switch (slideState) {
                        case SHOW_LEFT_MENU:
                            if (shouldScrollToLeftMenu()) {
                                scrollToLeftMenu();
                            } else {
                                scrollToContentFromLeftMenu();
                            }
                            break;
                        case HIDE_LEFT_MENU:
                            if (shouldScrollToContentFromLeftMenu()) {
                                scrollToContentFromLeftMenu();
                            } else {
                                scrollToLeftMenu();
                            }
                            break;
                        case SHOW_RIGHT_MENU:
                            if (shouldScrollToRightMenu()) {
                                scrollToRightMenu();
                            } else {
                                scrollToContentFromRightMenu();
                            }
                            break;
                        case HIDE_RIGHT_MENU:
                            if (shouldScrollToContentFromRightMenu()) {
                                scrollToContentFromRightMenu();
                            } else {
                                scrollToRightMenu();
                            }
                            break;
                        default:
                            break;
                    }
                } else if (upDistanceX < touchSlop && isLeftMenuVisible) {
                    // 当左侧菜单显示时，如果用户点击一下内容部分，则直接滚动到内容界面
                    scrollToContentFromLeftMenu();
                } else if (upDistanceX < touchSlop && isRightMenuVisible) {
                    // 当右侧菜单显示时，如果用户点击一下内容部分，则直接滚动到内容界面
                    scrollToContentFromRightMenu();
                }
                recycleVelocity();
                break;
        }
        if (v.isEnabled()) {
            if (isSliding) {
                // 正在滑动时让控件得不到焦点
                unFocusBindView();
                return true;
            }
            if (isLeftMenuVisible || isRightMenuVisible) {
                // 当左侧或右侧布局显示时，将绑定控件的事件屏蔽掉
                return true;
            }
            return false;
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
            if(!isSliding &&Math.abs(moveDistanceX)>=touchSlop && moveDistanceX>0
                    && Math.abs(moveDistanceY) < touchSlop)
            {
                isSliding = true;
                slideState = SHOW_LEFT_MENU;
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,0);
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                contentLayout.setLayoutParams(contentLayoutParams);

                leftMenuLayout.setVisibility(View.VISIBLE);
                rightMenuLayout.setVisibility(View.GONE);
            }
            else if(!isSliding && Math.abs(moveDistanceX)>=touchSlop && moveDistanceX <0
                    && Math.abs(moveDistanceY) < touchSlop)
            {
                isSliding = true;
                slideState = SHOW_RIGHT_MENU;
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,0);
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                contentLayout.setLayoutParams(contentLayoutParams);

                leftMenuLayout.setVisibility(GONE);
                rightMenuLayout.setVisibility(VISIBLE);
            }

        }
    }

    /**
     * 在滑动过程中检查左侧菜单的边界值，防止绑定布局滑出屏幕。
     */
    private void checkLeftMenuBorder() {
        if (contentLayoutParams.rightMargin > 0) {
            contentLayoutParams.rightMargin = 0;
        } else if (contentLayoutParams.rightMargin < -leftMenuLayoutParams.width) {
            contentLayoutParams.rightMargin = -leftMenuLayoutParams.width;
        }
    }

    /**
     * 在滑动过程中检查右侧菜单的边界值，防止绑定布局滑出屏幕。
     */
    private void checkRightMenuBorder() {
        if (contentLayoutParams.leftMargin > 0) {
            contentLayoutParams.leftMargin = 0;
        } else if (contentLayoutParams.leftMargin < -rightMenuLayoutParams.width) {
            contentLayoutParams.leftMargin = -rightMenuLayoutParams.width;
        }
    }



    private boolean shouldScrollToLeftMenu() {
        return xUp-xDown > leftMenuLayoutParams.width/2  || getVelocity()>SNAP_VELOCITY;
    }
    private boolean shouldScrollToRightMenu() {
        return xDown-xUp > rightMenuLayoutParams.width/2 || getVelocity()>SNAP_VELOCITY;
    }

    private boolean shouldScrollToContentFromLeftMenu() {
        return xDown - xUp > leftMenuLayoutParams.width/2 || getVelocity()>SNAP_VELOCITY;
    }

    private boolean shouldScrollToContentFromRightMenu() {
        return xUp-xDown > rightMenuLayoutParams.width/2 || getVelocity()>SNAP_VELOCITY;
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
            mBindView.setPressed(false);
            mBindView.setFocusable(false);
            mBindView.setFocusableInTouchMode(false);
        }
    }


    /**
     * 左侧菜单是否完全显示出来，滑动过程中此值无效。
     *
     * @return 左侧菜单完全显示返回true，否则返回false。
     */
    public boolean isLeftLayoutVisible() {
        return isLeftMenuVisible;
    }

    /**
     * 右侧菜单是否完全显示出来，滑动过程中此值无效。
     *
     * @return 右侧菜单完全显示返回true，否则返回false。
     */
    public boolean isRightLayoutVisible() {
        return isRightMenuVisible;
    }


    private class RightMenuScrollTask  extends AsyncTask<Integer,Integer,Integer>{
        @Override
        protected Integer doInBackground(Integer...speed) {
            int leftMargin = contentLayoutParams.leftMargin;
            // 根据传入的速度来滚动界面，当滚动到达边界值时，跳出循环。
            while (true) {
                leftMargin = leftMargin + speed[0];
                if (leftMargin < -rightMenuLayoutParams.width) {
                    leftMargin = -rightMenuLayoutParams.width;
                    break;
                }
                if (leftMargin > 0) {
                    leftMargin = 0;
                    break;
                }
                publishProgress(leftMargin);
                // 为了要有滚动效果产生，每次循环使线程睡眠一段时间，这样肉眼才能够看到滚动动画。
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (speed[0] > 0) {
                isRightMenuVisible = false;
            } else {
                isRightMenuVisible = true;
            }
            isSliding = false;
            return leftMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            contentLayoutParams.leftMargin = values[0];
            contentLayout.setLayoutParams(contentLayoutParams);
            unFocusBindView();
        }

        @Override
        protected void onPostExecute(Integer leftMargin) {
            contentLayoutParams.leftMargin = leftMargin;
            contentLayout.setLayoutParams(contentLayoutParams);
        }
    }

    class LeftMenuScrollTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer... speed) {
            int rightMargin = contentLayoutParams.rightMargin;
            // 根据传入的速度来滚动界面，当滚动到达边界值时，跳出循环。
            while (true) {
                rightMargin = rightMargin + speed[0];
                if (rightMargin < -leftMenuLayoutParams.width) {
                    rightMargin = -leftMenuLayoutParams.width;
                    break;
                }
                if (rightMargin > 0) {
                    rightMargin = 0;
                    break;
                }
                publishProgress(rightMargin);
                // 为了要有滚动效果产生，每次循环使线程睡眠一段时间，这样肉眼才能够看到滚动动画。
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            if (speed[0] > 0) {
                isLeftMenuVisible = false;
            } else {
                isLeftMenuVisible = true;
            }
            isSliding = false;
            return rightMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... rightMargin) {
            contentLayoutParams.rightMargin = rightMargin[0];
            contentLayout.setLayoutParams(contentLayoutParams);
            unFocusBindView();
        }

        @Override
        protected void onPostExecute(Integer rightMargin) {
            contentLayoutParams.rightMargin = rightMargin;
            contentLayout.setLayoutParams(contentLayoutParams);
        }
    }



}
