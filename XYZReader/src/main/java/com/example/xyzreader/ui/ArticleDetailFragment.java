package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;

    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;

    private View mUpButton;
    private MyTextView mTitleView;
    private TextView mAuthorView;
    private TextView mDateView;

    private int mTopInset;
    private View mPhotoContainerView;
    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    private int mStartingPosition = 0;
    private int mPosition = 0;

    NestedScrollView mNested= null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(ArticleListActivity.CURRENT_ARTICLE_POSITION, position);
        arguments.putInt(ArticleListActivity.STARTING_ARTICLE_POSITION, startingPosition);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID))
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        if (getArguments().containsKey(ArticleListActivity.STARTING_ARTICLE_POSITION))
            mStartingPosition = getArguments().getInt(ArticleListActivity.STARTING_ARTICLE_POSITION);
        if (getArguments().containsKey(ArticleListActivity.CURRENT_ARTICLE_POSITION))
            mPosition = getArguments().getInt(ArticleListActivity.CURRENT_ARTICLE_POSITION);

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }


    public static String getArticleDetailTag(final long articleId) {
        return ARG_ITEM_ID + articleId;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mRootView.setTag(getArticleDetailTag(getArguments().getLong(ARG_ITEM_ID)));
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout)
                mRootView.findViewById(R.id.draw_insets_frame_layout);
        if (mDrawInsetsFrameLayout != null) {

            mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {

                @Override
                public void onInsetsChanged(Rect insets) {
                    mTopInset = insets.top;
                }

            }); //DrawInsetsFrameLayout.OnInsetsCallback

            mScrollView = (ObservableScrollView) mRootView.findViewById(R.id.scrollview);
            mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {

                @Override
                public void onScrollChanged() {
                    mScrollY = mScrollView.getScrollY();
                    getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);
                    mPhotoContainerView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
                    updateStatusBar();
                }

            }); //ObservableScrollView.Callbacks

            mPhotoContainerView = mRootView.findViewById(R.id.photo_container);
        } else {
            mUpButton = ((View)container.getParent()).findViewById(R.id.action_up);
            mAppBarLayout = (AppBarLayout) mRootView.findViewById(R.id.app_bar_layout);
            mAppBarLayout.addOnOffsetChangedListener(this);
            mCollapsingToolbarLayout = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar_layout);
            //CoordinatorLayout coordLayout = (CoordinatorLayout) mRootView.findViewById(R.id.app_coord_layout);
            mNested = (NestedScrollView) mRootView.findViewById(R.id.nested_scroll);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            ArticleListActivity.setTransitionName(mRootView, getString(R.string.transition_card), mItemId);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            ArticleListActivity.setTransitionName(mPhotoView, getString(R.string.transition_photo), mItemId);

        mStatusBarColorDrawable = new ColorDrawable(0);

        mTitleView = (MyTextView) mRootView.findViewById(R.id.article_title);
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
//        ArticleListActivity.setTransitionName(mTitleView, getString(R.string.transition_title), mItemId);

        Log.d(TAG, "onCreateView: " + mTitleView.getPaddingBottom());

        mAuthorView = (TextView) mRootView.findViewById(R.id.article_author);
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
//        ArticleListActivity.setTransitionName(mAuthorView, getString(R.string.transition_author), mItemId);

        mDateView = (TextView) mRootView.findViewById(R.id.article_date);
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
//        ArticleListActivity.setTransitionName(mDateView, getString(R.string.transition_date), mItemId);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some share sample text")
                        .getIntent(), getString(R.string.action_share)));
            }

        }); //View.OnClickListener

        bindViews();
        updateStatusBar();
        return mRootView;
    }

    private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
        if (mDrawInsetsFrameLayout != null) mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
        else {
            mCollapsingToolbarLayout.setContentScrimColor(color);
            mAppBarLayout.setBackground(mStatusBarColorDrawable);
        }
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }
        Resources res = getResources();

        mAuthorView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        //bodyView.setTypeface(Typeface.createFromAsset(res.getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

//            authorView.setText(Html.fromHtml(
//                    DateUtils.getRelativeTimeSpanString(
//                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
//                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
//                            DateUtils.FORMAT_ABBREV_ALL).toString()
//                            + " by <font color='#ffffff'>"
//                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
//                            + "</font>"));

            mAuthorView.setText(String.format(res.getString(R.string.by_author),
                    mCursor.getString(ArticleLoader.Query.AUTHOR)));
            mDateView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,

                            DateUtils.FORMAT_ABBREV_ALL).toString());

