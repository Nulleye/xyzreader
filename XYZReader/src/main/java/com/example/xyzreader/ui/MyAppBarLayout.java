package com.example.xyzreader.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * BUG?: Solve a bug on AppBarLayout that clips its children if its size is bigger that all available screen space.
 * This may have some sense as an appBar should never be greater that the device screen, but not if
 * the appBar contains a collapsing object.
 *
 * For ex: On "An Empire State of Mind" detail, the text block was clipped almost entirely because on creation
 * Empire state Image height + Title Text block height > Device Screen height
 *
 * See "BUG?:" on ArticleDetailFragment.class for another possible bug.
 *
 * Has AppBarLayout some kind of problems inside Fragments??? or I'm missing something.
 *
 * Created by cristian on 12/2/16.
 */
public class MyAppBarLayout extends android.support.design.widget.AppBarLayout {

    public MyAppBarLayout(Context context) {
        super(context);
    }

    public MyAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.UNSPECIFIED);
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
