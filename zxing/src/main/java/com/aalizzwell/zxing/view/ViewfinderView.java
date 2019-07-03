package com.aalizzwell.zxing.view;


import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.google.zxing.ResultPoint;
import com.maizi.zxing.R;
import com.aalizzwell.zxing.bean.InitOption;
import com.aalizzwell.zxing.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

public final class ViewfinderView extends View {

    /*界面刷新间隔时间*/
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;

    private CameraManager cameraManager;
    private Paint paint, scanLinePaint, reactPaint, frameLinePaint;
    private Bitmap resultBitmap;
    private int maskColor; // 取景框外的背景颜色
    private int resultColor;// result Bitmap的颜色
    private int resultPointColor; // 特征点的颜色
    private int frameCornerColor;//四个角的颜色
    private int frameLineColor = -1;//边框线的颜色
    private int scanLineColor;//扫描线的颜色
    private int tipTextColor;//提示文字颜色
    private TextPaint tipTextPaint;

    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;
    // 扫描线移动的y
    private int scanLineTop;
    private InitOption initOption;
    private ValueAnimator valueAnimator;
    private Rect frame;

    public ViewfinderView(Context context) {
        this(context, null);
    }

    public ViewfinderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public void setInitConfig(InitOption initOption) {
        this.initOption = initOption;
        frameCornerColor = ContextCompat.getColor(getContext(), initOption.getFrameCornerColor());
        frameLineColor = ContextCompat.getColor(getContext(), initOption.getFrameLineColor());
        scanLineColor = ContextCompat.getColor(getContext(), initOption.getScanLineColor());
        tipTextColor = ContextCompat.getColor(getContext(), initOption.getTipTextColor());
        initPaint();
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        maskColor = ContextCompat.getColor(getContext(), R.color.viewfinder_mask_color);
        resultColor = ContextCompat.getColor(getContext(), R.color.result_view);
        resultPointColor = ContextCompat.getColor(getContext(), R.color.possible_result_points);
        possibleResultPoints = new ArrayList<ResultPoint>(10);
        lastPossibleResultPoints = null;


    }

