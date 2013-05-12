package jp.tkgktyk.wimaxhelperforaterm.my;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import jp.tkgktyk.wimaxhelperforaterm.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
	public static String getStringPreference(Context context, int keyId) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(keyId), "");
	}
	public static void setStringPreference(Context context, int keyId, String value) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.edit()
		.putString(context.getString(keyId), value)
		.apply();
	}
	public static Long getLongPreference(Context context, int keyId) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return Long.parseLong(pref.getString(context.getString(keyId), "0"));
	}
	public static void setLongPreference(Context context, int keyId, long value) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.edit()
		.putString(context.getString(keyId), String.valueOf(value))
		.apply();
	}
	public static <T> Set<T> getSetPreference(Context context, int keyId) {
		String filename = context.getString(keyId);
		try {
			FileInputStream fis = context.openFileInput(filename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			return (Set<T>)ois.readObject();
		} catch (FileNotFoundException e) {
			MyLog.i("a local file is not found: " + filename);
		} catch (IOException e) {
			MyLog.e(e.toString());
		} catch (ClassNotFoundException e) {
			MyLog.e(e.toString());
		}
		return new HashSet<T>();
	}
	public static <T> void setSetPreference(Context context, int keyId, Set<T> value) {
		String filename = context.getString(keyId);
		try {
			FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(value);
		} catch (FileNotFoundException e) {
			MyLog.i("a local file is not found: " + filename);
		} catch (IOException e) {
			MyLog.e(e.toString());
		}
	}
}
