/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xsh.ijkplaycmd.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.List;

import xsh.ijkplaycmd.api.R;


/**
 * WaveformView is an Android view that displays a visual representation
 * of an audio waveform.  It retrieves the frame gains from a CheapSoundFile
 * object and recomputes the shape contour at several zoom levels.
 *
 * This class doesn't handle selection or any of the touch interactions
 * directly, so it exposes a listener interface.  The class that embeds
 * this view should add itself as a listener and make the view scroll
 * and respond to other events appropriately.
 *
 * WaveformView doesn't actually handle selection, but it will just display
 * the selected part of the waveform in a different color.
 */
public class WaveformView extends View {

    public interface WaveformListener {
        void waveformTouchStart(float x);

        void waveformTouchMove(float x);

        void waveformTouchEnd();

        void waveformFling(float x);

        void waveformDraw();

        void waveformZoomIn();

        void waveformZoomOut();
    }

    private static int cutTime = 1;

    // Colors
    private Paint mGridPaint;
    private Paint mSelectedLinePaint;
    private Paint mUnselectedLinePaint;
    private Paint mUnselectedBkgndLinePaint;
    private Paint mBorderLinePaint;
    private Paint mPlaybackLinePaint;
    private Paint mTimecodePaint;

    private int[] mLenByZoomLevel;
    private double[][] mValuesByZoomLevel;
    private double[] mZoomFactorByZoomLevel;
    private int[] mHeightsAtThisZoomLevel;
    private int mZoomLevel;
    private int mNumZoomLevels;
    private int mSampleRate;
    private int mSamplesPerFrame;
    private int mOffset;
    private int mSelectionStart;
    private int mSelectionEnd;
    private int mPlaybackPos;
    private float mDensity;
    private float mInitialScaleSpan;
    private WaveformListener mListener;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private List<Bitmap> bitmaps;
    private boolean mInitialized;

    private long duration = 0;
    private int imageWidth = 0 ,imageHeight = 0;

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // We don't want keys, the markers get these
        setFocusable(false);

        Resources res = getResources();
        mGridPaint = new Paint();
        mGridPaint.setAntiAlias(false);
        mGridPaint.setColor(res.getColor(R.color.grid_line));
        mSelectedLinePaint = new Paint();
        mSelectedLinePaint.setAntiAlias(false);
        mSelectedLinePaint.setColor(res.getColor(R.color.waveform_selected));
        mUnselectedLinePaint = new Paint();
        mUnselectedLinePaint.setAntiAlias(false);
        mUnselectedLinePaint.setColor(res.getColor(R.color.waveform_unselected));
        mUnselectedBkgndLinePaint = new Paint();
        mUnselectedBkgndLinePaint.setAntiAlias(false);
        mUnselectedBkgndLinePaint.setColor(res.getColor(R.color.waveform_unselected_bkgnd_overlay));
        mBorderLinePaint = new Paint();
        mBorderLinePaint.setAntiAlias(true);
        mBorderLinePaint.setStrokeWidth(3f);
        mBorderLinePaint.setPathEffect(new DashPathEffect(new float[] { 3.0f, 2.0f }, 0.0f));
        mBorderLinePaint.setColor(res.getColor(R.color.selection_border));
        mPlaybackLinePaint = new Paint();
        mPlaybackLinePaint.setAntiAlias(false);
        mPlaybackLinePaint.setStrokeWidth(5f);
        mPlaybackLinePaint.setColor(res.getColor(R.color.playback_indicator));
        mTimecodePaint = new Paint();
        mTimecodePaint.setTextSize(13);
        mTimecodePaint.setAntiAlias(true);
        mTimecodePaint.setColor(res.getColor(R.color.timecode));
        mTimecodePaint.setShadowLayer(2, 1, 1, res.getColor(R.color.timecode_shadow));

