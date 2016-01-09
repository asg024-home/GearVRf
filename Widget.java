package com.samsung.smcl.vr.widgets;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRBitmapTexture;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRMaterial.GVRShaderType;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRRenderData.GVRRenderingOrder;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.GVRTransform;

import android.content.Context;
import android.view.MotionEvent;

import com.samsung.smcl.utility.Log;
import com.samsung.smcl.vr.gvrf_launcher.LauncherViewManager.OnInitListener;
import com.samsung.smcl.vr.gvrf_launcher.MainScene;
import com.samsung.smcl.vr.gvrf_launcher.R;
import com.samsung.smcl.vr.gvrf_launcher.TouchManager;

public class Widget {

    /**
     * Call to initialize the Widget infrastructure.
     *
     * @param touchManager
     *            The global {@link TouchManager} instance.
     */
    static public void init(TouchManager touchManager) {
        // TODO: I have a change in the works to make TouchManager a singleton,
        // so this step will be unnecessary
        sTouchManager = new WeakReference<TouchManager>(touchManager);
    }

    /**
     * Register this with LauncherViewManager. An alternative would be to have
     * {@link #init(TouchManager) init()} do this work and just call it directly
     * from LauncherViewManager.onInit().
     */
    static public final OnInitListener onInitListener = new OnInitListener() {
        @Override
        public void onInit(GVRContext gvrContext, MainScene scene) {
            FocusManager.init(gvrContext);
            sGLThread = new WeakReference<Thread>(Thread.currentThread());
            sDefaultTexture = gvrContext.loadTexture(new GVRAndroidResource(
                    gvrContext, R.drawable.default_bkgd));
            Log.d(TAG, "onInit(): default texture: %s", sDefaultTexture);
        }
    };

    /**
     * @return The time, in milliseconds, that a widget must have continuous
     *         focus before an {@link OnFocusListener#onLongFocus()
     *         onLongFocus()} event is sent.
     */
    static public long getLongFocusTime() {
        return FocusManager.LONG_FOCUS_TIMEOUT;
    }

    static public GVRTexture getDefaultTexture() {
        return sDefaultTexture;
    }

    /**
     * Implement and {@link Widget#addFocusListener(OnFocusListener) register}
     * this interface to listen for focus changes on widgets.
     */
    public interface OnFocusListener {
        /**
         * Called when a widget gains or loses focus.
         *
         * @param focused
         *            {@code True} is the widget has gained focus; {@code false}
         *            if the widget has lost focus.
         * @return {@code True} to indicate that no further processing of the
         *         focus change should take place; {@code false} to allow
         *         further processing.
         */
        public boolean onFocus(boolean focused, Widget widget);

        /**
         * Called when a widget has had focus for more than
         * {@link Widget#getLongFocusTime()} milliseconds.
         *
         * @return {@code True} to indicate that no further processing of the
         *         event should take place; {@code false} to allow further
         *         processing.
         */
        public boolean onLongFocus(Widget widget);
    }

    /**
     * Implement and {@link Widget#addBackKeyListener(OnBackKeyListener)
     * register} this interface to listen for back key events on widgets.
     */
    public interface OnBackKeyListener {
        /**
         * Called when widget is target of back key event.
         * 
         * @param widget
         *            {@link Widget} target by back key event.
         * @return {@code True} to indicate that no further processing of the
         *         touch event should take place; {@code false} to allow further
         *         processing.
         */
        public boolean onBackKey(Widget widget);
    }

    /**
     * Implement and {@link Widget#addTouchListener(OnTouchListener) register}
     * this interface to listen for touch events on widgets.
     */
    public interface OnTouchListener {
        /**
         * Called when a widget is touched (tapped).
         *
         * @param widget
         *            {@link Widget} target by touch event.
         * 
         * @return {@code True} to indicate that no further processing of the
         *         touch event should take place; {@code false} to allow further
         *         processing.
         */
        public boolean onTouch(Widget widget);
    }

    /**
     * Options for {@link Widget#setVisibility(Visibility)}.
     */
    public enum Visibility {
        /** Show the object and include in layout calculations. */
        VISIBLE,
        /** Hide the object, but include in layout calculations. */
        HIDDEN,
        /** Hide the object, but extract the size for layout calculations. */
        PLACEHOLDER,
        /** Hide the object, and do not include in layout calculations. */
        GONE
    }

