package com.example.xyzreader.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.example.xyzreader.remote.Config;

/**
 * Adaptive text view object, reacts to changes and adapts its text size to occupy the
 * same height as on its initial state.
 * Used on detail title while scrolling to prevent title from overlapping the back arrow.
 * LTR and RTL support
 *
 * Created by cristian on 13/2/16.
 */
public class MyTextView extends TextView implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener
{

    public static String TAG = MyTextView.class.getSimpleName();

    private boolean mIsRtl = false;

    //Initial object properties
    private float mInitialTextSize = 0;
    private int mInitialBottomPadding = 0;

    private ValueAnimator mAnimator = null;
    private boolean mAdjustText = false;
    private int mAdjustHeight = -1;
    private int mAdjustWidth = -1;

    public MyTextView(Context context) {
        super(context);
        getInitData();
    }

    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getInitData();
    }

    public MyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getInitData();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MyTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getInitData();
    }

    protected void getInitData() {
        mIsRtl = Config.isRTL();
        mInitialTextSize = getTextSize();
        mInitialBottomPadding = getPaddingBottom();
        doTextLog("getInitData: " + " textSize: " + mInitialTextSize + " bottomPadding: " + mInitialBottomPadding);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mAdjustText) {
            doTextLog("onMeasure: adjustProportional(" + mAdjustWidth + "/" + mAdjustHeight + ")" + getMeasureDataTextLog(widthMeasureSpec, heightMeasureSpec));

            int width = getMeasuredWidth();
            int height = (width * mAdjustHeight) / mAdjustWidth;

            //Restore original properties
            if (mInitialTextSize != getTextSize()) super.setTextSize(TypedValue.COMPLEX_UNIT_PX, mInitialTextSize);
            if (getPaddingBottom() != mInitialBottomPadding) super.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), mInitialBottomPadding);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int currentHeight = getMeasuredHeight();

            if (height != currentHeight) {
                //Calculate size to fit text in the same original height (original lines)
                int paddBot = mInitialBottomPadding;
                while (height != currentHeight) {
                    super.setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize() - 1F);
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    currentHeight = getMeasuredHeight();
                    if (height > currentHeight) {
                        paddBot = paddBot + (height - currentHeight);
                        super.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), paddBot);
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                        currentHeight = getMeasuredHeight();
                        doTextLog("onMeasure: resize, measuredMe(" + getMeasuredWidth() + "/" + currentHeight  + ",bP: " + paddBot + ") desired(" + width + "/" + height + ")");
                    }
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else doTextLog("onMeasure:" + getMeasureDataTextLog(widthMeasureSpec, heightMeasureSpec));
    }

    protected void doTextLog(final String logText) {
        final String txt = ((getText() != null)? getText().toString() : "N/A");
        Log.d(TAG, "id: " + System.identityHashCode(this) + " " + logText +
                ((txt != null) ? " txt: " + txt.substring(0, Math.min(15, txt.length())) : ""));
    }
    protected String getMeasureDataTextLog(int widthMeasureSpec, int heightMeasureSpec) {
        return " parent(" + MeasureSpec.getSize(widthMeasureSpec) + ":" + MeasureSpec.getMode(widthMeasureSpec) + "/" +
                MeasureSpec.getSize(heightMeasureSpec) + ":" + MeasureSpec.getMode(heightMeasureSpec) +
                ")  measuredMe(" + getMeasuredHeight() + "/" + getMeasuredWidth() + ",bP:" + getPaddingBottom() + ")";
    }


    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        doTextLog("setText: " + text);
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        mInitialTextSize = getTextSize();
        doTextLog("setTextSize1: " + mInitialTextSize);
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        mInitialTextSize = getTextSize();
        doTextLog("setTextSize2: " + mInitialTextSize);
    }


    public boolean checkCollision(final View collideView, final View pageHolder) {

        int[] collideViewLocation = new int[2];
        collideView.getLocationInWindow(collideViewLocation);
        //Rect collideViewDrawRect = new Rect();
        //collideView.getDrawingRect(collideViewDrawRect);

        int[] location = new int[2];
        this.getLocationInWindow(location);
        //Rect drawRect = new Rect();
        //getDrawingRect(drawRect);

        //Coordinates correction as this may be and offscreen fragment
        int[] pageHolderLocation = new int[2];
        pageHolder.getLocationInWindow(pageHolderLocation);
        if (pageHolderLocation[0] > 0) {
            location[0] = location[0] - pageHolderLocation[0];
            location[1] = location[1] - pageHolderLocation[1];
        } else if (pageHolderLocation[0] < 0) {
            location[0] = -(pageHolderLocation[0] - location[0]);
            location[1] = -(pageHolderLocation[1] - location[1]);
        }

        int dist;
        if (!mIsRtl) {
            dist = collideViewLocation[0] + collideView.getMeasuredWidth() - location[0];
            doTextLog("checkCollision(LTR): dist " + collideViewLocation[0] + "+" + collideView.getMeasuredWidth() + "-" + location[0] + "=" + dist);
        } else {
            dist = location[0] + getMeasuredWidth() - collideViewLocation[0];
            doTextLog("checkCollision(RTL): dist " + location[0] + "+" + getMeasuredWidth() + "-" + collideViewLocation[0] + "=" + dist);
        }

        if  (dist > 0) {

            //Check padding
            int newPadding = 0;
            //BUG?: Sometimes collideViewLocationY doesn't count the status bar height!!?
            int diff = location[1] - ((collideViewLocation[1] == 0)? getStatusBarHeight() : collideViewLocation[1]) - collideView.getMeasuredHeight();
            doTextLog("checkCollision: diff " + location[1] + "-" + collideViewLocation[1] + "-" + collideView.getMeasuredHeight() + "=" + diff);
            if (diff < 0) newPadding = Math.min( -diff, dist);
            int currentPadding = ViewCompat.getPaddingStart(this);

            if (currentPadding != newPadding) {
                int delta = (newPadding - currentPadding) / 3;
                //If we have delta and we are on the current page -> animate change
                if ((delta != 0) && (pageHolderLocation[0] == 0) ) {
                    if (mAnimator == null) {
                        mAnimator = ValueAnimator.ofInt(currentPadding, currentPadding + delta, currentPadding + (2*delta), newPadding);
                        mAnimator.setTarget(this);
                        mAnimator.addListener(this);
                        mAnimator.addUpdateListener(this);
                        mAnimator.setDuration(200);
                        mAnimator.start();
                        doTextLog("checkCollision: new animation " + currentPadding + "/" + (currentPadding + delta) + "/" + (currentPadding + (2 * delta)) + "/" + newPadding);
                    } else {
                        //Animator is already running -> update values
                        mAnimator.setIntValues(currentPadding, currentPadding + delta, currentPadding + (2*delta), newPadding);
                        doTextLog("checkCollision: update naimation " + currentPadding + "/" + (currentPadding + delta) + "/" + (currentPadding + (2 * delta)) + "/" + newPadding);
                    }
                } else {
                    setPadding(newPadding);
                    doTextLog("checkCollision: setPadding " + newPadding);
                }
                return true;
            }
        } //if (dist>0)
        doTextLog("checkCollision: skip");
        return false;
    }

    protected int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        mAnimator = null;
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        mAnimator = null;
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        setPadding((Integer) valueAnimator.getAnimatedValue());
    }

    protected void setPadding(final int newPadding) {
        doTextLog("setPadding: new " + newPadding);

        mAdjustText = true;
        mAdjustWidth = getMeasuredWidth(); // - ViewCompat.getPaddingStart(this) - ViewCompat.getPaddingEnd(this);
        mAdjustHeight = getMeasuredHeight(); // - getPaddingTop() - getPaddingBottom();
        if (!mIsRtl) super.setPadding(newPadding, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        else super.setPadding(getPaddingLeft(), getPaddingTop(), newPadding, getPaddingBottom());
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        mInitialBottomPadding = bottom;
        doTextLog("setPadding: (" + left + "," + top + "," + right + "," + bottom + ")");
    }

}
