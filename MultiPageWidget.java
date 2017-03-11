package com.samsung.smcl.vr.widgets;

import android.database.DataSetObserver;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.samsung.smcl.utility.Log;
import com.samsung.smcl.utility.Utility;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRSceneObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The list of the items combined into the multiple pages.
 *
 * The max number of the pages visible in the list can be set by
 * {@link MultiPageWidget#setMaxVisiblePageCount(int)}
 *
 * The custom page list layout can be applied by {@link MultiPageWidget#applyListLayout}
 *
 * The custom item layout (how the items positioned into the page) can be applied by
 * {@link MultiPageWidget#applyLayout}. If the page list is empty at the moment
 * the new item layout is applied, the item layout is stored in the list and it
 * is applied to the page as soon as new page is added to the list.
 *
 * The page list adapter can be either provided in constructor or set by
 * {@link MultiPageWidget#setListAdapter} The page list adapter has to construct
 * {@link ListWidget} type of the view
 *
 * The item adapter can be set by {@link MultiPageWidget#setAdapter}
 *
 */
public class MultiPageWidget extends ListWidget {

    private static final String TAG = Utility.tag(MultiPageWidget.class);
    /**
     * Adapter associated with the items in the pages
     */
    private Adapter mItemAdapter;

    /**
     * Keep tracking the item layouts in the page list. If the page list is empty at the moment
     * {@link MultiPageWidget#applyLayout is called, the item layout is stored in the list and it
     * is applied to the page as soon as the page is added to the list.
     */
    private final Set<Layout> mItemLayouts = new HashSet<>();

    private DataSetObserver mListDateSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            recalculateViewPort();
        }

        @Override
        public void onInvalidated() {
            recalculateViewPort();
        }
    };

    /**
     * Construct a new {@code MultiPageWidget} instance
     * @param context
     * @param pageAdapter  {@link Adapter} for the  pages. {@link Adapter#getView} should provide
     *                  {@link ListWidget}
     * @param sceneObject
     */
    public MultiPageWidget(GVRContext context, final Adapter pageAdapter, GVRSceneObject sceneObject) {
        super(context, pageAdapter, sceneObject);
        registerListDataSetObserver(mListDateSetObserver);
    }


    /**
     * Construct a new {@code MultiPageWidget} instance
     * @param context
     * @param pageAdapter  {@link Adapter} for the pages. {@link Adapter#getView} should provide
     *                  {@link ListWidget}
     * @param width
     * @param height
     */
    public MultiPageWidget(GVRContext context, final Adapter pageAdapter, float width, float height) {
        super(context, pageAdapter, width, height);
        registerListDataSetObserver(mListDateSetObserver);
    }

    /**
     * Construct a new {@code MultiPageWidget} instance
     * @param context
     * @param pageAdapter  {@link Adapter} for the  pages. {@link Adapter#getView} should provide
     *                  {@link ListWidget}
     * @param sceneObject
     */
    public MultiPageWidget(final GVRContext context, final Adapter pageAdapter,
                        final GVRSceneObject sceneObject, NodeEntry attributes)
            throws InstantiationException {
        super(context, pageAdapter, sceneObject, attributes);
        registerListDataSetObserver(mListDateSetObserver);
    }

    /**
     * Set the {@link Adapter} for the items presented into the pages. The list will
     * immediately attempt to load data from the adapter.
     * {@link Adapter#getView} is
     * guaranteed to be called from the GL thread.
     *
     * @param itemAdapter
     *            An adapter or {@code null} to clear the list.
     */
    @Override
    public void setAdapter(final Adapter itemAdapter) {
        onItemChanged(itemAdapter);
    }

    private Set<DataSetObserver> mItemObservers = new HashSet<>();

    @Override
    public void registerDataSetObserver(final DataSetObserver observer) {
        mItemObservers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(final DataSetObserver observer) {
        mItemObservers.remove(observer);
    }

    public void registerListDataSetObserver(final DataSetObserver observer) {
        super.registerDataSetObserver(observer);
    }

    public void unregisterListDataSetObserver(final DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
    }

    @Override
    public void clear() {
        List<Widget> views = getAllViews();
        for (Widget view: views) {
            ListWidget page = ((ListWidget)view);
            if (page != null) {
                ListOnChangedListener listener = mPagesListOnChangedListeners.get(view);
                page.removeListOnChangedListener(listener);

                page.clear();
            }
        }
        mPagesListOnChangedListeners.clear();
        super.clear();
    }

    @Override
    protected void recycle(int dataIndex) {
        final ListWidget page = (ListWidget)getItem(dataIndex);
        setAdapter(page, null);

        page.recycleChildren();
        ListOnChangedListener listener = mPagesListOnChangedListeners.get(dataIndex);
        if (listener != null) {
            page.removeListOnChangedListener(listener);
            mPagesListOnChangedListeners.remove(dataIndex);
        }

        super.recycle(dataIndex);
    }

    protected static class SelectingAdapter implements Adapter {
        private final static String TAG = Utility.tag(SelectingAdapter.class);

        private final Adapter mAdapter;
        private int mStart, mEnd;

        // [0, adapter.getCount() - 1]
        SelectingAdapter(Adapter adapter) {
            this(adapter, 0, adapter.getCount() - 1);
        }

        // [start, end]
        SelectingAdapter(Adapter adapter,int start, int end) {
            mAdapter = adapter;
            mStart = Math.max(0, start);
            mEnd = Math.min(adapter.getCount() - 1, end);
        }

        void setBounds(int start, int end) {
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setBounds  old [%d, %d] new [%d, %d]",
                    mStart, mEnd, start, end);
            mStart = start;
            mEnd = mStart + end;
        }

        void setStart(int start) {
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setStart  old [%d, %d] new [%d, %d]",
                    mStart, mEnd, start, mEnd);
            mStart = start;
        }

        void setEnd(int end) {
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setEnd  old [%d, %d] new [%d, %d]",
                    mStart, mEnd, mStart, end);
            mEnd = mStart + end;
        }

        private int getRealPosition(int position) {
            return position < 0 || position >= getCount() ?
                    -1 : mStart + position;
        }

        @Override
        public int getCount() {
            return mEnd - mStart + 1;
        }

        @Override
        public Object getItem(int position) {
            return mAdapter.getItem(getRealPosition(position));
        }

        @Override
        public long getItemId(int position) {
            return mAdapter.getItemId(getRealPosition(position));
        }

        @Override
        public int getItemViewType(int position) {
            return mAdapter.getItemViewType(getRealPosition(position));
        }

        @Override
        public Widget getView(int position, Widget convertView, GroupWidget parent) {
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "getView pos = %d, realPos = %d start = %d, end = %d",
                    position, getRealPosition(position), mStart, mEnd);
            return mAdapter.getView(getRealPosition(position), convertView, parent);
        }

        @Override
        public int getViewTypeCount() {
            return mAdapter.getViewTypeCount();
        }

        @Override
        public boolean hasStableIds() {
            return mAdapter.hasStableIds();
        }

        @Override
        public boolean hasUniformViewSize() {
            return mAdapter.hasUniformViewSize();
        }

        @Override
        public float getViewWidthGuess(int position) {
            return mAdapter.getViewWidthGuess(getRealPosition(position));
        }

        @Override
        public float getViewHeightGuess(int position) {
            return mAdapter.getViewHeightGuess(getRealPosition(position));
        }

        @Override
        public float getViewDepthGuess(int position) {
            return mAdapter.getViewDepthGuess(getRealPosition(position));
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mAdapter.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mAdapter.unregisterDataSetObserver(observer);
        }
    }

    protected DataSetObserver mInternalItemsObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            MultiPageWidget.this.onItemChanged(mItemAdapter);
            // make sure it is executed after finishing onChanged()
            runOnGlThread(new Runnable() {
                @Override
                public void run() {

                    for(DataSetObserver observer: mItemObservers) {
                        observer.onChanged();
                    }
                }
            });
        }

        @Override
        public void onInvalidated() {
            MultiPageWidget.this.onItemChanged(mItemAdapter);
            // make sure it is executed after finishing onChanged()
            runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    for(DataSetObserver observer: mItemObservers) {
                        observer.onInvalidated();
                    }
                }
            });
        }
    };

    private void superOnChanged() {
        super.onChanged();
    }

    protected void onItemChanged(final Adapter adapter) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                if (adapter != mItemAdapter) {
                    if (mItemAdapter != null) {
                        mItemAdapter.unregisterDataSetObserver(mInternalItemsObserver);

                        // clear items in pages
                        List<Widget> views = getAllViews();
                        for (Widget view: views) {
                            ((ListWidget)view).clear();
                        }
                    }
                    mItemAdapter = adapter;
                    if (mItemAdapter != null) {
                        mItemAdapter.registerDataSetObserver(mInternalItemsObserver);
                    }

                    for (DataSetObserver observer : mItemObservers) {
                        observer.onInvalidated();
                    }
                }
                MultiPageWidget.this.superOnChanged();
            }
        });
    }


    protected void
    setAdapter(ListWidget page, final Adapter adapter) {
        if (adapter == null) {
            page.setAdapter(null);
        } else if (page.mAdapter == null ||
                adapter != (((SelectingAdapter)page.mAdapter).mAdapter)) {

            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setAdapter page = %s adapter = %s",
                    page, adapter);
            page.setAdapter(new SelectingAdapter(adapter));
        }
    }

    /**
     * Set the {@link Adapter} for the page list. The list will
     * immediately attempt to load data from the adapter.
     * {@link Adapter#getView} is
     * guaranteed to be called from the GL thread.
     * {@link Adapter#getView} should provide {@link ListWidget}
     *
     * @param listAdapter
     *            An adapter or {@code null} to clear the list.
     */
    protected void setListAdapter(final Adapter listAdapter) {
        super.setAdapter(listAdapter);
    }

    private int mMaxVisiblePageCount = Integer.MAX_VALUE;

    /**
     * Set the max number of visible views in the list
     * It will automatically enable viewport flag {@link Layout#mApplyViewPort}
     * The existing viewport set for ListWidget will be overridden  based on the viewCount
     * @param pageCount
     */
    public void setMaxVisiblePageCount(final int pageCount) {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setMaxVisiblePageCount pageCount = %d mLayouts.size = %d",
                pageCount, mLayouts.size());
        if (mMaxVisiblePageCount != pageCount) {
            mMaxVisiblePageCount = pageCount;
            recalculateViewPort();
            requestLayout();
        }
    }

    /**
     * Get the max number of visible views in the list.
     */
    public int getMaxVisiblePageCount() {
        return mMaxVisiblePageCount;
    }

    /**
     * Apply the layout to the each page in the list
     * @param itemLayout item layout in the page
     */
    @Override
    public boolean applyLayout(Layout itemLayout) {
        boolean applied = false;
        if (itemLayout != null && mItemLayouts.add(itemLayout)) {

            // apply the layout to all visible pages
            List<Widget> views = getAllViews();
            for (Widget view: views) {
                view.applyLayout(itemLayout.clone());
            }
            applied = true;
        }
        return applied;
    }

    /**
     * Apply the layout to the page list
     * @param listLayout page list layout
     */
    public boolean applyListLayout(Layout listLayout) {
        boolean ret =  super.applyLayout(listLayout);
        recalculateViewPort();
        return ret;
    }

    protected void recalculateViewPort() {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "recalculateViewPort mMaxVisiblePageCount = %d mAdapter =%s " +
                "mAdapter.hasUniformViewSize() = %b",
                mMaxVisiblePageCount, mAdapter, (mAdapter != null ? mAdapter.hasUniformViewSize() : false));
        if (mMaxVisiblePageCount < Integer.MAX_VALUE && mAdapter != null && mAdapter.hasUniformViewSize()) {
            int[] ids = new int[mMaxVisiblePageCount];
            for (int i = 0; i < mMaxVisiblePageCount; ++i) {
                ids[i] = i;
            }
            float width = 0, height = 0, depth = 0;
            for (Layout listLayout: mLayouts) {
                listLayout.enableViewPort(true);
                width = Math.max(listLayout.calculateWidth(ids), width);
                height = Math.max(listLayout.calculateHeight(ids), height);
                depth = Math.max(listLayout.calculateDepth(ids), depth);
            }
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "recalculateViewPort mMaxVisiblePageCount = %d [%f, %f, %f]",
                    mMaxVisiblePageCount, width, height, depth);

            setViewPortWidth(width);
            setViewPortHeight(height);
            setViewPortDepth(depth);
        }
    }

    /**
     * Remove the item layout {@link Layout} from the chain
     * @param itemLayout {@link Layout} item layout
     * @return true if layout has been removed successfully , false - otherwise
     */
    public boolean removeLayout(final Layout itemLayout) {
        boolean removed = false;
        if (itemLayout != null && mItemLayouts.remove(itemLayout)) {
            // remove the layout from all visible pages
            List<Widget>  views = getAllViews();
            for (Widget view: views) {
                view.removeLayout(itemLayout);
            }
            removed = true;
        }
        return removed;
    }

    /**
     * Remove the layout {@link Layout} from the chain
     * @param listLayout {@link Layout} page list layout
     * @return true if layout has been removed successfully , false - otherwise
     */
    public boolean removeListLayout(final Layout listLayout) {
        boolean ret =  super.removeLayout(listLayout);
        recalculateViewPort();
        return ret;
    }

    private Map<Widget, ListOnChangedListener> mPagesListOnChangedListeners = new HashMap<>();

    class PageOnChangedListener implements ListOnChangedListener {
        private final int mPageIndex;

        PageOnChangedListener(int index) {
            mPageIndex = index;
        }

        @Override
        public void onChangedStart(ListWidget list) {
            if (list.mAdapter != null) {
                SelectingAdapter adapter = ((SelectingAdapter) list.mAdapter);
                int start = 0;
                if (mPageIndex > 0) {
                    ListWidget prevPage = (ListWidget)getItem(mPageIndex - 1);
                    start = prevPage.getAllViews().size();
                }
                Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "onChangedStart list = %s , index = %d start = %d",
                        list, mPageIndex, start);
                adapter.setStart(start);
            } else {
                Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "onChangedStart list = %s , index = %d adapter is null ",
                        list, mPageIndex);
            }
        }

        @Override
        public void onChangedFinished(ListWidget list, int numOfMeasuredViews) {
            if (list.mAdapter != null) {
                SelectingAdapter adapter = ((SelectingAdapter) list.mAdapter);

                Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "onChangedFinished list = %s , index = %d end = %d",
                        list, mPageIndex, (numOfMeasuredViews - 1));
                adapter.setEnd(numOfMeasuredViews - 1);
            } else {
                Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "onChangedStart list = %s , index = %d adapter is null ",
                        list, mPageIndex);
            }

        }
    }

    @Override
    protected void setupView(Widget view, final int dataIndex) {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "getViewFromAdapter index = %d", dataIndex);

        super.setupView(view, dataIndex);
        ListWidget page = (ListWidget)view;
        for (Layout layout: mItemLayouts) {
            if (!page.hasLayout(layout)) {
                page.applyLayout(layout.clone());
            }
        }
        ListOnChangedListener listener = new PageOnChangedListener(dataIndex);
        page.addListOnChangedListener(listener);
        mPagesListOnChangedListeners.put(page, listener);

        setAdapter(page, mItemAdapter);
    }
}