    /**
     * Construct a wrapper for an existing {@link GVRSceneObject}.
     *
     * @param context
     *            The current {@link GVRContext}.
     * @param sceneObject
     *            The {@link GVRSceneObject} to wrap.
     */
    public Widget(final GVRContext context, final GVRSceneObject sceneObject) {
        mContext = context;
        mSceneObject = sceneObject;
        final float[] dimensions = LayoutHelpers
                .calculateGeometricDimensions(sceneObject);
        mBaseWidth = mWidth = dimensions[0];
        mBaseHeight = mHeight = dimensions[1];
        mBaseDepth = mDepth = dimensions[2];

        Log.d(TAG,
              "Widget constructor: %s mWidth = %f mHeight = %f mDepth = %f",
              sceneObject.getName(), mWidth, mHeight, mDepth);
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
     *            TODO
     */
    public Widget(final GVRContext context, final GVRSceneObject sceneObject,
                  NodeEntry attributes) {
        this(context, sceneObject);

        // This gives us the demangled name, which is the name we'll use to
        // refer to the widget
        String attribute = attributes.getProperty("name");
        setName(attribute);

        final boolean hasRenderData = sceneObject.getRenderData() != null;

        attribute = attributes.getProperty("touchable");
        setTouchable(attribute != null &&
                     hasRenderData &&
                     attribute.compareToIgnoreCase("false") != 0);

        attribute = attributes.getProperty("focusenabled");
        setFocusEnabled(attribute != null &&
                        hasRenderData &&
                        attribute.compareToIgnoreCase("false") != 0);

        attribute = attributes.getProperty("visibility");
        setVisibility(attribute != null ? Visibility.valueOf(attribute.toUpperCase())
                : Visibility.VISIBLE);
    }

    private static final String pattern = Widget.class.getSimpleName()
            + "name : %s size = (%f, %f, %f) \n"
            + "touchable = %b focus_enabled = %b Visibile = %s";

    public String toString() {
        return String.format(pattern, getName(), mWidth, mHeight, mDepth,
                             mIsTouchable, mFocusEnabled, mVisibility);
    }

    public Widget(final GVRContext context, final float width,
            final float height) {
        this(context, new GVRSceneObject(context, width, height));

        GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            GVRMaterial material = new GVRMaterial(mContext,
                    GVRShaderType.Texture.ID);
            material.setMainTexture(sDefaultTexture);
            renderData.setMaterial(material);
        }
    }

    /**
     * @return The Android {@link Context} this {@code Widget} is in.
     */
    public Context getContext() {
        return getGVRContext().getContext();
    }

    /**
     * Set whether or not the object can receive line-of-sight focus. If
     * enabled, the object will receive {@link #onFocus(boolean)} and
     * {@link #onLongFocus()} notifications and
     * {@linkplain #addFocusListener(OnFocusListener) registered}
     * {@linkplain OnFocusListener listeners} can also receive those
     * notifications.
     * <p>
     * Focus is enabled by default.
     *
     * @param enabled
     *            {@code True} to enable line-of-sight focus, {@code false} to
     *            disable.
     */
    public void setFocusEnabled(boolean enabled) {
        if (mFocusEnabled != enabled) {
            mFocusEnabled = enabled;
        }
    }

    /**
     * @return Whether line-of-sight focus is enabled for this object.
     */
    public boolean isFocusEnabled() {
        return mFocusEnabled;
    }

    /**
     * @return Whether the object currently has line-of-sight focus.
     */
    public boolean isFocused() {
        return mIsFocused;
    }

    /**
     * Add a listener for {@linkplain OnFocusListener#onFocus(boolean) focus}
     * and {@linkplain OnFocusListener#onLongFocus() long focus} notifications
     * for this object.
     *
     * @param listener
     *            An implementation of {@link OnFocusListener}.
     * @return {@code True} if the listener was successfully registered,
     *         {@code false} if the listener is already registered.
     */
    public boolean addFocusListener(final OnFocusListener listener) {
        return mFocusListeners.add(listener);
    }

    /**
     * Remove a previously {@linkplain #addFocusListener(OnFocusListener)
     * registered} focus notification {@linkplain OnFocusListener listener}.
     *
     * @param listener
     *            An implementation of {@link OnFocusListener}
     * @return {@code True} if the listener was successfully unregistered,
     *         {@code false} if the specified listener was not previously
     *         registered with this object.
     */
    public boolean removeFocusListener(final OnFocusListener listener) {
        return mFocusListeners.remove(listener);
    }

    /**
     * Set whether or not the {@code Widget} can receive touch and back key
     * events. If enabled, the object will receive {@link #onTouch()} and
     * {@link #onBackKey()} notifications and registered
     * {@linkplain #addTouchListener(OnTouchListener) touch} and
     * {@linkplain #addBackKeyListener(OnBackKeyListener) back key} listeners
     * can also receive those notifications.
     * <p>
     * Objects are touchable by default.
     *
     * @param touchable
     *            {@code True} to enable touch events for this object,
     *            {@code false} to disable.
     */
    public void setTouchable(boolean touchable) {
        if (touchable != mIsTouchable) {
            mIsTouchable = touchable;
            registerPickable();
        }
    }

    /**
     * @return Whether touch and back key events are enabled for this object.
     */
    public boolean isTouchable() {
        return mIsTouchable;
    }

