package com.samsung.smcl.vr.widgets;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRRenderData.GVRRenderingOrder;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.scene_objects.GVRTextViewSceneObject;

import android.graphics.Color;
import android.view.Gravity;

import com.samsung.smcl.utility.Log;
import com.samsung.smcl.vr.gvrf_launcher.R;
import com.samsung.smcl.vr.gvrf_launcher.TouchManager;
import com.samsung.smcl.vr.gvrf_launcher.util.Helpers;

public class NumberPicker extends GVRSceneObject {

    public interface OnValueChangeListener {
        public void onValueChange(int oldValue, int newValue);
    }

    public NumberPicker(GVRContext gvrContext, TouchManager touchManager,
            float width, float height) {
        super(gvrContext, width, height);
        final float selectionHeight = height / 3;
        final float buttonHeight = selectionHeight / 2;
        Log.d(TAG, "NumberPicker: height %.2f, childHeight: %.2f", height,
              selectionHeight);
        mUpButton = makeButton(buttonHeight, R.drawable.up_arrow_circle);
        mDownButton = makeButton(buttonHeight, R.drawable.down_arrow_circle);
        mSelection = new GVRTextViewSceneObject(gvrContext,
                gvrContext.getActivity(), width, selectionHeight,
                Integer.toString(mValue));
        mSelection.setGravity(Gravity.CENTER);
        mSelection.setTextSize(120);
        mSelection.setTextColor(Color.BLACK);
        mSelection.setBackgroundColor(Color.WHITE);

        mUpButton.getTransform().setPositionY(selectionHeight);
        mDownButton.getTransform().setPositionY(-selectionHeight);

        touchManager.makeTouchable(gvrContext, mUpButton,
                                   new TouchManager.OnTouch() {

                                       @Override
                                       public boolean touch(
                                               GVRSceneObject sceneObject) {
                                           setValue(getValue() + 1);
                                           return true;
                                       }
                                   });

        touchManager.makeTouchable(gvrContext, mDownButton,
                                   new TouchManager.OnTouch() {

                                       @Override
                                       public boolean touch(
                                               GVRSceneObject sceneObject) {
                                           setValue(getValue() - 1);
                                           return false;
                                       }
                                   });

        addChildObject(mUpButton);
        addChildObject(mDownButton);
        addChildObject(mSelection);
    }

    public int getMinValue() {
        return mMinValue;
    }

    public void setMinValue(int minValue) {
        mMinValue = minValue;
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(int maxValue) {
        mMaxValue = maxValue;
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value) {
        if (value != mValue && value >= mMinValue && value <= mMaxValue) {
            final int oldValue = mValue;
            mValue = value;
            mSelection.setText(Integer.toString(mValue));
            if (mListener != null) {
                try {
                    mListener.onValueChange(oldValue, mValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public OnValueChangeListener getValueChangeListener() {
        return mListener;
    }

    public void setValueChangeListener(OnValueChangeListener listener) {
        mListener = listener;
    }

    private GVRSceneObject makeButton(final float size, final int bitmapId) {
        final GVRContext gvrContext = getGVRContext();

        GVRSceneObject button = new GVRSceneObject(gvrContext, size, size);
        Helpers.setTextureMaterial(gvrContext, button, bitmapId,
                                   GVRRenderingOrder.TRANSPARENT);

        return button;
    }

    private final GVRSceneObject mUpButton;
    private final GVRSceneObject mDownButton;
    private final GVRTextViewSceneObject mSelection;
    private int mMinValue = 0;
    private int mMaxValue = Integer.MAX_VALUE;
    private int mValue;
    private OnValueChangeListener mListener;

    private static final String TAG = NumberPicker.class.getSimpleName();
}
