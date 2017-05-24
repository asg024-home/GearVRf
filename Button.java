package com.samsung.smcl.vr.widgets;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRRenderData.GVRRenderingOrder;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.scene_objects.GVRTextViewSceneObject.IntervalFrequency;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.drawable.Drawable;
import android.view.Gravity;

import com.samsung.smcl.utility.Log;

import java.util.List;

import static com.samsung.smcl.utility.Exceptions.RuntimeAssertion;
import static com.samsung.smcl.vr.widgets.JSONHelpers.getInt;
import static com.samsung.smcl.vr.widgets.JSONHelpers.has;
import static com.samsung.smcl.vr.widgets.JSONHelpers.optBoolean;
import static com.samsung.smcl.vr.widgets.JSONHelpers.optEnum;

/**
 * A basic Button widget.
 * <p>
 * The widget has two parts -- the text and the "graphic" -- which are children
 * of the Button itself. This structure facilitates custom layout of the two
 * parts relative to each other. The {@linkplain Widget#getName() name} of the
 * text part is {@code ".text"}, the graphic part is {@code ".graphic"}. The
 * ordering of these children is not guaranteed.
 */
public class Button extends Widget implements TextContainer {

    public Button(GVRContext context, float width, float height,
                  float textWidgetWidth, float textWidgetHeight) {
        super(context, width, height);
        init();

        mTextWidgetWidth = textWidgetWidth;
        mTextWidgetHeight = textWidgetHeight;
    }

    public Button(GVRContext context, float width, float height) {
        super(context, width, height);
        init();
    }

    public Button(GVRContext context, GVRSceneObject sceneObject,
            NodeEntry attributes) throws InstantiationException {
        super(context, sceneObject, attributes);
        init();

        int width = getDimensionAttribute(attributes, ButtonProperties.textWidgetWidth);
        if (width >= 0) {
            mTextWidgetWidth = width;
        }
        int height = getDimensionAttribute(attributes, ButtonProperties.textWidgetHeight);
        if (height >= 0) {
            mTextWidgetHeight = height;
        }
    }

    private int getDimensionAttribute(NodeEntry attributes, ButtonProperties property) {
        int dimension = -1;
        final String attr = attributes.getProperty(property);
        if (attr != null) {
            dimension = Integer.parseInt(attr);
        }
        return dimension;
    }

    private enum ButtonProperties {
        textWidgetWidth, textWidgetHeight;
    }

    @Override
    protected void onSetupMetadata(JSONObject metaData) throws JSONException {
        super.onSetupMetadata(metaData);
        if (has(metaData, ButtonProperties.textWidgetWidth)) {
            mTextWidgetWidth = getInt(metaData, ButtonProperties.textWidgetWidth);
        }
        if (has(metaData, ButtonProperties.textWidgetHeight)) {
            mTextWidgetHeight = getInt(metaData, ButtonProperties.textWidgetHeight);
        }
    }

    public Button(GVRContext context, GVRSceneObject sceneObject) {
        super(context, sceneObject);
        init();
    }

    @Override
    public Drawable getBackGround() {
        return mTextContainer.getBackGround();
    }

    @Override
    public void setBackGround(Drawable drawable) {
        mTextContainer.setBackGround(drawable);
    }

    @Override
    public int getBackgroundColor() {
        return mTextContainer.getBackgroundColor();
    }

    @Override
    public void setBackgroundColor(int color) {
        mTextContainer.setBackgroundColor(color);
    }

    @Override
    public int getGravity() {
        return mTextContainer.getGravity();
    }

    @Override
    public void setGravity(int gravity) {
        mTextContainer.setGravity(gravity);
    }

    @Override
    public IntervalFrequency getRefreshFrequency() {
        return mTextContainer.getRefreshFrequency();
    }

    @Override
    public void setRefreshFrequency(IntervalFrequency frequency) {
        mTextContainer.setRefreshFrequency(frequency);
    }

    @Override
    public CharSequence getText() {
        return mTextContainer.getText();
    }

    @Override
    public void setText(CharSequence text) {
        if (text != null && text.length() > 0) {
            if (!(mTextContainer instanceof TextWidget)) {
                // Text was previously empty or null; swap our cached TextParams
                // for a new TextWidget to display the text
                createText();
            } else {
                // TODO: Remove when 2nd+ TextWidget issue is resolved
                ((Widget) mTextContainer).setVisibility(Visibility.VISIBLE);
            }
        } else {
            if (mTextContainer instanceof TextWidget) {
                // TODO: Figure out why adding the 2nd+ TextWidget doesn't work
                /*
                 * // If the text has been cleared, swap our TextWidget for the
                 * // TextParams so we don't hang on to the resources final
                 * TextWidget textWidget = (TextWidget) mTextContainer;
                 * removeChild(textWidget, textWidget.getSceneObject(), false);
                 * mTextContainer = textWidget.getTextParams();
                 */
                ((TextWidget) mTextContainer).setVisibility(Visibility.HIDDEN);
            }
        }
        mTextContainer.setText(text);
    }

