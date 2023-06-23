/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.battery;

import static com.android.systemui.DejankUtils.whitelistIpcs;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.IntDef;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;

import com.android.settingslib.graph.CircleBatteryDrawable;
import com.android.settingslib.graph.FullCircleBatteryDrawable;
import com.android.settingslib.graph.LandscapeBatteryDrawable;
import com.android.settingslib.graph.RLandscapeBatteryDrawable;
import com.android.settingslib.graph.ThemedBatteryDrawable;
import com.android.settingslib.graph.LandscapeBatteryA;
import com.android.settingslib.graph.LandscapeBatteryC;
import com.android.settingslib.graph.LandscapeBatteryH;
import com.android.settingslib.graph.LandscapeBatteryI;
import com.android.settingslib.graph.LandscapeBatteryK;
import com.android.settingslib.graph.LandscapeBatteryL;
import com.android.settingslib.graph.LandscapeBatteryM;
import com.android.systemui.DualToneHandler;
import com.android.systemui.R;
import com.android.systemui.animation.Interpolators;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.policy.BatteryController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.text.NumberFormat;
import java.util.ArrayList;

public class BatteryMeterView extends LinearLayout implements DarkReceiver {

    @Retention(SOURCE)
    @IntDef({MODE_DEFAULT, MODE_ON, MODE_OFF, MODE_ESTIMATE})
    public @interface BatteryPercentMode {}
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_ON = 1;
    public static final int MODE_OFF = 2;
    public static final int MODE_ESTIMATE = 3;

    public static final int BATTERY_STYLE_PORTRAIT = 0;
    public static final int BATTERY_STYLE_CIRCLE = 1;
    public static final int BATTERY_STYLE_DOTTED_CIRCLE = 2;
    public static final int BATTERY_STYLE_FULL_CIRCLE = 3;
    public static final int BATTERY_STYLE_TEXT = 4; /*hidden icon*/
    public static final int BATTERY_STYLE_HIDDEN = 5;
    public static final int BATTERY_STYLE_BIG_CIRCLE = 6;
    public static final int BATTERY_STYLE_BIG_DOTTED_CIRCLE = 7;
    public static final int BATTERY_STYLE_RLANDSCAPE = 8;
    public static final int BATTERY_STYLE_LANDSCAPE = 9;
    protected static final int BATTERY_STYLE_LANDSCAPEA = 10;
    protected static final int BATTERY_STYLE_LANDSCAPEC = 11;
    protected static final int BATTERY_STYLE_LANDSCAPEH = 12;
    protected static final int BATTERY_STYLE_LANDSCAPEI = 13;
    protected static final int BATTERY_STYLE_LANDSCAPEK = 14;
    protected static final int BATTERY_STYLE_LANDSCAPEL = 15;
    protected static final int BATTERY_STYLE_LANDSCAPEM = 16;

    public static final int BATTERY_PERCENT_HIDDEN = 0;
    public static final int BATTERY_PERCENT_SHOW_INSIDE = 1;
    public static final int BATTERY_PERCENT_SHOW_OUTSIDE = 2;

    private final CircleBatteryDrawable mCircleDrawable;
    private final FullCircleBatteryDrawable mFullCircleDrawable;
    private final ThemedBatteryDrawable mThemedDrawable;
    private final RLandscapeBatteryDrawable mRLandscapeDrawable;
    private final LandscapeBatteryDrawable mLandscapeDrawable;
    private final LandscapeBatteryA mLandscapeBatteryA;
    private final LandscapeBatteryC mLandscapeBatteryC;
    private final LandscapeBatteryH mLandscapeBatteryH;
    private final LandscapeBatteryI mLandscapeBatteryI;
    private final LandscapeBatteryK mLandscapeBatteryK;
    private final LandscapeBatteryL mLandscapeBatteryL;
    private final LandscapeBatteryM mLandscapeBatteryM;
    private final ImageView mBatteryIconView;
    private TextView mBatteryPercentView;

