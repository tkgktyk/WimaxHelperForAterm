package jp.tkgktyk.wimaxhelperforaterm.my;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class MyPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	protected Preference findPreference(int id) {
		return this.findPreference(this.getString(id));
	}

	public String getSharedString(int id) {
		return getSharedString(getString(id));
	}
	public String getSharedString(String key) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		return pref.getString(key, "");
	}
}
