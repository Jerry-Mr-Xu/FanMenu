package com.jerry.fanmenu.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.PopupWindow;

/**
 * 扇形菜单（用PopupWindow实现）
 * <p>
 * Created by xujierui on 2018/6/4.
 */
public class FanMenu {
    // 菜单显示位置与手指点击位置的距离
    private static final int FINGER_DISTANCE = 200;
    // 扇形最小半径
    private static final int PAN_MIN_RADIUS = 500;

    private static FanMenu _Instance;
    private PopupWindow menu;
    private FanView fanView;

    // 背景颜色ID（注意这里PopupWindow和父容器是一样大的）
    @ColorRes
    private int bgColorId;
    // 触摸坐标
    private PointF touchPoint;
    // 扇形个数
    private int[] menuDrawableIds;
    // 动画总时长
    private int totalAnimDuration;
    // 每个动画时长
    private int eachAnimDuration;

    private OnFanSelectedListener onFanSelectedListener;

    private FanMenu() {
    }

    /**
     * 创建扇形菜单
     *
     * @return 扇形菜单
     */
    static FanMenu create() {
        if (_Instance == null) {
            _Instance = new FanMenu();
        }
        _Instance.resetParam();
        return _Instance;
    }

    private void resetParam() {
        this.menu = new PopupWindow();
        this.menu.setAnimationStyle(0);

        // 给予参数默认值
        this.bgColorId = -1;
        this.touchPoint = null;
        this.menuDrawableIds = null;
        this.onFanSelectedListener = null;
        this.totalAnimDuration = 500;
        this.eachAnimDuration = 200;
    }

    /**
     * 设置触摸坐标
     *
     * @param touchPoint 触摸坐标
     * @return 扇形菜单
     */
    FanMenu setTouchPoint(@NonNull PointF touchPoint) {
        this.touchPoint = new PointF(touchPoint.x, touchPoint.y);
        return this;
    }

    /**
     * 设置菜单背景颜色（PopupWindow与父容器等大）
     *
     * @param bgColorId 背景颜色ID
     * @return 扇形菜单
     */
    public FanMenu setBgColorId(@ColorRes int bgColorId) {
        this.bgColorId = bgColorId;
        return this;
    }

    /**
     * 设置菜单图标ID
     *
     * @param menuDrawableIds 图标ID
     * @return 扇形菜单
     */
    FanMenu setMenuDrawableIds(int[] menuDrawableIds) {
        this.menuDrawableIds = menuDrawableIds;
        return this;
    }

    FanMenu setTotalAnimDuration(int totalAnimDuration) {
        this.totalAnimDuration = totalAnimDuration;
        return this;
    }

    FanMenu setEachAnimDuration(int eachAnimDuration) {
        this.eachAnimDuration = eachAnimDuration;
        return this;
    }

    FanMenu setOnFanSelectedListener(OnFanSelectedListener onFanSelectedListener) {
        this.onFanSelectedListener = onFanSelectedListener;
        return this;
    }