    private final @StyleRes int mPercentageStyleId;
    private int mTextColor;
    private int mLevel;
    private int mShowPercentMode = MODE_DEFAULT;
    // Error state where we know nothing about the current battery state
    private boolean mBatteryStateUnknown;
    // Lazily-loaded since this is expected to be a rare-if-ever state
    private Drawable mUnknownStateDrawable;
    private boolean mShowBatteryEstimate;

    private boolean mCharging;
    public int mBatteryStyle = BATTERY_STYLE_PORTRAIT;
    public int mShowBatteryPercent;

    private DualToneHandler mDualToneHandler;

    private boolean mIsQsHeader;

    private final ArrayList<BatteryMeterViewCallbacks> mCallbacks = new ArrayList<>();

    private int mNonAdaptedSingleToneColor;
    private int mNonAdaptedForegroundColor;
    private int mNonAdaptedBackgroundColor;

    private BatteryEstimateFetcher mBatteryEstimateFetcher;

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        final int frameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(R.color.meter_background_color));
        mPercentageStyleId = atts.getResourceId(R.styleable.BatteryMeterView_textAppearance, 0);
        mThemedDrawable = new ThemedBatteryDrawable(context, frameColor);
        mRLandscapeDrawable = new RLandscapeBatteryDrawable(context, frameColor);
        mLandscapeDrawable = new LandscapeBatteryDrawable(context, frameColor);
        mCircleDrawable = new CircleBatteryDrawable(context, frameColor);
        mFullCircleDrawable = new FullCircleBatteryDrawable(context, frameColor);
        mLandscapeBatteryA = new LandscapeBatteryA(context, frameColor);
        mLandscapeBatteryC = new LandscapeBatteryC(context, frameColor);
        mLandscapeBatteryH = new LandscapeBatteryH(context, frameColor);
        mLandscapeBatteryI = new LandscapeBatteryI(context, frameColor);
        mLandscapeBatteryK = new LandscapeBatteryK(context, frameColor);
        mLandscapeBatteryL = new LandscapeBatteryL(context, frameColor);
        mLandscapeBatteryM = new LandscapeBatteryM(context, frameColor);
        atts.recycle();

        setupLayoutTransition();

        mBatteryIconView = new ImageView(context);
        mBatteryIconView.setImageDrawable(mThemedDrawable);
        final MarginLayoutParams mlp = new MarginLayoutParams(
                getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_width),
                getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_height));
        mlp.setMargins(0, 0, 0,
                getResources().getDimensionPixelOffset(R.dimen.battery_margin_bottom));
        addView(mBatteryIconView, mlp);

        updateShowPercent();
        mDualToneHandler = new DualToneHandler(context);
        // Init to not dark at all.
        onDarkChanged(new Rect(), 0, DarkIconDispatcher.DEFAULT_ICON_TINT);

        setClipChildren(false);
        setClipToPadding(false);
    }

    private void setupLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(200);

        ObjectAnimator appearAnimator = ObjectAnimator.ofFloat(null, "alpha", 0f, 1f);
        transition.setAnimator(LayoutTransition.APPEARING, appearAnimator);
        transition.setInterpolator(LayoutTransition.APPEARING, Interpolators.ALPHA_IN);

        ObjectAnimator disappearAnimator = ObjectAnimator.ofFloat(null, "alpha", 1f, 0f);
        transition.setInterpolator(LayoutTransition.DISAPPEARING, Interpolators.ALPHA_OUT);
        transition.setAnimator(LayoutTransition.DISAPPEARING, disappearAnimator);

        setLayoutTransition(transition);
    }

    public void setForceShowPercent(boolean show) {
        setPercentShowMode(show ? MODE_ON : MODE_DEFAULT);
    }

    /**
     * Force a particular mode of showing percent
     *
     * 0 - No preference
     * 1 - Force on
     * 2 - Force off
     * 3 - Estimate
     * @param mode desired mode (none, on, off)
     */
    public void setPercentShowMode(@BatteryPercentMode int mode) {
        if (mode == mShowPercentMode) return;
        mShowPercentMode = mode;
        updateShowPercent();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updatePercentView();
    }

    public void setColorsFromContext(Context context) {
        if (context == null) {
            return;
        }

        mDualToneHandler.setColorsFromContext(context);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    void onBatteryLevelChanged(int level, boolean pluggedIn) {
        if (mLevel != level) {
            mLevel = level;
            mThemedDrawable.setBatteryLevel(mLevel);
            mRLandscapeDrawable.setBatteryLevel(mLevel);
            mLandscapeDrawable.setBatteryLevel(mLevel);
            mCircleDrawable.setBatteryLevel(mLevel);
            mFullCircleDrawable.setBatteryLevel(mLevel);
            mLandscapeBatteryA.setBatteryLevel(level);
            mLandscapeBatteryC.setBatteryLevel(level);
            mLandscapeBatteryH.setBatteryLevel(level);
            mLandscapeBatteryI.setBatteryLevel(level);
            mLandscapeBatteryK.setBatteryLevel(level);
            mLandscapeBatteryL.setBatteryLevel(level);
            mLandscapeBatteryM.setBatteryLevel(level);
        }
        if (mCharging != pluggedIn) {
            mCharging = pluggedIn;
            mThemedDrawable.setCharging(mCharging);
            mRLandscapeDrawable.setCharging(mCharging);
            mLandscapeDrawable.setCharging(mCharging);
            mCircleDrawable.setCharging(mCharging);
            mFullCircleDrawable.setCharging(mCharging);
            mLandscapeBatteryA.setCharging(mCharging);
            mLandscapeBatteryC.setCharging(mCharging);
            mLandscapeBatteryH.setCharging(mCharging);
            mLandscapeBatteryI.setCharging(mCharging);
            mLandscapeBatteryK.setCharging(mCharging);
            mLandscapeBatteryL.setCharging(mCharging);
            mLandscapeBatteryM.setCharging(mCharging);
            updateShowPercent();
        } else {
            updatePercentText();
        }
    }

    void onPowerSaveChanged(boolean isPowerSave) {
        mThemedDrawable.setPowerSaveEnabled(isPowerSave);
        mRLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
        mCircleDrawable.setPowerSaveEnabled(isPowerSave);
        mFullCircleDrawable.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryA.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryC.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryH.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryI.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryK.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryL.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryM.setPowerSaveEnabled(isPowerSave);
        updateShowPercent();
    }

    private TextView loadPercentView() {
        return (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.battery_percentage_view, null);
    }

    /**
     * Updates percent view by removing old one and reinflating if necessary
     */
    public void updatePercentView() {
        updateShowPercent();
    }

    /**
     * Sets the fetcher that should be used to get the estimated time remaining for the user's
     * battery.
     */
    void setBatteryEstimateFetcher(BatteryEstimateFetcher fetcher) {
        mBatteryEstimateFetcher = fetcher;
    }

    void updatePercentText() {
        if (mBatteryStateUnknown) {
            setContentDescription(getContext().getString(R.string.accessibility_battery_unknown));
            return;
        }

        if (mBatteryEstimateFetcher == null) {
            return;
        }

        if (mBatteryPercentView != null) {
            if (mShowPercentMode == MODE_ESTIMATE && !mCharging && mShowBatteryEstimate) {
                mBatteryEstimateFetcher.fetchBatteryTimeRemainingEstimate(
                        (String estimate) -> {
                    if (mBatteryPercentView == null) {
                        return;
                    }
                    if (estimate != null) {
                        if (mBatteryPercentView != null) {
                            batteryPercentViewSetText(estimate);
                        }
                        setContentDescription(getContext().getString(
                                R.string.accessibility_battery_level_with_estimate,
                                mLevel, estimate));
                    } else {
                        setPercentTextAtCurrentLevel();
                    }
                });
            } else {
                setPercentTextAtCurrentLevel();
            }
        } else {
            setContentDescription(
                    getContext().getString(mCharging ? R.string.accessibility_battery_level_charging
                            : R.string.accessibility_battery_level, mLevel));
        }
    }

    private void setPercentTextAtCurrentLevel() {
        if (mBatteryPercentView != null) {
            // Use the high voltage symbol ⚡ (u26A1 unicode) but prevent the system
            // to load its emoji colored variant with the uFE0E flag
            String bolt = "\u26A1\uFE0E";
            CharSequence mChargeIndicator = mCharging && (mBatteryStyle == BATTERY_STYLE_TEXT)
                ? (bolt + " ") : "";
            batteryPercentViewSetText(mChargeIndicator +
                NumberFormat.getPercentInstance().format(mLevel / 100f));
            setContentDescription(
                    getContext().getString(mCharging ? R.string.accessibility_battery_level_charging
                            : R.string.accessibility_battery_level, mLevel));
        }
    }

    private void removeBatteryPercentView() {
        if (mBatteryPercentView != null) {
            removeView(mBatteryPercentView);
            mBatteryPercentView = null;
        }
    }

    void updateShowPercent() {
        final boolean showing = mBatteryPercentView != null;
        final boolean drawPercentInside = mShowPercentMode == MODE_DEFAULT &&
                mShowBatteryPercent == BATTERY_PERCENT_SHOW_INSIDE;
        final boolean drawPercentOnly = mShowPercentMode == MODE_ESTIMATE ||
                mShowPercentMode == MODE_ON || mShowBatteryPercent == BATTERY_PERCENT_SHOW_OUTSIDE;
        if (!(!mIsQsHeader && mBatteryStyle == BATTERY_STYLE_HIDDEN)
                && drawPercentOnly && (!drawPercentInside || mCharging)) {
            mThemedDrawable.setShowPercent(false);
            mRLandscapeDrawable.setShowPercent(false);
            mLandscapeDrawable.setShowPercent(false);
            mCircleDrawable.setShowPercent(false);
            mFullCircleDrawable.setShowPercent(false);
            if (!showing) {
                mBatteryPercentView = loadPercentView();
                if (mPercentageStyleId != 0) { // Only set if specified as attribute
                    mBatteryPercentView.setTextAppearance(mPercentageStyleId);
                }
                if (mTextColor != 0) mBatteryPercentView.setTextColor(mTextColor);
                addView(mBatteryPercentView,
                        new ViewGroup.LayoutParams(
                                LayoutParams.WRAP_CONTENT,
                                LayoutParams.MATCH_PARENT));
            }
            if (mBatteryStyle == BATTERY_STYLE_TEXT) {
                mBatteryPercentView.setPaddingRelative(0, 0, 0, 0);
            } else {
                Resources res = getContext().getResources();
                mBatteryPercentView.setPaddingRelative(
                        res.getDimensionPixelSize(R.dimen.battery_level_padding_start), 0, 0, 0);
            }
        } else {
            removeBatteryPercentView();
            mThemedDrawable.setShowPercent(drawPercentInside);
            mRLandscapeDrawable.setShowPercent(drawPercentInside);
            mLandscapeDrawable.setShowPercent(drawPercentInside);
            mCircleDrawable.setShowPercent(drawPercentInside);
            mFullCircleDrawable.setShowPercent(drawPercentInside);
            mLandscapeBatteryA.setShowPercent(drawPercentInside);
            mLandscapeBatteryC.setShowPercent(drawPercentInside);
            mLandscapeBatteryH.setShowPercent(drawPercentInside);
            mLandscapeBatteryI.setShowPercent(drawPercentInside);
            mLandscapeBatteryK.setShowPercent(drawPercentInside);
            mLandscapeBatteryL.setShowPercent(drawPercentInside);
            mLandscapeBatteryM.setShowPercent(drawPercentInside);
        }
        updatePercentText();
    }

    public void setIsQsHeader(boolean isQs) {
        mIsQsHeader = isQs;
    }

    public void updateVisibility() {
        if (mBatteryStyle == BATTERY_STYLE_TEXT || mBatteryStyle == BATTERY_STYLE_HIDDEN) {
            mBatteryIconView.setVisibility(View.GONE);
            mBatteryIconView.setImageDrawable(null);
            //setVisibility(View.GONE);
        } else {
            mBatteryIconView.setVisibility(View.VISIBLE);
            //setVisibility(View.VISIBLE);
            scaleBatteryMeterViews();
        }
    }

    private void batteryPercentViewSetText(CharSequence text) {
        CharSequence currentText = mBatteryPercentView.getText();
        if (!currentText.toString().equals(text.toString())) {
            mBatteryPercentView.setText(text);
        }
    }

    private Drawable getUnknownStateDrawable() {
        if (mUnknownStateDrawable == null) {
            mUnknownStateDrawable = mContext.getDrawable(R.drawable.ic_battery_unknown);
            mUnknownStateDrawable.setTint(mTextColor);
        }

        return mUnknownStateDrawable;
    }

    void onBatteryUnknownStateChanged(boolean isUnknown) {
        if (mBatteryStateUnknown == isUnknown) {
            return;
        }

        mBatteryStateUnknown = isUnknown;

        if (mBatteryStateUnknown) {
            mBatteryIconView.setImageDrawable(getUnknownStateDrawable());
        } else {
            mBatteryIconView.setImageDrawable(mThemedDrawable);
        }

        updateShowPercent();
    }

    public void onOverlayChanged() {
        updateShowPercent();
        updateSettings();
    }

    /**
     * Looks up the scale factor for status bar icons and scales the battery view by that amount.
     */
    void scaleBatteryMeterViews() {
        if (mBatteryIconView == null) {
            return;
        }

        Resources res = getContext().getResources();
        TypedValue typedValue = new TypedValue();

        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float iconScaleFactor = typedValue.getFloat();

        int batteryHeight = mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE
                || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width) :
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
        batteryHeight = mBatteryStyle == BATTERY_STYLE_BIG_CIRCLE || mBatteryStyle == BATTERY_STYLE_BIG_DOTTED_CIRCLE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_bigcircle_icon_height) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPEA ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEC ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEH ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEI ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEK ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEL ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEM ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_a_o) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape) : batteryHeight;
        int batteryWidth = mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE
                || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width) :
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);
        batteryWidth = mBatteryStyle == BATTERY_STYLE_BIG_CIRCLE || mBatteryStyle == BATTERY_STYLE_BIG_DOTTED_CIRCLE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_bigcircle_icon_width) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPEA ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEC ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEH ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEI ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEK ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEL ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEM ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_a_o) : batteryWidth;
        int marginBottom = res.getDimensionPixelSize(R.dimen.battery_margin_bottom);

        LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
        scaledLayoutParams.setMargins(0, 0, 0, marginBottom);

        if (mBatteryIconView != null) {
            mBatteryIconView.setLayoutParams(scaledLayoutParams);
        }
    }

    public void updateBatteryStyle() {
        switch (mBatteryStyle) {
            case BATTERY_STYLE_TEXT:
            case BATTERY_STYLE_HIDDEN:
            break;
            case BATTERY_STYLE_PORTRAIT:
            mBatteryIconView.setImageDrawable(mThemedDrawable);
            break;
            case BATTERY_STYLE_RLANDSCAPE:
            mBatteryIconView.setImageDrawable(mRLandscapeDrawable);
            break;
            case BATTERY_STYLE_LANDSCAPE:
            mBatteryIconView.setImageDrawable(mLandscapeDrawable);
            break;
            case BATTERY_STYLE_FULL_CIRCLE:
            mBatteryIconView.setImageDrawable(mFullCircleDrawable);
            break;
            default:
            mCircleDrawable.setMeterStyle(mBatteryStyle);
            mBatteryIconView.setImageDrawable(mCircleDrawable);
            break;
            case BATTERY_STYLE_LANDSCAPEA:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryA);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEC:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryC);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEH:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryH);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEI:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryI);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEK:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryK);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEL:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryL);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEM:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryM);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
        }
    }


    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        float intensity = DarkIconDispatcher.isInArea(area, this) ? darkIntensity : 0;
        updateSettings();
        mNonAdaptedSingleToneColor = mDualToneHandler.getSingleColor(intensity);
        mNonAdaptedForegroundColor = mDualToneHandler.getFillColor(intensity);
        mNonAdaptedBackgroundColor = mDualToneHandler.getBackgroundColor(intensity);

        updateColors(mNonAdaptedForegroundColor, mNonAdaptedBackgroundColor,
                mNonAdaptedSingleToneColor);
    }

    /**
     * Sets icon and text colors. This will be overridden by {@code onDarkChanged} events,
     * if registered.
     *
     * @param foregroundColor
     * @param backgroundColor
     * @param singleToneColor
     */
    public void updateColors(int foregroundColor, int backgroundColor, int singleToneColor) {
        mThemedDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mFullCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryC.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryH.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryI.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryK.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryL.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryM.setColors(foregroundColor, backgroundColor, singleToneColor);
        mTextColor = singleToneColor;
        if (mBatteryPercentView != null) {
            mBatteryPercentView.setTextColor(singleToneColor);
        }

        if (mUnknownStateDrawable != null) {
            mUnknownStateDrawable.setTint(singleToneColor);
        }
    }

    public int getBatteryStyle() {
        return mBatteryStyle;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String powerSave = mThemedDrawable  == null ? null : mThemedDrawable .getPowerSaveEnabled() + "";
        CharSequence percent = mBatteryPercentView == null ? null : mBatteryPercentView.getText();
        pw.println("  BatteryMeterView:");
        pw.println("    mThemedDrawable .getPowerSave: " + powerSave);
        pw.println("    mBatteryPercentView.getText(): " + percent);
        pw.println("    mTextColor: #" + Integer.toHexString(mTextColor));
        pw.println("    mBatteryStateUnknown: " + mBatteryStateUnknown);
        pw.println("    mLevel: " + mLevel);
        pw.println("    mMode: " + mShowPercentMode);
    }

    @VisibleForTesting
    CharSequence getBatteryPercentViewText() {
        return mBatteryPercentView.getText();
    }

    /** An interface that will fetch the estimated time remaining for the user's battery. */
    public interface BatteryEstimateFetcher {
        void fetchBatteryTimeRemainingEstimate(
                BatteryController.EstimateFetchCompletion completion);
    }

    public void updateSettings() {
        updateSbBatteryStyle();
        updateSbShowBatteryPercent();
        updateQsBatteryEstimate();
    }

    public void updateQsBatteryEstimate() {
        mShowBatteryEstimate = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QS_SHOW_BATTERY_ESTIMATE, 1,
                UserHandle.USER_CURRENT) == 1;
        updatePercentView();
    }

    public void updateSbBatteryStyle() {
        mBatteryStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT);
        updateBatteryStyle();
        updateVisibility();
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onHiddenBattery(mBatteryStyle == BATTERY_STYLE_HIDDEN);
        }
    }

    public void updateSbShowBatteryPercent() {
        //updateSbBatteryStyle already called
        switch (mBatteryStyle) {
            case BATTERY_STYLE_TEXT:
                mShowBatteryPercent = BATTERY_PERCENT_SHOW_OUTSIDE;
                updatePercentView();
                return;
            case BATTERY_STYLE_HIDDEN:
                mShowBatteryPercent = BATTERY_PERCENT_HIDDEN;
                updatePercentView();
                return;
            default:
                mShowBatteryPercent = Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, BATTERY_PERCENT_HIDDEN);
                updatePercentView();
        }
    }

    public interface BatteryMeterViewCallbacks {
        default void onHiddenBattery(boolean hidden) {}
    }

    public void addCallback(BatteryMeterViewCallbacks callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(BatteryMeterViewCallbacks callbacks) {
        mCallbacks.remove(callbacks);
    }
}

