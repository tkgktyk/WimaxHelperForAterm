package jp.tkgktyk.wimaxhelperforaterm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class MainService extends Service {
	
	private static final int NOTIFICATION_ID = R.xml.main_preference;

	private AtermHelper _aterm;
	
	private final BroadcastReceiver _receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			
			if (action.equals(AtermHelper.ACTION_GET_INFO)) {
				_showNotification();
			} else if (action.equals(Intent.ACTION_SCREEN_ON)) {
				if (!_aterm.hasConnection(MainService.this)) {
					MyLog.i("wake up when screen on.");
					_showWakeUpNotification();
					_aterm.wakeUp();
				} else {
					_aterm.updateInfo(context);
				}
			} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				// wifi event only
				NetworkInfo info	= intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				switch (info.getState()) {
				case CONNECTED:
					MyLog.i("wifi is connected.");
					_aterm.updateInfo(context);
					break;
				case DISCONNECTED:
					MyLog.i("wifi is disconnected.");
					_showPrepareNotification();
					break;
				}
			} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int state
				= intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
				switch (state) {
				case WifiManager.WIFI_STATE_DISABLING:
				case WifiManager.WIFI_STATE_DISABLED:
				case WifiManager.WIFI_STATE_UNKNOWN:
					_showPrepareNotification();
					break;
				}
			}
		}
	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		MyLog.v("start main service");
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(AtermHelper.ACTION_GET_INFO);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		this.registerReceiver(_receiver, filter);

		_showPrepareNotification();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		MyLog.v("destroy main service");
		
		this.unregisterReceiver(_receiver);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		_aterm = new AtermHelper(this);
		
		return Service.START_STICKY;
	}
	
	private void _showNotification(String content, int antenna) {
		// if API level is greater than 11, use Notification.Builder.
		Notification notification = new Notification(
				android.R.drawable.ic_menu_sort_by_size,
				content,
				System.currentTimeMillis()
				);
		notification.number = antenna;
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent
		= PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(
				this,
				this.getString(R.string.app_name),
				content,
				contentIntent
				);

		this.startForeground(NOTIFICATION_ID, notification);
	}
	
	private void _showNotification() {
		AtermHelper.Info info = _aterm.getInfo();
		String content = String.format("電波: %d本、バッテリー: %s", info.antenna, info.getBatteryText());
		_showNotification(content, info.antenna);
	}

	private void _showPrepareNotification() {
		_showNotification("WiMAX準備中", 0);
	}
	
	private void _showWakeUpNotification() {
		_showNotification("リモード起動中", 0);
	}
}
