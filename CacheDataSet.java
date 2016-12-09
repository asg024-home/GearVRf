package com.samsung.smcl.vr.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.SparseArray;

import com.samsung.smcl.utility.Log;

interface CacheDataSet {
    enum InvalidateOp {
        ALL,
        POSITION,
        OFFSET,
        SIZE,
        PADDING
    };

    int count();
    void invalidate();
    void invalidate(InvalidateOp op);
    void copyTo(CacheDataSet to);
    float getTotalSize();
    void dump();
    float getTotalSizeWithPadding();
    void shiftBy(final float offset);
    float uniformSize();
    float uniformPadding(final float uniformPadding);

    float addData(final int id, final int pos,
            final float size, final float startPadding, final float endPadding);
    boolean contains(final int id);
    void removeData(final int id);
    float getDataOffset(final int id);
    float getSizeWithPadding(final int id);

    float getStartDataOffset(final int id);
    float getEndDataOffset(final int id);
    float setDataOffsetBefore(final int id, float endDataOffset, final float sign);
    float setDataOffsetAfter(final int id, float startDataOffset, final float sign);
    int getId(final int pos);
    int getPos(final int id);
    int searchPos(final int dataIndex);
}

class CacheData {
    protected float mSize;
    protected float mOffset;
    protected float mStartPadding;
    protected float mEndPadding;
    protected int mId;

    CacheData(final int id) {
        mId = id;
    }

    CacheData(final CacheData data) {
        mId = data.mId;
        mSize = data.mSize;
        mOffset = data.mOffset;
        mStartPadding = data.mStartPadding;
        mEndPadding = data.mEndPadding;
    }

    void setSize(final float size) {
        mSize = size;
    }

    float getSize() {
        return mSize;
    }

    void setOffset(final float offset) {
        mOffset = offset;
    }

    float getOffset() {
        return mOffset;
    }

    void setPadding(final float start, final float end) {
        mStartPadding = start;
        mEndPadding = end;
    }

    float getStartPadding() {
        return mStartPadding;
    }

    float getEndPadding() {
        return mEndPadding;
    }

    private static final String pattern = "id [%d] size [%f] offset [%f] startPadding [%f] endPadding [%f]";

    /**
     * Return the string representation of the LinearLayout
     */
    public String toString() {
        return super.toString() + String.format(pattern, mId, mSize, mOffset, mStartPadding, mEndPadding);
    }
}

class LinearCacheDataSet implements CacheDataSet {
    private static final String TAG = "CacheDataSet";
    protected float mTotalSize;
    protected float mTotalPadding;

    SparseArray<CacheData> mCacheDataSet = new SparseArray<CacheData>();
    List<Integer> mIdsSet = new ArrayList<Integer>();

    @Override
    public void copyTo(CacheDataSet to) {
        if (to != null && to instanceof LinearCacheDataSet) {
            LinearCacheDataSet copy = (LinearCacheDataSet)to;
            copy.mTotalPadding = mTotalPadding;
            copy.mTotalSize = mTotalSize;

            for (int pos = 0; pos < count(); ++pos) {
                copy.mCacheDataSet.put(mCacheDataSet.keyAt(pos),
                          new CacheData(mCacheDataSet.valueAt(pos)));
                copy.mIdsSet.add(pos, mIdsSet.get(pos));
            }
            if (Log.isEnabled(Log.SUBSYSTEM.LAYOUT)) {
                to.dump();
            }

        } else {
            Log.w(TAG, "Cannot copy the data set to %s", to);
        }
    }

    public void dump() {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "\n==== DUMP CACHE start ======\nCache size = %d " +
                "totalSize = %f totalPadding = %f",
                count(), mTotalSize, mTotalPadding);

