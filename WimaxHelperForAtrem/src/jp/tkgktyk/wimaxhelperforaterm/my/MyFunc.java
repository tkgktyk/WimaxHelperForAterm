package jp.tkgktyk.wimaxhelperforaterm.my;

import jp.tkgktyk.wimaxhelperforaterm.R;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class MyFunc {
	public static void showToast(Context context, int id) {
		showToast(context, context.getString(id));
	}
	public static void showToast(Context context, String text) {
		Toast
		.makeText(context, text, Toast.LENGTH_SHORT)
		.show();
	}
	private static void showLongToast(Context context, String text) {
		Toast
		.makeText(context, text, Toast.LENGTH_LONG)
		.show();
	}
	public static void showFinishedToast(Context context, int id) {
		showToast(context, context.getString(R.string.s1_finished, context.getString(id)));
	}
	public static void showFailedToast(Context context, int id) {
		showToast(context, context.getString(R.string.s1_failed, context.getString(id)));
	}
	public static void runtimeError(Context context, int id) {
		runtimeError(context, context.getString(id));
	}
	public static void runtimeError(Context context, String text) {
		String tag = Thread.currentThread().getStackTrace()[3].getMethodName();
		Log.e(tag, text);
		MyFunc.showLongToast(context, "Runtime Error: function is failed");
	}
	public static void runtimeError(Context context, Throwable e) {
		String tag = Thread.currentThread().getStackTrace()[3].getMethodName();
		Log.e(tag, e.toString());
		MyFunc.showLongToast(context, "Runtime Error: function is failed");
	} 
}
