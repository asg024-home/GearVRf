package com.samsung.smcl.vr.widgets;

import android.database.DataSetObserver;

import com.samsung.smcl.utility.Log;
import com.samsung.smcl.utility.Utility;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRSceneObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private int mItemsPerPage = -1;

    /**
     * Keep tracking the item layouts in the page list. If the page list is empty at the moment
     * {@link MultiPageWidget#applyLayout is called, the item layout is stored in the list and it
     * is applied to the page as soon as the page is added to the list.
     */
    private final Set<Layout> mItemLayouts = new HashSet<>();

    /**
     * Construct a new {@code MultiPageWidget} instance
     * @param context
     * @param pageAdapter  {@link Adapter} for the  pages. {@link Adapter#getView} should provide
     *                  {@link ListWidget}
     * @param sceneObject
     */
    public MultiPageWidget(GVRContext context, final Adapter pageAdapter, GVRSceneObject sceneObject,
            int maxVisiblePageCount) {
        super(context, sceneObject, pageAdapter);
        setMaxVisiblePageCount(maxVisiblePageCount);
    }


    /**
     * Construct a new {@code MultiPageWidget} instance
     * @param context
     * @param pageAdapter  {@link Adapter} for the pages. {@link Adapter#getView} should provide
     *                  {@link ListWidget}
     * @param width
     * @param height
     */
    public MultiPageWidget(GVRContext context, final Adapter pageAdapter,
                           float width, float height, int maxVisiblePageCount) {
        super(context, pageAdapter, width, height);
        setMaxVisiblePageCount(maxVisiblePageCount);
    }

    /**
     * Construct a new {@code MultiPageWidget} instance
     * @param context
     * @param pageAdapter  {@link Adapter} for the  pages. {@link Adapter#getView} should provide
     *                  {@link ListWidget}
     * @param sceneObject
     */
    public MultiPageWidget(final GVRContext context, final Adapter pageAdapter,
                   final GVRSceneObject sceneObject, NodeEntry attributes, int maxVisiblePageCount)
            throws InstantiationException {
        super(context, sceneObject, attributes, pageAdapter);
        setMaxVisiblePageCount(maxVisiblePageCount);
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

    private Set<OnItemTouchListener> mOnItemTouchListeners = new LinkedHashSet<>();

    @Override
    public boolean addOnItemTouchListener(OnItemTouchListener listener) {
        boolean added = mOnItemTouchListeners.add(listener);
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "addOnItemTouchListener listener %s added = %b", listener, added);
        return added;
    }

    @Override
    public boolean removeOnItemTouchListener(OnItemTouchListener listener) {
        boolean removed = mOnItemTouchListeners.remove(listener);
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "removeOnItemTouchListener listener %s removed = %b", listener, removed);
        return removed;
    }

    private Set<OnItemFocusListener> mOnItemFocusListeners = new LinkedHashSet<>();
    @Override
    public boolean addOnItemFocusListener(OnItemFocusListener listener) {
        return mOnItemFocusListeners.add(listener);
    }

    @Override
    public boolean removeOnItemFocusListener(OnItemFocusListener listener) {
        return mOnItemFocusListeners.remove(listener);
    }

    @Override
    public void clear() {
        List<Widget> views = getAllViews();
        for (Widget view: views) {
            ListWidget page = ((ListWidget)view);
            if (page != null) {
                ListOnChangedListener listener = mPagesListOnChangedListeners.get(page);
                page.removeListOnChangedListener(listener);
                page.clear();
            }
        }
        mPagesListOnChangedListeners.clear();
        super.clear();
    }

    @Override
    protected void onRecycle(Widget view, int dataIndex) {
        if (view != null) {
            final ListWidget page = (ListWidget) view;
            setAdapter(page, dataIndex, null);

            page.recycleChildren();
            ListOnChangedListener listener = mPagesListOnChangedListeners.get(page);
            if (listener != null) {
                page.removeListOnChangedListener(listener);
                mPagesListOnChangedListeners.remove(page);
            }
            page.clearSelectedItemsList();
        }

        super.onRecycle(view, dataIndex);
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

        void setBounds(int start, int length) {
            int end = start + length - 1;
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setBounds  old [%d, %d] new [%d, %d]",
                    mStart, mEnd, start, end);
            mStart = start;
            mEnd = end;
        }

        void setStart(int start) {
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setStart  old [%d, %d] new [%d, %d]",
                    mStart, mEnd, start, mEnd);
            mStart = start;
        }

        void setLength(int length) {
            int end = mStart + length - 1;
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setLength  old [%d, %d] new [%d, %d]",
                    mStart, mEnd, mStart, end);
            mEnd = end;
        }

        private int getGlobalPosition(int position) {
            return position < 0 || position >= getCount() ?
                    -1 : mStart + position;
        }

        private int getLocalPosition(int position) {
            return containsGlobalPosition(position) ? position - mStart : -1;
        }

        private boolean containsGlobalPosition(int dataIndex) {
            return dataIndex >= mStart && dataIndex <= mEnd;
        }

        @Override
        public int getCount() {
            return mEnd - mStart + 1;
        }

        @Override
        public Object getItem(int position) {
            return mAdapter.getItem(getGlobalPosition(position));
        }

        @Override
        public long getItemId(int position) {
            return mAdapter.getItemId(getGlobalPosition(position));
        }

        @Override
        public int getItemViewType(int position) {
            return mAdapter.getItemViewType(getGlobalPosition(position));
        }

        @Override
        public Widget getView(int position, Widget convertView, GroupWidget parent) {
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "getView pos = %d, realPos = %d start = %d, end = %d",
                    position, getGlobalPosition(position), mStart, mEnd);
            return mAdapter.getView(getGlobalPosition(position), convertView, parent);
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
            return mAdapter.getViewWidthGuess(getGlobalPosition(position));
        }

        @Override
        public float getViewHeightGuess(int position) {
            return mAdapter.getViewHeightGuess(getGlobalPosition(position));
        }

        @Override
        public float getViewDepthGuess(int position) {
            return mAdapter.getViewDepthGuess(getGlobalPosition(position));
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

        @Override
        public void unregisterAllDataSetObservers() {
            mAdapter.unregisterAllDataSetObservers();
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

    private OnItemTouchListener mInternalOnItemTouchListener = new OnItemTouchListener() {
        public boolean onTouch(ListWidget list, int dataIndex) {

            if (isSelectOnTouchEnabled()) {
                Log.d(Log.SUBSYSTEM.LAYOUT, TAG,
                        "mSelectOnTouchListener[%s] for index = %d", list.getName(), dataIndex);
                toggleItem(list, dataIndex);
            }

            Set<OnItemTouchListener> copyList = new HashSet<>(mOnItemTouchListeners);
            SelectingAdapter adapter = ((SelectingAdapter) list.mAdapter);
            int globalPosition = adapter.getGlobalPosition(dataIndex);

            for (OnItemTouchListener listener: copyList) {
                listener.onTouch(MultiPageWidget.this, globalPosition);
            }
            return true;
        };
    };

    private OnItemFocusListener mInternalOnItemFocusListener = new OnItemFocusListener() {
        public void onFocus(ListWidget list, boolean focused, int dataIndex) {
            Set<OnItemFocusListener> copyList = new HashSet<>(mOnItemFocusListeners);

            SelectingAdapter adapter = ((SelectingAdapter) list.mAdapter);
            int globalPosition = adapter.getGlobalPosition(dataIndex);
            for (OnItemFocusListener listener: copyList) {
                listener.onFocus(MultiPageWidget.this, focused, globalPosition);
            }

        }
        public void onLongFocus(ListWidget list, int dataIndex) {
            Set<OnItemFocusListener> copyList = new HashSet<>(mOnItemFocusListeners);

            SelectingAdapter adapter = ((SelectingAdapter) list.mAdapter);
            int globalPosition = adapter.getGlobalPosition(dataIndex);
            for (OnItemFocusListener listener: copyList) {
                listener.onLongFocus(MultiPageWidget.this, globalPosition);
            }

        }
    };

    protected void onItemChanged(final Adapter adapter) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                if (adapter != mItemAdapter) {
                    if (mItemAdapter != null) {
                        try {
                            mItemAdapter.unregisterDataSetObserver(mInternalItemsObserver);
                        } catch (IllegalStateException e) {
                            Log.w(TAG, "onItemChanged(%s): internal observer not registered on adapter!", getName());
                        }
                        clear();
                    }
                    mItemAdapter = adapter;
                    if (mItemAdapter != null) {
                        mItemAdapter.registerDataSetObserver(mInternalItemsObserver);
                    }

                    for (DataSetObserver observer : mItemObservers) {
                        observer.onInvalidated();
                    }
                }
                MultiPageWidget.this.onChanged();
            }
        });
    }


    protected void setAdapter(ListWidget page, final int pageIndex, final Adapter adapter) {
        if (adapter == null) {
            page.setAdapter(null);
        } else if (page.mAdapter == null ||
                adapter != (((SelectingAdapter)page.mAdapter).mAdapter)) {

            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setAdapter page = %s adapter = %s",
                    page, adapter);
            SelectingAdapter pageAdapter = new SelectingAdapter(adapter);
            if (mItemsPerPage >= 0) {
                int start = pageIndex * mItemsPerPage;
                int length = Math.min(mItemsPerPage, adapter.getCount() - start);
                pageAdapter.setBounds(start, length);
                page.updateSelectedItemsList(getLocalSelectedItemsList(pageAdapter), true);
            }
            page.setAdapter(pageAdapter);
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
            recalculateViewPort(mAdapter);
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
        return super.applyLayout(listLayout);
    }

    protected void recalculateViewPort(final Adapter adapter) {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "recalculateViewPort mMaxVisiblePageCount = %d mAdapter =%s " +
                "mAdapter.hasUniformViewSize() = %b",
                mMaxVisiblePageCount, adapter, (adapter != null ? adapter.hasUniformViewSize() : false));

        if (mMaxVisiblePageCount < Integer.MAX_VALUE && adapter != null && adapter.hasUniformViewSize()) {
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
        return super.removeLayout(listLayout);
    }

    @Override
    protected void onChanged(final Adapter adapter) {
        mItemsPerPage = -1;
        recalculateViewPort(adapter);
        super.onChanged(adapter);
    }

    private Map<ListWidget, ListOnChangedListener> mPagesListOnChangedListeners = new HashMap<>();

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
                        list, mPageIndex, numOfMeasuredViews);
                adapter.setLength(numOfMeasuredViews);
                selectItems(list, getLocalSelectedItemsList(adapter), true);

                if (adapter.hasUniformViewSize() && mAdapter.hasUniformViewSize()) {
                    mItemsPerPage = numOfMeasuredViews;
                    Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "onChangedFinished mItemsPerPage = %d", mItemsPerPage);
                }
            } else {
                Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "onChangedStart list = %s , index = %d adapter is null ",
                        list, mPageIndex);
            }
        }
    }

    @Override
    protected boolean setupView(Widget view, final int dataIndex) {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "getViewFromAdapter index = %d", dataIndex);

        super.setupView(view, dataIndex);
        ListWidget page = (ListWidget)view;
        for (Layout layout: mItemLayouts) {
            if (!page.hasLayout(layout)) {
                page.applyLayout(layout.clone());
            }
        }
        if (mItemsPerPage == -1) {
            ListOnChangedListener listener = new PageOnChangedListener(dataIndex);
            page.addListOnChangedListener(listener);
            mPagesListOnChangedListeners.put(page, listener);
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "getViewFromAdapter index = %d registerOnChangeListener",
                    dataIndex);
        }

        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "setupView: page %s, mSelectItemOnTouchEnabled = %b",
                page.getName(), mSelectItemOnTouchEnabled);
        setAdapter(page, dataIndex, mItemAdapter);
        return false;
    }

    protected void doOnItemAdded(Widget item) {
        super.doOnItemAdded(item);

        ListWidget page = (ListWidget)item;

        page.addOnItemFocusListener(mInternalOnItemFocusListener);
        page.addOnItemTouchListener(mInternalOnItemTouchListener);
        page.enableMultiSelection(isMultiSelectionEnabled());
    }

    protected void doOnItemRemoved(Widget item) {
        ListWidget page = (ListWidget)item;
        page.removeOnItemFocusListener(mInternalOnItemFocusListener);
        page.addOnItemTouchListener(mInternalOnItemTouchListener);
        super.doOnItemRemoved(item);
    }

    private void selectItems(ListWidget page, List<Integer> dataIndexes, boolean select) {
        // select items in the page
        for (int dataIndex: dataIndexes) {
            page.selectItem(dataIndex, select);
        }
    }

    private List<Integer> getLocalSelectedItemsList(SelectingAdapter adapter) {
        List<Integer> list = new ArrayList<>();
        for (int index: mSelectedItemsList) {
            int localIndex = adapter.getLocalPosition(index);
            if (localIndex >= 0) {
                list.add(localIndex);
            }
        }
        return list;
    }

    // use the separate flag for the item selection because the pages are not selectable
    private boolean mSelectItemOnTouchEnabled;

    @Override
    public void enableSelectOnTouch(boolean enable) {
        mSelectItemOnTouchEnabled = enable;
    }

    @Override
    public void enableMultiSelection(boolean enable) {
        if (isMultiSelectionEnabled() != enable) {
            super.enableMultiSelection(enable);
            for (Widget view : getAllViews()) {
                ListWidget page = (ListWidget) view;
                page.enableMultiSelection(enable);
            }
        }
    }

    @Override
    public boolean isSelectOnTouchEnabled() {
        return mSelectItemOnTouchEnabled;
    }

    @Override
    protected boolean clearSelection(boolean requestLayout) {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "clearSelection [%d]", mSelectedItemsList.size());

        boolean updateLayout = false;
        List<Widget>  views = getAllViews();

        for (Widget view: views) {
            ListWidget page = (ListWidget)view;
            updateLayout = page.clearSelection(requestLayout) || updateLayout;
        }
        if (updateLayout && requestLayout) {
            requestLayout();
        }
        clearSelectedItemsList();
        return updateLayout;
    }

    /**
     * Select or deselect an item at position {@code pos}.
     *
     * @param dataIndex
     *            item position in the adapter
     * @param select
     *            operation to perform select or deselect.
     * @return {@code true} if the requested operation is successful,
     *         {@code false} otherwise.
     */
    public boolean selectItem(int dataIndex, boolean select) {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "selectItem [%d] select [%b]", dataIndex, select);
        if (dataIndex < 0 || dataIndex >= mItemAdapter.getCount()) {
            throw new IndexOutOfBoundsException("Selection index [" + dataIndex + "] is out of bounds!");
        }

        boolean done = updateSelectedItemsList(dataIndex, select);

        if (done) {
            List<Widget> views = new ArrayList<>();

            if (mItemsPerPage >= 0) {
                int pageIndex = dataIndex / mItemsPerPage;
                Widget view = getListView(pageIndex);
                if (view != null) {
                    views.add(view);
                }
            } else {
                views = getAllViews();
            }

            for (Widget view: views) {
                if (selectItem(((ListWidget) view), dataIndex, select)) {
                    requestLayout();
                    break;
                }
            }
        }

        return done;
    }

    protected Widget getListView(int dataIndex) {
        return  super.getView(dataIndex);
    }

    @Override
    public Widget getView(int dataIndex) {
        Widget itemView = null;
        for (Widget view: getAllViews()) {
            ListWidget page = (ListWidget)view;
            SelectingAdapter adapter = ((SelectingAdapter) page.mAdapter);
            int localPosition = adapter.getLocalPosition(dataIndex);
            if (localPosition != -1) {
                itemView = page.getView(localPosition);
                break;
            }
        }
        return itemView;
    }

    private boolean toggleItem(ListWidget page, int dataIndex) {
        SelectingAdapter adapter = ((SelectingAdapter) page.mAdapter);
        int globalPosition = adapter.getGlobalPosition(dataIndex);
        if (globalPosition < 0) {
            throw new IndexOutOfBoundsException("Selection index [" + dataIndex + "] is out of bounds!");
        }

        boolean select = !isSelected(globalPosition);

        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "selectItem dataIndex [%d] global [%d]  select [%b]",
                dataIndex, globalPosition, select);

        return updateSelectedItemsList(globalPosition, select) ?
                page.selectItem(dataIndex, select) : false;
    }

    private boolean selectItem(ListWidget page, int dataIndex, boolean select) {
        SelectingAdapter adapter = ((SelectingAdapter) page.mAdapter);
        int localPosition = adapter.getLocalPosition(dataIndex);
        return (localPosition >= 0)  ? page.selectItem(localPosition, select) : false;
    }

    /**
     * Check whether the item at position {@code pos} is selected.
     *
     * @param dataIndex
     *            item position in adapter
     * @return {@code true} if the item is selected, {@code false} otherwise.
     */
    public boolean isSelected(int dataIndex) {
        return mItemAdapter != null &&
                dataIndex < mItemAdapter.getCount() &&
                mSelectedItemsList.contains(dataIndex);
    }

    // default ScrollableList implementation should work with the items but not pages
    // getPageScrollable should be used to operate with pages

    @Override
    public int getScrollingItemsCount() {
        return mItemAdapter == null ? 0 : mItemAdapter.getCount();
    }

    @Override
    public float getViewPortWidth() {
        return mAdapter != null && mAdapter.hasUniformViewSize() ?
                mAdapter.getViewWidthGuess(0) : MultiPageWidget.super.getViewPortWidth();
    }

    @Override
    public float getViewPortHeight() {
        return mAdapter != null && mAdapter.hasUniformViewSize() ?
                mAdapter.getViewHeightGuess(0) : MultiPageWidget.super.getViewPortHeight();
    }

    @Override
    public float getViewPortDepth() {
        return mAdapter != null && mAdapter.hasUniformViewSize() ?
                mAdapter.getViewDepthGuess(0) : MultiPageWidget.super.getViewPortDepth();
    }

    @Override
    public boolean scrollToPosition(int pos) {
        // needs to be reimplemented to work with items not pages
        return  MultiPageWidget.super.scrollToPosition(pos);
    }

    @Override
    public boolean scrollByOffset(float xOffset, float yOffset, float zOffset) {
        // needs to be reimplemented to work with items not pages
        return  MultiPageWidget.super.scrollByOffset(xOffset, yOffset, zOffset);
    }

    // provides the scrollableList implementation for page scrolling

    public LayoutScroller.ScrollableList getPageScrollable() {
        return new LayoutScroller.ScrollableList() {

            @Override
            public int getScrollingItemsCount() {
                return  MultiPageWidget.super.getScrollingItemsCount();
            }

            @Override
            public float getViewPortWidth() {
                return  MultiPageWidget.super.getViewPortWidth();
            }

            @Override
            public float getViewPortHeight() {
                return  MultiPageWidget.super.getViewPortHeight();
            }

            @Override
            public float getViewPortDepth() {
                return  MultiPageWidget.super.getViewPortDepth();
            }

            @Override
            public boolean scrollToPosition(int pos) {
                return  MultiPageWidget.super.scrollToPosition(pos);
            }

            @Override
            public boolean scrollByOffset(float xOffset, float yOffset, float zOffset) {
                return  MultiPageWidget.super.scrollByOffset(xOffset, yOffset, zOffset);
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {
                MultiPageWidget.super.registerDataSetObserver(observer);
            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {
                MultiPageWidget.super.unregisterDataSetObserver(observer);
            }
        };
    }
}
