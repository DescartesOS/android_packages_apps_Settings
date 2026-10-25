/*
 * SPDX-FileCopyrightText: DescartesOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class DescartesBlurView extends FrameLayout {
    private static final float BLUR_RADIUS_PX = 25.0f;

    private final Paint mTintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Consumer<Boolean> mBlurListener;
    private boolean mBlurSupported;
    private int mSurfaceColor;

    public DescartesBlurView(@NonNull Context context) {
        this(context, null);
    }

    public DescartesBlurView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DescartesBlurView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        mSurfaceColor = getContext().getColor(
                com.android.settingslib.widget.theme.R.color.settingslib_materialColorSurfaceContainerHigh);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float radius = view.getHeight() / 2f;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        setClipToOutline(true);
        setElevation(getResources().getDimensionPixelSize(R.dimen.descartes_search_pill_elevation));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateBlurState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            final WindowManager wm = getContext().getSystemService(WindowManager.class);
            if (wm != null) {
                mBlurListener = enabled -> updateBlurState();
                try {
                    wm.addCrossWindowBlurEnabledListener(mBlurListener);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mBlurListener != null) {
            final WindowManager wm = getContext().getSystemService(WindowManager.class);
            if (wm != null) {
                try {
                    wm.removeCrossWindowBlurEnabledListener(mBlurListener);
                } catch (Throwable ignored) {
                }
            }
            mBlurListener = null;
        }
    }

    public void updateBlurState() {
        mBlurSupported = isBlurSupported(getContext());
        final int alpha = mBlurSupported ? 0xCC : 0xEE;
        mTintPaint.setColor((mSurfaceColor & 0x00FFFFFF) | (alpha << 24));

        if (mBlurSupported) {
            applyBackdropBlur(this, BLUR_RADIUS_PX);
        } else {
            applyBackdropBlur(this, 0);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float radius = getHeight() / 2f;
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), radius, radius, mTintPaint);
        super.onDraw(canvas);
    }

    public static boolean isBlurSupported(Context context) {
        if (!SystemProperties.getBoolean("ro.surface_flinger.supports_background_blur", false)) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            WindowManager wm = context.getSystemService(WindowManager.class);
            if (wm != null && !wm.isCrossWindowBlurEnabled()) {
                return false;
            }
        }
        PowerManager pm = context.getSystemService(PowerManager.class);
        if (pm != null && pm.isPowerSaveMode()) {
            return false;
        }
        return true;
    }

    public static void applyBackdropBlur(View view, float radius) {
        try {
            if (radius > 0) {
                RenderEffect effect = RenderEffect.createBlurEffect(
                        radius, radius, Shader.TileMode.CLAMP);
                Method method = View.class.getMethod("setBackdropRenderEffect", RenderEffect.class);
                method.invoke(view, effect);
            } else {
                Method method = View.class.getMethod("setBackdropRenderEffect", RenderEffect.class);
                method.invoke(view, (Object) null);
            }
        } catch (Throwable ignored) {
        }
    }
}
