package com.gepinfo.shiftanddrift;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class ZoomableImageView extends androidx.appcompat.widget.AppCompatImageView implements ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {

    private Matrix matrix = new Matrix();
    private float[] matrixValues = new float[9];

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private float minScale = 1f;
    private float maxScale = 6f;

    private float scaleFactor = 1f;

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, this);
        gestureDetector = new GestureDetector(context, this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fitImageToView();
    }

    private void fitImageToView() {
        Drawable drawable = getDrawable();
        if (drawable == null) return;

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float imageWidth = drawable.getIntrinsicWidth();
        float imageHeight = drawable.getIntrinsicHeight();

        if (imageWidth == 0 || imageHeight == 0) return;

        float scale = Math.max(viewWidth / imageWidth, viewHeight / imageHeight);
        minScale = scale;
        scaleFactor = minScale;

        matrix.reset();
        matrix.postScale(scale, scale);
        float dx = (viewWidth - imageWidth * scale) / 2f;
        float dy = (viewHeight - imageHeight * scale) / 2f;
        matrix.postTranslate(dx, dy);

        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = detector.getScaleFactor();
        float newScale = scaleFactor * scale;

        if (newScale >= minScale && newScale <= maxScale) {
            scaleFactor = newScale;
            matrix.postScale(scale, scale, detector.getFocusX(), detector.getFocusY());
            fixTranslation();
            setImageMatrix(matrix);
        }

        return true;
    }

    @Override public boolean onDown(MotionEvent e) { return true; }
    @Override public void onShowPress(MotionEvent e) {}
    @Override public boolean onSingleTapUp(MotionEvent e) { return false; }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        matrix.postTranslate(-distanceX, -distanceY);
        fixTranslation();
        setImageMatrix(matrix);
        return true;
    }

    @Override public void onLongPress(MotionEvent e) {}
    @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) { return false; }
    @Override public boolean onScaleBegin(ScaleGestureDetector detector) { return true; }
    @Override public void onScaleEnd(ScaleGestureDetector detector) {}

    private void fixTranslation() {
        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scale = matrixValues[Matrix.MSCALE_X];

        Drawable drawable = getDrawable();
        if (drawable == null) return;

        float imageWidth = drawable.getIntrinsicWidth() * scale;
        float imageHeight = drawable.getIntrinsicHeight() * scale;

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float maxTransX = 0;
        float maxTransY = 0;
        float minTransX = viewWidth - imageWidth;
        float minTransY = viewHeight - imageHeight;

        float newTransX = Math.min(Math.max(transX, minTransX), maxTransX);
        float newTransY = Math.min(Math.max(transY, minTransY), maxTransY);

        float dx = newTransX - transX;
        float dy = newTransY - transY;

        matrix.postTranslate(dx, dy);
    }

    public Matrix getImageMatrixCopy() {
        return new Matrix(matrix);
    }

    public void setMinZoom(float minZoom) {
        this.minScale = minZoom;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxScale = maxZoom;
    }
}
