package com.samsung.smcl.vr.widgets;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRSceneObject;

import com.samsung.smcl.utility.Log;

public class WrapAroundList extends RingList {
    private static final String TAG = WrapAroundList.class.getSimpleName();

    /**
     * Construct a new {@code WrapAroundList} instance with a
     * {@link #setRho(double) radius} of zero.
     *
     * @param gvrContext
     *            The active {@link GVRContext}.
     */
    public WrapAroundList(final GVRContext gvrContext, GVRSceneObject sceneObj) {
        super(gvrContext, sceneObj);
    }

    /**
     * Construct a new {@code WrapAroundList} instance with the specified
     * radius.
     *
     * @param gvrContext
     *            The active {@link GVRContext}.
     * @param rho
     *            The radius of the {@code WrapAroundList}.
     */
    public WrapAroundList(final GVRContext gvrContext, GVRSceneObject sceneObj,
            final double rho) {
        super(gvrContext, sceneObj, rho);
    }

    /**
     * Construct a new {@code WrapAroundList} instance with the specified
     * radius.
     *
     * @param gvrContext
     *            The active {@link GVRContext}.
     * @param rho
     *            The radius of the {@code WrapAroundList}.
     */

    public WrapAroundList(final GVRContext gvrContext, float width,
            float height, final double rho) {
        super(gvrContext, width, height, rho);
    }

    /**
     * Construct a new {@code WrapAroundList} instance with the specified
     * radius, page number, max number of items, incrementation of items per
     * page.
     *
     * @param gvrContext
     *            The active {@link GVRContext}.
     * @param rho
     *            The radius of the {@code WrapAroundList}.
     * @param pageNumber
     *            The page number {@code WrapAroundList}.
     * @param maxItemsDisplayed
     *            The max number of items {@code WrapAroundList}.
     * @param itemIncrementPerPage
     *            The increment of items per page of the {@code WrapAroundList}.
     */

    public WrapAroundList(final GVRContext gvrContext, float width,
            float height, final double rho, int pageNumber,
            int maxItemsDisplayed, int maxItemsPerPage, float scaleOnPage,
            float scaleOutsidePage) {
        super(gvrContext, width, height, rho, pageNumber, maxItemsDisplayed,
                maxItemsPerPage);
        mItemScaleOnPage = scaleOnPage;
        mItemScaleOutsidePage = scaleOutsidePage;
        mShiftFactor = (maxItemsPerPage - 1) / 2f;
    }

    private float mShiftFactor = 1.0f;
    private float mItemScaleOnPage = 1.0f;
    private float mItemScaleOutsidePage = 1.0f;

    private void wrapAroundCenterLayout(final float[] angularWidths,
            final int numItems) {
        int pos = 0;
        int maxItemsPerPage = getMaxNumPerPage();
        int itemIndex = getLayoutStart();
        float itemPadding = getItemPadding();

        /*
         * If we have to have an empty spot on the page, it will be on the last
         * column, to insure this we have to pad this page with previously shown
         * items
         */
        if (numItems >= maxItemsPerPage
                && numItems - itemIndex < maxItemsPerPage) {
            itemIndex = numItems - maxItemsPerPage;
        }
        int totalNumPerPage = getNumItemsToDisplay(numItems);
        int numItemsFirstHalf = totalNumPerPage / 2;
        if (maxItemsPerPage > 0) {
            // if we have less than maxItemsPerPage,
            // it will just show all the items
            if (totalNumPerPage <= maxItemsPerPage) {
                numItemsFirstHalf = totalNumPerPage;
            }
            // if we have more than maxItemsPerPage,
            // we will have more items on the first half
            else {
                numItemsFirstHalf += maxItemsPerPage - 1;
            }
        }
        float startPhi = angularWidths[0] * mShiftFactor;
        float phi = angularWidths[0] * mShiftFactor;

        // layout items on right half space
        for (int i = 0; i < numItemsFirstHalf; i++) {
            int appListIndex = itemIndex + i;

            // items wrap around when we get to the last item
            if (appListIndex >= numItems) {
                appListIndex = appListIndex % numItems;
            }
            ListItemHostWidget item = layoutItem(pos, appListIndex, phi);
            if (i < maxItemsPerPage) {
                item.setScale(mItemScaleOnPage, mItemScaleOnPage, 1.f);
            } else {
                item.setScale(mItemScaleOutsidePage, mItemScaleOutsidePage, 1.f);
            }
            phi -= angularWidths[pos] + itemPadding;
            pos++;
        }

        phi = startPhi + angularWidths[0];

        // layout items on the left half space
        int numItemsSecondHalf = totalNumPerPage - numItemsFirstHalf;
        int index = numItems - (numItemsSecondHalf - itemIndex);

        for (int j = index + numItemsSecondHalf - 1; j >= index; j--) {
            int appListIndex = j;

            // items wrap around when we get to the last item
            if (j >= numItems) {
                appListIndex = j % numItems;
            }
            ListItemHostWidget item = layoutItem(pos, appListIndex, phi);
            item.setScale(mItemScaleOutsidePage, mItemScaleOutsidePage, 1.f);

            phi += angularWidths[pos] - itemPadding;
            pos++;
        }
    }

    @Override
    protected void onLayout() {
        boolean proportionalItemPadding = getProportionalItemPadding();
        final int numItems = getAdapterCount();

        if (numItems == 0) {
            Log.d(TAG, "layout(%s): no items to layout!", getName());
        } else {
            Log.d(TAG, "layout(%s): laying out %d items", getName(), numItems);
            try {
                final float[] angularWidths = calculateUniformAngularWidth(proportionalItemPadding);
                wrapAroundCenterLayout(angularWidths, numItems);
            } catch (Throwable t) {
                Log.e(TAG, t, "onLayout(%s): exception: %s", getName(),
                      t.getMessage());
            }
        }
        for (Widget child : getChildren()) {
            child.layout();
        }
        requestLayout();

    }

    protected ListItemHostWidget layoutItem(int pos, int appListIndex,
            final float phi) {
        ListItemHostWidget item = getRecycleableView(pos, appListIndex);

        float rho = (float) getRho();
        Log.d(TAG, "layoutItem(%s): phi [%f] (%s)", getName(), phi,
              item.guestWidget.getName());
        item.setRotation(1, 0, 0, 0);
        item.setPosition(0, 0, -rho);
        item.rotateByAxisWithPivot(phi, 0, 1, 0, 0, 0, 0);
        item.setPhi(phi);

        return item;
    }

}
