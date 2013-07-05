package jp.tkgktyk.wimaxhelperforaterm;

import jp.tkgktyk.wimaxhelperforaterm.YesNoPreference.OnYesClickedListner;
import jp.tkgktyk.wimaxhelperforaterm.my.MyApplication;
import jp.tkgktyk.wimaxhelperforaterm.my.MyFunc;
import jp.tkgktyk.wimaxhelperforaterm.my.MyPreferenceActivity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

/**
 * An activity executed launch application.
 * This extends PreferenceActivity through MyPreferenceActivity.
 * So acitivity's layout and performing are following Preferences.
 */
public class MainActivity extends MyPreferenceActivity {
	/**
	 * Receives these actions.
	 *	ACTION_GET_INFO
	 *		When Aterm's information is updated, notify the new information.
	 */
	private final BroadcastReceiver _receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			
			if (action.equals(AtermHelper.ACTION_GET_INFO))
				_onGetInfo();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.main_preference);
		this.setTitle(MyFunc.getAppTitle());

		// for service
		this.startService(new Intent(this, MainService.class));
		
		// initialize
		this._onGetInfo();
		IntentFilter filter = new IntentFilter();
		filter.addAction(AtermHelper.ACTION_GET_INFO);
		this.registerReceiver(_receiver, filter);
		
		// set commands
		_setCommand(R.string.pref_key_stop_service, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				stopService(new Intent(MainActivity.this, MainService.class));
			}
		});
		_setCommand(R.string.pref_key_update_information, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				if (_getAterm().forceUpdateInfo())
					MyFunc.showToast("���X�V��");
				else
					MyFunc.showFailedToast("�R�}���h���s", "���[�^�[�Ɩ��ڑ������̃R�}���h�����s���ł��B");
			}
		});
		_setCommand(R.string.pref_key_wake_up, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				_getAterm().wakeUp();
				MyFunc.showToast("�����[�g�N���T�[�r�X�N��");
			}
		});
		_setCommand(R.string.pref_key_standby, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				if (_getAterm().standby())
					MyFunc.showToast("�X�^���o�C���s��");
				else
					MyFunc.showFailedToast("�R�}���h���s", "���[�^�[�Ɩ��ڑ������̃R�}���h�����s���ł��B");
			}
		});
		_setCommand(R.string.pref_key_reboot, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				if (_getAterm().reboot())
					MyFunc.showToast("�ċN����");
				else
					MyFunc.showFailedToast("�R�}���h���s", "���[�^�[�Ɩ��ڑ������̃R�}���h�����s���ł��B");
			}
		});
		_setCommand(R.string.pref_key_quick_web, new OnYesClickedListner() {
			@Override
			public void onYesClicked(Preference preference) {
				Uri uri = Uri.fromParts("http", _getAterm().getHostName(), null);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		});
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		this.unregisterReceiver(_receiver);
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
	
	private void _onGetInfo() {
		AtermHelper.Product product = _getAterm().getRouter().toProduct();
		String summary = "�^�b�v���ď����X�V���Ă�������";
		if (product != AtermHelper.Product.UNSUPPORTED) {
			summary = product.toString();
			if (_getAterm().getInfo().updateNotified) {
				summary += "�i�A�b�v�f�[�g����j";
			}
		}
		YesNoPreference yesno = (YesNoPreference)this.findPreference(R.string.pref_key_update_information);
		yesno.setSummary(summary);
	}
	
	private AtermHelper _getAterm() { return ((MyApplication)this.getApplication()).getAterm(); }
	
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
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
		return false;
	}
}