    @Override
    public int getTextColor() {
        return mTextContainer.getTextColor();
    }

    @Override
    public void setTextColor(int color) {
        mTextContainer.setTextColor(color);
    }

    @Override
    public float getTextSize() {
        return mTextContainer.getTextSize();
    }

    @Override
    public void setTextSize(float size) {
        mTextContainer.setTextSize(size);
    }

    @Override
    public String getTextString() {
        return mTextContainer.getTextString();
    }

    protected Button(GVRContext context, GVRMesh mesh) {
        super(context, mesh);
        init();
    }

    /**
     * Override to provide a specific implementation of the {@code ".graphic"}
     * child.
     * <p>
     * By convention, this returns an embedded class named "Graphic"; in
     * {@link Button}'s case, the class is {@code Button.Graphic}. This is
     * primarily useful for hooking up to default metadata:
     *
     * <pre>
     * {
     *   "objects": {
     *     "com.samsung.smcl.vr.widgets.Button.Graphic": {
     *       "states": {
     *         ...
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * However, an instance of <em>any</em> class can be returned by this
     * method, or {@code null} if there is no need for a separate {@code Widget}
     * . Keep in mind that specifying default metadata for a class applies to
     * all instances, regardless of where or how they are instantiated.
     *
     * @return {@code Widget} instance or {@code null}
     */
    protected Widget createGraphicWidget() {
        return new Graphic(getGVRContext(), getWidth(), getHeight());
    }

    protected Widget getGraphic() {
        return mGraphic;
    }

    @Override
    protected void onLayout() {
        Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "layout() called (%s)", getName());
        List<Widget> children = getChildren();
        if (children.isEmpty()) {
            Log.d(Log.SUBSYSTEM.LAYOUT, TAG, "layout: no items to layout! %s", getName());
            return;
        }

        for (Widget child : children) {
            child.layout();
        }

        super.onLayout();
    }

    @Override
    public Widget get(final int dataIndex) {
        return getChildren().get(dataIndex);
    }

    @Override
    public int size() {
        return getChildren().size();
    }

    @Override
    protected void onCreate() {
        mGraphic = findChildByName(".graphic");
        if (mGraphic == null) {
            // Delegate the non-text bits to a child object so we can layout the
            // text relative to the "graphic".
            mGraphic = createGraphicWidget();
            if (mGraphic != null) {
                mGraphic.setName(".graphic");
                addChild(mGraphic, 0);
                for (Layout layout: mLayouts) {
                    layout.invalidate();
                }
                requestLayout();
            }
        }
    }

    @Override
    protected boolean onTouch() {
        super.onTouch();
        return true;
    }

    protected float getTextWidgetWidth() {
        return mTextWidgetWidth;
    }

    protected float getTextWidgetHeight() {
        return mTextWidgetHeight;
    }

    protected TextWidget createTextWidget() {
        Log.d(TAG, "createTextWidget(%s) [%f, %f]", getName(), getTextWidgetWidth(), getTextWidgetHeight());
        final TextWidget textWidget = new TextWidget(getGVRContext(),
                getTextWidgetWidth(), getTextWidgetHeight());
        Log.d(TAG, "createTextWidget(%s): setting rendering order",
                getName());
        textWidget.setRenderingOrder(GVRRenderingOrder.TRANSPARENT);
        Log.d(TAG, "createTextWidget(%s): setting gravity", getName());
        textWidget.setGravity(Gravity.CENTER);
        textWidget.setName(".text");
        Log.d(TAG, "createTextWidget(%s): setting params", getName());
        textWidget.setTextParams(mTextContainer);
        return textWidget;
    }

    private void createText() {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                TextWidget textWidget = createTextWidget();
                mTextContainer = textWidget;
                addChild(textWidget);
                Log.d(TAG, "createTextWidget(%s): requesting layout", getName());

                for (Layout layout: mLayouts) {
                    layout.invalidate();
                }
                requestLayout();
            }
        });
    }


    private void init() {
        mTextWidgetWidth = getWidth();
        mTextWidgetHeight = getHeight();

        setRenderingOrder(GVRRenderingOrder.TRANSPARENT);

        setChildrenFollowFocus(true);
        setChildrenFollowInput(true);
        setChildrenFollowState(true);

        // setup default layout
        mDefaultLayout.setOrientation(OrientedLayout.Orientation.STACK);
        mDefaultLayout.setDividerPadding(0.025f, Layout.Axis.Z);
    }

    @Override
    public Layout getDefaultLayout() {
        return mDefaultLayout;
    }

    private static class Graphic extends Widget {
        Graphic(GVRContext context, float width, float height) {
            super(context, width, height);
        }
    }

    private TextContainer mTextContainer = new TextWidget.TextParams();
    private Widget mGraphic;

    private final OrientedLayout mDefaultLayout = new LinearLayout() {
        @Override
        protected int getOffsetSign() {
            return -1;
        }
    };

    private static final String TAG = Button.class.getSimpleName();
    private float mTextWidgetWidth, mTextWidgetHeight;
}
