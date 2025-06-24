package com.gepinfo.shiftanddrift;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatTextView;

public class NumberPopup {

    public static void showNumber(final Activity activity, final String numberText) {
        // Usa la TextView con bordo
        final StrokeTextView textView = new StrokeTextView(activity);
        textView.setText(numberText);
        textView.setTextSize(64);
        textView.setTextColor(Color.WHITE); // Colore interno
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setPadding(50, 50, 50, 50);
        textView.setAlpha(0f);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;

        FrameLayout rootLayout = activity.findViewById(android.R.id.content);
        rootLayout.addView(textView, params);

        // Animazioni
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(textView, View.SCALE_X, 0f, 1.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(textView, View.SCALE_Y, 0f, 1.5f);
        ObjectAnimator alphaIn = ObjectAnimator.ofFloat(textView, View.ALPHA, 0f, 1f);
        ObjectAnimator alphaOut = ObjectAnimator.ofFloat(textView, View.ALPHA, 1f, 0f);
        alphaOut.setStartDelay(1500);

        scaleX.setDuration(100);
        scaleY.setDuration(100);
        alphaIn.setDuration(100);
        alphaOut.setDuration(100);

        AnimatorSet animSet = new AnimatorSet();
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animSet.playTogether(scaleX, scaleY, alphaIn);
        animSet.play(alphaOut).after(100);

        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rootLayout.removeView(textView);
            }
        });

        animSet.start();
    }

    // ✅ Classe interna per testo con bordo
    private static class StrokeTextView extends AppCompatTextView {
        private final Paint strokePaint = new Paint();

        public StrokeTextView(Context context) {
            super(context);
            init();
        }

        private void init() {
            strokePaint.setAntiAlias(true);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.BLACK); // Colore del bordo
            strokePaint.setStrokeWidth(8);     // Spessore del bordo
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // Sincronizza font e dimensione
            strokePaint.setTextSize(getTextSize());
            strokePaint.setTypeface(getTypeface());

            // Disegna prima il bordo
            String text = getText().toString();
            float x = (getWidth() - strokePaint.measureText(text)) / 2;
            float y = getBaseline();
            canvas.drawText(text, x, y, strokePaint);

            // Poi il testo normale
            super.onDraw(canvas);
        }
    }
}


