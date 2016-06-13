package com.samsung.smcl.vr.widgets;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRDrawFrameListener;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRPicker.GVRPickedObject;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;

import android.app.Activity;

import com.samsung.smcl.utility.Log;
import com.samsung.smcl.vr.gvrf_launcher.Holder;
import com.samsung.smcl.vr.gvrf_launcher.HolderHelper;
import com.samsung.smcl.vr.gvrf_launcher.MainThread;

/**
 * A class for tracking line-of-sight focus for {@link Widget} instances. In
 * addition to notifying gain and loss of focus, also manages "long focus".
 * "Long focus" is similar to "long press" and occurs when an object has held
 * line-of-sight focus for {@link #LONG_FOCUS_TIMEOUT} milliseconds or longer.
 * The long focus timeout is reset each time an object gains focus and is
 * stopped entirely when no object has line-of-sight focus.
 */
public class FocusManager {
    public interface Focusable {
        boolean isFocusEnabled();
        boolean onFocus(boolean focused);
        void onLongFocus();
    }

    public interface FocusInterceptor {
        boolean onFocus(GVRSceneObject sceneObject);
    }

    interface LongFocusTimeout {
        long getLongFocusTimeout();
    }

    static public FocusManager get(Activity activity) {
        return ((Holder) activity).get(FocusManager.class);
    }

    static public FocusManager get(GVRContext gvrContext) {
        FocusManager focusManager = null;
        if (gvrContext != null) {
            Activity activity = gvrContext.getActivity();
            focusManager = get(activity);
        }
        return focusManager;
    }

    /**
     * Creates FocusManager
     * An instance of {@link Holder} must be supplied and can only be associated
     * with one {@link FocusManager}. If the supplied {@code Holder} instance has
     * already been initialized, an {@link IllegalArgumentException} will be
     * thrown.
     *
     * @param holder
     *            An {@link Activity} that implements {@link Holder}.
     * @throws IllegalArgumentException
     *             if {@code holder} is {@code null} or is already holding
     *             another instance of {@code FocusManager}.
     */
    public <T extends Activity & Holder> FocusManager(T holder) {
        HolderHelper.register(holder, this);
    }


    /**
     * The focus manager will not hold strong references to the sceneObject and the
     * focusable.
     * @param sceneObject
     * @param focusable
     */
    public void register(final GVRSceneObject sceneObject, final Focusable focusable) {
        log("register sceneObject %s , focusable = %s", sceneObject.getName(), focusable);
        mFocusableMap.put(sceneObject, new WeakReference<Focusable>(focusable));
    }

    public void unregister(final GVRSceneObject sceneObject) {
        unregister(sceneObject, false);
    }

