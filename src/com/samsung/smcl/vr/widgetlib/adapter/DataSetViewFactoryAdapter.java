package com.samsung.smcl.vr.widgetlib.adapter;

import com.samsung.smcl.vr.widgetlib.widget.GroupWidget;
import com.samsung.smcl.vr.widgetlib.widget.Widget;

public class DataSetViewFactoryAdapter<T extends Object> extends DataSetAdapter<T> {
    public DataSetViewFactoryAdapter(DataSet dataSet, AdapterViewFactory viewFactory) {
        super(dataSet);
        mViewFactory = viewFactory;
    }

    @Override
    public Widget getView(int position, Widget convertView, GroupWidget parent) {
        return mViewFactory.getView(this, position, convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return mViewFactory.getItemViewType(this, position);
    }

    @Override
    public int getViewTypeCount() {
        return mViewFactory.getViewTypeCount();
    }

    private final AdapterViewFactory mViewFactory;
}
