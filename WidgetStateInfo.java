package com.samsung.smcl.vr.widgets;

import java.io.IOException;
import java.util.Iterator;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRMaterial.GVRShaderType;
import org.json.JSONException;
import org.json.JSONObject;

import com.samsung.smcl.utility.Exceptions;
import com.samsung.smcl.utility.Log;
import com.samsung.smcl.utility.Utility;
import com.samsung.smcl.vr.gvrf_launcher.util.Helpers;
import com.samsung.smcl.vr.widgets.Widget.Visibility;

class WidgetStateInfo {
    public enum Properties {
        scene_object, material, animation, id
    }

    public WidgetStateInfo(Widget parent, JSONObject info)
            throws JSONException, NoSuchMethodException, IOException {
        Widget levelWidget = null;
        GVRMaterial material = null;
        AnimationFactory.Factory factory = null;

        Iterator<String> iter = info.keys();
        while (iter.hasNext()) {
            final String type = iter.next();
            Log.d(TAG, "WidgetStateInfo(%s): type: %s", parent.getName(), type);

            final JSONObject typeInfo = info.optJSONObject(type);
            switch (Properties.valueOf(type)) {
                case scene_object:
                    levelWidget = getWidget(parent, typeInfo);
                    break;
                case material:
                    material = getMaterial(parent, typeInfo);
                    break;
                case animation:
                    factory = getAnimationFactory(typeInfo);
                    break;
                default:
                    throw new RuntimeException();
            }
        }

        mLevelWidget = levelWidget;
        mAnimationFactory = factory;
        mMaterial = material;
    }

    public void set(Widget widget, boolean set) {
        if (set) {
            Log.d(TAG, "set(%s): setting state ...", widget.getName());
            if (mLevelWidget != null) {
                Log.d(TAG, "set(%s): setting level widget %s", widget.getName(), mLevelWidget);
                // We shouldn't have to do this, but it's not a
                // thread-safe operation and in some instances the
                // geometry will render after it's been removed from
                // it's parent, which can result in flashes when the
                // geometry is rendered in the wrong location.
                widget.runOnGlThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: Pull out of GL thread
                        mLevelWidget.setVisibility(Visibility.VISIBLE);
                    }
                });
            }
            if (mMaterial != null) {
                Log.d(TAG, "set(%s): setting material ...", widget.getName(), mMaterial);
                widget.setMaterial(mMaterial);
            }
            if (mAnimation != null) {
                mAnimation.finish();
            }
            if (mAnimationFactory != null) {
                try {
                    Log.d(TAG, "set(%s): setting animation ...", widget.getName());
                    mAnimation = mAnimationFactory.create(widget);
                    mAnimation.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e, "set()");
                }
            }
        } else {
            if (mAnimation != null) {
                mAnimation.finish();
            }
            if (mLevelWidget != null) {
                widget.runOnGlThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: Pull out of GL thread
                        mLevelWidget.setVisibility(Visibility.HIDDEN);
                    }
                });
            }
        }
    }

    // TODO: Handle indexing into animations already on the object from the model
    private AnimationFactory.Factory getAnimationFactory(
            final JSONObject animationSpec) throws JSONException,
            NoSuchMethodException {
        if (animationSpec.has("duration")) {
            Log.d(TAG, "getAnimationFactory(): making factory for spec: %s", animationSpec);
            return AnimationFactory.makeFactory(animationSpec);
        } else if (animationSpec.has("id")) {
            final String id = animationSpec.getString("id");
            Log.d(TAG, "getAnimationFactory(): getting factory for '%s'", id);
            return AnimationFactory.getFactory(id);
        } else {
            throw Exceptions.RuntimeAssertion("Invalid animation spec: %s",
                                              animationSpec);
        }
    }

    private enum MaterialProperties {
        shader_type, main_texture, textures, color, ambient_color, diffuse_color, specular_color, specular_exponent, opacity
    }

    private GVRMaterial getMaterial(Widget widget, JSONObject materialSpec)
            throws JSONException, IOException {
        final GVRContext gvrContext = widget.getGVRContext();
        return getMaterial(gvrContext, materialSpec);
    }

    // TODO: MaterialFactory
    static private GVRMaterial getMaterial(final GVRContext gvrContext,
            JSONObject materialSpec) throws JSONException, IOException {
        GVRMaterial material = new GVRMaterial(gvrContext);

        final Iterator<String> iter = materialSpec.keys();
        while (iter.hasNext()) {
            final String key = iter.next();
            switch (MaterialProperties.valueOf(key)) {
                case shader_type:
                    final String shaderType = materialSpec.getString(key);
                    if (shaderType.equalsIgnoreCase("texture")) {
                        material.setShaderType(GVRShaderType.Texture.ID);
                    } else if (shaderType.equalsIgnoreCase("cubemap")) {
                        material.setShaderType(GVRShaderType.Cubemap.ID);
                    } else {
                        throw Exceptions
                                .RuntimeAssertion("Unsupported shader type '%s' specified for state",
                                                  shaderType);
                    }
                    break;
                case color:
                    final float[] color = Helpers.getJSONColorGl(materialSpec,
                                                               key);
                    material.setColor(color[0], color[1], color[2]);
                    break;
                case ambient_color:
                    final float[] ambientColor = Helpers
                            .getJSONColorGl(materialSpec, key);
                    material.setAmbientColor(ambientColor[0], ambientColor[1],
                                             ambientColor[2], ambientColor[3]);
                    break;
                case diffuse_color:
                    final float[] diffuseColor = Helpers
                            .getJSONColorGl(materialSpec, key);
                    material.setDiffuseColor(diffuseColor[0], diffuseColor[1],
                                             diffuseColor[2], diffuseColor[3]);
                    break;
                case specular_color:
                    final float[] specularColor = Helpers
                            .getJSONColorGl(materialSpec, key);
                    material.setSpecularColor(specularColor[0],
                                              specularColor[1],
                                              specularColor[2],
                                              specularColor[3]);
                    break;
                case specular_exponent:
                    material.setSpecularExponent((float) materialSpec
                            .getDouble(key));
                    break;
                case opacity:
                    material.setOpacity((float) materialSpec.getDouble(key));
                    break;
            }
        }

        TextureFactory.loadMaterialTextures(material, materialSpec);

        return material;
    }

    private Widget getWidget(Widget parent, JSONObject stateSpec)
            throws JSONException {
        Widget levelWidget;
        String id = JSONHelpers.getString(stateSpec, Properties.id);
        levelWidget = parent.findChildByName(id);
        if (levelWidget == null) {
            throw Exceptions
                    .RuntimeAssertion("State widget '%s' not found", id);
        }
        Log.d(TAG, "getWidget(): got state widget '%s'", id);
        levelWidget.setVisibility(Visibility.HIDDEN);
        levelWidget.setFollowParentFocus(true);
        levelWidget.setFollowParentInput(true);
        levelWidget.setChildrenFollowFocus(true);
        levelWidget.setChildrenFollowInput(true);
        return levelWidget;
    }

    final private Widget mLevelWidget;
    final private AnimationFactory.Factory mAnimationFactory;
    final private GVRMaterial mMaterial;
    private Animation mAnimation;

    private static final String TAG = Utility.tag(WidgetStateInfo.class);
}