    private void initPaint() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        //四个角的画笔
        reactPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reactPaint.setColor(frameCornerColor);
        reactPaint.setStyle(Paint.Style.FILL);
        reactPaint.setStrokeWidth(dp2px(1));
        //边框线画笔
        frameLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        frameLinePaint.setColor(ContextCompat.getColor(getContext(), initOption.getFrameLineColor()));
        frameLinePaint.setStrokeWidth(dp2px(1));
        frameLinePaint.setStyle(Paint.Style.STROKE);
        //扫描线画笔
        scanLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scanLinePaint.setStrokeWidth(dp2px(2));
        scanLinePaint.setStyle(Paint.Style.FILL);
        scanLinePaint.setDither(true);
        scanLinePaint.setColor(scanLineColor);
        //提示文字画笔
        tipTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tipTextPaint.setTextSize(dp2px(initOption.getTipTextSize()));
        tipTextPaint.setColor(tipTextColor);

    }

    private void initAnimator() {

        if (valueAnimator == null) {
            valueAnimator = ValueAnimator.ofInt(frame.top, frame.bottom);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.setRepeatMode(ValueAnimator.RESTART);
            valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    scanLineTop = (int) animation.getAnimatedValue();
                    invalidate();

                }
            });

            valueAnimator.start();
        }


    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;


    }

    public void stopAnimator() {
        if (valueAnimator != null) {
            valueAnimator.end();
            valueAnimator.cancel();
            valueAnimator = null;
        }

    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return;
        }
        //frame为取景框
        frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        initAnimator();
        //绘制灰色遮罩
        drawExterior(canvas, getWidth(), getHeight());
        //绘制取景框边框
        drawFrame(canvas);
        //绘制提示文字
        drawTipText(canvas, frame);
        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            // 如果有二维码结果的Bitmap，在扫取景框内绘制不透明的result Bitmap
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            //绘制扫描线
            drawLaserScanner(canvas);
            //绘制闪动的点
            // drawPoint(canvas, frame, previewFrame);
        }
    }

    private void drawTipText(Canvas canvas, Rect frame) {
        int baseX = (int) (canvas.getWidth() / 2 - tipTextPaint.measureText(initOption.getTipTextContent()) / 2);
        canvas.drawText(initOption.getTipTextContent(), baseX, frame.bottom + dp2px(30), tipTextPaint);
    }

    private void drawPoint(Canvas canvas, Rect frame, Rect previewFrame) {
        float scaleX = frame.width() / (float) previewFrame.width();
        float scaleY = frame.height() / (float) previewFrame.height();

        // 绘制扫描线周围的特征点
        List<ResultPoint> currentPossible = possibleResultPoints;
        List<ResultPoint> currentLast = lastPossibleResultPoints;
        int frameLeft = frame.left;
        int frameTop = frame.top;
        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new ArrayList<ResultPoint>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(CURRENT_POINT_OPACITY);
            paint.setColor(resultPointColor);
            synchronized (currentPossible) {
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frameLeft
                                    + (int) (point.getX() * scaleX), frameTop
                                    + (int) (point.getY() * scaleY), POINT_SIZE,
                            paint);
                }
            }
        }
        if (currentLast != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY / 2);
            paint.setColor(resultPointColor);
            synchronized (currentLast) {
                float radius = POINT_SIZE / 2.0f;
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frameLeft
                            + (int) (point.getX() * scaleX), frameTop
                            + (int) (point.getY() * scaleY), radius, paint);
                }
            }
        }

        // Request another update at the animation interval, but only
        // repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY, frame.left - POINT_SIZE,
                frame.top - POINT_SIZE, frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE);
    }

    private void drawExterior(Canvas canvas, int width, int height) {
        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom, paint);
        canvas.drawRect(frame.right, frame.top, width, frame.bottom, paint);
        canvas.drawRect(0, frame.bottom, width, height, paint);
    }


    /**
     * 绘制取景框边框
     */
    private void drawFrame(Canvas canvas) {

        //扫描框的边框线
        canvas.drawRect(frame, frameLinePaint);

        //四个角的长度和宽度
        int width = frame.width();
        int corLength = (int) (width * 0.07);
        int corWidth = (int) (corLength * 0.2);
        corWidth = corWidth > 15 ? 15 : corWidth;

        //角在线外
        //左上角
        canvas.drawRect(frame.left - corWidth, frame.top, frame.left, frame.top + corLength, reactPaint);
        canvas.drawRect(frame.left - corWidth, frame.top - corWidth, frame.left
                + corLength, frame.top, reactPaint);
        //右上角
        canvas.drawRect(frame.right, frame.top, frame.right + corWidth, frame.top + corLength, reactPaint);
        canvas.drawRect(frame.right - corLength, frame.top - corWidth, frame.right + corWidth, frame.top, reactPaint);
        //左下角
        canvas.drawRect(frame.left - corWidth, frame.bottom - corLength, frame.left, frame.bottom, reactPaint);
        canvas.drawRect(frame.left - corWidth, frame.bottom, frame.left + corLength, frame.bottom + corWidth, reactPaint);
        // 右下角
        canvas.drawRect(frame.right, frame.bottom - corLength, frame.right
                + corWidth, frame.bottom, reactPaint);
        canvas.drawRect(frame.right - corLength, frame.bottom, frame.right
                + corWidth, frame.bottom + corWidth, reactPaint);
    }


    /**
     * 绘制扫描线
     */
    private void drawLaserScanner(Canvas canvas) {
        canvas.drawLine(frame.left, scanLineTop, frame.right, scanLineTop, scanLinePaint);
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live
     * scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }


    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());

    }

}
