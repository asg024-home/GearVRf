package com.samsung.smcl.vr.widgets;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.samsung.smcl.utility.Log;

public class AnimationFactory {
    // @formatter:off
    public enum Type {
        COLOR,
        OPACITY,
        POSITION,
        RELATIVE_MOTION,
        ROTATION_BY_AXIS,
        ROTATION_BY_AXIS_WITH_PIVOT,
        SCALE
    }
    // @formatter:on

    public enum Properties { type }

    public static class Factory {
        public Factory(final JSONObject animSpec,
                Class<? extends Animation> animClass)
                throws NoSuchMethodException {
            Log.d(TAG, "Factory(): creating factory for '%s': %s",
                  animClass.getSimpleName(), animSpec);
            mAnimSpec = animSpec;
            mCtor = animClass.getConstructor(Widget.class, JSONObject.class);
        }

        public Animation create(final Widget target) {
            try {
                return mCtor.newInstance(target, mAnimSpec);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getLocalizedMessage(), e);
            }
        }

        private final Constructor<? extends Animation> mCtor;
        private final JSONObject mAnimSpec;
    }

    static public Animation createAnimation(final String animationName,
            final Widget target) throws InstantiationException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Factory factory = sFactoryMap.get(animationName);
        return factory.create(target);
    }

    static public Factory getFactory(final String animationName) {
        return sFactoryMap.get(animationName);
    }

    /* package */
    static void init(final JSONObject animationMetadata) throws JSONException,
            NoSuchMethodException {
        Factory factory;
        JSONObject animSpec;
        String key;

        if (animationMetadata != null) {
            Iterator<String> iter = animationMetadata.keys();
            while (iter.hasNext()) {
                key = iter.next();
                if (!sFactoryMap.containsKey(key)) {
                    animSpec = animationMetadata.getJSONObject(key);
                    Log.d(TAG, "init(): creating factory for '%s': %s", key,
                          animSpec);
                    factory = makeFactory(animSpec);
                    sFactoryMap.put(key, factory);
                }
            }
        }
    }

    public static Factory makeFactory(final JSONObject animSpec) {
        final Factory factory;
        try {
            String type = animSpec.getString("type").toUpperCase(Locale.ENGLISH);
            Log.d(TAG, "makeFactory(): making factory for '%s': %s", type, animSpec);
            switch (Type.valueOf(type)) {
                case COLOR:
                    factory = new Factory(animSpec, ColorAnimation.class);
                    break;
                case OPACITY:
                    factory = new Factory(animSpec, OpacityAnimation.class);
                    break;
                case POSITION:
                    factory = new Factory(animSpec, PositionAnimation.class);
                    break;
                case RELATIVE_MOTION:
                    factory = new Factory(animSpec, RelativeMotionAnimation.class);
                    break;
                case ROTATION_BY_AXIS:
                    factory = new Factory(animSpec, RotationByAxisAnimation.class);
                    break;
                case ROTATION_BY_AXIS_WITH_PIVOT:
                    factory = new Factory(animSpec,
                            RotationByAxisWithPivotAnimation.class);
                    break;
                case SCALE:
                    factory = new Factory(animSpec, ScaleAnimation.class);
                    break;
                default:
                    throw new RuntimeException("Invalid animation type specified: "
                            + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
        return factory;
    }

    private static Map<String, Factory> sFactoryMap = new HashMap<String, Factory>();
    private final static String TAG = AnimationFactory.class.getSimpleName();
}
