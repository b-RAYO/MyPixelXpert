package sh.siava.pixelxpert.modpacks.utils.toolkit;

import static de.robv.android.xposed.XposedBridge.hookMethod;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;

/** @noinspection unused*/
public class ReflectedMethod {
	Method method;

	private ReflectedMethod(Method method) {
		this.method = method;
	}

	public static ReflectedMethod of(Method method) {
		return new ReflectedMethod(method);
	}

	public static ReflectedMethod ofName(ReflectedClass clazz, String exactName)
	{
		return new ReflectedMethod(findMethod(clazz.getClazz(), exactName));
	}

	public void runBefore(ReflectedClass.ReflectionConsumer consumer)
	{
		hookMethod(method, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				consumer.run(param);
			}
		});
	}

	public void runAfter(ReflectedClass.ReflectionConsumer consumer)
	{
		hookMethod(method, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				consumer.run(param);
			}
		});
	}

	public static Method findMethod(Class<?> clazz, String namePattern)
	{
		Method[] methods = clazz.getMethods();

		for(Method method : methods)
		{
			if(Pattern.matches(namePattern, method.getName()))
			{
				return method;
			}
		}
		return null;
	}

}
