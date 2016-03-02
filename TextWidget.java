package com.samsung.smcl.vr.widgets;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.scene_objects.GVRTextViewSceneObject;
import org.gearvrf.scene_objects.GVRTextViewSceneObject.IntervalFrequency;

import android.graphics.drawable.Drawable;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class TextWidget extends Widget {

    /**
     * Construct a wrapper for an existing {@link GVRSceneObject}.
     *
     * @param context
     *            The current {@link GVRContext}.
     * @param sceneObject
     *            The {@link GVRSceneObject} to wrap.
     */
    public TextWidget(final GVRContext context, final GVRSceneObject sceneObject) {
        super(context, sceneObject);
        // TODO: Implement property fetching
        mTextViewSceneObject = (GVRTextViewSceneObject) getSceneObject();
    }

    /**
     * A constructor for wrapping existing {@link GVRSceneLayout} instances.
     * Deriving classes should override and do whatever processing is
     * appropriate.
     *
     * @param context
     *            The current {@link GVRContext}
     * @param sceneObject
     *            The {@link GVRSceneObject} to wrap.
     * @param attributes
     *            A set of class-specific attributes.
     */
    @SuppressWarnings("deprecation")
    public TextWidget(GVRContext context, GVRSceneObject sceneObject,
                      NodeEntry attributes) {
        super(context, sceneObject, attributes);

        String attribute = attributes.getProperty("text");
        if (attribute != null) {
            setText(attribute);
        }

        attribute = attributes.getProperty("text_size");
        if (attribute != null) {
            setTextSize(Float.parseFloat(attribute));
        }

        attribute = attributes.getProperty("background");
        if (attribute != null) {
            setBackGround(context.getContext().getResources()
                    .getDrawable(Integer.parseInt(attribute)));
        }

        attribute = attributes.getProperty("background_color");
        if (attribute != null) {
            setBackgroundColor(Integer.parseInt(attribute));
        }

        attribute = attributes.getProperty("gravity");
        if (attribute != null) {
            setGravity(Integer.parseInt(attribute));
        }

        attribute = attributes.getProperty("refresh_freq");
        if (attribute != null) {
            setRefreshFrequency(IntervalFrequency.valueOf(attribute));
        }

        attribute = attributes.getProperty("text_color");
        if (attribute != null) {
            setTextColor(Integer.parseInt(attribute));
        }

        mTextViewSceneObject = maybeWrap(sceneObject);
    }

    /**
     * Shows a {@link TextView} on a {@linkplain Widget widget} with view's
     * default height and width.
     *
     * @param gvrContext
     *            current {@link GVRContext}
     * @param width
     *            Widget height, in GVRF scene graph units.
     *
     *            Please note that your widget's size is independent of the size
     *            of the internal {@code TextView}: a large mismatch between the
     *            scene object's size and the view's size will result in
     *            'spidery' or 'blocky' text.
     *
     * @param height
     *            Widget width, in GVRF scene graph units.
     */
    public TextWidget(GVRContext context, float width, float height) {
        this(context, width, height, null);
    }

    /**
     * Shows a {@link TextView} on a {@linkplain Widget widget} with view's
     * default height and width.
     *
     * @param gvrContext
     *            current {@link GVRContext}
     * @param width
     *            Widget height, in GVRF scene graph units.
     *
     *            Please note that your widget's size is independent of the size
     *            of the internal {@code TextView}: a large mismatch between the
     *            scene object's size and the view's size will result in
     *            'spidery' or 'blocky' text.
     *
     * @param height
     *            Widget width, in GVRF scene graph units.
     * @param text
     *            {@link CharSequence} to show on the textView
     */

    public TextWidget(GVRContext context, float width, float height,
            CharSequence text) {
        super(context, new GVRTextViewSceneObject(context, width, height, text));
        mTextViewSceneObject = (GVRTextViewSceneObject) getSceneObject();
    }

    public Drawable getBackGround() {
        return mTextViewSceneObject.getBackGround();
    }

    public int getGravity() {
        return mTextViewSceneObject.getGravity();
    }

    public IntervalFrequency getRefreshFrequency() {
        return mTextViewSceneObject.getRefreshFrequency();
    }

    public CharSequence getText() {
        return mTextViewSceneObject.getText();
    }

    public float getTextSize() {
        return mTextViewSceneObject.getTextSize();
    }

    public String getTextString() {
        return mTextViewSceneObject.getTextString();
    }

    public void setBackGround(Drawable drawable) {
        mTextViewSceneObject.setBackGround(drawable);
    }

    public void setBackgroundColor(int color) {
        mTextViewSceneObject.setBackgroundColor(color);
    }

    public void setGravity(int gravity) {
        mTextViewSceneObject.setGravity(gravity);
    }

    public void setRefreshFrequency(IntervalFrequency frequency) {
        mTextViewSceneObject.setRefreshFrequency(frequency);
    }

    public void setText(CharSequence text) {
        mTextViewSceneObject.setText(text);
    }

    public void setTextColor(int color) {
        mTextViewSceneObject.setTextColor(color);
    }

    public void setTextSize(float size) {
        mTextViewSceneObject.setTextSize(size);
    }

    private GVRTextViewSceneObject maybeWrap(GVRSceneObject sceneObject) {
        if (sceneObject instanceof GVRTextViewSceneObject) {
            return (GVRTextViewSceneObject) sceneObject;
        } else {
            final float sizes[] = LayoutHelpers.calculateGeometricDimensions(sceneObject);
            final GVRSceneObject temp = new GVRTextViewSceneObject(sceneObject.getGVRContext(), sizes[0], sizes[1], "");
            sceneObject.addChildObject(temp);
            return (GVRTextViewSceneObject) temp;
        }
    }

    private final GVRTextViewSceneObject mTextViewSceneObject;
}
