package jp.tkgktyk.wimaxhelperforaterm;

import jp.tkgktyk.wimaxhelperforaterm.YesNoPreference.OnYesClickedListner;
import jp.tkgktyk.wimaxhelperforaterm.my.MyApplication;
import jp.tkgktyk.wimaxhelperforaterm.my.MyFunc;
import jp.tkgktyk.wimaxhelperforaterm.my.MyLog;
import jp.tkgktyk.wimaxhelperforaterm.my.MyPreferenceActivity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.Menu;

/**
 * An activity executed launch application.
 * This extends PreferenceActivity through MyPreferenceActivity.
 * So acitivity's layout and performing are following Preferences.
 */
public class MainActivity extends MyPreferenceActivity {
	
	private AtermHelper _aterm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.main_preference);

		// for service
		this.startService(new Intent(this, MainService.class));
		
		// to access to aterm
		_aterm = ((MyApplication)this.getApplication()).getAterm();
		
		// set commands
		_setCommand(R.string.pref_key_stop_service, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				stopService(new Intent(MainActivity.this, MainService.class));
			}
		});
		_setCommand(R.string.pref_key_wake_up, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				if (_aterm.wakeUp()) {
					MyFunc.showToast(MainActivity.this, "リモート起動中");
				} else {
					MyFunc.showToast(MainActivity.this, "リモート起動に失敗しました");
					MyLog.w("failed to wake up");
				}
			}
		});
		_setCommand(R.string.pref_key_standby, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				_aterm.standby();
				MyFunc.showToast(MainActivity.this, "スタンバイ状態に移行中");
			}
		});
		_setCommand(R.string.pref_key_reboot, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				_aterm.reboot();
				MyFunc.showToast(MainActivity.this, "再起動中");
			}
		});
	}

	/**
	 * Set an OnYesClickedListner to the YesNoPreference specified id.
	 * @param id
	 * specify YesNoPreference to link command.
	 * @param listner
	 * execution command.
	 */
	private void _setCommand(int id, OnYesClickedListner listner) {
		YesNoPreference yesno = (YesNoPreference)this.findPreference(id);
		yesno.setOnYesClickedListener(listner);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * A workaround in https://code.google.com/p/android/issues/detail?id=4611.
	 * When use nested preferenceScreen, the child preferenceScreen is not applied application theme.
	 */
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		super.onPreferenceTreeClick(preferenceScreen, preference);
		if (preference!=null)
			if (preference instanceof PreferenceScreen)
				if (((PreferenceScreen)preference).getDialog()!=null)
					((PreferenceScreen)preference).getDialog().getWindow().getDecorView().setBackgroundDrawable(this.getWindow().getDecorView().getBackground().getConstantState().newDrawable());
		return false;
	}
}
