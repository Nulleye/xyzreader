package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener {

    public static String TAG = ArticleListActivity.class.getSimpleName();

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private int mScrollToPos = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.addOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (mScrollToPos != -1) {
                        mScrollToPos = -1;
                        startPostponedEnterTransition();
                    }
                }
            }

        }); //OnScrollListener

        getLoaderManager().initLoader(0, null, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setExitSharedElementCallback(new SharedElementCallback() {

                protected void addView(final List<String> names, final Map<String, View> sharedElements,
                        final View rootView, final int viewId, final int viewTransitionNameId, final Long id) {
                    final View sharedElement = rootView.findViewById(viewId);
                    if (sharedElement != null) {
                        final String transitionName = buildTransitionName(getString(viewTransitionNameId), id);
                        names.add(transitionName);
                        sharedElements.put(transitionName, sharedElement);
                    }
                }

                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    if (mTmpReenterState != null) {
                        int startingPosition = mTmpReenterState.getInt(STARTING_ARTICLE_POSITION);
                        int currentPosition = mTmpReenterState.getInt(CURRENT_ARTICLE_POSITION);
                        if (startingPosition != currentPosition) {
                            // If startingPosition != currentPosition the user must have swiped to a
                            // different page in the DetailsActivity. We must update the shared element
                            // so that the correct one falls into place.
                            Long id = mRecyclerView.getAdapter().getItemId(currentPosition);  //adapter may be null if activity returned with different device orientation
                            String transitionName = buildTransitionName(getString(R.string.transition_card), id);
                            View cardSharedElement = mRecyclerView.findViewWithTag(transitionName);
                            if (cardSharedElement != null) {
                                names.clear();
                                sharedElements.clear();
                                names.add(transitionName);
                                sharedElements.put(transitionName, cardSharedElement);
                                //Get sub-views
                                addView(names, sharedElements, cardSharedElement, R.id.article_photo, R.string.transition_photo, id);
//                                addView(names, sharedElements, cardSharedElement, R.id.article_title, R.string.transition_title, id);
//                                addView(names, sharedElements, cardSharedElement, R.id.article_author, R.string.transition_author, id);
//                                addView(names, sharedElements, cardSharedElement, R.id.article_date, R.string.transition_date, id);
                            }
                        }
                        mTmpReenterState = null;
                    }
//                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        // If mTmpReenterState is null, then the activity is exiting.
//                        View navigationBar = findViewById(android.R.id.navigationBarBackground);
//                        View statusBar = findViewById(android.R.id.statusBarBackground);
//                        if (navigationBar != null) {
//                            names.add(navigationBar.getTransitionName());
//                            sharedElements.put(navigationBar.getTransitionName(), navigationBar);
//                        }
//                        if (statusBar != null) {
//                            names.add(statusBar.getTransitionName());
//                            sharedElements.put(statusBar.getTransitionName(), statusBar);
//                        }
//                    }
                }

            }); //SharedElementCallback
        }

        if (savedInstanceState == null) {
            refresh();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            } else {
                Log.d(TAG, intent.getAction());
            }
        }

    }; //BroadcastReceiver

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mScrollToPos != -1)
                mRecyclerView.scrollToPosition(mScrollToPos);
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onRefresh() {
        if (!mIsRefreshing) refresh();
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            mCursor.moveToPosition(position);
            holder.position = position;

            Long id = mCursor.getLong(ArticleLoader.Query._ID);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                setTransitionName(holder.holderView, getString(R.string.transition_card), id);
                holder.holderView.setTag(holder.holderView.getTransitionName());
            }


            holder.photoView.setImageURI(Uri.parse(mCursor.getString(ArticleLoader.Query.THUMB_URL)));

            holder.photoView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
                setTransitionName(holder.photoView, getString(R.string.transition_photo), id);

//            holder.photoView.setImageUrl(
//                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
//                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());

            holder.photoView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    CustomVolleyRequestQueue.getInstance(ArticleListActivity.this).getImageLoader());

