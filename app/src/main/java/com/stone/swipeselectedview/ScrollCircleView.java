package com.stone.swipeselectedview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 自定义滑动弹性的view
 * Created by stone on 2016/2/8.
 */
public class ScrollCircleView extends FrameLayout {
    private Context context;
    //内部个数
    private int itemNum;
    //内部字体大小
    private float itemTextSize;
    //内部背景
    private int itemBg;
    //内部宽度
    private int itemWidth;
    private AttributeSet itemAttrs;
    //全局画笔
    private Paint paint;
    //hscroll内部的线性布局
    private LinearLayout LL;
    //hscroll偏移量，以便适应内部item滑动正好为50的倍数
    private int hscrollX;
    //该自定义view的宽度
    private int width;

    //滑动效果调整
    private static int x_last = 0;
    private static int x_adjust;
    private int x_down = 0;
    private int x_up = 0;
    //滑动效果的对比中点，用来控制点击房客滑动
    private int x_center = 0;
    //区分滑动还是点击的阈值20
    private final static int threshold = 20;
    //调节滑动handler
    private MyHandler handler;
    //当前选中的数字
    private int tenantNum;
    //当前应该变成红色的textView;
    private TextView tv;


    public ScrollCircleView(Context context) {
        this(context, null);
    }

    public ScrollCircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        //继承layout，默认是不执行onDraw方法的，如果要是onDraw执行，就需要设置false。
        //setWillNotDraw(false);
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ScrollCircleView, defStyleAttr, 0);
        itemNum = array.getInt(R.styleable.ScrollCircleView_itemNum, 0);
        itemTextSize = array.getDimensionPixelSize(R.styleable.ScrollCircleView_itemTextSize, 0);
        itemWidth = array.getDimensionPixelSize(R.styleable.ScrollCircleView_itemWidth, 0);
        itemBg = array.getColor(R.styleable.ScrollCircleView_itemBg, 0);
        array.recycle();
        paint = new Paint();
        initItemView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //该控件的宽度
        width = MeasureSpec.getSize(widthMeasureSpec);
        hscrollX = (width % itemWidth) / 2;//hscroll两侧分别偏移一半
        //onMeasure会走多次，不要再这里添加view
        //initItemView();
        LL.setPadding(hscrollX, 0, hscrollX, 0);

    }

    private void initItemView() {
        //定义包含内部item的linearLayout
        LinearLayout.LayoutParams LLLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        LL = new LinearLayout(context);
        LL.setLayoutParams(LLLayoutParams);

        //定义内部每一个子textView
        ViewGroup.LayoutParams textParams = new ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        //左边多画两个空白的textview 右边多画两个   4个

        for (int i = 0; i < itemNum + 4; i++) {
            TextView itemView = new TextView(context);
            itemView.setTextSize(itemTextSize);
            itemView.setGravity(Gravity.CENTER);
            itemView.setLayoutParams(textParams);
            if(i != 2)
                itemView.setTextColor(getResources().getColor(R.color.gray2));
            else{
                tv = itemView;
                itemView.setTextColor(getResources().getColor(R.color.red));
            }

            if (i > 1 && i < itemNum + 2) {
                itemView.setText((i - 1) + "");
                itemView.setBackgroundColor(itemBg);
            }
            LL.addView(itemView);
        }

        //定义水平滑动的HorizontalScrollView
        ViewGroup.LayoutParams ScrollLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        HorizontalScrollView hscrollView = new HorizontalScrollView(context);
        hscrollView.setLayoutParams(ScrollLayoutParams);
        hscrollView.setHorizontalScrollBarEnabled(false);
        //水平滑动的布局中添加LL
        hscrollView.addView(LL);
        //最外层的Framelayout添加水平滑动
        this.addView(hscrollView);
        setScrollListener(hscrollView, LL);
        handler = new MyHandler(hscrollView, LL);
    }

    /**
     * 给水平滑动的scrollview设置listener
     *
     * @param scrollView
     */
    private void setScrollListener(final HorizontalScrollView scrollView, final LinearLayout scrollViewLL) {
        scrollView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                TextView tenantV = null;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x_down = (int) event.getX();

                        Log.d("<<", "x_down:-->" + x_down);

                        if (x_center == 0)
                            x_center = (int) width / 2 - itemWidth / 2;

                        Log.d("<<", "x_center:-->" + x_center);
                        break;

                    case MotionEvent.ACTION_UP:
                        int x_up = (int) event.getX();
                        int x_scrollView = (int) scrollView.getScrollX();
                        Log.d("<<", "x_up:-->" + x_up);

                        //为滑动事件
                        if (Math.abs(x_up - x_down) > threshold) {
                            Log.d("<<", "handler--->>>111");
                            handler.sendEmptyMessage(1);
                        }
                        //点击事件
                        else {
                            int x_scroll = ((x_up - x_center) / itemWidth) * itemWidth;
                            //往左边滑动，需要调整一个控件的宽度
                            if (x_up - x_center < 0)
                                x_scrollView -= itemWidth;

                            x_scrollView += x_scroll;
                            Log.d("<<", "handler-->" + x_scrollView);
                            //在控件的滑动范围内
                            if (x_scrollView <= (itemNum - 1) * itemWidth && x_scrollView >= 0) {
                                scrollView.smoothScrollTo(x_scrollView, 0);

                                //设置上一个控件字体为黑色
                                if (tv != null)
                                    tv.setTextColor(getResources().getColor(R.color.gray2));
                                //此时的空间字体为红色
                                //这里+2为左侧空白处为了调整添加的两个textview
                                tv = (TextView) scrollViewLL.getChildAt(x_scrollView / itemWidth + 2);
                                Log.d("<<", "curNum:-->>" + (x_scrollView / itemWidth + 2));
                                if (tv != null)
                                    tv.setTextColor(getResources().getColor(R.color.red));
                                tenantNum = x_scrollView / itemWidth + 1;
                            }

                        }

                        break;
                }
                return false;
            }
        });
    }

    /**
     * 执行弹性动画，这里简单使用scrollview的smoothScrollTo
     */
    class MyHandler extends Handler {
        private HorizontalScrollView scrollView;
        private LinearLayout scrollViewLL;

        public MyHandler(HorizontalScrollView scrollView, LinearLayout scrollViewLL) {
            this.scrollView = scrollView;
            this.scrollViewLL = scrollViewLL;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                //判断滚动是否停止
                if (x_last != scrollView.getScrollX()) {
                    x_last = scrollView.getScrollX();
                    handler.sendMessageDelayed(handler.obtainMessage(1), 5);
                } else {
                    //计算出滑动停止后，scrollview应该调整的滑动距离
                    Log.d("<<", "x_last>>>" + x_last);
                    int d = itemWidth / 2;
                    int a = x_last / d;
                    int b = (x_last / d) / 2;
                    int c = (x_last / d) % 2;
                    if (c == 0)
                        x_adjust = b * itemWidth;
                    else
                        x_adjust = (a + 1) * d;

                    //不使用ScrollTo，容易被动画覆盖，影响效果

                    Log.d("<<", "x_adjust" + x_adjust);
                    scrollView.smoothScrollTo(x_adjust, 0);
                    tenantNum = (a + 1) / 2 + 1;


                    if (tenantNum >= 0 && tenantNum <= itemNum) {
                        if (tv != null) {
                            tv.setTextColor(getResources().getColor(R.color.gray2));
                        }
                        tv = (TextView) scrollViewLL.getChildAt(tenantNum + 1);
                        tv.setTextColor(getResources().getColor(R.color.red));
                    }
                    Log.d("<<", ">>>tenant num" + tenantNum);
                }

            }
        }
    }

    /**
     * 使用该view时，获取当前选中的数字
     * @return
     */
    public int getCurrentNum() {
        return tenantNum;
    }


}
