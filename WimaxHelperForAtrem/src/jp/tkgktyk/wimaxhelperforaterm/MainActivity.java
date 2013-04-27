package jp.tkgktyk.wimaxhelperforaterm;

import jp.tkgktyk.wimaxhelperforaterm.YesNoPreference.OnYesClickedListner;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.view.Menu;

public class MainActivity extends MyPreferenceActivity {
	
	AtermHelper _aterm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.main_preference);

		// for service
		this.startService(new Intent(this, MainService.class));
		
		// to access to aterm
		_aterm = new AtermHelper(this);
		
		YesNoPreference yesno = (YesNoPreference)this.findPreference(R.string.pref_key_wake_up);
		yesno.setOnYesClickedListener(new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				if (_aterm.wakeUp()) {
					MyFunc.showToast(MainActivity.this, "�����[�g�N����");
				} else {
					MyFunc.showToast(MainActivity.this, "�����[�g�N���Ɏ��s���܂���");
					MyLog.w("failed to wake up");
				}
			}
		});
		yesno = (YesNoPreference)this.findPreference(R.string.pref_key_standby);
		yesno.setOnYesClickedListener(new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				_aterm.standby();
				MyFunc.showToast(MainActivity.this, "�X�^���o�C��ԂɈڍs��");
			}
		});
		yesno = (YesNoPreference)this.findPreference(R.string.pref_key_reboot);
		yesno.setOnYesClickedListener(new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				_aterm.reboot();
				MyFunc.showToast(MainActivity.this, "�ċN����");
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
