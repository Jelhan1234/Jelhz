package com.example.fakemenu;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.View;

/** JelhzHax logo: red orb, gold ring, white "J" with a gold crown on top, pulsing glow. */
public class JCrownView extends View {
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gold = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint jPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path crown = new Path();
    private final Path jPath = new Path();
    private final RectF band = new RectF();
    private float pulse;
    private ValueAnimator anim;

    public JCrownView(Context c) {
        super(c);
        ring.setStyle(Paint.Style.STROKE);
        glow.setStyle(Paint.Style.STROKE);
        glow.setColor(0xFFFF3030);
        jPaint.setStyle(Paint.Style.STROKE);
        jPaint.setStrokeCap(Paint.Cap.ROUND);
        jPaint.setStrokeJoin(Paint.Join.ROUND);
        jPaint.setColor(0xFFFFFFFF);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        body.setShader(new LinearGradient(0, 0, w, h, 0xFFFF4D4D, 0xFF8A0000, Shader.TileMode.CLAMP));
        ring.setStrokeWidth(w * 0.04f);
        ring.setShader(new SweepGradient(w / 2f, h / 2f,
                new int[]{0xFFFFE08A, 0xFFFFA726, 0xFFFFE08A}, null));
        glow.setStrokeWidth(w * 0.05f);
        gold.setShader(new LinearGradient(0, h * 0.17f, 0, h * 0.44f,
                0xFFFFE08A, 0xFFFFA726, Shader.TileMode.CLAMP));
        jPaint.setStrokeWidth(w * 0.085f);

        crown.reset();
        crown.moveTo(0.34f * w, 0.37f * h);
        crown.lineTo(0.32f * w, 0.24f * h);
        crown.lineTo(0.42f * w, 0.31f * h);
        crown.lineTo(0.50f * w, 0.18f * h);
        crown.lineTo(0.58f * w, 0.31f * h);
        crown.lineTo(0.68f * w, 0.24f * h);
        crown.lineTo(0.66f * w, 0.37f * h);
        crown.close();
        band.set(0.34f * w, 0.39f * h, 0.66f * w, 0.44f * h);

        jPath.reset();
        jPath.moveTo(0.40f * w, 0.56f * h);
        jPath.lineTo(0.68f * w, 0.56f * h);
        jPath.moveTo(0.57f * w, 0.56f * h);
        jPath.lineTo(0.57f * w, 0.68f * h);
        jPath.cubicTo(0.57f * w, 0.80f * h, 0.36f * w, 0.82f * h, 0.34f * w, 0.70f * h);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1800);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.addUpdateListener(a -> {
            pulse = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (anim != null) anim.cancel();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f, r = w / 2f - w * 0.10f;

        glow.setAlpha((int) (50 + 90 * pulse));
        c.drawCircle(cx, cy, r + w * 0.03f + w * 0.03f * pulse, glow);
        c.drawCircle(cx, cy, r, body);
        c.drawCircle(cx, cy, r, ring);

        c.drawPath(jPath, jPaint);
        c.drawPath(crown, gold);
        c.drawRoundRect(band, w * 0.015f, w * 0.015f, gold);
        float gem = w * 0.022f;
        c.drawCircle(0.32f * w, 0.23f * h, gem, gold);
        c.drawCircle(0.50f * w, 0.17f * h, gem, gold);
        c.drawCircle(0.68f * w, 0.23f * h, gem, gold);
    }
}