    /**
     * Add a listener for {@linkplain OnBackKeyListener#onBackKey(Widget) back
     * key} notifications for this object.
     * 
     * @param listener
     *            An implementation of {@link OnBackKeyListener}.
     * @return {@code True} if the listener was successfully registered,
     *         {@code false} if the listener was already registered.
     */
    public boolean addBackKeyListener(final OnBackKeyListener listener) {
        return mBackKeyListeners.add(listener);
    }

    /**
     * Remove a previously {@linkplain #addBackKeyListener(OnBackKeyListener)
     * registered} back key notification {@linkplain OnBackKeyListener listener}
     * .
     * 
     * @param listener
     *            An implementation of {@link OnBackKeyListener}
     * @return {@code True} if the listener was successfully unregistered,
     *         {@code false} if the specified listener was not previously
     *         registered with this object.
     */
    public boolean removeBackKeyListener(final OnBackKeyListener listener) {
        return mBackKeyListeners.remove(listener);
    }

    /**
     * Add a listener for {@linkplain OnTouchListener#onTouch() touch}
     * notifications for this object.
     *
     * @param listener
     *            An implementation of {@link OnTouchListener}.
     * @return {@code True} if the listener was successfully registered,
     *         {@code false} if the listener was already registered.
     */
    public boolean addTouchListener(final OnTouchListener listener) {
        return mTouchListeners.add(listener);
    }

    /**
     * Remove a previously {@linkplain #addTouchListener(OnTouchListener)
     * registered} touch notification {@linkplain OnTouchListener listener}.
     *
     * @param listener
     *            An implementation of {@link OnTouchListener}
     * @return {@code True} if the listener was successfully unregistered,
     *         {@code false} if the specified listener was not previously
     *         registered with this object.
     */
    public boolean removeTouchListener(final OnTouchListener listener) {
        return mTouchListeners.remove(listener);
    }

    /**
     * Get the (optional) name of the {@link Widget}.
     *
     * @return The name of the {@code Widget}.
     */
    public String getName() {
        return mName;
    }

    /**
     * Set the (optional) name of the {@link Widget}. {@code Widget} names are
     * not needed: they are only for the application's convenience.
     *
     * @param name
     */
    public void setName(String name) {
        mName = name;
    }

    public String getMetadata() {
        return mSceneObject.getName();
    }

    /**
     * @return The {@link Widget Widget's} parent. If the {@code Widget} has not
     *         been {@linkplain GroupWidget#addChild(Widget) added} to a
     *         {@code GroupWidget}, returns {@code null}.
     */
    public final Widget getParent() {
        return mParent;
    }

