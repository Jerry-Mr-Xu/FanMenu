package com.jerry.fanmenu.view;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.jerry.fanmenu.Constant;
import com.jerry.fanmenu.R;

/**
 * 扇形菜单容器
 * <p>
 * Created by xujierui on 2018/6/4.
 */

public class FanContainerLinearLayout extends LinearLayout {
    private static final String TAG = "FanLinearLayout";

    private FanMenu fanMenu;
    private PointF touchPoint = new PointF();
    private FanMenu.OnFanSelectedListener onFanSelectedListener;

    public FanContainerLinearLayout(Context context) {
        super(context);
    }

    public FanContainerLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FanContainerLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnFanSelectedListener(FanMenu.OnFanSelectedListener onFanSelectedListener) {
        this.onFanSelectedListener = onFanSelectedListener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() < Constant.ACTION_MAP.length) {
            Log.e(TAG, "dispatchTouchEvent: " + Constant.ACTION_MAP[ev.getAction()] + " (" + ev.getX() + "," + ev.getY() + ")");
        }

        touchPoint.set(ev.getX(), ev.getY());

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                // 生成扇形菜单
                fanMenu = FanMenu.create().setTouchPoint(touchPoint).setMenuDrawableIds(new int[]{R.mipmap.ic_launcher_round, R.mipmap.ic_launcher_round, R.mipmap.ic_launcher_round, R.mipmap.ic_launcher_round, R.mipmap.ic_launcher_round}).setTotalAnimDuration(500).setEachAnimDuration(200).setOnFanSelectedListener(onFanSelectedListener).show(getContext(), this);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                // 将滑动事件传递到扇形菜单中
                if (fanMenu != null) {
                    fanMenu.onTouchEvent(ev);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                // 销毁扇形菜单
                if (fanMenu != null) {
                    fanMenu.onTouchEvent(ev);
                    fanMenu.dismiss();
                }
                break;
            }
        }

        super.dispatchTouchEvent(ev);
        return true;
    }
}
