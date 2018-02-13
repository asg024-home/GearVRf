package com.samsung.smcl.vr.widgetlib.widget.basic;

import org.gearvrf.scene_objects.GVRTextViewSceneObject.IntervalFrequency;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

public interface TextContainer {

    enum Properties {
        background,
        background_color,
        gravity,
        refresh_freq,
        text,
        text_color,
        text_size,
        typeface
    }

    Drawable getBackGround();

    int getBackgroundColor();

    int getGravity();

    IntervalFrequency getRefreshFrequency();

    CharSequence getText();

    int getTextColor();

    float getTextSize();

    Typeface getTypeface();

    String getTextString();

    void setBackGround(Drawable drawable);

    void setBackgroundColor(int color);

    void setGravity(int gravity);

    void setRefreshFrequency(IntervalFrequency frequency);

    void setText(CharSequence text);

    void setTextColor(int color);

    void setTextSize(float size);

    void setTypeface(Typeface typeface);
}