    /**
     * Set the order in which this {@link Widget} will be rendered.
     *
     * @param renderingOrder
     *            See {@link GVRRenderingOrder}.
     */
    public void setRenderingOrder(final int renderingOrder) {
        final GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            renderData.setRenderingOrder(renderingOrder);
        }
    }

    /**
     * @return The order in which this {@link Widget} will be rendered.
     * @see GVRRenderingOrder
     */
    public final int getRenderingOrder() {
        final GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            return renderData.getRenderingOrder();
        }
        return -1;
    }

    /**
     * Sets the {@linkplain GVRMaterial#setMainTexture(GVRTexture) main texture}
     * of the {@link Widget}.
     *
     * @param texture
     *            The new texture.
     */
    public void setTexture(final GVRTexture texture) {
        getMaterial().setMainTexture(texture);
    }

    /**
     * Sets the {@linkplain GVRMaterial#setMainTexture(GVRTexture) main texture}
     * of the {@link Widget}.
     *
     * @param name
     *            Name of the texture
     * @param texture
     *            The new texture.
     */
    public void setTexture(String name, final GVRTexture texture) {
        getMaterial().setTexture(name, texture);
    }

    /**
     * Sets the {@linkplain GVRMaterial#setMainTexture(GVRTexture) main texture}
     * of the {@link Widget}.
     *
     * @param texture
     *            The new texture.
     */
    public void setTexture(final Future<GVRTexture> texture) {
        getMaterial().setMainTexture(texture);
    }

    /**
     * Sets the {@linkplain GVRMaterial#setMainTexture(GVRTexture) main texture}
     * of the {@link Widget}.
     *
     * @param bitmapId
     *            Resource ID of the bitmap to create the texture from.
     */
    public void setTexture(final int bitmapId) {
        final GVRAndroidResource resource = new GVRAndroidResource(
                mContext.getContext(), bitmapId);
        setTexture(mContext.loadFutureTexture(resource));
    }

    public float getWidth() {
        return mWidth;
    }

    public float getHeight() {
        return mHeight;
    }

    public float getDepth() {
        return mDepth;
    }

    /**
     * Set the {@code GL_POLYGON_OFFSET_FILL} option
     * 
     * @param offset
     *            {@code true} if {@code GL_POLYGON_OFFSET_FILL} should be
     *            enabled, {@code false} if not.
     */
    public void setOffset(boolean offset) {
        GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            renderData.setOffset(offset);
        }
    }

    /**
     * @return {@code true} if {@code GL_POLYGON_OFFSET_FILL} is enabled,
     *         {@code false} if not.
     */
    public boolean getOffset() {
        GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            return renderData.getOffset();
        }
        return false;
    }

    /**
     * Set the {@code factor} value passed to {@code glPolygonOffset()} if
     * {@code GL_POLYGON_OFFSET_FILL} is enabled.
     * 
     * @param offsetFactor
     *            Per OpenGL docs: Specifies a scale factor that is used to
     *            create a variable depth offset for each polygon. The initial
     *            value is 0.
     * @see #setOffset(boolean)
     */
    public void setOffsetFactor(float offsetFactor) {
        GVRRenderData renderData = getRenderData();
        renderData.setOffsetFactor(offsetFactor);
        if (renderData != null) {
        }
    }

    /**
     * @return The {@code factor} value passed to {@code glPolygonOffset()} if
     *         {@code GL_POLYGON_OFFSET_FILL} is enabled.
     * @see #setOffset(boolean)
     */
    public float getOffsetFactor() {
        GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            return renderData.getOffsetFactor();
        }
        return 0;
    }

    /**
     * Set the {@code units} value passed to {@code glPolygonOffset()} if
     * {@code GL_POLYGON_OFFSET_FILL} is enabled.
     * 
     * @param offsetUnits
     *            Per OpenGL docs: Is multiplied by an implementation-specific
     *            value to create a constant depth offset. The initial value is
     *            0.
     * @see #setOffset(boolean)
     */
    public void setOffsetUnits(float offsetUnits) {
        GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            renderData.setOffsetUnits(offsetUnits);
        }
    }

    /**
     * @return The {@code units} value passed to {@code glPolygonOffset()} if
     *         {@code GL_POLYGON_OFFSET_FILL} is enabled.
     * @see #setOffset(boolean)
     */
    public float getOffsetUnits() {
        GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            return renderData.getOffsetUnits();
        }
        return 0;
    }

    /**
     * Get the X component of the widget's position.
     *
     * @return 'X' component of the widget's position.
     */
    public float getPositionX() {
        return getTransform().getPositionX();
    }

    /**
     * Get the 'Y' component of the widget's position.
     *
     * @return 'Y' component of the widget's position.
     */
    public float getPositionY() {
        return getTransform().getPositionY();
    }

    /**
     * Get the 'Z' component of the widget's position.
     *
     * @return 'Z' component of the widget's position.
     */
    public float getPositionZ() {
        return getTransform().getPositionZ();
    }

    /**
     * Set absolute position.
     *
     * Use {@link #translate(float, float, float)} to <em>move</em> the object.
     *
     * @param x
     *            'X' component of the absolute position.
     * @param y
     *            'Y' component of the absolute position.
     * @param z
     *            'Z' component of the absolute position.
     */
    public void setPosition(float x, float y, float z) {
        getTransform().setPosition(x, y, z);
    }

    /**
     * Set the 'X' component of absolute position.
     *
     * Use {@link #translate(float, float, float)} to <em>move</em> the object.
     *
     * @param x
     *            New 'X' component of the absolute position.
     */
    public void setPositionX(float x) {
        getTransform().setPositionX(x);
    }

    /**
     * Set the 'Y' component of the absolute position.
     *
     * Use {@link #translate(float, float, float)} to <em>move</em> the object.
     *
     * @param y
     *            New 'Y' component of the absolute position.
     */
    public void setPositionY(float y) {
        getTransform().setPositionY(y);
    }

    /**
     * Set the 'Z' component of the absolute position.
     *
     * Use {@link #translate(float, float, float)} to <em>move</em> the object.
     *
     * @param z
     *            New 'Z' component of the absolute position.
     */
    public void setPositionZ(float z) {
        getTransform().setPositionZ(z);
    }

    /**
     * Get the quaternion 'W' component.
     *
     * @return 'W' component of the widget's rotation, treated as a quaternion.
     */
    public float getRotationW() {
        return getTransform().getRotationW();
    }

    /**
     * Get the quaternion 'X' component.
     *
     * @return 'X' component of the widget's rotation, treated as a quaternion.
     */
    public float getRotationX() {
        return getTransform().getRotationX();
    }

    /**
     * Get the quaternion 'Y' component.
     *
     * @return 'Y' component of the widget's rotation, treated as a quaternion.
     */
    public float getRotationY() {
        return getTransform().getRotationY();
    }

    /**
     * Get the quaternion 'Z' component.
     *
     * @return 'Z' component of the widget's rotation, treated as a quaternion.
     */
    public float getRotationZ() {
        return getTransform().getRotationZ();
    }

    /**
     * Get the rotation around the 'Y' axis, in degrees.
     *
     * @return The widget's current rotation around the 'Y' axis, in degrees.
     */
    public float getRotationYaw() {
        return getTransform().getRotationYaw();
    }

    /**
     * Get the rotation around the 'X' axis, in degrees.
     *
     * @return The widget's rotation around the 'X' axis, in degrees.
     */
    public float getRotationPitch() {
        return getTransform().getRotationPitch();
    }

    /**
     * Get the rotation around the 'Z' axis, in degrees.
     *
     * @return The widget's rotation around the 'Z' axis, in degrees.
     */
    public float getRotationRoll() {
        return getTransform().getRotationRoll();
    }

    /**
     * Set rotation, as a quaternion.
     *
     * Sets the widget's current rotation in quaternion terms. Overrides any
     * previous rotations using {@link #rotate(float, float, float, float)
     * rotate()}, {@link #rotateByAxis(float, float, float, float)
     * rotateByAxis()} , or
     * {@link #rotateByAxisWithPivot(float, float, float, float, float, float, float)
     * rotateByAxisWithPivot()} .
     *
     * @param w
     *            'W' component of the quaternion.
     * @param x
     *            'X' component of the quaternion.
     * @param y
     *            'Y' component of the quaternion.
     * @param z
     *            'Z' component of the quaternion.
     */
    public void setRotation(float w, float x, float y, float z) {
        getTransform().setRotation(w, x, y, z);
    }

    /**
     * Get the 'X' scale
     *
     * @return The widget's current scaling on the 'X' axis.
     */
    public float getScaleX() {
        return getTransform().getScaleX();
    }

    /**
     * Get the 'Y' scale
     *
     * @return The widget's current scaling on the 'Y' axis.
     */
    public float getScaleY() {
        return getTransform().getScaleY();
    }

    /**
     * Get the 'Z' scale
     *
     * @return The widget's current scaling on the 'Z' axis.
     */
    public float getScaleZ() {
        return getTransform().getScaleZ();
    }

    /**
     * Set [X, Y, Z] current scale
     *
     * @param x
     *            Scaling factor on the 'X' axis.
     * @param y
     *            Scaling factor on the 'Y' axis.
     * @param z
     *            Scaling factor on the 'Z' axis.
     */
    public void setScale(float x, float y, float z) {
        mWidth = mBaseWidth * x;
        mHeight = mBaseHeight * y;
        mDepth = mBaseDepth * z;
        getTransform().setScale(x, y, z);
    }

    /**
     * Set the widget's current scaling on the 'X' axis.
     *
     * @param x
     *            Scaling factor on the 'X' axis.
     */
    public void setScaleX(float x) {
        mWidth = mBaseWidth * x;
        getTransform().setScaleX(x);
    }

    /**
     * Set the widget's current scaling on the 'Y' axis.
     *
     * @param y
     *            Scaling factor on the 'Y' axis.
     */
    public void setScaleY(float y) {
        mHeight = mBaseHeight * y;
        getTransform().setScaleY(y);
    }

    /**
     * Set the widget's current scaling on the 'Z' axis.
     *
     * @param z
     *            Scaling factor on the 'Z' axis.
     */
    public void setScaleZ(float z) {
        mDepth = mBaseDepth * z;
        getTransform().setScaleZ(z);
    }

    /**
     * Get the 4x4 single matrix.
     *
     * @return An array of 16 {@code float}s representing a 4x4 matrix in
     *         OpenGL-compatible column-major format.
     */
    public float[] getModelMatrix() {
        return getTransform().getModelMatrix();
    }

    /**
     * Set the 4x4 model matrix and set current scaling, rotation, and
     * transformation based on this model matrix.
     *
     * @param mat
     *            An array of 16 {@code float}s representing a 4x4 matrix in
     *            OpenGL-compatible column-major format.
     */
    public void setModelMatrix(float[] mat) {
        if (mat.length != 16) {
            throw new IllegalArgumentException("Size not equal to 16.");
        }
        getTransform().setModelMatrix(mat);
    }

    /**
     * Move the object, relative to its current position.
     *
     * Modify the tranform's current translation by applying translations on all
     * 3 axes.
     *
     * @param x
     *            'X' delta
     * @param y
     *            'Y' delta
     * @param z
     *            'Z' delta
     */
    public void translate(float x, float y, float z) {
        getTransform().translate(x, y, z);
    }

    /**
     * Sets the absolute rotation in angle/axis terms.
     *
     * Rotates using the right hand rule.
     *
     * <p>
     * Contrast this with {@link #rotate(float, float, float, float) rotate()},
     * {@link #rotateByAxis(float, float, float, float) rotateByAxis()}, or
     * {@link #rotateByAxisWithPivot(float, float, float, float, float, float, float)
     * rotateByAxisWithPivot()}, which all do relative rotations.
     *
     * @param angle
     *            Angle of rotation in degrees.
     * @param x
     *            'X' component of the axis.
     * @param y
     *            'Y' component of the axis.
     * @param z
     *            'Z' component of the axis.
     */
    public void setRotationByAxis(float angle, float x, float y, float z) {
        getTransform().setRotationByAxis(angle, x, y, z);
    }

    /**
     * Modify the tranform's current rotation in quaternion terms.
     *
     * @param w
     *            'W' component of the quaternion.
     * @param x
     *            'X' component of the quaternion.
     * @param y
     *            'Y' component of the quaternion.
     * @param z
     *            'Z' component of the quaternion.
     */
    public void rotate(float w, float x, float y, float z) {
        getTransform().rotate(w, x, y, z);
    }

    /**
     * Modify the tranform's current rotation in quaternion terms, around a
     * pivot other than the origin.
     * 
     * @param w
     *            'W' component of the quaternion.
     * @param x
     *            'X' component of the quaternion.
     * @param y
     *            'Y' component of the quaternion.
     * @param z
     *            'Z' component of the quaternion.
     * @param pivotX
     *            'X' component of the pivot's location.
     * @param pivotY
     *            'Y' component of the pivot's location.
     * @param pivotZ
     *            'Z' component of the pivot's location.
     */
    public void rotateWithPivot(float w, float x, float y, float z,
            float pivotX, float pivotY, float pivotZ) {
        getTransform().rotateWithPivot(w, x, y, z, pivotX, pivotY, pivotZ);
    }

    /**
     * Modify the widget's current rotation in angle/axis terms.
     *
     * @param angle
     *            Angle of rotation in degrees.
     * @param x
     *            'X' component of the axis.
     * @param y
     *            'Y' component of the axis.
     * @param z
     *            'Z' component of the axis.
     */
    public void rotateByAxis(float angle, float x, float y, float z) {
        getTransform().rotateByAxis(angle, x, y, z);
    }

    /**
     * Modify the widget's current rotation in angle/axis terms, around a pivot
     * other than the origin.
     *
     * @param angle
     *            Angle of rotation in degrees.
     * @param axisX
     *            'X' component of the axis.
     * @param axisY
     *            'Y' component of the axis.
     * @param axisZ
     *            'Z' component of the axis.
     * @param pivotX
     *            'X' component of the pivot's location.
     * @param pivotY
     *            'Y' component of the pivot's location.
     * @param pivotZ
     *            'Z' component of the pivot's location.
     */
    public void rotateByAxisWithPivot(float angle, float axisX, float axisY,
            float axisZ, float pivotX, float pivotY, float pivotZ) {
        getTransform().rotateByAxisWithPivot(angle, axisX, axisY, axisZ,
                                             pivotX, pivotY, pivotZ);
    }

    /**
     * Reset the widget's transform.
     * <p>
     * This will undo any translations, rotations, or scaling and reset them
     * back to default values. This is the equivalent to setting the widget's
     * transform to an identity matrix.
     */
    public void reset() {
        getTransform().reset();
    }

    /**
     * Set the visibility of the object.
     *
     * @see Visibility
     * @param visibility
     *            The visibility of the object.
     */
    public void setVisibility(final Visibility visibility) {
        if (visibility != mVisibility) {
            if (mParent != null) {
                switch (visibility) {
                    case VISIBLE:
                        mParent.getSceneObject().addChildObject(mSceneObject);
                        break;
                    case HIDDEN:
                    case GONE:
                        if (mVisibility == Visibility.VISIBLE) {
                            mParent.getSceneObject()
                                    .removeChildObject(mSceneObject);
                        }
                        break;
                    case PLACEHOLDER:
                        getSceneObject().detachRenderData();
                        break;
                }
            }
            mVisibility = visibility;
        }
    }

    /**
     * @see Visibility
     * @return The object's current visibility
     */
    public Visibility getVisibility() {
        return mVisibility;
    }

    protected Widget(final GVRContext context, final GVRMesh mesh) {
        this(context, new GVRSceneObject(context, mesh, sDefaultTexture));
    }

    /**
     * Initialize the instance on the GL thread. This method is called
     * automatically for you when the instance is
     * {@linkplain GroupWidget#addChild(Widget) attached} to another
     * {@code Widget}, but you may call it explicitly to do early
     * initialization. However many times this method is called, the creation
     * code will only be executed <em>once</em>.
     * <p>
     * Override {@link #onCreate()} to implement your GL thread initialization.
     */
    // TODO: Should this be public?
    protected final void create() {
        runOnGlThread(new Runnable() {

            @Override
            public void run() {
                if (!mIsCreated) {
                    onCreate();
                    mIsCreated = true;
                }
            }
        });
    }

    /**
     * Determine whether the calling thread is the GL thread.
     *
     * @return {@code True} if called from the GL thread, {@code false}
     *         otherwise.
     */
    protected final boolean isGLThread() {
        final Thread glThread = sGLThread.get();
        return glThread != null && glThread.equals(Thread.currentThread());
    }

    /**
     * Execute a {@link Runnable} on the GL thread. If this method is called
     * from the GL thread, the {@code Runnable} is executed immediately.
     * <p>
     * This differs from {@link GVRContext#runOnGlThread(Runnable)}: that method
     * always queues the {@code Runnable} for execution in the next frame.
     *
     * @param r
     *            {@link Runnable} to execute on the GL thread.
     */
    protected final void runOnGlThread(final Runnable r) {
        if (isGLThread()) {
            r.run();
        } else {
            getGVRContext().runOnGlThread(r);
        }
    }

    /**
     * Get the {@link GVRMaterial material} for the underlying
     * {@link GVRSceneObject scene object}.
     *
     * @return The scene object's material or {@code null}.
     */
    protected GVRMaterial getMaterial() {
        final GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            return renderData.getMaterial();
        }
        return null;
    }

    /**
     * Set the {@linkplain GVRMaterial material} for the underlying
     * {@linkplain GVRSceneObject scene object}.
     *
     * @param material
     *            The new material.
     */
    protected void setMaterial(final GVRMaterial material) {
        final GVRRenderData renderData = getRenderData();
        if (renderData != null) {
            renderData.setMaterial(material);
        }
    }

    /**
     * A hook method called after the {@code Widget} instance has been
     * {@linkplain GroupWidget#addChild(Widget) added} to another {@link Widget}
     * as a child.
     * <p>
     * <b>NOTE:</b> The order of execution between this method and
     * {@link #onCreate()} is <em>not</em> guaranteed. As a general rule, you
     * should not write code that has dependencies between this method and
     * {@code onCreate()}.
     */
    protected void onAttached() {

    }

    /**
     * A hook method for doing any initialization that must be performed on the
     * GL Thread (e.g., creation of {@link GVRBitmapTexture bitmap textures}).
     * <p>
     * If {@link #create()} has not been explicitly called, this method will be
     * called automatically when the instance is added to another {@link Widget}
     * as a child.
     * <p>
     * <b>NOTE:</b> The order of execution between the
     * {@linkplain #onAttached() attach} and {@linkplain #onDetached() detach}
     * hooks and this method is <em>not</em> guaranteed. As a general rule, you
     * should not write code that has dependencies between the attachment hooks
     * and this method!
     *
     * @see #create()
     */
    protected void onCreate() {

    }

    /**
     * A hook method called after the {@code Widget} instance has been
     * {@linkplain WidgetGroup#removeChild(Widget) removed} from another
     * {@link GroupWidget} as a child. At this point, the instance has no
     * {@linkplain #getParent() parent}.
     * <p>
     * <b>NOTE:</b> The order of execution between this method and
     * {@link #onCreate()} is <em>not</em> guaranteed. As a general rule, you
     * should not write code that has dependencies between this method and
     * {@code onCreate()}.
     */
    protected void onDetached() {

    }

    /**
     * Hook method for handling changes in focus for this object.
     *
     * @param focused
     *            {@code True} if the object has gained focus, {@code false} if
     *            it has lost focus.
     */
    protected void onFocus(boolean focused) {

    }

    /**
     * Hook method for handling long focus events. Called when the object has
     * held focus for longer than a certain period of time. This is similar to
     * {@link android.View.GestureDetector.OnGestureListener#onLongPress(MotionEvent)
     * OnGestureListener.onLongPress()}.
     */
    protected void onLongFocus() {

    }

    /**
     * ???
     */
    protected void onLayout() {

    }

    /**
     * Hook method for handling back key events.
     * 
     * @return {@code True} if the back key event was successfully processed,
     *         {@code false} otherwise.
     */
    protected boolean onBackKey() {
        return false;
    }

    /**
     * Hook method for handling touch events.
     *
     * @return {@code True} if the touch event was successfully processed,
     *         {@code false} otherwise.
     */
    protected boolean onTouch() {
        return false;
    }

    /* package */
    /**
     * <b>NOT FOR GENERAL USE!</b>
     * <p>
     * This is for use by {@link GroupWidget} <em>exclusively</em>.
     * <p>
     * Does post-{@linkplain GroupWidget#addChild(Widget) attachment} setup:
     * <ul>
     * <li>Runs GL thread {@linkplain #create() initialization}</li>
     * <li>Registers for touch and focus notifications, if they are enabled</li>
     * <li>Invokes {@link #onAttached()}
     * </ul>
     *
     * @param parent
     *            The {@link GroupWidget} this instance is being
     *            {@linkplain GroupWidget#addChild(Widget) attached} to.
     */
    synchronized final void doOnAttached(final GroupWidget parent) {
        if (parent != mParent) {
            create();
            mParent = parent;
            mIsAttached = true;
            registerPickable();
            onAttached();
        }
    }

    /* package */
    /**
     * <b>NOT FOR GENERAL USE!</b>
     * <p>
     * This is for use by {@link GroupWidget} <em>exclusively</em>.
     * <p>
     * Does post-{@linkplain GroupWidget#removeChild(Widget) detachment}
     * cleanup:
     * <ul>
     * <li>Clears parent reference</li>
     * <li>Unregisters for touch and focus notifications</li>
     * <li>Invokes {@link #onDetached()}</li>
     * </ul>
     */
    synchronized final void doOnDetached() {
        mIsAttached = false;
        mParent = null;
        registerPickable();
        onDetached();
    }

    /* package */
    /**
     * <b>NOT FOR GENERAL USE!</b>
     * <p>
     * This is for use by {@link FocusManager} <em>exclusively</em>.
     * <p>
     * Called by {@link FocusManager} when this {@link Widget} gains
     * line-of-sight focus. Notifies all
     * {@linkplain OnFocusListener#onFocus(boolean) listeners}; if none of the
     * listeners has completely handled the event, {@link #onFocus(boolean)} is
     * called.
     */
    void doOnFocus(boolean focused) {
        for (OnFocusListener listener : mFocusListeners) {
            if (listener.onFocus(focused, this)) {
                return;
            }
        }
        onFocus(focused);
    }

    /* package */
    /**
     * <b>NOT FOR GENERAL USE!</b>
     * <p>
     * This is for use by {@link FocusManager} <em>exclusively</em>.
     * <p>
     * Called by {@link FocusManager} when this {@link Widget} has had
     * line-of-sight focus for more than {@link #getLongFocusTime()}
     * milliseconds. Notifies all {@linkplain OnFocusListener#onLongFocus()
     * listeners}; if none of the listeners has completely handled the event,
     * {@link #onLongFocus()} is called.
     */
    void doOnLongFocus() {
        for (OnFocusListener listener : mFocusListeners) {
            if (listener.onLongFocus(this)) {
                return;
            }
        }
        onLongFocus();
    }

    public GVRSceneObject getSceneObject() {
        return mSceneObject;
    }

    /* package */
    GVRRenderData getRenderData() {
        return mSceneObject.getRenderData();
    }

    private boolean doOnBackKey() {
        for (OnBackKeyListener listener : mBackKeyListeners) {
            if (listener.onBackKey(this)) {
                return true;
            }
        }
        return onBackKey();
    }

    private boolean doOnTouch() {
        for (OnTouchListener listener : mTouchListeners) {
            if (listener.onTouch(this)) {
                return true;
            }
        }
        return onTouch();
    }

    private void registerPickable() {
        final TouchManager touchManager = sTouchManager.get();
        if (touchManager == null) {
            Log.e(TAG,
                  "Attempted to register widget as touchable with NULL TouchManager!");
            return;
        }

        if (mIsAttached && (mIsTouchable || mFocusEnabled)) {
            if (mIsTouchable) {
                touchManager.makeTouchable(getGVRContext(), mSceneObject,
                                           mTouchHandler);
            } else {
                TouchManager.makePickable(getGVRContext(), mSceneObject);
            }
            if (mFocusEnabled) {
                FocusManager.getInstance().register(this);
            }
        } else {
            touchManager.removeHandlerFor(mSceneObject);
            FocusManager.getInstance().unregister(this);
        }
    }

    private GVRContext getGVRContext() {
        return mContext;
    }

    /**
     * @return The {@code Widget's} {@linkplain GVRTransform transform}.
     */
    public GVRTransform getTransform() {
        return mSceneObject.getTransform();
    }

    private final GVRSceneObject mSceneObject;

    private final GVRContext mContext;

    private boolean mIsAttached;
    private boolean mIsCreated;

    private boolean mFocusEnabled = true;
    private boolean mIsFocused;
    private boolean mIsTouchable = true;
    private Visibility mVisibility = Visibility.VISIBLE;
    private GroupWidget mParent;
    private final float mBaseWidth;
    private final float mBaseHeight;
    private final float mBaseDepth;
    private float mWidth;
    private float mHeight;
    private float mDepth;
    private String mName;

    private final Set<OnBackKeyListener> mBackKeyListeners = new HashSet<OnBackKeyListener>();
    private final Set<OnFocusListener> mFocusListeners = new HashSet<OnFocusListener>();
    private final Set<OnTouchListener> mTouchListeners = new HashSet<OnTouchListener>();

    private final TouchManager.OnTouch mTouchHandler = new TouchManager.OnBackKey() {
        @Override
        public boolean touch(GVRSceneObject sceneObject) {// , float[] hit) {
            return doOnTouch();
        }
    
        @Override
        public boolean onBackKey(GVRSceneObject sceneObject) {
            return doOnBackKey();
        }
    };

    private static WeakReference<Thread> sGLThread;
    private static WeakReference<TouchManager> sTouchManager;
    private static GVRTexture sDefaultTexture;

    private static final String TAG = Widget.class.getSimpleName();
}
