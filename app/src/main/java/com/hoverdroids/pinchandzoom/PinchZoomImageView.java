package com.hoverdroids.pinchandzoom;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class PinchZoomImageView extends AppCompatImageView {

    private enum PinchZoomMode{
        NONE, DRAG, ZOOM
    }
    private static final int CLICK = 3;

    private PinchZoomMode mode = PinchZoomMode.NONE;
    private Context context;
    private ScaleGestureDetector scaleDetector;

    //Zooming
    private PointF last = new PointF();
    private PointF start = new PointF();
    private float minScale = 1f;
    private float maxScale = 3f;
    private float currScale = 1f;
    private Matrix imgMatrix = new Matrix();

    //View
    private int viewWidth, viewHeight;
    private float origWidth, origHeight;
    private int oldWidthMeasure, oldHeightMeasure;

    public PinchZoomImageView(Context context) {
        super(context);
        init(context);
    }

    public PinchZoomImageView(Context context, AttributeSet attrs){
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        super.setClickable(true);

        this.context = context;
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        setImageMatrix(imgMatrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener (new OnTouchListener (){

            @Override
            public boolean onTouch (View v, MotionEvent event){
                scaleDetector.onTouchEvent(event);
                PointF curr = new PointF(event.getX(), event.getY());

                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        last.set(curr);
                        start.set(last);
                        mode = PinchZoomMode.DRAG;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if(mode == PinchZoomMode.DRAG){
                            float deltaX = curr.x - last.x;
                            float deltaY = curr.y - last.y;
                            float fixTransX = getFixDragTrans (deltaX, viewWidth, origWidth * currScale);
                            float fixTransY = getFixDragTrans (deltaY, viewHeight, origHeight * currScale);
                            imgMatrix.postTranslate(fixTransX, fixTransY);
                            fixTrans();
                            last.set(curr.x, curr.y);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        mode = PinchZoomMode.NONE;
                        int xDiff = (int) Math.abs(curr.x - start.x);
                        int yDiff = (int) Math.abs(curr.y - start.y);
                        if( xDiff < CLICK && yDiff < CLICK ){
                            performClick();
                        }
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = PinchZoomMode.NONE;
                        break;
                }

                setImageMatrix(imgMatrix);
                invalidate();//redraw

                return true; //indicate the event was handled
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        //Rescales image on rotation
        if(oldHeightMeasure == viewWidth && oldHeightMeasure == viewHeight){
            return;
        }
        oldHeightMeasure = viewHeight;
        oldWidthMeasure = viewWidth;

        if(currScale == 1){
            //Fit to screen

            Drawable drawable = getDrawable();
            if(drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0){
                return;
            }
            int bmWidth = drawable.getIntrinsicWidth();
            int bmHeight = drawable.getIntrinsicHeight();

            float scaleX = (float) viewWidth / (float) bmWidth;
            float scaleY = (float) viewHeight / (float) bmHeight;
            float scale = Math.min(scaleX, scaleY);
            imgMatrix.setScale(scale, scale);

            //Center the image
            float redundantYSpace = (float) viewHeight - (scale * (float) bmHeight);
            float redundantXSpace = (float) viewWidth - (scale * (float) bmWidth);
            redundantYSpace /= (float) 2;
            redundantXSpace /= (float) 2;

            imgMatrix.postTranslate(redundantXSpace, redundantYSpace);

            origWidth = viewWidth - 2 * redundantXSpace;
            origHeight = viewHeight - 2 * redundantYSpace;

            setImageMatrix(imgMatrix);
        }
        fixTrans();
    }

    public void setMaxZoom(float scale){
        maxScale = scale;
    }

    public float getMaxZoom(){
        return maxScale;
    }

    private void fixTrans(){
        float[] m = new float[9];
        imgMatrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];

        float fixTransX = getFixTrans(transX, viewWidth, origWidth * currScale);
        float fixTransY = getFixTrans(transY, viewHeight, origHeight * currScale);

        if(fixTransX != 0 || fixTransY != 0){
            imgMatrix.postTranslate(fixTransX, fixTransY);
        }
    }

    private float getFixTrans(float trans, float viewSize, float contentSize){
        float minTrans, maxTrans;
        if(contentSize <= viewSize){
            minTrans = 0;
            maxTrans = viewSize - contentSize;
        }else{
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans){
            return -trans + minTrans;
        }else if(trans > maxTrans){
            return -trans + maxTrans;
        }else{
            return 0;
        }
    }

    private float getFixDragTrans(float delta, float viewSize, float contentSize){
        return contentSize <= viewSize ? 0 : delta;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector){
            mode = PinchZoomMode.ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector){
            float scaleFactor = detector.getScaleFactor();
            float origScale = currScale;//currScale starts as 1; ever after it's set here
            currScale *= scaleFactor;

            if(currScale > maxScale){
                currScale = maxScale;
                scaleFactor = maxScale / origScale;
            }else if (currScale < minScale){
                currScale = minScale;
                scaleFactor = minScale / origScale;
            }

            if(origWidth * currScale <= viewWidth || origHeight * currScale <= viewHeight){
                imgMatrix.postScale(scaleFactor, scaleFactor, viewWidth / 2, viewHeight / 2);
            }else{
                imgMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            }

            fixTrans();
            return true;
        }
    }
}