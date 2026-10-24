/*
 * SPDX-FileCopyrightText: DescartesOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.android.settings.R;

public class WavyProgressView extends View {

    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wave = new Path();
    private final float stroke;
    private final float amplitude;
    private final float wavelength;
    private final float gap;
    private ValueAnimator phaseAnimator;
    private float phase;
    private int progress;

    public WavyProgressView(Context context) {
        this(context, null);
    }

    public WavyProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        stroke = 4 * density;
        amplitude = 3 * density;
        wavelength = 28 * density;
        gap = 6 * density;
        activePaint.setStyle(Paint.Style.STROKE);
        activePaint.setStrokeCap(Paint.Cap.ROUND);
        activePaint.setStrokeWidth(stroke);
        activePaint.setColor(context.getColor(R.color.settingslib_materialColorPrimary));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setStrokeWidth(stroke);
        trackPaint.setColor(context.getColor(R.color.settingslib_materialColorSecondaryContainer));
    }

    public void setProgress(int value) {
        progress = Math.max(0, Math.min(100, value));
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        phaseAnimator = ValueAnimator.ofFloat(0f, 1f);
        phaseAnimator.setDuration(1600);
        phaseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        phaseAnimator.setInterpolator(new LinearInterpolator());
        phaseAnimator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        phaseAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (phaseAnimator != null) {
            phaseAnimator.cancel();
            phaseAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (int) (stroke + amplitude * 2 + getPaddingTop() + getPaddingBottom());
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float start = getPaddingStart() + stroke / 2;
        float end = getWidth() - getPaddingEnd() - stroke / 2;
        float mid = getHeight() / 2f;
        float split = start + (end - start) * progress / 100f;
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        if (rtl) {
            canvas.save();
            canvas.scale(-1, 1, getWidth() / 2f, mid);
        }
        if (split - start > stroke) {
            wave.reset();
            float shift = phase * wavelength;
            for (float x = start; x <= split; x += 1) {
                double angle = 2 * Math.PI * (x - start - shift) / wavelength;
                float y = mid + (float) (Math.sin(angle) * amplitude);
                if (x == start) wave.moveTo(x, y); else wave.lineTo(x, y);
            }
            canvas.drawPath(wave, activePaint);
        }
        float trackStart = progress > 0 ? split + gap : start;
        if (end > trackStart) {
            canvas.drawLine(trackStart, mid, end, mid, trackPaint);
        }
        canvas.drawPoint(end, mid, activePaint);
        if (rtl) canvas.restore();
    }
}
