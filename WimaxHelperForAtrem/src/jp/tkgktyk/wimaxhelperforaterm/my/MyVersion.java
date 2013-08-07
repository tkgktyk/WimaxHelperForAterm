package jp.tkgktyk.wimaxhelperforaterm.my;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;

public class MyVersion {
	public static final int BASE = 1000;

	int major = 0;
	int minor = 0;
	int revision = 0;
	
	public MyVersion(String version) { set(version); }
	
	public MyVersion(Context context) {
		// current package's version
		PackageManager pm = context.getPackageManager();
		String version = null;
		try {
			PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
			version = info.versionName;
		} catch (NameNotFoundException e) {
			MyLog.e(e.toString());
		}
		if (version == null)
			version = "1.0";
		set(version);
	}
	
	public void set(String version) {
		if (TextUtils.isEmpty(version))
			return;

		String[] v = version.split("\\.");
		int n = v.length;
		if (n >= 1)
			major = Integer.parseInt(v[0]);
		if (n >= 2)
			minor = Integer.parseInt(v[1]);
		if (n >= 3)
			revision = Integer.parseInt(v[2]);
	}
	
	public int toInt() {
		return major*BASE*BASE + minor*BASE + revision;
	}
	
	public boolean isNewerThan(MyVersion v) {
		return toInt() > v.toInt();
	}
	
	@Override
	public String toString() {
		String version = "";
		if (major >= 0)
			version += Integer.toString(major);
		if (minor >= 0)
			version += "." + Integer.toString(minor);
		if (revision > 0)
			version += "." + Integer.toString(revision);
		return version;
	}
}
