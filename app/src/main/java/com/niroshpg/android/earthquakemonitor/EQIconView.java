package com.niroshpg.android.earthquakemonitor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Custom image view showing significance and alert level based on the earth quake data received
 */
public class EQIconView  extends ImageView {
    Paint mPaint = new Paint(0);
    private Drawable mEQWaveImage;
    private int mSig;
    private String mAlert;

    public EQIconView(Context context) {
        super(context);
    }

    public EQIconView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.EQIconView,
                0, 0
        );
        mSig = a.getInt(R.styleable.EQIconView_sig, 1);
        mAlert = a.getString(R.styleable.EQIconView_alert);
    }

    public EQIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Figure out the aspect ratio of the image content
        int desiredSize;
        float aspect;
        Drawable d = getDrawable();
        if (d == null) {
            desiredSize = 0;
            aspect = 1f;
        } else {
            desiredSize = d.getIntrinsicWidth();
            aspect = (float) d.getIntrinsicWidth() / (float) d.getIntrinsicHeight();
        }
        //Get the width based on the measure specs
        int widthSize = getMeasurement(widthMeasureSpec, desiredSize);

        //Calculate height based on aspect
        int heightSize = (int)(widthSize / aspect);

        //Make sure the height we want is not too large
        int specMode = MeasureSpec.getMode(heightMeasureSpec);
        int specSize = MeasureSpec.getSize(heightMeasureSpec);
        if (specMode == MeasureSpec.AT_MOST || specMode == MeasureSpec.EXACTLY) {
            //If our measurement exceeds the max height, shrink back
            if (heightSize > specSize) {
                heightSize = specSize;
                widthSize = (int)(heightSize * aspect);
            }
        }

        //MUST do this to store the measurements
        setMeasuredDimension(widthSize, heightSize);
    }

    private  int getMeasurement(int measureSpec, int contentSize) {
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);
        int resultSize = 0;
        switch (specMode) {
            case View.MeasureSpec.UNSPECIFIED:
                //Big as we want to be
                resultSize = contentSize;
                break;
            case View.MeasureSpec.AT_MOST:
                //Big as we want to be, up to the spec
                resultSize = Math.min(contentSize, specSize);
                break;
            case View.MeasureSpec.EXACTLY:
                //Must be the spec size
                resultSize = specSize;
                break;
        }

        return resultSize;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setColor(getColorForSig(mSig));
        mPaint.setStyle(Paint.Style.FILL);

        final double scale = 0.9;
        int width = (int)(getMeasuredWidth());
        int height = (int)(getMeasuredHeight()*scale);
        mPaint.setStrokeWidth(height/10);
        int r = (int)Math.min(width*scale/2,height*scale/2);
        int xc=(int)(width/2);
        int yc=(int)(height/2);
        canvas.drawCircle(xc,yc,r,mPaint);

        super.onDraw(canvas);
    }

    public int getSig() {
        return mSig;
    }

    public void setSig(int mSig) {
        this.mSig = mSig;
    }

    public String getAlert() {
        return mAlert;
    }

    public void setAlert(String mAlert) {
        this.mAlert = mAlert;
    }

    private int getColorForSig(int sig)
    {
        int color = Color.BLACK;
        if(sig > 500 && sig <=1000)
        {
            color = Color.RED;
        }
        else if (sig > 400  && sig <=500)
        {
            color = Color.rgb(0xFF, 0xA5, 0x00);
        }
        else if (sig > 300  && sig <=400)
        {
            color = Color.YELLOW;
        }
        else if (sig > 200  && sig <=300)
        {
            color = Color.GREEN;
        }
        else
        {
            color = Color.BLACK;
        }
        return color;
    }
}
