package sh.siava.pixelxpert.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getLongField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.hardware.SensorEvent;
import android.os.SystemClock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;

/** @noinspection RedundantThrows*/
public class FaceUpScreenSleep extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;
	public static final float FACE_UP_Z_THRESHOLD = 9f;
	public static final int DEFAULT_DISPLAY_GROUP = 0;
	public static final int GO_TO_SLEEP_REASON_TIMEOUT = 2;
	long mFirstStableMillis = 0;
	boolean mIsMoving = true;
	private Object mPowerManagerServiceInstance;
	long mLastSleepOrderMillis = 0;
	private static boolean SleepOnFlatScreen = false;
	public static long FlatStandbyTimeMillis = 5000;

	public FaceUpScreenSleep(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		SleepOnFlatScreen = Xprefs.getBoolean("SleepOnFlatScreen", false);
		FlatStandbyTimeMillis = Xprefs.getSliderInt("FlatStandbyTime", 5) * 1000L;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		Class<?> FaceDownDetectorClass = findClass("com.android.server.power.FaceDownDetector", lpParam.classLoader);
		Class<?> PowerManagerServiceClass = findClass("com.android.server.power.PowerManagerService", lpParam.classLoader);

		List<Set<XC_MethodHook.Unhook>> unHooks = new ArrayList<>();
		unHooks.add(hookAllMethods(PowerManagerServiceClass, "updatePowerStateLocked", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mPowerManagerServiceInstance = param.thisObject;
				unHooks.get(0).forEach(Unhook::unhook);
				unHooks.clear();
			}
		}));

		hookAllMethods(FaceDownDetectorClass, "onSensorChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!SleepOnFlatScreen || getBooleanField(param.thisObject, "mFaceDown")) return; //device is already facing down or feature not enabled

				SensorEvent event = (SensorEvent) param.args[0];

				if(!(event.values[2] > FACE_UP_Z_THRESHOLD)) return; //not facing up

				long currentTimeNanos = event.timestamp;
				long currentTimeMillis = currentTimeNanos / 1000000;

				Duration mTimeThreshold = (Duration) getObjectField(param.thisObject, "mTimeThreshold");

				long sleepDurationMillis = FlatStandbyTimeMillis - mTimeThreshold.toMillis();

				boolean moving = currentTimeNanos - getLongField(param.thisObject, "mPrevAccelerationTime") <= mTimeThreshold.toNanos();

				if(mIsMoving && !moving) {
					mIsMoving = false;
					mFirstStableMillis = currentTimeMillis;
				}
				else if(!mIsMoving && moving) {
					mIsMoving = true;
				}
				if(mIsMoving) return; //not stationary

				Object mPowerGroups = getObjectField(mPowerManagerServiceInstance, "mPowerGroups");
				Object defaultPowerGroup = callMethod(mPowerGroups, "get", DEFAULT_DISPLAY_GROUP);

				long lastUserActivityMillis = (long) callMethod(defaultPowerGroup, "getLastUserActivityTimeLocked");

				if(currentTimeMillis - lastUserActivityMillis < sleepDurationMillis) {
					return; //user is touching the screen
				}

				if(currentTimeMillis - mFirstStableMillis > sleepDurationMillis && (currentTimeMillis - mLastSleepOrderMillis) > 5000) {
					mLastSleepOrderMillis = currentTimeMillis;
					callMethod(mPowerManagerServiceInstance, "goToSleepInternal", getObjectField(mPowerManagerServiceInstance, "DEFAULT_DISPLAY_GROUP_IDS"), SystemClock.uptimeMillis(), GO_TO_SLEEP_REASON_TIMEOUT, 0 /* flag */);
				}
			}
		});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}