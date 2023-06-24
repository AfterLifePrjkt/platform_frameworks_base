/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.android.systemui.battery.BatteryMeterView.BATTERY_STYLE_CIRCLE;

import android.annotation.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;
import android.widget.*;
import android.view.*;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.animation.PathInterpolator;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Space;

import androidx.annotation.NonNull;

import com.android.internal.policy.SystemBarUtils;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.R;
import com.android.systemui.battery.BatteryMeterView;
import com.android.systemui.qs.QSDetail.Callback;
import com.android.systemui.statusbar.phone.StatusBarContentInsetsProvider;
import com.android.systemui.statusbar.phone.StatusBarIconController.TintedIconManager;
import com.android.systemui.statusbar.phone.StatusIconContainer;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.VariableDateView;
import com.android.systemui.tuner.TunerService;

import java.util.List;
import com.evillium.prjct.utils.EvlUtils;

/**
 * View that contains the top-most bits of the QS panel (primarily the status bar with date, time,
 * battery, carrier info and privacy icons) and also contains the {@link QuickQSPanel}.
 */
public class QuickStatusBarHeader extends FrameLayout implements TunerService.Tunable,
        View.OnClickListener, View.OnLongClickListener {

    private static final String ANCIENT_UI_HEADER_HEIGHT =
            "system:" + "ANCIENT_UI_HEADER_HEIGHT"; 
    private static final String ANCIENT_UI_HEADER_HEIGHT_LAND =
            "system:" + "ANCIENT_UI_HEADER_HEIGHT_LAND"; 
    private static final String ANCIENT_UI_HEADERIMG_STYLE =
            "system:" + "ANCIENT_UI_HEADERIMG_STYLE"; 
    private static final String ANCIENT_UI_HEADERIMG_SET =
            "system:" + "ANCIENT_UI_HEADERIMG_SET";  
    private static final String ANCIENT_UI_HEADERIMG_SWITCH =
            "system:" + "ANCIENT_UI_HEADERIMG_SWITCH";
    private static final String ANCIENT_UI_HEADERIMG_LAND_SWITCH =
            "system:" + "ANCIENT_UI_HEADERIMG_LAND_SWITCH";    
    private static final String ANCIENT_UI_HEADERIMG_ANIMATION =
            "system:" + "ANCIENT_UI_HEADERIMG_ANIMATION";  
    private static final String ANCIENT_UI_HEADERIMG_TINT =
            "system:" + "ANCIENT_UI_HEADERIMG_TINT"; 
    private static final String ANCIENT_UI_HEADERIMG_TINT_CUSTOM =
            "system:" + "ANCIENT_UI_HEADERIMG_TINT_CUSTOM";      
    private static final String ANCIENT_UI_HEADERIMG_ALPHA =
            "system:" + "ANCIENT_UI_HEADERIMG_ALPHA";      
    private static final String ANCIENT_UI_HEADERIMG_USECUSTOMHEIGHT =
            "system:" + "ANCIENT_UI_HEADERIMG_USECUSTOMHEIGHT";

    private static final String IMAGE_HEADER_HEIGHTP =
            "system:" + "IMAGE_HEADER_HEIGHTP"; 
    private static final String IMAGE_HEADER_HEIGHTL =
            "system:" + "IMAGE_HEADER_HEIGHTL";      
    private static final String IMAGE_HEADER_HEIGHTPING =
            "system:" + "IMAGE_HEADER_HEIGHTPING";      
    private static final String IMAGE_HEADER_HEIGHTNDU =
            "system:" + "IMAGE_HEADER_HEIGHTNDU";
    private static final String IMAGE_HEADER_SCALETYPE =
            "system:" + "IMAGE_HEADER_SCALETYPE";
    private static final String IMAGE_HEADER_CLIPOUTLINE =
            "system:" + "IMAGE_HEADER_CLIPOUTLINE";

    private boolean mExpanded;
    private boolean mQsDisabled;

    private TouchAnimator mAlphaAnimator;
    private TouchAnimator mTranslationAnimator;
    private TouchAnimator mIconsAlphaAnimator;
    private TouchAnimator mIconsAlphaAnimatorFixed;
    private TouchAnimator mAnciHeaderimgAnimator;

    //headerimg
    private boolean mHeaderImageEnabled;
    private boolean mHeaderImageLandDisabled;
    private boolean mHeaderImageHeightEnabled;
    private boolean digawefull;
    private ImageView mBackgroundImage;
    private View mStatusBarHeaderMachineLayout;
    private View mStatusBarHeaderInnerLayout;
    private int mAncientUIheaderheight;
    private int mAncientUIheaderheightLand;
    private int mAncientUIheaderStyle;
    private int mAncientUIheaderImgStyle;
    private int mAncientUIheaderAniStyle;
    private int mAncientUIheaderAlphaStyle; 
    private int mAncientUIheaderTintStyle;
    private int mAncientUIheaderTintStyleCustom;
    private int mColorAccent; 
    private int mColorTextPrimary; 
    private int mColorPutihIreng;   
    private int mColorWindow; 

    private int jembutheight;  
    private int jembutpanjangheight;  
    private int jembutpinggir;   
    private int jembutduwur;

    protected QuickQSPanel mHeaderQsPanel;
    private View mDatePrivacyView;
    private View mDateView;
    // DateView next to clock. Visible on QQS
    private VariableDateView mClockDateView;
    private View mSecurityHeaderView;
    private View mStatusIconsView;
    private View mContainer;

    private View mQsWeatherHeaderView;

    private ViewGroup mClockContainer;
    private Clock mClockView;
    private Space mDatePrivacySeparator;
    private View mClockIconsSeparator;
    private boolean mShowClockIconsSeparator;
    private View mRightLayout;
    //private View mDateContainer;
    private View mPrivacyContainer;

    private BatteryMeterView mBatteryRemainingIcon;
    private StatusIconContainer mIconContainer;
    private View mPrivacyChip;

    private TintedIconManager mTintedIconManager;
    private QSExpansionPathInterpolator mQSExpansionPathInterpolator;
    private StatusBarContentInsetsProvider mInsetsProvider;

    private int mRoundedCornerPadding = 0;
    private int mWaterfallTopInset;
    private int mCutOutPaddingLeft;
    private int mCutOutPaddingRight;
    private float mKeyguardExpansionFraction;
    private int mTextColorPrimary = Color.TRANSPARENT;
    private int mTopViewMeasureHeight;

    private boolean mHasCenterCutout;
    private boolean mConfigShowBatteryEstimate;

    private boolean mUseCombinedQSHeader;

    private final ActivityStarter mActivityStarter;
    private final Vibrator mVibrator;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivityStarter = Dependency.get(ActivityStarter.class);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * How much the view containing the clock and QQS will translate down when QS is fully expanded.
     *
     * This matches the measured height of the view containing the date and privacy icons.
     */
    public int getOffsetTranslation() {
        return mTopViewMeasureHeight;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHeaderQsPanel = findViewById(R.id.quick_qs_panel);
        mDatePrivacyView = findViewById(R.id.quick_status_bar_date_privacy);
        mStatusIconsView = findViewById(R.id.quick_qs_status_icons);
        mContainer = findViewById(R.id.qs_container);
        mIconContainer = findViewById(R.id.statusIcons);
        mPrivacyChip = findViewById(R.id.privacy_chip);
        mClockDateView = findViewById(R.id.date_clock);
        mClockDateView.setOnClickListener(this);
        mQsWeatherHeaderView = findViewById(R.id.weather_view_header);
        mQsWeatherHeaderView.setOnLongClickListener(this);
        mClockDateView.setVisibility(View.GONE);
        mSecurityHeaderView = findViewById(R.id.header_text_container);
        mClockIconsSeparator = findViewById(R.id.separator);
        mRightLayout = findViewById(R.id.rightLayout);
        mPrivacyContainer = findViewById(R.id.privacy_container);

        mClockContainer = findViewById(R.id.clock_container);
        mClockView = findViewById(R.id.clock);
        mClockView.setQsHeader();
        mClockView.setOnClickListener(this);
        mClockView.setOnLongClickListener(this);
        mDatePrivacySeparator = findViewById(R.id.space);
        // Tint for the battery icons are handled in setupHost()
        mBatteryRemainingIcon = findViewById(R.id.batteryRemainingIcon);
        mBackgroundImage = findViewById(R.id.qs_header_image_view);
        mStatusBarHeaderMachineLayout = findViewById(R.id.layout_header);
        mStatusBarHeaderInnerLayout = findViewById(R.id.layout_inner_header);
        mBackgroundImage.setClipToOutline(true);

        updateResources();
        Configuration config = mContext.getResources().getConfiguration();
        setDatePrivacyContainersWidth(config.orientation == Configuration.ORIENTATION_LANDSCAPE);
        setSecurityHeaderContainerVisibility(
                config.orientation == Configuration.ORIENTATION_LANDSCAPE);

        mBatteryRemainingIcon.setIsQsHeader(true);
        // QS will always show the estimate, and BatteryMeterView handles the case where
        // it's unavailable or charging
        mBatteryRemainingIcon.setPercentShowMode(BatteryMeterView.MODE_ESTIMATE);
        mBatteryRemainingIcon.setOnClickListener(this);

        mIconsAlphaAnimatorFixed = new TouchAnimator.Builder()
                .addFloat(mIconContainer, "alpha", 0, 1)
                .addFloat(mBatteryRemainingIcon, "alpha", 0, 1)
                .build();

        Dependency.get(TunerService.class).addTunable(this,
                ANCIENT_UI_HEADERIMG_SWITCH,
                ANCIENT_UI_HEADERIMG_STYLE, 
                ANCIENT_UI_HEADERIMG_SET, 
                ANCIENT_UI_HEADERIMG_TINT,
                ANCIENT_UI_HEADERIMG_TINT_CUSTOM,
                ANCIENT_UI_HEADERIMG_ALPHA,
                ANCIENT_UI_HEADERIMG_USECUSTOMHEIGHT,
                IMAGE_HEADER_HEIGHTP,
                IMAGE_HEADER_HEIGHTL,
                IMAGE_HEADER_HEIGHTPING,
                IMAGE_HEADER_HEIGHTNDU,
                ANCIENT_UI_HEADERIMG_ANIMATION,
                IMAGE_HEADER_SCALETYPE,
                ANCIENT_UI_HEADERIMG_LAND_SWITCH);
    }

    void onAttach(TintedIconManager iconManager,
            QSExpansionPathInterpolator qsExpansionPathInterpolator,
            List<String> rssiIgnoredSlots,
            boolean useCombinedQSHeader,
            StatusBarContentInsetsProvider insetsProvider) {
        mUseCombinedQSHeader = useCombinedQSHeader;
        mTintedIconManager = iconManager;
        mInsetsProvider = insetsProvider;
        int fillColor = Utils.getColorAttrDefaultColor(getContext(),
                android.R.attr.textColorPrimary);

        // Set the correct tint for the status icons so they contrast
        iconManager.setTint(fillColor);

        mQSExpansionPathInterpolator = qsExpansionPathInterpolator;
        updateAnimators();
        //updateAnciHeaderimgAnimator();
    }

    public QuickQSPanel getHeaderQsPanel() {
        return mHeaderQsPanel;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mDatePrivacyView.getMeasuredHeight() != mTopViewMeasureHeight) {
            mTopViewMeasureHeight = mDatePrivacyView.getMeasuredHeight();
        }
        updateAnimators();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
        setDatePrivacyContainersWidth(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
        setSecurityHeaderContainerVisibility(
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    @Override
    public void onClick(View v) {
        if (v == mClockView) {
            mActivityStarter.postStartActivityDismissingKeyguard(new Intent(
                    AlarmClock.ACTION_SHOW_ALARMS), 0);
        } else if (v == mClockDateView) {
            Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
            builder.appendPath("time");
            builder.appendPath(Long.toString(System.currentTimeMillis()));
            Intent todayIntent = new Intent(Intent.ACTION_VIEW, builder.build());
            mActivityStarter.postStartActivityDismissingKeyguard(todayIntent, 0);
        } else if (v == mBatteryRemainingIcon) {
            mActivityStarter.postStartActivityDismissingKeyguard(new Intent(
                    Intent.ACTION_POWER_USAGE_SUMMARY),0);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == mClockView || v == mClockDateView) {
            Intent nIntent = new Intent(Intent.ACTION_MAIN);
            nIntent.setClassName("com.android.settings",
                    "com.android.settings.Settings$DateTimeSettingsActivity");
            mActivityStarter.startActivity(nIntent, true /* dismissShade */);
            mVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            return true;
        } else if (v == mQsWeatherHeaderView) {
            Intent wIntent = new Intent(Intent.ACTION_MAIN);
            wIntent.setClassName("org.omnirom.omnijaws",
                    "org.omnirom.omnijaws.SettingsActivity");
            mActivityStarter.startActivity(wIntent, true /* dismissShade */);
            mVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            return true;
        }
        return false;
    }

    private void setDatePrivacyContainersWidth(boolean landscape) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mPrivacyContainer.getLayoutParams();
        lp.width = landscape ? WRAP_CONTENT : 0;
        lp.weight = landscape ? 0f : 1f;
        mPrivacyContainer.setLayoutParams(lp);
    }

    private void setSecurityHeaderContainerVisibility(boolean landscape) {
        mSecurityHeaderView.setVisibility(landscape ? VISIBLE : GONE);
    }

    private void updateBatteryMode() {
        if (mConfigShowBatteryEstimate) {
            mBatteryRemainingIcon.setPercentShowMode(BatteryMeterView.MODE_ESTIMATE);
        } else {
            mBatteryRemainingIcon.setPercentShowMode(BatteryMeterView.MODE_ON);
        }
    }

    void updateResources() {
        Resources resources = mContext.getResources();
        // status bar is already displayed out of QS in split shade
        boolean shouldUseSplitShade =
                resources.getBoolean(R.bool.config_use_split_notification_shade);

        boolean gone = shouldUseSplitShade || mUseCombinedQSHeader || mQsDisabled;
        mStatusIconsView.setVisibility(gone ? View.GONE : View.VISIBLE);
        mDatePrivacyView.setVisibility(gone ? View.GONE : View.VISIBLE);
            
            int statusBarSideMargin = mHeaderImageEnabled ? mContext.getResources().getDimensionPixelSize(
                R.dimen.qs_header_image_side_margin) : 0;

        mConfigShowBatteryEstimate = resources.getBoolean(R.bool.config_showBatteryEstimateQSBH);

        mRoundedCornerPadding = resources.getDimensionPixelSize(
                R.dimen.rounded_corner_content_padding);

        int qsOffsetHeight = SystemBarUtils.getQuickQsOffsetHeight(mContext);

        mDatePrivacyView.getLayoutParams().height =
                Math.max(qsOffsetHeight, mDatePrivacyView.getMinimumHeight());
        mDatePrivacyView.setLayoutParams(mDatePrivacyView.getLayoutParams());

        mStatusIconsView.getLayoutParams().height =
                Math.max(qsOffsetHeight, mStatusIconsView.getMinimumHeight());
        mStatusIconsView.setLayoutParams(mStatusIconsView.getLayoutParams());

        ViewGroup.LayoutParams lp = getLayoutParams();
        if (mQsDisabled) {
            lp.height = mStatusIconsView.getLayoutParams().height;
        } else {
            lp.height = WRAP_CONTENT;
        }
        setLayoutParams(lp);

        int textColor = Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);
        if (textColor != mTextColorPrimary) {
            int textColorSecondary = Utils.getColorAttrDefaultColor(mContext,
                    android.R.attr.textColorSecondary);
            mTextColorPrimary = textColor;
            mClockView.setTextColor(textColor);
            if (mTintedIconManager != null) {
                mTintedIconManager.setTint(textColor);
            }
            if (mBatteryRemainingIcon.getBatteryStyle() == BATTERY_STYLE_CIRCLE) {
                textColorSecondary = Utils.getColorAttrDefaultColor(mContext,
                        android.R.attr.textColorHint);
            }
            mBatteryRemainingIcon.updateColors(mTextColorPrimary, textColorSecondary,
                    mTextColorPrimary);
        }

        MarginLayoutParams qqsLP = (MarginLayoutParams) mHeaderQsPanel.getLayoutParams();
        qqsLP.topMargin = shouldUseSplitShade || !mUseCombinedQSHeader ? mContext.getResources()
                .getDimensionPixelSize(R.dimen.qqs_layout_margin_top) : qsOffsetHeight;
        mHeaderQsPanel.setLayoutParams(qqsLP);

        updateBatteryMode();
        updateHeadersPadding();
        updateAnimators();

        updateClockDatePadding();
        updateAnciHeaderimgSet();
    }

    private void updateAnciHeaderimgSet() {

        Resources resources = mContext.getResources();

        int orientation = getResources().getConfiguration().orientation; 
	mColorAccent = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorAccent);
        mColorTextPrimary = Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);
	mColorWindow = Utils.getColorAttrDefaultColor(mContext, android.R.attr.windowBackground);
        mColorPutihIreng = mContext.getResources().getColor(R.color.puteh_ireng);

	int fkheightzero = resources.getDimensionPixelSize(R.dimen.ancient_qs_zero);  
	int headersmall = resources.getDimensionPixelSize(R.dimen.ancient_header_small);   
	int headerbig = resources.getDimensionPixelSize(R.dimen.ancient_header_big);

	if (mHeaderImageEnabled) {
	     if (mHeaderImageLandDisabled && orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mBackgroundImage.setVisibility(View.GONE);
             } else {
                mBackgroundImage.setVisibility(View.VISIBLE);
             mBackgroundImage.setVisibility(View.VISIBLE);
             }

	     if (mAncientUIheaderImgStyle == 0) {
		 mBackgroundImage.setImageResource(R.drawable.anime1);
             } else if (mAncientUIheaderImgStyle == 1) {
		 mBackgroundImage.setImageResource(R.drawable.anime2);
             } else if (mAncientUIheaderImgStyle == 2) {
		 mBackgroundImage.setImageResource(R.drawable.anime3);
             } else if (mAncientUIheaderImgStyle == 3) {
		 mBackgroundImage.setImageResource(R.drawable.anime4);
             } else if (mAncientUIheaderImgStyle == 4) {
		 mBackgroundImage.setImageResource(R.drawable.anime5);
             } else if (mAncientUIheaderImgStyle == 5) {
		 mBackgroundImage.setImageResource(R.drawable.anime6);
             } else if (mAncientUIheaderImgStyle == 6) {
		 mBackgroundImage.setImageResource(R.drawable.anime7);
             } else if (mAncientUIheaderImgStyle == 7) {
		 mBackgroundImage.setImageResource(R.drawable.anime8);
             } else if (mAncientUIheaderImgStyle == 8) {
		 mBackgroundImage.setImageResource(R.drawable.anime9);
             } else if (mAncientUIheaderImgStyle == 9) {
		 mBackgroundImage.setImageResource(R.drawable.anime10);
             } else if (mAncientUIheaderImgStyle == 10) {
		 mBackgroundImage.setImageResource(R.drawable.anime11);
             } else if (mAncientUIheaderImgStyle == 11) {
		 mBackgroundImage.setImageResource(R.drawable.anime12);
             } else if (mAncientUIheaderImgStyle == 12) {
		 mBackgroundImage.setImageResource(R.drawable.anime13);
             } else if (mAncientUIheaderImgStyle == 13) {
		 mBackgroundImage.setImageResource(R.drawable.anime14);
	     } else if (mAncientUIheaderImgStyle == 14) {
		 mBackgroundImage.setImageResource(R.drawable.anime15);
	     } else if (mAncientUIheaderImgStyle == 15) {
		 mBackgroundImage.setImageResource(R.drawable.banner1);
	     } else if (mAncientUIheaderImgStyle == 16) {
		 mBackgroundImage.setImageResource(R.drawable.banner2);
	     } else if (mAncientUIheaderImgStyle == 17) {
		 mBackgroundImage.setImageResource(R.drawable.flower1);
	     } else if (mAncientUIheaderImgStyle == 18) {
	 	 mBackgroundImage.setImageResource(R.drawable.flower2);
	     } else if (mAncientUIheaderImgStyle == 19) {
		 mBackgroundImage.setImageResource(R.drawable.planetary1);
             } else if (mAncientUIheaderImgStyle == 20) {
		 mBackgroundImage.setImageResource(R.drawable.planetary2);
	     } else if (mAncientUIheaderImgStyle == 21) {
		 mBackgroundImage.setImageResource(R.drawable.scene1);
	     } else if (mAncientUIheaderImgStyle == 22) {
		 mBackgroundImage.setImageResource(R.drawable.scene2);
	     } else if (mAncientUIheaderImgStyle == 23) {
	 	 mBackgroundImage.setImageResource(R.drawable.scene3);
	     } else if (mAncientUIheaderImgStyle == 24) {
		 mBackgroundImage.setImageResource(R.drawable.white1);
	     }

             if (digawefull) {
                mBackgroundImage.setScaleType(ImageView.ScaleType.FIT_XY);
	     } else {
                mBackgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
	     }

             mBackgroundImage.setAlpha(mAncientUIheaderAlphaStyle);

             if (mAncientUIheaderTintStyle == 0) {
		 mBackgroundImage.setColorFilter(null);
	     } else if (mAncientUIheaderTintStyle == 1) {
		 mBackgroundImage.setColorFilter(mColorAccent);
	     } else if (mAncientUIheaderTintStyle == 2) {
		 mBackgroundImage.setColorFilter(mColorTextPrimary);
	     } else if (mAncientUIheaderTintStyle == 3) {
		 mBackgroundImage.setColorFilter(mColorWindow);
	     } else if (mAncientUIheaderTintStyle == 4) {
		 mBackgroundImage.setColorFilter(mColorPutihIreng);
	     } else if (mAncientUIheaderTintStyle == 5) {
		 mBackgroundImage.setColorFilter(EvlUtils.getRandomColor(mContext));
	     } else if (mAncientUIheaderTintStyle == 6) {
		 mBackgroundImage.setColorFilter(mAncientUIheaderTintStyleCustom);
	     }

        } else { 

	     mBackgroundImage.setVisibility(View.GONE);	

	}

	ViewGroup.MarginLayoutParams jembut = (ViewGroup.MarginLayoutParams) mStatusBarHeaderMachineLayout.getLayoutParams();
	          if (mHeaderImageEnabled) {    
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {  
                if (mHeaderImageHeightEnabled) {  
                   jembut.height = jembutpanjangheight;
                } else {  
                   jembut.height = headersmall;
                }
            } else {    
                if (mHeaderImageHeightEnabled) {  
                   jembut.height = jembutheight;
                } else {  
                     jembut.height = headersmall;
                }
            }
          } else {  
              jembut.height = fkheightzero;
          }
	jembut.setMargins(jembutpinggir, jembutduwur, jembutpinggir, 0);
	mStatusBarHeaderMachineLayout.setLayoutParams(jembut);
    }

    private void updateClockDatePadding() {
        int startPadding = mContext.getResources()
                .getDimensionPixelSize(R.dimen.status_bar_left_clock_starting_padding);
        int endPadding = mContext.getResources()
                .getDimensionPixelSize(R.dimen.status_bar_left_clock_end_padding);
        mClockView.setPaddingRelative(
                startPadding,
                mClockView.getPaddingTop(),
                endPadding,
                mClockView.getPaddingBottom()
        );
    }

    private void updateAnimators() {
        if (mUseCombinedQSHeader) {
            mTranslationAnimator = null;
            return;
        }
        updateAlphaAnimator();
        int offset = mTopViewMeasureHeight;

        mTranslationAnimator = new TouchAnimator.Builder()
                .addFloat(mContainer, "translationY", 0, offset)
                .setInterpolator(mQSExpansionPathInterpolator != null
                        ? mQSExpansionPathInterpolator.getYInterpolator()
                        : null)
                .build();
    }

    private void updateAlphaAnimator() {
        if (mUseCombinedQSHeader) {
            mAlphaAnimator = null;
            return;
        }
        TouchAnimator.Builder builder = new TouchAnimator.Builder()
                .addFloat(mSecurityHeaderView, "alpha", 0, 1)
                // These views appear on expanding down
                .addFloat(mClockDateView, "alpha", 1, 0, 0)
                .addFloat(mQsWeatherHeaderView, "alpha", 0, 0, 1)
                .setListener(new TouchAnimator.ListenerAdapter() {
                    @Override
                    public void onAnimationAtEnd() {
                        super.onAnimationAtEnd();
                    }

                    @Override
                    public void onAnimationStarted() {
                        setSeparatorVisibility(false);
                    }

                    @Override
                    public void onAnimationAtStart() {
                        super.onAnimationAtStart();
                        setSeparatorVisibility(mShowClockIconsSeparator);
                    }
                });
        mAlphaAnimator = builder.build();
    }

    void setChipVisibility(boolean visibility) {
        if (visibility) {
            // Animates the icons and battery indicator from alpha 0 to 1, when the chip is visible
            mBatteryRemainingIcon.setOnClickListener(null);
            mBatteryRemainingIcon.setClickable(false);
            mIconsAlphaAnimator = mIconsAlphaAnimatorFixed;
            mIconsAlphaAnimator.setPosition(mKeyguardExpansionFraction);
        } else {
            mIconsAlphaAnimator = null;
            mIconContainer.setAlpha(1);
            mBatteryRemainingIcon.setAlpha(1);
            mBatteryRemainingIcon.setOnClickListener(this);
        }
    }

    /** */
    public void setExpanded(boolean expanded, QuickQSPanelController quickQSPanelController) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        quickQSPanelController.setExpanded(expanded);
	mClockDateView.setVisibility(mClockView.isClockDateEnabled() ? View.GONE : View.VISIBLE);
        updateEverything();
    }

    /**
     * Animates the inner contents based on the given expansion details.
     *
     * @param forceExpanded whether we should show the state expanded forcibly
     * @param expansionFraction how much the QS panel is expanded/pulled out (up to 1f)
     * @param panelTranslationY how much the panel has physically moved down vertically (required
     *                          for keyguard animations only)
     */
    public void setExpansion(boolean forceExpanded, float expansionFraction,
                             float panelTranslationY) {
        final float keyguardExpansionFraction = forceExpanded ? 1f : expansionFraction;

        if (mAlphaAnimator != null) {
            mAlphaAnimator.setPosition(keyguardExpansionFraction);
        }
        if (mTranslationAnimator != null) {
            mTranslationAnimator.setPosition(keyguardExpansionFraction);
        }
        if (mIconsAlphaAnimator != null) {
            mIconsAlphaAnimator.setPosition(keyguardExpansionFraction);
        }
        // If forceExpanded (we are opening QS from lockscreen), the animators have been set to
        // position = 1f.
        if (forceExpanded) {
            setTranslationY(panelTranslationY);
        } else {
            setTranslationY(0);
        }

        mKeyguardExpansionFraction = keyguardExpansionFraction;
    }

    public void disable(int state1, int state2, boolean animate) {
        final boolean disabled = (state2 & DISABLE2_QUICK_SETTINGS) != 0;
        if (disabled == mQsDisabled) return;
        mQsDisabled = disabled;
        mHeaderQsPanel.setDisabledByPolicy(disabled);
        mStatusIconsView.setVisibility(mQsDisabled ? View.GONE : View.VISIBLE);
        updateResources();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Handle padding of the views
        DisplayCutout cutout = insets.getDisplayCutout();

        Pair<Integer, Integer> sbInsets = mInsetsProvider
                .getStatusBarContentInsetsForCurrentRotation();
        boolean hasCornerCutout = mInsetsProvider.currentRotationHasCornerCutout();

        mDatePrivacyView.setPadding(sbInsets.first, 0, sbInsets.second, 0);
        mStatusIconsView.setPadding(sbInsets.first, 0, sbInsets.second, 0);
        LinearLayout.LayoutParams datePrivacySeparatorLayoutParams =
                (LinearLayout.LayoutParams) mDatePrivacySeparator.getLayoutParams();
        LinearLayout.LayoutParams mClockIconsSeparatorLayoutParams =
                (LinearLayout.LayoutParams) mClockIconsSeparator.getLayoutParams();
        if (cutout != null) {
            Rect topCutout = cutout.getBoundingRectTop();
            if (topCutout.isEmpty() || hasCornerCutout) {
                datePrivacySeparatorLayoutParams.width = 0;
                mDatePrivacySeparator.setVisibility(View.GONE);
                mClockIconsSeparatorLayoutParams.width = 0;
                setSeparatorVisibility(false);
                mShowClockIconsSeparator = false;
                mHasCenterCutout = false;
            } else {
                datePrivacySeparatorLayoutParams.width = topCutout.width();
                mDatePrivacySeparator.setVisibility(View.VISIBLE);
                mClockIconsSeparatorLayoutParams.width = topCutout.width();
                mShowClockIconsSeparator = true;
                setSeparatorVisibility(mKeyguardExpansionFraction == 0f);
                mHasCenterCutout = true;
            }
        }
        mDatePrivacySeparator.setLayoutParams(datePrivacySeparatorLayoutParams);
        mClockIconsSeparator.setLayoutParams(mClockIconsSeparatorLayoutParams);
        mCutOutPaddingLeft = sbInsets.first;
        mCutOutPaddingRight = sbInsets.second;
        mWaterfallTopInset = cutout == null ? 0 : cutout.getWaterfallInsets().top;

        updateBatteryMode();
        updateHeadersPadding();
        return super.onApplyWindowInsets(insets);
    }

    /**
     * Sets the visibility of the separator between clock and icons.
     *
     * This separator is "visible" when there is a center cutout, to block that space. In that
     * case, the clock and the layout on the right (containing the icons and the battery meter) are
     * set to weight 1 to take the available space.
     * @param visible whether the separator between clock and icons should be visible.
     */
    private void setSeparatorVisibility(boolean visible) {
        int newVisibility = visible ? View.VISIBLE : View.GONE;
        if (mClockIconsSeparator.getVisibility() == newVisibility) return;

        mClockIconsSeparator.setVisibility(visible ? View.VISIBLE : View.GONE);

        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) mClockContainer.getLayoutParams();
        lp.width = visible ? 0 : WRAP_CONTENT;
        lp.weight = visible ? 1f : 0f;
        mClockContainer.setLayoutParams(lp);

        lp = (LinearLayout.LayoutParams) mRightLayout.getLayoutParams();
        lp.width = visible ? 0 : WRAP_CONTENT;
        lp.weight = visible ? 1f : 0f;
        mRightLayout.setLayoutParams(lp);
    }

    private void updateHeadersPadding() {
        setContentMargins(mDatePrivacyView, 0, 0);
        setContentMargins(mStatusIconsView, 0, 0);
        int paddingLeft = 0;
        int paddingRight = 0;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        int leftMargin = lp.leftMargin;
        int rightMargin = lp.rightMargin;

        // The clock might collide with cutouts, let's shift it out of the way.
        // We only do that if the inset is bigger than our own padding, since it's nicer to
        // align with
        if (mCutOutPaddingLeft > 0) {
            // if there's a cutout, let's use at least the rounded corner inset
            int cutoutPadding = Math.max(mCutOutPaddingLeft, mRoundedCornerPadding);
            paddingLeft = Math.max(cutoutPadding - leftMargin, 0);
        }
        if (mCutOutPaddingRight > 0) {
            // if there's a cutout, let's use at least the rounded corner inset
            int cutoutPadding = Math.max(mCutOutPaddingRight, mRoundedCornerPadding);
            paddingRight = Math.max(cutoutPadding - rightMargin, 0);
        }

        mDatePrivacyView.setPadding(paddingLeft,
                mWaterfallTopInset,
                paddingRight,
                0);
        mStatusIconsView.setPadding(paddingLeft,
                mWaterfallTopInset,
                paddingRight,
                0);
    }

    public void updateEverything() {
        post(() -> setClickable(!mExpanded));
    }

    public void setCallback(Callback qsPanelCallback) {
        mHeaderQsPanel.setCallback(qsPanelCallback);
    }

    private void setContentMargins(View view, int marginStart, int marginEnd) {
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        lp.setMarginStart(marginStart);
        lp.setMarginEnd(marginEnd);
        view.setLayoutParams(lp);
    }

    /**
     * Scroll the headers away.
     *
     * @param scrollY the scroll of the QSPanel container
     */
    public void setExpandedScrollAmount(int scrollY) {
        mStatusIconsView.setScrollY(scrollY);
        mDatePrivacyView.setScrollY(scrollY);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (ANCIENT_UI_HEADERIMG_STYLE.equals(key)) {
            mAncientUIheaderStyle = TunerService.parseInteger(newValue, 0);
            updateResources();
	} else if (ANCIENT_UI_HEADERIMG_ANIMATION.equals(key)) {
            mAncientUIheaderAniStyle = TunerService.parseInteger(newValue, 0);
            updateResources();	
	} else if (ANCIENT_UI_HEADERIMG_SET.equals(key)) {
            mAncientUIheaderImgStyle = TunerService.parseInteger(newValue, 0);
            updateResources();	
	} else if (ANCIENT_UI_HEADERIMG_SWITCH.equals(key)) {
            mHeaderImageEnabled = TunerService.parseIntegerSwitch(newValue, false);
            updateResources();
        } else if (ANCIENT_UI_HEADERIMG_LAND_SWITCH.equals(key)) {
            mHeaderImageLandDisabled = TunerService.parseIntegerSwitch(newValue, true);
            updateResources();	
	} else if (ANCIENT_UI_HEADERIMG_TINT.equals(key)) {
            mAncientUIheaderTintStyle = TunerService.parseInteger(newValue, 0);
            updateResources();	
	} else if (ANCIENT_UI_HEADERIMG_TINT_CUSTOM.equals(key)) {
            mAncientUIheaderTintStyleCustom = TunerService.parseInteger(newValue, 0XFFFFFFFF);
            updateResources();		
	} else if (ANCIENT_UI_HEADERIMG_ALPHA.equals(key)) {
            mAncientUIheaderAlphaStyle = TunerService.parseInteger(newValue, 255);
            updateResources();			
	} else if (ANCIENT_UI_HEADERIMG_USECUSTOMHEIGHT.equals(key)) {
            mHeaderImageHeightEnabled = TunerService.parseIntegerSwitch(newValue, false);
            updateResources();			
	} else if (IMAGE_HEADER_HEIGHTP.equals(key)) {
            jembutheight = TunerService.parseInteger(newValue, 155);
            updateResources();			
	} else if (IMAGE_HEADER_HEIGHTL.equals(key)) {
            jembutpanjangheight = TunerService.parseInteger(newValue, 155);
            updateResources();			
	} else if (IMAGE_HEADER_HEIGHTPING.equals(key)) {
            jembutpinggir = TunerService.parseInteger(newValue, 0);
            updateResources();			
	} else if (IMAGE_HEADER_HEIGHTNDU.equals(key)) {
            jembutduwur = TunerService.parseInteger(newValue, 0);
            updateResources();
        } else if (IMAGE_HEADER_SCALETYPE.equals(key)) {
            digawefull = TunerService.parseIntegerSwitch(newValue, false);
            updateResources();
        }
    }
}