        mGestureDetector = new GestureDetector(
            context,
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                    mListener.waveformFling(vx);
                    return true;
                }
            }
        );

        mScaleGestureDetector = new ScaleGestureDetector(
            context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScaleBegin(ScaleGestureDetector d) {
                    Log.v("Ringdroid", "ScaleBegin " + d.getCurrentSpanX());
                    mInitialScaleSpan = Math.abs(d.getCurrentSpanX());
                    return true;
                }
                @Override
                public boolean onScale(ScaleGestureDetector d) {
//                    float scale = Math.abs(d.getCurrentSpanX());
//                    Log.v("Ringdroid", "Scale " + (scale - mInitialScaleSpan));
//                    if (scale - mInitialScaleSpan > 40) {
//                        mListener.waveformZoomIn();
//                        mInitialScaleSpan = scale;
//                    }
//                    if (scale - mInitialScaleSpan < -40) {
//                        mListener.waveformZoomOut();
//                        mInitialScaleSpan = scale;
//                    }
                    return true;
                }
                @Override
                public void onScaleEnd(ScaleGestureDetector d) {
                    Log.v("Ringdroid", "ScaleEnd " + d.getCurrentSpanX());
                }
            }
        );

        mLenByZoomLevel = null;
        mValuesByZoomLevel = null;
        mHeightsAtThisZoomLevel = null;
        mOffset = 0;
        mPlaybackPos = -1;
        mSelectionStart = 0;
        mSelectionEnd = 0;
        mDensity = 1.0f;
        mInitialized = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mListener.waveformTouchStart(event.getX());
                break;
            case MotionEvent.ACTION_MOVE:
                mListener.waveformTouchMove(event.getX());
                break;
            case MotionEvent.ACTION_UP:
                mListener.waveformTouchEnd();
                break;
            default:
                break;
        }
        return true;
    }

    public boolean hasSoundFile() {
        return duration != 0;
    }

    public void setBitmaps(List<Bitmap> bitmaps) {
        this.bitmaps = bitmaps;
//        invalidate();
        postInvalidate();
    }

    public void setDuration(long duration) {
        this.duration = duration;
        mSampleRate = 100000;
        mSamplesPerFrame = 1000;
        computeDoublesForAllZoomLevels();
        mHeightsAtThisZoomLevel = null;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public int getZoomLevel() {
        return mZoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        while (mZoomLevel > zoomLevel) {
            zoomIn();
        }
        while (mZoomLevel < zoomLevel) {
            zoomOut();
        }
    }

    public boolean canZoomIn() {
        return (mZoomLevel > 0);
    }

    public void zoomIn() {
        if (canZoomIn()) {
            mZoomLevel--;
            mSelectionStart *= 2;
            mSelectionEnd *= 2;
            mHeightsAtThisZoomLevel = null;
            int offsetCenter = mOffset + getMeasuredWidth() / 2;
            offsetCenter *= 2;
            mOffset = offsetCenter - getMeasuredWidth() / 2;
            if (mOffset < 0) {
                mOffset = 0;
            }
            invalidate();
        }
    }

    public boolean canZoomOut() {
        return (mZoomLevel < mNumZoomLevels - 1);
    }

    public void zoomOut() {
        if (canZoomOut()) {
            mZoomLevel++;
            mSelectionStart /= 2;
            mSelectionEnd /= 2;
            int offsetCenter = mOffset + getMeasuredWidth() / 2;
            offsetCenter /= 2;
            mOffset = offsetCenter - getMeasuredWidth() / 2;
            if (mOffset < 0) {
                mOffset = 0;
            }
            mHeightsAtThisZoomLevel = null;
            invalidate();
        }
    }

    public int maxPos() {
        return mLenByZoomLevel[mZoomLevel];
    }

    public int secondsToFrames(double seconds) {
        return (int)(1.0 * seconds * mSampleRate / mSamplesPerFrame + 0.5);
    }

    public int secondsToPixels(double seconds) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int)(z * seconds * mSampleRate / mSamplesPerFrame + 0.5);
    }

    public double pixelsToSeconds(int pixels) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (pixels * (double)mSamplesPerFrame / (mSampleRate * z));
    }

    public int millisecsToPixels(int msecs) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int)((msecs * 1.0 * mSampleRate * z) /
                     (1000.0 * mSamplesPerFrame) + 0.5);
    }

    public int pixelsToMillisecs(int pixels) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int)(pixels * (1000.0 * mSamplesPerFrame) /
                     (mSampleRate * z) + 0.5);
    }

    public void setParameters(int start, int end, int offset) {
        mSelectionStart = start;
        mSelectionEnd = end;
        mOffset = offset;
    }

    public int getStart() {
        return mSelectionStart;
    }

    public int getEnd() {
        return mSelectionEnd;
    }

    public int getOffset() {
        return mOffset;
    }

    public void setPlayback(int pos) {
        mPlaybackPos = pos;
    }

    public int getPlaybackPos() {
        return mPlaybackPos;
    }

    public void setListener(WaveformListener listener) {
        mListener = listener;
    }

    public void recomputeHeights(float density) {
        mHeightsAtThisZoomLevel = null;
        mDensity = density;
        mTimecodePaint.setTextSize((int)(13 * density));

        invalidate();
    }

    protected void drawWaveformLine(Canvas canvas,
                                    int x, int y0, int y1,
                                    Paint paint) {
        canvas.drawLine(x, y0, x, y1, paint);
    }

    /**
     * 绘制所有帧缩略图
     * @param canvas 画布
     */
    private void
    drawFrameAtTime(Canvas canvas, int width) {
        double onePixelInSecs = pixelsToSeconds(1);
        double fractionalSecs = mOffset * onePixelInSecs;
        int integerSecs = (int) fractionalSecs;
        int i = 0;
        int s = 0;
        while (i < width) {
            i++;
            fractionalSecs += onePixelInSecs;
            int integerSecsNew = (int) fractionalSecs;
            // 绘制时间刻度
            if (integerSecsNew != integerSecs) {
                integerSecs = integerSecsNew;
                int c = integerSecs % cutTime;
                if(c == 0){
                    if(imageWidth == 0){
                        if(integerSecs / cutTime == 1){
                            imageWidth = i;
                        }
                    }
                    imageHeight = imageWidth;
//                    if(imageHeight == 0){
//                        imageHeight = getMeasuredHeight() - 10 - (int) (12 * mDensity + 5);
//                    }
                }
                // Turn, e.g. 67 seconds into "1:07"
                String timecodeMinutes = "" + (integerSecs / 60);
                String timecodeSeconds = "" + (integerSecs % 60);
                if ((integerSecs % 60) < 10) {
                    timecodeSeconds = "0" + timecodeSeconds;
                }
                String timecodeStr = timecodeMinutes + ":" + timecodeSeconds;
                float offset = (float) (0.5 * mTimecodePaint.measureText(timecodeStr));
                canvas.drawText(timecodeStr, i - offset, (int)(12 * mDensity), mTimecodePaint);
            }
            if(bitmaps != null && s < bitmaps.size()) {
                if(i >= s * imageWidth){
                    Bitmap bitmap = bitmaps.get(s);
                    canvas.drawBitmap(bitmap,i - mOffset, getMeasuredHeight() * 0.5f - imageHeight * 0.5f, mSelectedLinePaint);
                    s++;
                }
            }
        }
        if(imageWidth == 0){
            imageWidth = width;
            imageHeight = getMeasuredHeight() - 10 - (int) (12 * mDensity+5);
        }
        // 绘制超出屏幕之外的预览图
        if(bitmaps != null){
            int m = i + mOffset;
            while (i < m && s < bitmaps.size()){
                i++;
                if(i >= s * imageWidth){
                    Bitmap bitmap = bitmaps.get(s);
                    canvas.drawBitmap(bitmap,i - mOffset,getMeasuredHeight() * 0.5f - imageHeight * 0.5f, mSelectedLinePaint);
                    s++;
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (duration == 0) {
            return;
        }

        if (mHeightsAtThisZoomLevel == null) {
            computeIntsForThisZoomLevel();
        }

        // Draw waveform
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int start = mOffset;
        int width = mHeightsAtThisZoomLevel.length - start;
        int ctr = measuredHeight / 2;

        if (width > measuredWidth) {
            width = measuredWidth;
        }

        // 绘制预览帧图片
        drawFrameAtTime(canvas, width);

        // 绘制未选中及播放反馈线
        for (int i = 0; i < width; i++) {
            Paint paint;
            if (i + start >= mSelectionStart &&
                i + start < mSelectionEnd) {
                paint = mSelectedLinePaint;
            } else {
                drawWaveformLine(canvas, i, 0, measuredHeight,
                                 mUnselectedBkgndLinePaint);
                paint = mUnselectedLinePaint;
            }

            // 绘制播放位置反馈线
            if (i + start == mPlaybackPos) {
                canvas.drawLine(i, 0, i, measuredHeight, mPlaybackLinePaint);
            }
        }

        // If we can see the right edge of the waveform, draw the
        // non-waveform area to the right as unselected
        for (int i = width; i < measuredWidth; i++) {
            drawWaveformLine(canvas, i, 0, measuredHeight,
                             mUnselectedBkgndLinePaint);
        }

        // Draw borders 绘制选中区域边框
        canvas.drawLine(
            mSelectionStart - mOffset + 0.5f, 0,
            mSelectionStart - mOffset + 0.5f, measuredHeight,
            mBorderLinePaint);
        canvas.drawLine(
            mSelectionEnd - mOffset + 0.5f, 0,
            mSelectionEnd - mOffset + 0.5f, measuredHeight,
            mBorderLinePaint);

        if (mListener != null) {
            mListener.waveformDraw();
        }
    }

    /**
     * Called once when a new sound file is added
     */
    private void computeDoublesForAllZoomLevels() {
        int numFrames = (int) (duration/10);

        mNumZoomLevels = 5;
        mLenByZoomLevel = new int[5];
        mZoomFactorByZoomLevel = new double[5];
        mValuesByZoomLevel = new double[5][];

        // Level 0 is doubled, with interpolated values
        mLenByZoomLevel[0] = numFrames * 2;
        mZoomFactorByZoomLevel[0] = 2.0;

        // Level 1 is normal
        mLenByZoomLevel[1] = numFrames;
        mValuesByZoomLevel[1] = new double[mLenByZoomLevel[1]];
        mZoomFactorByZoomLevel[1] = 1.0;
        for (int i = 0; i < mLenByZoomLevel[1]; i++) {
            mValuesByZoomLevel[1][i] = 0;
        }

        // 3 more levels are each halved
        for (int j = 2; j < 5; j++) {
            mLenByZoomLevel[j] = mLenByZoomLevel[j - 1] / 2;
            mValuesByZoomLevel[j] = new double[mLenByZoomLevel[j]];
            mZoomFactorByZoomLevel[j] = mZoomFactorByZoomLevel[j - 1] / 2.0;
            for (int i = 0; i < mLenByZoomLevel[j]; i++) {
                mValuesByZoomLevel[j][i] =
                    0.5 * (mValuesByZoomLevel[j - 1][2 * i] +
                           mValuesByZoomLevel[j - 1][2 * i + 1]);
            }
        }

        mZoomLevel = 1;

        mInitialized = true;
    }

    /**
     * Called the first time we need to draw when the zoom level has changed
     * or the screen is resized
     */
    private void computeIntsForThisZoomLevel() {
        int halfHeight = (getMeasuredHeight() / 2) - 1;
        mHeightsAtThisZoomLevel = new int[mLenByZoomLevel[mZoomLevel]];
        for (int i = 0; i < mLenByZoomLevel[mZoomLevel]; i++) {
            mHeightsAtThisZoomLevel[i] =
                (int)(mValuesByZoomLevel[mZoomLevel][i] * halfHeight);
        }
    }

    public int getCutTime() {
        return cutTime;
    }
}
