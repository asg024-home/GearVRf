package com.samsung.smcl.vr.widgets;

import org.gearvrf.GVRHybridObject;
import org.gearvrf.animation.GVRRotationByAxisWithPivotAnimation;
import org.json.JSONException;
import org.json.JSONObject;

public class RotationByAxisWithPivotAnimation extends TransformAnimation {
    /**
     * Convenience method; assumes the pivot is at the origin (0, 0, 0).
     * 
     * @param target
     *            The {@link Widget} to rotate.
     * @param duration
     *            Length of the animation, in seconds.
     * @param angle
     *            Total angle of rotation
     * @param axisX
     *            How much of {@angle} to apply to rotating around the X axis
     *            (0f - 1f).
     * @param axisY
     *            How much of {@angle} to apply to rotating around the Y axis
     *            (0f - 1f).
     * @param axisZ
     *            How much of {@angle} to apply to rotating around the Z axis
     *            (0f - 1f).
     */
    public RotationByAxisWithPivotAnimation(final Widget target,
            float duration, float angle, float axisX, float axisY, float axisZ) {
        this(target, duration, angle, axisX, axisY, axisZ, 0, 0, 0);
    }

    public RotationByAxisWithPivotAnimation(final Widget target,
            float duration, float angle, float axisX, float axisY, float axisZ,
            float pivotX, float pivotY, float pivotZ) {
        super(target);
        mAngle = angle;
        mAxisX = axisX;
        mAxisY = axisY;
        mAxisZ = axisZ;
        mPivotX = pivotX;
        mPivotY = pivotY;
        mPivotZ = pivotZ;
        mAdapter = new Adapter(target, duration, angle, axisX, axisY, axisZ,
                pivotX, pivotY, pivotZ);
    }

    public RotationByAxisWithPivotAnimation(final Widget target,
            final JSONObject params) throws JSONException {
        this(target, (float) params.getDouble("duration"), //
                (float) params.getDouble("angle"), //
                (float) params.getDouble("axis_x"), //
                (float) params.getDouble("axis_y"), //
                (float) params.getDouble("axis_z"), //
                (float) params.getDouble("pivot_x"), //
                (float) params.getDouble("pivot_y"), //
                (float) params.getDouble("pivot_z"));
    }

    public float getAngle() {
        return mAngle;
    }

    public float getAxisX() {
        return mAxisX;
    }

    public float getAxisY() {
        return mAxisY;
    }

    public float getAxisZ() {
        return mAxisZ;
    }

    public float getCurrentAngle() {
        return mCurrentAngle;
    }

    public float getPivotX() {
        return mPivotX;
    }

    public float getPivotY() {
        return mPivotY;
    }

    public float getPivotZ() {
        return mPivotZ;
    }

    @Override
    protected void animate(Widget target, float ratio) {
        mAdapter.superAnimate(target, ratio);
        target.checkTransformChanged();
        mCurrentAngle = ratio * mAngle;
    }

    @Override
    Animation.AnimationAdapter getAnimation() {
        return mAdapter;
    }

    private class Adapter extends GVRRotationByAxisWithPivotAnimation implements
            Animation.AnimationAdapter {
        Adapter(Widget widget, float duration, float angle, float axisX,
                float axisY, float axisZ, float pivotX, float pivotY,
                float pivotZ) {
            super(widget.getSceneObject(), duration, angle, axisX, axisY,
                    axisZ, pivotX, pivotY, pivotZ);
        }

        @Override
        public void animate(GVRHybridObject target, float ratio) {
            doAnimate(ratio);
        }

        void superAnimate(Widget target, float ratio) {
            super.animate(target.getSceneObject(), ratio);
        }
    }

    private final Adapter mAdapter;
    private final float mAngle;
    private final float mAxisX;
    private final float mAxisY;
    private final float mAxisZ;
    private final float mPivotX;
    private final float mPivotY;
    private final float mPivotZ;
    private float mCurrentAngle;
}
