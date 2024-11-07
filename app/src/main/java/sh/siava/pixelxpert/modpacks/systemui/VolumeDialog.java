package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

/** @noinspection RedundantThrows*/
@SuppressLint({"deprecation", "DiscouragedApi"})
public class VolumeDialog extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static final int DISMISS_REASON_TIMEOUT = 3;
	private static final int DISMISS = 2;

	private static int VolumeDialogTimeout = 3000;

	private boolean mShowVolumeLevel = false;
	private final List<TextView> volumeTextViews = new ArrayList<>();

	public VolumeDialog(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		VolumeDialogTimeout = Xprefs.getSliderInt( "VolumeDialogTimeout", 3000);
		mShowVolumeLevel = Xprefs.getBoolean("VolumeDialogLevel", false);

		if (Key.length > 0 && Key[0].equals("VolumeDialogLevel")) {
			setVolumeTextViewsVisibility(mShowVolumeLevel ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		Class<?> VolumeDialogImplClass = findClass("com.android.systemui.volume.VolumeDialogImpl", lpParam.classLoader);
		hookAllMethods(VolumeDialogImplClass, "rescheduleTimeoutH", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(VolumeDialogTimeout != 3000
						&& !getBooleanField(param.thisObject, "mHovering")
						&& getObjectField(param.thisObject, "mSafetyWarning") == null
						&& getObjectField(param.thisObject, "mODICaptionsTooltipView") == null)
				{
					Handler mHandler = (Handler) getObjectField(param.thisObject, "mHandler");
					mHandler.removeMessages(DISMISS);
					mHandler.sendMessageDelayed(mHandler.obtainMessage(DISMISS,DISMISS_REASON_TIMEOUT,0),VolumeDialogTimeout);
				}
			}
		});

		hookAllMethods(VolumeDialogImplClass, "initRow", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Resources res = mContext.getResources();
				TextView rowHeader = (TextView) getObjectField(param.args[0], "header");
				int volumeNumberId = res.getIdentifier("volume_number", "id", mContext.getPackageName());
				TextView existingVolumeNumber = ((ViewGroup) rowHeader.getParent()).findViewById(volumeNumberId);

				if (existingVolumeNumber != null) {
					if (!volumeTextViews.contains(existingVolumeNumber)) {
						volumeTextViews.add(existingVolumeNumber);
					}
					existingVolumeNumber.setVisibility(mShowVolumeLevel ? View.VISIBLE : View.GONE);
					return;
				}

				TextView volumeNumber = createVolumeTextView(volumeNumberId);
				((ViewGroup) rowHeader.getParent()).addView(volumeNumber, 0);
				volumeNumber.setVisibility(mShowVolumeLevel ? View.VISIBLE : View.GONE);
				volumeTextViews.add(volumeNumber);

				setObjectField(param.args[0], "number", ((View) getObjectField(param.args[0], "view")).findViewById(volumeNumberId));
			}
		});

		hookAllMethods(VolumeDialogImplClass, "updateVolumeRowH", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Resources res = mContext.getResources();
				int volumeNumberId = res.getIdentifier("volume_number", "id", mContext.getPackageName());
				TextView volumeNumber = ((View) getObjectField(param.args[0], "view")).findViewById(volumeNumberId);
				Object mState = getObjectField(param.thisObject, "mState");

				if (volumeNumber == null || mState == null) {
					return;
				}

				Object ss = callMethod(getObjectField(mState, "states"), "get", getObjectField(param.args[0], "stream"));

				if (ss == null) {
					return;
				}

				if (volumeNumber.getText().toString().isEmpty()) {
					volumeNumber.setText("0");
				}

				if (volumeNumber.getText().toString().endsWith("%")) {
					volumeNumber.setText(volumeNumber.getText().toString().subSequence(0, volumeNumber.getText().toString().length() - 1));
				}

				int levelMax = (int) getObjectField(ss, "levelMax");
				int level = (int) Math.ceil(Float.parseFloat(volumeNumber.getText().toString()) / levelMax * 100f);

				if (level > 100) {
					level = 100;
				} else if (level < 0) {
					level = 0;
				}

				volumeNumber.setText(String.format(Locale.getDefault(), "%d%%", level));
			}
		});
	}

	private void setVolumeTextViewsVisibility(int visibility) {
		Iterator<TextView> iterator = volumeTextViews.iterator();

		while (iterator.hasNext()) {
			TextView volumeTextView = iterator.next();

			if (volumeTextView != null && volumeTextView.getParent() != null) {
				volumeTextView.post(() -> volumeTextView.setVisibility(visibility));
			} else {
				iterator.remove();
			}
		}
	}

	private TextView createVolumeTextView(int viewId) {
		Resources res = mContext.getResources();

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, res.getDisplayMetrics());

		TextView volumeNumber = new TextView(mContext);
		volumeNumber.setLayoutParams(lp);
		volumeNumber.setId(viewId);
		volumeNumber.setGravity(Gravity.CENTER);
		volumeNumber.setTextSize(12f);
		volumeNumber.setTextColor(ContextCompat.getColor(mContext, res.getIdentifier("android:color/system_accent1_300", "color", mContext.getPackageName())));
		volumeNumber.setText(String.format(Locale.getDefault(), "%d%%", 0));

		return volumeNumber;
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
