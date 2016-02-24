package com.huangzhiwei.slidemenueasyway;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements View.OnTouchListener {

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
    private LinearLayout.LayoutParams menuParams;

    /**
     * menu最多可以滑动到的左边缘。值由menu布局的宽度来定，marginLeft到达此值之后，不能再减少。
     */
    private int leftEdge;

    /**
     * menu最多可以滑动到的右边缘。值恒为0，即marginLeft到达0之后，不能增加。
     */
    private int rightEdge = 0;

    /**
     * 屏幕宽度
     */
    private int screenWidth;
    /**
     * menu右侧留出的距离
     */
    private final int menuPadding = 80;


    /**
     * 记录手指按下时的横坐标。
     */
    private float xDown;

    /**
     * 记录手指移动时的横坐标。
     */
    private float xMove;

    /**
     * 记录手机抬起时的横坐标。
     */
    private float xUp;

    /**
     * menu当前是显示还是隐藏。只有完全显示或隐藏menu时才会更改此值，滑动过程中此值无效。
     */
    private boolean isMenuVisible;

    /**
     * 用于计算手指滑动的速度。
     */
    private VelocityTracker mVelocityTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initValues();
        content.setOnTouchListener(this);
    }

    /**
     * 初始化一些关键性数据。包括获取屏幕的宽度，给content布局重新设置宽度，给menu布局重新设置宽度和偏移距离等。
     */
    private void initValues()
    {
        WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        screenWidth = windowManager.getDefaultDisplay().getWidth();
        menu = findViewById(R.id.menu);
        content = findViewById(R.id.content);
        menuParams = (LinearLayout.LayoutParams) menu.getLayoutParams();
        // 将menu的宽度设置为屏幕宽度减去menuPadding
        menuParams.width = screenWidth - menuPadding;
        // 左边缘的值赋值为menu宽度的负数
        leftEdge = - menuParams.width;
        // menu的leftMargin设置为左边缘的值，这样初始化时menu就变为不可见
        menuParams.leftMargin = leftEdge;
        // 将content的宽度设置为屏幕宽度
        content.getLayoutParams().width = screenWidth;

    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        createVelocityTracker(event);
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                // 手指按下时，记录按下时的横坐标
                xDown = event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                // 手指移动时，对比按下时的横坐标，计算出移动的距离，来调整menu的leftMargin值，从而显示和隐藏menu
                xMove = event.getRawX();
                int distance = (int) (xMove - xDown);
                if(isMenuVisible)//menu栏显示
                {
                    menuParams.leftMargin = distance;
                }
                else if(!isMenuVisible)//menu栏不显示
                {
                    menuParams.leftMargin = leftEdge + distance;
                }

                if(menuParams.leftMargin < leftEdge)
                    menuParams.leftMargin = leftEdge;
                else if(menuParams.leftMargin > rightEdge)
                    menuParams.leftMargin = rightEdge;

                menu.setLayoutParams(menuParams);
                break;
            case MotionEvent.ACTION_UP:
                // 手指抬起时，进行判断当前手势的意图，从而决定是滚动到menu界面，还是滚动到content界面
                xUp = event.getRawX();
                if(wantShowMenu())
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
                else if(wantShowContent())
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
                recycleVelocityTracker();
                break;
        }
        return true;
    }










    /**
     * 判断当前手势的意图是不是想显示menu。如果手指移动的距离是正数，且当前menu是不可见的，则认为当前手势是想要显示menu。
     *
     * @return 当前手势想显示menu返回true，否则返回false。
     */
    private boolean wantShowMenu() {
        return xUp>xDown&& !isMenuVisible;
    }

    /**
     * 判断当前手势的意图是不是想显示content，如果手指移动的距离是负数，且当前menu时可见的，则认为当前手势是想要显示content
     *
     * @return 当前手势想显示content
     */
    private boolean wantShowContent() {
        return xUp<xDown && isMenuVisible;
    }

    /**
     * 判断是否应该滚动将menu展示出来。如果手指移动距离大于屏幕的1/2，或者手指移动速度大于SNAP_VELOCITY，
     * 就认为应该滚动将menu展示出来。
     *
     * @return 如果应该滚动将menu展示出来返回true，否则返回false。
     */
    private boolean shouldScrollToMenu() {
        return xUp-xDown>screenWidth/2 || getScrollVelocity() > SNAP_VELOCITY;
    }

    /**
     * 判断是否应该滚动将content展示出来。如果手指移动距离加上menuPadding大于屏幕的1/2，
     * 或者手指移动速度大于SNAP_VELOCITY， 就认为应该滚动将content展示出来。
     *
     * @return 如果应该滚动将content展示出来返回true，否则返回false。
     */
    private boolean shouldScrollToContent() {
        return xDown-xUp+menuPadding>screenWidth/2 || getScrollVelocity() > SNAP_VELOCITY;
    }

    /**
     * 将屏幕滚动到menu界面，滚动速度设定为30.
     */
    private void scrollToMenu() {
        new ScrollTask().execute(30);
    }

    /**
     * 将屏幕滚动到content界面，滚动速度设定为-30.
     */
    private void scrollToContent() {
        new ScrollTask().execute(-30);
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


    /**
     * 获取手指在content界面滑动的速度。
     *
     * @return 滑动速度，以每秒钟移动了多少像素值为单位。
     */
    private int getScrollVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        int velocity = (int) mVelocityTracker.getXVelocity();
        return Math.abs(velocity);
    }

    /**
     * 回收VelocityTracker对象。
     */
    private void recycleVelocityTracker()
    {
        mVelocityTracker.recycle();
        mVelocityTracker = null;

    }
    class ScrollTask extends AsyncTask<Integer,Integer,Integer>
    {

        @Override
        protected Integer doInBackground(Integer... params) {
            int leftMargin = menuParams.leftMargin;
            int speed = params[0];
            while(true)
            {
                Log.d(TAG,leftMargin+" ");
                leftMargin += speed;
                if(leftMargin>rightEdge)
                {
                    leftMargin = rightEdge;
                    break;
                }
                else if(leftMargin<leftEdge)
                {
                    leftMargin = leftEdge;
                    break;
                }
                publishProgress(leftMargin);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(speed>0)
            {
                isMenuVisible = true;
            }
            else
            {
                isMenuVisible = false;
            }
            return leftMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            menuParams.leftMargin = values[0];
//            Log.d(TAG,menuParams.leftMargin+" ");
            menu.setLayoutParams(menuParams);

        }

        @Override
        protected void onPostExecute(Integer integer) {
            menuParams.leftMargin = integer;
            menu.setLayoutParams(menuParams);
        }
    }

}
