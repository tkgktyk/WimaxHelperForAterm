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
	private static Context _context;
	
	public static void setContext(Context context) { _context = context; }
	
	public static void showToast(int id) {
		showToast(_context.getString(id));
	}
	public static void showToast(String text) {
		Toast
		.makeText(_context, text, Toast.LENGTH_SHORT)
		.show();
	}
	private static void showLongToast(String text) {
		Toast
		.makeText(_context, text, Toast.LENGTH_LONG)
		.show();
	}
	public static void showFinishedToast(int id) {
		showToast(_context.getString(R.string.s1_finished, _context.getString(id)));
	}
	public static void showFailedToast(int id) {
		showToast(_context.getString(R.string.s1_failed, _context.getString(id)));
	}
	public static void runtimeError(int id) {
		runtimeError(_context.getString(id));
	}
	public static void runtimeError(String text) {
		String tag = Thread.currentThread().getStackTrace()[3].getMethodName();
		Log.e(tag, text);
		MyFunc.showLongToast("Runtime Error: function is failed");
	}
	public static void runtimeError(Throwable e) {
		String tag = Thread.currentThread().getStackTrace()[3].getMethodName();
		Log.e(tag, e.toString());
		MyFunc.showLongToast("Runtime Error: function is failed");
	}
	public static String getStringPreference(int keyId) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
		return pref.getString(_context.getString(keyId), "");
	}
	public static void setStringPreference(int keyId, String value) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
		pref.edit()
		.putString(_context.getString(keyId), value)
		.apply();
	}
	public static Long getLongPreference(int keyId) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
		return Long.parseLong(pref.getString(_context.getString(keyId), "0"));
	}
	public static void setLongPreference(int keyId, long value) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
		pref.edit()
		.putString(_context.getString(keyId), String.valueOf(value))
		.apply();
	}
	public static <T> Set<T> getSetPreference(int keyId) {
		String filename = _context.getString(keyId);
		Set<T> result = null;
		try {
			FileInputStream fis = _context.openFileInput(filename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			result = (Set<T>)ois.readObject();
		} catch (FileNotFoundException e) {
			MyLog.i("a local file is not found: " + filename);
		} catch (IOException e) {
			MyLog.e(e.toString());
		} catch (ClassNotFoundException e) {
			MyLog.e(e.toString());
		}
		if (result == null)
			result = new HashSet<T>();
		return result;
	}
	public static <T> void setSetPreference(int keyId, Set<T> value) {
		String filename = _context.getString(keyId);
		try {
			FileOutputStream fos = _context.openFileOutput(filename, Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(value);
		} catch (FileNotFoundException e) {
			MyLog.i("a local file is not found: " + filename);
		} catch (IOException e) {
			MyLog.e(e.toString());
		}
	}
	public static void removePreference(int keyId) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
		pref.edit().remove(_context.getString(keyId)).apply();
	}
}