    /**
     * 显示菜单
     */
    FanMenu show(Context context, View container) {
        checkIsOk();

        Rect containerRect = new Rect();
        container.getDrawingRect(containerRect);

        menu.setWidth(containerRect.width());
        menu.setHeight(containerRect.height());
        if (bgColorId > 0) {
            menu.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(context, bgColorId)));
        } else {
            // 默认透明背景
            menu.setBackgroundDrawable(new ColorDrawable(0));
        }

        fanView = FanView.create(context).setContainerRect(containerRect).setTouchPoint(touchPoint).setTotalAnimDuration(totalAnimDuration).setEachAnimDuration(eachAnimDuration).setMenuBitmaps(menuDrawableIds).setOnFanSelectedListener(onFanSelectedListener);
        fanView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        menu.setContentView(fanView);
        fanView.startShowAnim();

        menu.showAsDropDown(container, 0, -containerRect.height());

        return this;
    }

    private void checkIsOk() {
        if (menu == null) {
            throw new RuntimeException("menu is null");
        }
        if (touchPoint == null) {
            throw new RuntimeException("touchPoint is null");
        }
        if (menuDrawableIds == null || menuDrawableIds.length <= 0) {
            throw new RuntimeException("menuDrawableIds is null or empty");
        }
        if (totalAnimDuration <= 0) {
            throw new RuntimeException("Invalid number " + totalAnimDuration + " totalAnimDuration must bigger than 0");
        }
        if (eachAnimDuration <= 0) {
            throw new RuntimeException("Invalid number " + eachAnimDuration + " eachAnimDuration must bigger than 0");
        }
    }

    /**
     * 关闭菜单
     */
    void dismiss() {
        menu.dismiss();
    }

    public void onTouchEvent(MotionEvent ev) {
        if (fanView != null) {
            fanView.onTouchEvent(ev);
        }
    }

    private static class FanView extends View {
        private static final String TAG = "FanView";
        private float fanRadius;
        private Paint fanPaint;

        private PointF touchPoint;
        private Rect containerRect;

        private Bitmap[] menuBitmaps;

        // 选中的扇形序号
        private int selFanIndex;
        // 扇形个数
        private int fanCount;
        // 扇形角度
        private int fanAngle;

        // 扇形入场动画
        private ObjectAnimator showAnim;
        private int curTime;
        private int totalAnimDuration;
        private int eachAnimDuration;
        private AnimStartEnd[] eachAnimStartEnd;
        // 是否在播放动画
        private boolean isPlayingAnim;

        private OnFanSelectedListener onFanSelectedListener;

        private static FanView _Instance;

        public static FanView create(Context context) {
            if (_Instance == null) {
                _Instance = new FanView(context);
            }
            _Instance.resetParam();
            return _Instance;
        }

        /**
         * 创建扇形菜单
         *
         * @param context 上下文
         */
        private FanView(Context context) {
            super(context);
        }

        /**
         * 重置参数
         */
        public void resetParam() {
            this.curTime = 0;
            this.fanRadius = 0;
            this.containerRect = null;
            this.menuBitmaps = null;
            this.selFanIndex = -1;
            this.fanCount = 0;
            this.fanAngle = 0;
            this.totalAnimDuration = 500;
            this.eachAnimDuration = 200;
            this.eachAnimStartEnd = null;
            this.isPlayingAnim = false;

            if (this.showAnim != null) {
                this.showAnim.end();
                this.showAnim = null;
            }

            this.fanPaint = new Paint();
            this.fanPaint.setAntiAlias(true);
            this.fanPaint.setDither(true);

            this.onFanSelectedListener = null;
        }

        /**
         * 设置ObjectAnim的进度
         *
         * @param curTime 当前动画播放时间
         */
        private void setCurTime(int curTime) {
            this.curTime = curTime;
            invalidate();
        }

        /**
         * 动画总时长
         *
         * @param totalAnimDuration 总时长
         */
        public FanView setTotalAnimDuration(@Size(min = 0) int totalAnimDuration) {
            this.totalAnimDuration = totalAnimDuration;
            return this;
        }

        /**
         * 每个动画时长
         *
         * @param eachAnimDuration 动画时长
         */
        public FanView setEachAnimDuration(@Size(min = 0) int eachAnimDuration) {
            this.eachAnimDuration = eachAnimDuration;
            return this;
        }

        public FanView setMenuBitmaps(@DrawableRes @NonNull int[] iconIds) {
            if (iconIds.length > 0) {
                this.fanCount = iconIds.length;
                this.menuBitmaps = new Bitmap[fanCount];
                for (int i = 0; i < fanCount; i++) {
                    // 将资源ID转为位图
                    menuBitmaps[i] = BitmapFactory.decodeResource(getResources(), iconIds[i]);
                }
            }
            return this;
        }

        public FanView setTouchPoint(@NonNull PointF touchPoint) {
            this.touchPoint = touchPoint;
            return this;
        }

        public FanView setFanPaint(@NonNull Paint fanPaint) {
            this.fanPaint = fanPaint;
            return this;
        }

        public FanView setContainerRect(@NonNull Rect containerRect) {
            this.containerRect = containerRect;
            return this;
        }

        public FanView setOnFanSelectedListener(OnFanSelectedListener onFanSelectedListener) {
            this.onFanSelectedListener = onFanSelectedListener;
            return this;
        }

        public FanView startShowAnim() {
            post(new Runnable() {
                @Override
                public void run() {
                    initAnim();
                    showAnim.start();
                    showAnim.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            isPlayingAnim = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            isPlayingAnim = false;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            isPlayingAnim = false;
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                            isPlayingAnim = true;
                        }
                    });
                }
            });
            return this;
        }

        /**
         * 计算每个扇形所占的角度
         *
         * @param fanRadius      扇形的半径
         * @param containerWidth 容器的宽度
         * @param bitmapRadius   位图的半径
         */
        private void calFanAngle(float fanRadius, int containerWidth, int bitmapRadius) {
            // 扇形菜单展示的最大半径
            float maxRadius = fanRadius + bitmapRadius;

            // 如果最大半径超过容器宽度，那么把扇形角度变小防止图标显示到外面去
            if (maxRadius <= containerWidth) {
                fanAngle = 90 / fanCount;
                return;
            }

            int forbiddenAngle = (int) Math.toDegrees(Math.acos(containerWidth / maxRadius));
            fanAngle = (90 - forbiddenAngle) / fanCount;
        }

        private void calFanRadius(int containerWidth, int containerHeight, PointF touchPoint, int bitmapRadius) {
            // 加上200是为了让菜单能出现在手指前面，而不会被盖掉
            this.fanRadius = new PointF(containerWidth - touchPoint.x, containerHeight - touchPoint.y).length() + FINGER_DISTANCE;
            // 扇形菜单展示的最大半径
            float maxRadius = fanRadius + bitmapRadius;

            // 如果最大半径超过容器高度，那么把半径设成不会超过容器高度
            if (maxRadius > containerHeight) {
                this.fanRadius = containerHeight - bitmapRadius;
            }

            // 设定最小半径为500
            this.fanRadius = Math.max(PAN_MIN_RADIUS, this.fanRadius);
        }

        /**
         * 开始动画前的准备工作
         */
        private void initAnim() {
            checkIsOk();

            // 设置当前动画进度为0
            this.curTime = 0;

            this.showAnim = ObjectAnimator.ofInt(this, "curTime", 0, totalAnimDuration);
            this.showAnim.setDuration(totalAnimDuration);
            this.showAnim.setInterpolator(new LinearInterpolator());

            this.eachAnimStartEnd = new AnimStartEnd[fanCount];

            // 计算扇形半径
            calFanRadius(containerRect.width(), containerRect.height(), touchPoint, menuBitmaps[0].getHeight() / 2);
            // 计算每个扇形分到的角度
            calFanAngle(fanRadius, containerRect.width(), menuBitmaps[fanCount - 1].getHeight() / 2);

            // 每个动画的时间偏移
            int offset = fanCount > 1 ? (totalAnimDuration - eachAnimDuration) / (fanCount - 1) : 0;
            for (int i = 0; i < fanCount; i++) {
                // 计算每个动画的开始和结束时间
                eachAnimStartEnd[i] = new AnimStartEnd(i * offset, i * offset + eachAnimDuration);
            }
        }

        private void checkIsOk() {
            if (totalAnimDuration <= 0) {
                throw new RuntimeException("Invalid number " + totalAnimDuration + " totalAnimDuration must bigger than 0");
            }
            if (eachAnimDuration <= 0) {
                throw new RuntimeException("Invalid number " + eachAnimDuration + " eachAnimDuration must bigger than 0");
            }
            if (menuBitmaps == null || menuBitmaps.length <= 0) {
                throw new RuntimeException("Array menuBitmaps is null or empty");
            }
            if (touchPoint == null) {
                throw new RuntimeException("touchPoint is null");
            }
            if (containerRect == null) {
                throw new RuntimeException("containerRect is null");
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            // 如果在播放动画那么不进行选中判断
            if (isPlayingAnim) {
                return true;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE: {
                    // 处理异常情况
                    if (containerRect == null || fanAngle <= 0 || menuBitmaps == null || menuBitmaps.length <= 0 || selFanIndex >= menuBitmaps.length || fanRadius <= 0) {
                        return true;
                    }

                    // 保存上次的选择，减少UI刷新次数
                    int lastSelIndex = selFanIndex;

                    // 获取触摸点坐标（坐标原点在右下角，为了方便计算）
                    PointF touchPoint = new PointF(containerRect.width() - event.getX(), containerRect.height() - event.getY());
                    // 根据角度确定选中菜单的序号
                    int touchAngle = (int) Math.toDegrees(Math.atan(touchPoint.x / touchPoint.y));
                    selFanIndex = touchAngle / fanAngle;

                    // 根据距离来确定是否选中对应序号的菜单
                    float touchRange = touchPoint.length();
                    float bitmapRadius = menuBitmaps[selFanIndex].getHeight() / 2;
                    if (touchRange < fanRadius - bitmapRadius || touchRange > fanRadius + bitmapRadius) {
                        selFanIndex = -1;
                    }

                    if (lastSelIndex != selFanIndex) {
                        // 如果和上次选中的不一样，那么刷新UI
                        invalidate();
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (onFanSelectedListener != null) {
                        onFanSelectedListener.onFanSelected(selFanIndex);
                    }
                    break;
                }
            }

            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            // 将坐标调整到右下角，并旋转
            canvas.translate(containerRect.width(), containerRect.height());
            canvas.rotate(180);

            // 绘制扇形菜单
            for (int i = 0; i < fanCount; i++) {
                if (curTime < eachAnimStartEnd[i].start) {
                    // 如果这个动画还没开始那么之后的也没开始
                    break;
                }

                float eachAnimProgress;
                if (curTime >= eachAnimStartEnd[i].end) {
                    // 当前动画结束
                    eachAnimProgress = 1f;
                } else {
                    // 计算当前动画的进度
                    eachAnimProgress = Math.min((curTime - eachAnimStartEnd[i].start) * 1.0f / eachAnimDuration, 1);
                }

                // 转动坐标
                if (i == 0) {
                    canvas.rotate(-fanAngle / 2);
                } else {
                    canvas.rotate(-fanAngle);
                }
                canvas.save();
                // 逐渐放大的动画
                canvas.scale(eachAnimProgress, eachAnimProgress);
                // 将坐标移至绘画点，并转回正常方向，便于绘制位图
                canvas.translate(0, fanRadius);
                canvas.rotate(-(180 - fanAngle * i - fanAngle / 2));
                if (!isPlayingAnim && i == selFanIndex) {
                    // 放大选中的图标
                    canvas.scale(1.2f, 1.2f);
                }

                canvas.drawBitmap(menuBitmaps[i], -menuBitmaps[i].getWidth() / 2, -menuBitmaps[i].getHeight() / 2, fanPaint);
                canvas.restore();
            }

            canvas.restore();
        }
    }

    private static class AnimStartEnd {
        float start;
        float end;

        AnimStartEnd(float start, float end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "AnimStartEnd{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }
    }

    public interface OnFanSelectedListener {
        public void onFanSelected(int selIndex);
    }
}
