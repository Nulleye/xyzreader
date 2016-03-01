package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.remote.Config;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset = 0;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private View mUpButton;

    public final String SHOW_SWIPE_MESSAGE = "show_swipe_message";



    private ArticleDetailFragment mCurrentDetailFragment;
    private int mCurrentPosition = 0;
    private int mStartingPosition = 0;
    private boolean mIsReturning;


    //Invert pager in RTL mode
    protected int getPositionToCursor(int position) {
        if (!Config.isRTL()) return position;
        else return (mCursor.getCount()-1) - position;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().getDecorView().setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
//                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//        }


        setContentView(R.layout.activity_article_detail);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            postponeEnterTransition();

            setEnterSharedElementCallback(new SharedElementCallback() {

                protected void addView(final List<String> names, final Map<String, View> sharedElements,
                        final View rootView, final int viewId) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        final View sharedElement = rootView.findViewById(viewId);
                        if (sharedElement != null) {
                            names.add(sharedElement.getTransitionName());
                            sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                        }
                    }
                }

                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        if (mIsReturning) {
//                            View sharedElement = mCurrentDetailFragment.getArticlePhoto();
//                            sharedElement = mCurrentDetailFragment.getView();
                            View cardSharedElement = mCurrentDetailFragment.getView();
                            if (cardSharedElement != null) {
                                names.clear();
                                sharedElements.clear();
                                names.add(cardSharedElement.getTransitionName());
                                sharedElements.put(cardSharedElement.getTransitionName(), cardSharedElement);
                                //Get sub-views
                                addView(names, sharedElements, cardSharedElement, R.id.article_photo);
//                                addView(names, sharedElements, cardSharedElement, R.id.article_title);
//                                addView(names, sharedElements, cardSharedElement, R.id.article_author);
//                                addView(names, sharedElements, cardSharedElement, R.id.article_date);
                            }
                        }
                }

            }); //SharedElementCallback
        }

        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            private boolean snackShown = false;

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(300);
            }

            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;

                if (mCursor != null) {
                    mCursor.moveToPosition(getPositionToCursor(position));
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                updateUpButtonPosition();

                //SNACK message "Swipe right or left to move between articles"
                //Will be dismissed by pressing the "Dismiss" snack message button or by swiping between articles once.
                if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SHOW_SWIPE_MESSAGE, true)) {
                    View vw = mPager.getRootView();
                    if (vw != null) {
                        vw = vw.findViewWithTag(ArticleDetailFragment.getArticleDetailTag(mCursor.getLong(ArticleLoader.Query._ID)));
                        if (vw != null) {
                            if (!snackShown) {
                                snackShown = true;
                                @SuppressWarnings("ResourceType") Snackbar snack = Snackbar.make(vw, R.string.swipe_message, Snackbar.LENGTH_LONG).setDuration(3000);
                                snack.setAction(R.string.dismiss, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                                        editor.putBoolean(SHOW_SWIPE_MESSAGE, false);
                                        editor.apply();
                                    }
                                });
                                snack.show();
                            } else {
                                //User has swiped once, dismiss message
                                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                                editor.putBoolean(SHOW_SWIPE_MESSAGE, false);
                                editor.apply();
                            }
                        }
                    }
                } //SNACK message

            } //onPageSelected

        }); //ViewPager.SimpleOnPageChangeListener

        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                supportFinishAfterTransition();
            }
        });

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
//                @SuppressLint("NewApi")
//                @Override
//                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
//                        view.onApplyWindowInsets(windowInsets);
//                    mTopInset = windowInsets.getSystemWindowInsetTop();
//                    mUpButtonContainer.setTranslationY(mTopInset);
//                    updateUpButtonPosition();
//                    return windowInsets;
//                }
//            });
//        }

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
                mStartingPosition = getIntent().getIntExtra(ArticleListActivity.STARTING_ARTICLE_POSITION, 0);
                mCurrentPosition = mStartingPosition;
            }
        } else {
            mCurrentPosition = savedInstanceState.getInt(ArticleListActivity.CURRENT_ARTICLE_POSITION);
        }

    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ArticleListActivity.CURRENT_ARTICLE_POSITION, mCurrentPosition);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(ArticleListActivity.STARTING_ARTICLE_POSITION, mStartingPosition);
        data.putExtra(ArticleListActivity.CURRENT_ARTICLE_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        if (mCurrentPosition >= 0)
            mPager.setCurrentItem(getPositionToCursor(mCurrentPosition), false);

        //Original code as fallback, should never happen
        // Select the start ID
        else if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(getPositionToCursor(position), false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }



    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
                mCurrentDetailFragment = fragment;
                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
                updateUpButtonPosition();
                //fragment.refresh();
            }
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(getPositionToCursor(position));
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID), position, mStartingPosition);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }

    } //MyPagerAdapter


}