        for (int pos = 0; pos < count(); ++pos) {
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "data[%d, %d]: %s" , mIdsSet.get(pos), pos,
                  mCacheDataSet.valueAt(pos).toString());
        }

        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "\n==== DUMP CACHE end ======\n");
    }

    @Override
    public boolean contains(final int id) {
        return mCacheDataSet.indexOfKey(id) >= 0;
    }

    @Override
    public float addData(final int id, final int pos,
            final float size, final float startPadding, final float endPadding) {
        CacheData data = new CacheData(id);

        data.setSize(size);
        data.setPadding(startPadding, endPadding);

        mCacheDataSet.append(id, data);

        mTotalSize += data.getSize();

        int actualPos = pos;
        if (actualPos < 0 ) {
            actualPos = 0;
        } else if (actualPos > count()) {
            actualPos = count();
        }
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "addData id = %d pos = %d", id, actualPos);
        mIdsSet.add(actualPos, id);

        // update total padding
        float paddingSpace = updateTotalPadding(actualPos, data, true);

        return paddingSpace + size;
    }

    @Override
    public int getId(final int pos) {
        return pos < 0 || pos >= mIdsSet.size() ? -1 : mIdsSet.get(pos);
    }

    @Override
    public int getPos(final int id) {
        int pos = 0;
        for (int nextId: mIdsSet) {
            if (nextId == id) {
                break;
            }
            pos++;
        }
        return pos == mIdsSet.size() ? -1 : pos;
    }

    public int searchPos(final int dataIndex) {
        return Collections.binarySearch(mIdsSet, dataIndex);
    }


    @Override
    public float getDataOffset(final int id) {
        float offset = Float.NaN;
        CacheData data =  mCacheDataSet.get(id);
        if (data != null) {
            offset = data.getOffset();
        }
        return offset;
    }

    @Override
    public float getSizeWithPadding(final int id) {
        float sizeWithPadding = Float.NaN;
        CacheData data =  mCacheDataSet.get(id);
        if (data != null) {
            sizeWithPadding = data.getSize();

            int pos = getPos(id);
            if (pos > 0) {
                sizeWithPadding += data.getStartPadding();
            }
            if (pos < count() - 1) {
                sizeWithPadding += data.getEndPadding();
            }
        }
        return sizeWithPadding;

    }

    @Override
    public float getStartDataOffset(final int id) {
        float offset = Float.NaN;
        CacheData data =  mCacheDataSet.get(id);
        if (data != null) {
            offset = data.getOffset() - data.getSize() / 2;
            int pos = getPos(id);
            if (pos > 0) {
                offset -= data.getStartPadding();
            }
        }
        return offset;
    }

    @Override
    public float getEndDataOffset(final int id) {
        float offset = Float.NaN;
        CacheData data =  mCacheDataSet.get(id);
        if (data != null) {
            offset = data.getOffset() + data.getSize() / 2;
            int pos = getPos(id);
            if (pos < count() - 1) {
                offset += data.getEndPadding();
            }
        }
        return offset;
    }

    @Override
    public void removeData(final int id) {
        CacheData data =  mCacheDataSet.get(id);
        int pos = getPos(id);
        if (data != null && pos >= 0) {
            mTotalSize -= data.getSize();
            updateTotalPadding(pos, data, false);

            mCacheDataSet.remove(id);
            mIdsSet.remove(pos);
        }
    }

    private float updateTotalPadding(final int pos, final CacheData data, final boolean include) {
        float paddingSpace = 0;
        // update total padding
        boolean first = pos == 0;
        boolean last = pos == count() - 1;
        int sign = include ? 1 : -1;

        if (!first) {
            paddingSpace += data.getStartPadding();
        }

        if (!last) {
            paddingSpace += data.getEndPadding();
        }
        mTotalPadding += sign * paddingSpace;

        // exclude the start padding for new first item and end padding for new last item
        if (count() > 1) {
            if (first) {
                mTotalPadding += sign *  mCacheDataSet.get(mIdsSet.get(pos + 1)).getStartPadding();
            }
            if (last) {
                mTotalPadding += sign * mCacheDataSet.get(mIdsSet.get(pos - 1)).getEndPadding();
            }
        }

        return paddingSpace;
    }

    @Override
    public void invalidate() {
        invalidate(InvalidateOp.ALL);
    }

    @Override
    public void invalidate(InvalidateOp op) {
        switch(op) {
            case ALL:
                mCacheDataSet.clear();
                mIdsSet.clear();
                mTotalSize = 0;
                mTotalPadding = 0;
                break;
            case OFFSET:
                for (int pos = mCacheDataSet.size(); --pos >= 0;) {
                    CacheData data =  mCacheDataSet.valueAt(pos);
                    data.setOffset(Float.NaN);
                }
                break;
            case SIZE:
                mTotalSize = 0;
                mTotalPadding = 0;
                for (int pos = mCacheDataSet.size(); --pos >= 0;) {
                    CacheData data =  mCacheDataSet.valueAt(pos);
                    data.setOffset(Float.NaN);
                    data.setSize(0);
                }
                break;
            case PADDING:
                mTotalSize = 0;
                mTotalPadding = 0;
                for (int pos = mCacheDataSet.size(); --pos >= 0;) {
                    CacheData data =  mCacheDataSet.valueAt(pos);
                    data.setOffset(Float.NaN);
                }
                break;
            case POSITION:
            default:
                break;
        }
    }

    @Override
    public float uniformSize() {
        float maxSize = 0;
        for (int pos = mCacheDataSet.size(); --pos >= 0;) {
            CacheData data =  mCacheDataSet.valueAt(pos);
            maxSize = Math.max(maxSize, data.getSize());
        }

        for (int pos = mCacheDataSet.size(); --pos >= 0;) {
            CacheData data =  mCacheDataSet.valueAt(pos);
            data.setSize(maxSize);
            mCacheDataSet.setValueAt(pos, data);
        }
        mTotalSize = mCacheDataSet.size() * maxSize;
        invalidate(InvalidateOp.OFFSET);

        return maxSize;
    }

    @Override
    public float uniformPadding(final float uniformPadding) {
        for (int pos = mCacheDataSet.size(); --pos >= 0;) {
            CacheData data =  mCacheDataSet.valueAt(pos);
            data.setPadding(uniformPadding / 2, uniformPadding / 2);
            mCacheDataSet.setValueAt(pos, data);
        }
        mTotalPadding = (mCacheDataSet.size() - 1) * uniformPadding;
        invalidate(InvalidateOp.OFFSET);

        return uniformPadding;
    }

    @Override
    public float setDataOffsetAfter(final int id, float startDataOffset, final float sign) {
        CacheData data =  mCacheDataSet.get(id);
        if (data != null) {
            int pos = getPos(id);
            float startPadding = pos > 0 ? data.getStartPadding() : 0;
            float offset = startDataOffset + sign * (startPadding + data.getSize()/ 2);
            data.setOffset(offset);
            mCacheDataSet.put(id, data);

            float endPadding = pos < count() - 1 ? data.getEndPadding() : 0;
            return startDataOffset + sign * (startPadding + data.getSize() + endPadding);
        }
        return Float.NaN;
    }

    @Override
    public float setDataOffsetBefore(final int id, float endDataOffset, final float sign) {
        CacheData data =  mCacheDataSet.get(id);
        if (data != null) {
            int pos = getPos(id);
            float endPadding = pos < count() - 1 ? data.getEndPadding() : 0;
            float offset = endDataOffset - sign * (endPadding + data.getSize()/ 2);
            data.setOffset(offset);
            mCacheDataSet.put(id, data);

            float startPadding = pos > 0 ? data.getStartPadding() : 0;
            return endDataOffset - sign * (startPadding + data.getSize() + endPadding);
        }
        return Float.NaN;
    }

    @Override
    public void shiftBy(final float amount) {
        for (int pos = mCacheDataSet.size(); --pos >= 0;) {
            CacheData data =  mCacheDataSet.valueAt(pos);
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "shiftBy item[%s] newOffset = %f",
                    data, (data.getOffset() + amount));

            data.setOffset(data.getOffset() + amount);
            mCacheDataSet.setValueAt(pos, data);
        }
    }


    @Override
    public float getTotalSizeWithPadding() {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "getTotalSizeWithPadding = %f", (mTotalPadding + mTotalSize));

        return mTotalPadding + mTotalSize;
    }

    @Override
    public float getTotalSize() {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "mTotalSize = %f", mTotalSize);

        return mTotalSize;
    }

    @Override
    public int count() {
        return mCacheDataSet.size();
    }
}