    void unregister(final GVRSceneObject sceneObject,
            final boolean softUnregister) {
        log("unregister sceneObject %s", sceneObject.getName());
        final WeakReference<Focusable> focusableRef = mFocusableMap
                .remove(sceneObject);
        if (focusableRef != null) {
            final boolean allowRelease = !softUnregister
                    || !containsFocusable(focusableRef);
            MainThread.get(mContext).runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (allowRelease && mCurrentFocus == focusableRef.get()) {
                        releaseCurrentFocus();
                    }
                }
            });
        }
    }

    void init(GVRContext context) {
        if (mContext == null) {
            mContext = context;
            mContext.registerDrawFrameListener(mDrawFrameListener);
        }
    }

    public void clear() {
        if (mContext != null) {
            mContext.unregisterDrawFrameListener(mDrawFrameListener);
        }
    }

    private boolean containsFocusable(
            final WeakReference<Focusable> focusableRef) {
        final Focusable focusable = focusableRef.get();
        for (WeakReference<Focusable> ref : mFocusableMap.values()) {
            final Focusable f = ref.get();
            if (f != null && f == focusable) {
                return true;
            }
        }
        return false;
    }

    private void cancelLongFocusRunnable() {
        MainThread.get(mContext).removeCallbacks(mLongFocusRunnable);
    }

    private void postLongFocusRunnable(long timeout) {
        if (mCurrentFocus != null) {
            MainThread.get(mContext).runOnMainThreadDelayed(mLongFocusRunnable,
                                                            timeout);
        }
    }

    private GVRDrawFrameListener mDrawFrameListener = new GVRDrawFrameListener() {
        @Override
        public void onDrawFrame(float frameTime) {
            MainThread.get(mContext).runOnMainThread(mFocusRunnable);
        }
    };

    private FocusInterceptor focusInterceptor;

    public void setFocusInterceptor(FocusInterceptor interceptor) {
        focusInterceptor = interceptor;
    }

    private final Runnable mFocusRunnable = new Runnable() {
        @Override
        public void run() {
            final GVRScene mainScene = mContext.getMainScene();
            final List<GVRPickedObject> pickedObjectList = GVRPicker
                    .findObjects(mainScene, 0, 0, 0, 0, 0, -1.0f);

            // release old focus
            if (pickedObjectList == null || pickedObjectList.isEmpty()) {
                if (mCurrentFocus != null) {
                    log("onDrawFrame(): empty/null pick list; releasing current focus (%s)",
                        mCurrentFocusName);
                }
                releaseCurrentFocus();
                return;
            }

            Focusable focusable = null;
            for (GVRPickedObject picked : pickedObjectList) {
                final GVRSceneObject quad = picked.getHitObject();
                if (quad != null) {
                    if (!compareCurrentFocusName(quad)) {
                        log("onDrawFrame(): checking '%s' for focus",
                            quad.getName());
                    }
                    WeakReference<Focusable> ref = mFocusableMap.get(quad);
                    if (null != ref) {
                        focusable = ref.get();
                    } else {
                        mFocusableMap.remove(quad);
                        focusable = null;
                    }
                }

                // already has a focus - do nothing
                if (mCurrentFocus != null && mCurrentFocus == focusable) {
                    log("onDrawFrame(): already has focus (%s)",
                        quad != null ? quad.getName() : "<null>");
                    break;
                }

                if (null == focusable || !focusable.isFocusEnabled()) {
                    continue;
                }

                releaseCurrentFocus();

                if (takeNewFocus(quad, focusable)) {
                    mCurrentFocusName = quad.getName();
                    log("onDrawFrame(): '%s' took focus", mCurrentFocusName);
                    break;
                }
            }

            if (mCurrentFocus != null && focusable != mCurrentFocus) {
                log("onDrawFrame(): no eligible focusable found! (%s)", mCurrentFocusName);
                releaseCurrentFocus();
            }
        }

        private boolean compareCurrentFocusName(final GVRSceneObject quad) {
            final String quadName = quad.getName();
            return (mCurrentFocusName == null && quadName == null)
                    || (mCurrentFocusName != null && mCurrentFocusName
                            .equalsIgnoreCase(quadName));
        }
    };

    private boolean releaseCurrentFocus() {
        boolean ret = true;
        if (mCurrentFocus != null) {
            log("releaseCurrentFocus(): releasing focus for '%s'", mCurrentFocusName);
            cancelLongFocusRunnable();
            ret = mCurrentFocus.onFocus(false);
            mCurrentFocus = null;
            mCurrentFocusName = null;
        }
        return ret;
    }

    private boolean takeNewFocus(final GVRSceneObject quad, final Focusable newFocusable) {
        boolean ret = false;
        if (newFocusable != null &&
                newFocusable.isFocusEnabled()) {

            if (focusInterceptor != null) {
                ret = focusInterceptor.onFocus(quad);
            } else {
                ret = newFocusable.onFocus(true);
            }

            if (ret) {
                mCurrentFocus = newFocusable;
                final long longFocusTimeout;
                if (newFocusable instanceof LongFocusTimeout) {
                    longFocusTimeout = ((LongFocusTimeout) newFocusable)
                            .getLongFocusTimeout();
                } else {
                    longFocusTimeout = LONG_FOCUS_TIMEOUT;
                }
                postLongFocusRunnable(longFocusTimeout);
            }
        }
        return ret;
    }

    private void log(final String msg, Object...args) {
        if (LOGGING_ENABLED) {
            Log.d(TAG, msg, args);
        }
    }

    private GVRContext mContext;
    private Focusable mCurrentFocus = null;
    private String mCurrentFocusName = "";
    private Map<GVRSceneObject, WeakReference<Focusable>> mFocusableMap = new WeakHashMap<GVRSceneObject, WeakReference<Focusable>>();

    private final Runnable mLongFocusRunnable = new Runnable() {
        
        @Override
        public void run() {
            if (mCurrentFocus != null) {
                mCurrentFocus.onLongFocus();
            }
        }
    };

    private static final boolean LOGGING_ENABLED = false;
    static final int LONG_FOCUS_TIMEOUT = 5000;

    @SuppressWarnings("unused")
    private static final String TAG = FocusManager.class.getSimpleName();
}
