package jp.tkgktyk.wimaxhelperforaterm;

import android.util.Log;

public class MyLog {
	private static final boolean D = true;

	private static String _getMethodName() {
		String method = Thread.currentThread().getStackTrace()[4].getClassName();
		method += "#" + Thread.currentThread().getStackTrace()[4].getMethodName();
		method = method.substring(method.lastIndexOf(".")+1);
		return method;
	}
	public static void d(String text) {
		if (D)
			Log.d(_getMethodName(), text);
	}
	public static void d() {
		if (D)
			Log.d("LogD", _getMethodName());
	}
	public static void e(String text) {
		Log.e(_getMethodName(), text);
	}
	public static void i(String text) {
		Log.i(_getMethodName(), text);
	}
	public static void v(String text) {
		Log.v(_getMethodName(), text);
	}
	public static void w(String text) {
		Log.w(_getMethodName(), text);
	}
}