//            ImageLoader imgLoader = ImageLoaderHelper.getInstance(getActivity()).getImageLoader();
            ImageLoader imgLoader = CustomVolleyRequestQueue.getInstance(getActivity()).getImageLoader();

            if (mPhotoView instanceof DynamicHeightNetworkImageView)
                ((DynamicHeightNetworkImageView) mPhotoView).setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

//            Picasso.with(getActivity()).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL)).into(
//                    new Target() {
//
//                        @Override
//                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                            if (bitmap != null) {
//                                Palette p = Palette.generate(bitmap, 12);
//                                mMutedColor = p.getDarkMutedColor(0xFF333333);
//
//                                if (mPhotoView instanceof DynamicHeightNetworkImageView)
//                                    ((DynamicHeightNetworkImageView) mPhotoView).setLocalImageBitmap(bitmap);
//                                else mPhotoView.setImageBitmap(bitmap);
//
//                                mRootView.findViewById(R.id.meta_bar).setBackgroundColor(mMutedColor);
//                                updateStatusBar();
//                            } else mPhotoView.setImageDrawable(getResources().getDrawable(R.drawable.empty_detail));
//                            if (mStartingPosition == mPosition) startPostponedEnterTransition();
//                        }
//
//                        @Override
//                        public void onBitmapFailed(Drawable errorDrawable) {
//                            mPhotoView.setImageDrawable(getResources().getDrawable(R.drawable.empty_detail));
//                            if (mStartingPosition == mPosition) startPostponedEnterTransition();
//                        }
//
//                        @Override
//                        public void onPrepareLoad(Drawable placeHolderDrawable) {
//                            mPhotoView.setImageDrawable(getResources().getDrawable(R.drawable.photo_background_protection));
//                        }
//
//                    });

            imgLoader.get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {

                @Override
                public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                    Bitmap bitmap = imageContainer.getBitmap();
                    if (bitmap != null) {
                        Palette p = Palette.generate(bitmap, 12);
                        mMutedColor = p.getDarkMutedColor(0xFF333333);

                        if (mPhotoView instanceof DynamicHeightNetworkImageView)
                            ((DynamicHeightNetworkImageView) mPhotoView).setLocalImageBitmap(bitmap);
                        else mPhotoView.setImageBitmap(bitmap);

                        mRootView.findViewById(R.id.meta_bar).setBackgroundColor(mMutedColor);
                        updateStatusBar();
                        if (mStartingPosition == mPosition) startPostponedEnterTransition();
                    }
                }

                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    if (mStartingPosition == mPosition) startPostponedEnterTransition();
                }

            }); //ImageLoader.ImageListener


            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

        } else {
            mRootView.setVisibility(View.GONE);
            mTitleView.setText("N/A");
            mAuthorView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    //BUG?: In some rare situations (must open and close detail activity several times on the
    // first items), a grey ribbon appears at the end of the screen and seems to be caused by
    // AppBarLayout or NestedScrollView. Calling glitchCorrection() in onOffsetChanged() called
    // by ViewPager.onLayout() the first load time, seems to force correct calculations and
    // no grey ribbon appears.
    // See "BUG?:" on MyAppBarLayout.class for another possibly related bug.
    protected boolean glitchCorrection = true;
    protected void glitchCorrection() {
        Log.d(TAG, "glitchCorrection: " + glitchCorrection);

        if (!glitchCorrection) return;
        glitchCorrection = false;

//Calling mAppBarLayout.requestLayout() seems to detach NestedScroll from CoordinatorLayout on API 16 and 17
//so coordinator is broken and scrolling doesn't affect to NestedScroll!?!?
//        if (mAppBarLayout != null) mAppBarLayout.requestLayout();

        if (mNested != null) mNested.requestLayout();
    }

    private Integer prev = null;

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

        //Prevent reenter due to checkCollision animation repaint
        if ((prev != null) && (prev == verticalOffset)) return;
        prev = verticalOffset;

        //notify TitleView to check for collision with UpButton and adjust text accordingly
        mTitleView.checkCollision(mUpButton, this.getView());

        //See BUG? above
        glitchCorrection();

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Transition stuff

    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on the screen.
     */
    @Nullable
    ImageView getArticlePhoto() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }


    private void startPostponedEnterTransition() {

        if (mPosition == mStartingPosition)
            mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                @Override
                public boolean onPreDraw() {
                    mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getActivity().startPostponedEnterTransition();
                    }
                    return true;
                }

            }); //ViewTreeObserver.OnPreDrawListener

    }

}
