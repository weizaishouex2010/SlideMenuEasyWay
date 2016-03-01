package com.huangzhiwei.myapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.RelativeLayout;

/**
 * Created by huangzhiwei on 16/2/26.
 */
public class SlideLayout extends RelativeLayout implements View.OnTouchListener {

    private final String TAG = "MainActivity";

    /**
     * 滚动显示和隐藏menu时，手指滑动需要达到的速度。
     */
    public static final int SNAP_VELOCITY = 200;

    /**
     * 左侧menu视图
     */
    private View menu;

    /**
     * 右侧content视图
     */
    private View content;

    /**
     * menu布局的参数，通过此参数来更改leftMargin的值。
     */
    private MarginLayoutParams menuParams;

    /**
     * content布局的参数，
     */
    private MarginLayoutParams contentParams;

    /**
     * 在被判定为滚动之前用户手指可以移动的最大值。
     */
    private int touchSlop;

    /**
     * menu最多可以滑动到的左边缘。值由menu布局的宽度来定，marginLeft到达此值之后，不能再减少。
     */
    private int leftEdge = 0;

    /**
     * menu最多可以滑动到的右边缘。值恒为0，即marginLeft到达0之后，不能增加。
     */
    private int rightEdge = 0;

    /**
     * 屏幕宽度
     */
    private int screenWidth;



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
     * menu当前是显示还是隐藏。只有完全显示或隐藏menu时才会更改此值，滑动过程中此值无效。
     */
    private boolean isMenuVisible;

    /**
     * 是否正在滑动。
     */
    private boolean isSliding;

    /**
     * 用于计算手指滑动的速度。
     */
    private VelocityTracker mVelocityTracker;

    /**
     * 用于监听侧滑事件的View。
     */
    private View mBindView;

    /**
     * 重写SlidingLayout的构造函数，其中获取了屏幕的宽度。
     *
     * @param context
     * @param attrs
     */
    public SlideLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        screenWidth = wm.getDefaultDisplay().getWidth();
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /**
     * 绑定监听侧滑事件的View，即在绑定的View进行滑动才可以显示和隐藏左侧布局。
     *
     * @param bindView
     *            需要绑定的View对象。
     */
    public void setScrollEvent(View bindView)
    {
        mBindView = bindView;
        mBindView.setOnTouchListener(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed)
        {
            menu = getChildAt(0);
            menuParams = (MarginLayoutParams)menu.getLayoutParams();
            rightEdge = -menuParams.width;

            content = getChildAt(1);
            contentParams = (MarginLayoutParams) content.getLayoutParams();
            contentParams.width = screenWidth;
            content.setLayoutParams(contentParams);
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(menu.getVisibility() != View.VISIBLE)
        {
            menu.setVisibility(View.VISIBLE);
        }
        createVelocityTracker(event);
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                xDown = event.getRawX();
                yDown = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                xMove = event.getRawX();
                yMove = event.getRawY();
                //移动的逻辑
                int moveDistanceX = (int) (xMove - xDown);
                int moveDistancY = (int) (yMove - yDown);
                if(!isMenuVisible && moveDistanceX >= touchSlop
                        &&(isSliding || moveDistancY <= touchSlop))
                {
                    isSliding = true;
                    contentParams.rightMargin = -moveDistanceX;
                    if(contentParams.rightMargin > leftEdge)
                    {
                        contentParams.rightMargin = leftEdge;
                    }
                    content.setLayoutParams(contentParams);
                }
                if(isMenuVisible && Math.abs(moveDistanceX)>touchSlop)
                {
                    isSliding = true;
                    contentParams.rightMargin = rightEdge - moveDistanceX;
                    if(contentParams.rightMargin < rightEdge)
                    {
                        contentParams.rightMargin = rightEdge;
                    }
                    content.setLayoutParams(contentParams);
                }

                break;
            case MotionEvent.ACTION_UP:
                xUp = event.getRawX();
                int upDistanceX = (int) (xUp - xDown);
//                抬起后的逻辑
                if(isSliding)
                {
                    if(wantScrollToContent())
                    {
                        if(shouldScrollToContent())
                        {
                            scrollToContent();
                        }
                        else
                        {
                            scrollToMenu();
                        }
                    }
                    else if(wantScrollToMenu())
                    {
                        if(shouldScrollToMenu())
                        {
                            scrollToMenu();
                        }
                        else
                        {
                            scrollToContent();
                        }
                    }
                }
                else if(upDistanceX < touchSlop && isMenuVisible)
                {
                    scrollToContent();
                }
                destroyVelocity();
                break;
            default:
                break;

        }

        if (v.isEnabled()) {
            if (isSliding) {
                unFocusBindView();
                return true;
            }
            if (isMenuVisible) {
                return true;
            }
            return false;
        }
        return true;
    }

    public void scrollToMenu() {
        new ScrollTask().execute(-30);
    }

    public void scrollToContent() {
        new ScrollTask().execute(30);
    }


    private boolean shouldScrollToMenu() {
        return getVelocity()>SNAP_VELOCITY || xUp - xDown > screenWidth/2;
    }

    private boolean shouldScrollToContent() {
        return getVelocity()>SNAP_VELOCITY || xDown - xUp > screenWidth/2;
    }

    private boolean wantScrollToMenu() {
        return xUp-xDown>0 && !isMenuVisible;
    }

    private boolean wantScrollToContent() {
        return xUp-xDown<0 && isMenuVisible;
    }

    /**
     * 左侧布局是否完全显示出来，或完全隐藏，滑动过程中此值无效。
     *
     * @return 左侧布局完全显示返回true，完全隐藏返回false。
     */
    public boolean isMenuVisible() {
        return isMenuVisible;
    }
    /**
     * 创建VelocityTracker对象，并将触摸content界面的滑动事件加入到VelocityTracker当中。
     *
     * @param motionEvent
     *            content界面的滑动事件
     */
    private void createVelocityTracker(MotionEvent motionEvent)
    {
        if(mVelocityTracker==null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(motionEvent);
    }

    private int getVelocity()
    {
        mVelocityTracker.computeCurrentVelocity(1000);
        return Math.abs((int) mVelocityTracker.getXVelocity());
    }

    private void destroyVelocity()
    {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    /**
     * 使用可以获得焦点的控件在滑动的时候失去焦点。
     */
    private void unFocusBindView()
    {
        if(mBindView!=null)
        {
            mBindView.setPressed(false);
            mBindView.setFocusable(false);
            mBindView.setFocusableInTouchMode(false);
        }
    }

    class ScrollTask extends AsyncTask<Integer,Integer,Integer>
    {

        @Override
        protected Integer doInBackground(Integer... params) {
            int rightMargin = contentParams.rightMargin;
            int speed = params[0];
            while(true)
            {
                Log.d(TAG,rightMargin+" ");
                rightMargin += speed;
                if(rightMargin>leftEdge)
                {
                    rightMargin = leftEdge;
                    break;
                }
                else if(rightMargin<rightEdge)
                {
                    rightMargin = rightEdge;
                    break;
                }
                publishProgress(rightMargin);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(speed>0)
            {
                isMenuVisible = false;
            }
            else
            {
                isMenuVisible = true;
            }
            isSliding = false;
            return rightMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            contentParams.rightMargin = values[0];
//            Log.d(TAG,menuParams.leftMargin+" ");
            content.setLayoutParams(contentParams);

        }

        @Override
        protected void onPostExecute(Integer integer) {
            contentParams.rightMargin = integer;
            content.setLayoutParams(contentParams);
        }
    }
}