//            Picasso.with(ArticleListActivity.this).load(Uri.parse(mCursor.getString(ArticleLoader.Query.THUMB_URL))).into(
//                    new Target() {
//
//                        @Override
//                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                            if (bitmap != null) holder.photoView.setLocalImageBitmap(bitmap);
//                            else holder.photoView.setImageDrawable(getResources().getDrawable(R.drawable.empty_detail));
//                        }
//
//                        @Override
//                        public void onBitmapFailed(Drawable errorDrawable) {
//                            holder.photoView.setImageDrawable(getResources().getDrawable(R.drawable.empty_detail));
//                        }
//
//                        @Override
//                        public void onPrepareLoad(Drawable placeHolderDrawable) {
//                            holder.photoView.setImageDrawable(getResources().getDrawable(R.drawable.photo_background_protection));
//                        }
//
//                    });

            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
//                setTransitionName(holder.titleView, getString(R.string.transition_title), id);

            holder.authorView.setText(String.format(getString(R.string.by_author),
                    mCursor.getString(ArticleLoader.Query.AUTHOR)));
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
//                setTransitionName(holder.authorView, getString(R.string.transition_author), id);

            holder.dateView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString());
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
//                setTransitionName(holder.dateView, getString(R.string.transition_date), id);

            holder.holderView.setOnClickListener(new View.OnClickListener() {

                private ViewHolder vh =  holder;

                @Override
                public void onClick(View view) {
                    if (mIsRefreshing) return;              //Prevent exception if clicked when loading data
                    if (mIsDetailsActivityStarted) return;  //Prevent double click
                    mIsDetailsActivityStarted = true;

                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.position)));
                    intent.putExtra(STARTING_ARTICLE_POSITION, vh.position);
                    vh.photoView.getDrawable();
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        if ((vh.holderView != null) && (vh.photoView != null)) {
                            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(ArticleListActivity.this,
                                    new Pair<>(vh.holderView, vh.holderView.getTransitionName()),
                                    new Pair<>((View) vh.photoView, vh.photoView.getTransitionName())
//                                    new Pair<>((View) vh.titleView, vh.titleView.getTransitionName())
//                                    new Pair<>((View) vh.authorView, vh.authorView.getTransitionName()),
//                                    new Pair<>((View) vh.dateView, vh.dateView.getTransitionName())
                            ).toBundle();
                            startActivity(intent, bundle);
                            return;
                        }
                    }
                    startActivity(intent);
                }

            }); //View.OnClickListener

        } //onBindViewHolder

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

    } //Adapter


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setTransitionName(final View view, final String name, final Long id) {
        view.setTransitionName(buildTransitionName(name, id));
    }


    public static String buildTransitionName(String name, final Long id) {
        if (name != null) return name + ((id != null)? "_" + id.toString() : "");
        if (id != null) return id.toString();
        return null;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        public View holderView;

        public DynamicHeightNetworkImageView photoView;
        public TextView titleView;
        public TextView authorView;
        public TextView dateView;

        public int position = -1;

        public ViewHolder(View view) {
            super(view);
            holderView = view;
            photoView = (DynamicHeightNetworkImageView) view.findViewById(R.id.article_photo);
            titleView = (TextView) view.findViewById(R.id.article_title);
            authorView = (TextView) view.findViewById(R.id.article_author);
            dateView = (TextView) view.findViewById(R.id.article_date);
        }

    } //ViewHolder


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Animation transition stuff


    public static String STARTING_ARTICLE_POSITION = "starting_article_position";
    public static String CURRENT_ARTICLE_POSITION = "current_article_position";

    private Bundle mTmpReenterState;
    private boolean mIsDetailsActivityStarted;


    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);

        postponeEnterTransition();

        mTmpReenterState = new Bundle(data.getExtras());
        int startingPosition = mTmpReenterState.getInt(STARTING_ARTICLE_POSITION);
        int currentPosition = mTmpReenterState.getInt(CURRENT_ARTICLE_POSITION);
        if (startingPosition != currentPosition) {
            if (mRecyclerView.getAdapter() != null)
                //Force creation of item in recycler so objects could be find later for transition
                mRecyclerView.scrollToPosition(currentPosition);
            else
                mScrollToPos = currentPosition;
        }

        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
                mRecyclerView.requestLayout();
                if (mRecyclerView.getAdapter() != null) {
                    if (mScrollToPos != -1) mRecyclerView.scrollToPosition(mScrollToPos);
                    else startPostponedEnterTransition();
                }
                return true;
            }

        }); //ViewTreeObserver.OnPreDrawListener

    }


}