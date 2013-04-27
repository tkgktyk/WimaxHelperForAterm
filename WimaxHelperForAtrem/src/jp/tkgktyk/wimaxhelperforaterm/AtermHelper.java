package jp.tkgktyk.wimaxhelperforaterm;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class AtermHelper {
    // Default connection and socket timeout of 60 seconds.  Tweak to taste.
    private static final int SOCKET_OPERATION_TIMEOUT = 60 * 1000;

    public static final String ACTION_GET_INFO = "ACTION_GET_INFO";
	private static String KEY_BT_ADDRESS = "";
	private static String KEY_HOST_NAME = "";
	private static String KEY_PRODUCT = "KEY_PRODUCT";
	
	public static class Info implements Serializable {
		private Context _context;
		public String version = "";
		public boolean updateNotified = false;
		private int _battery = -1;
		public boolean charging = false;
		public int rssi = -999;
		public int cinr = -1;
		public boolean wanTogether = false;
		public String btName = "";
		private String _btAddress = "";
		public String status = "";
		public int antenna;
		public String ipAddress = "";
		
		public Info(Context context) {
			_context = context;
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
			_btAddress = pref.getString(KEY_BT_ADDRESS, "");
		}
		
		public String getBatteryText() {
			String text = String.format("%d%%", _battery);
			return charging? text + "+": text;
		}
		public void setBattery(int battery) {
			_battery = battery;
		}
		
		public String getBtAddress() {
			return _btAddress;
		}
		public void setBtAddress(String address) {
			String newAddress = address.toUpperCase(Locale.US);
			if (!_btAddress.equals(newAddress)) {
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
				pref.edit().putString(KEY_BT_ADDRESS, newAddress).commit();
				_btAddress = newAddress;
			}
		}
	}

	public interface Router {
		public Info parseDocument(Document doc, Context context);
		public String getStandbyCommand();
		public String getRebootCommand();
	}
	
	protected class AtermUnsupported implements Router {
		@Override
		public Info parseDocument(Document doc, Context context) {
			// return empty info
			return new Info(context);
		}

		@Override
		public String getStandbyCommand() {
			// return empty
			return "";
		}
		
		@Override
		public String getRebootCommand() {
			// return empty
			return "";
		}
	}
	
	public enum Product {
		WM3800R("A t e r m　W M 3 8 0 0 R"),
		NONE("");

	    private final String _name;

	    private Product(String name) {
	        _name = name;
	    }

	    @Override
	    public String toString() {
	        return _name;
	    }

	    public static Product toProduct(String name) {
	        for (Product product : values()) {
	            if (product.toString().equals(name))
	            	return product;
	        }
	        return NONE;
	    }

	}
	
	private Context _context;
	private Router _router;
	private Info _info;
	
	public AtermHelper(Context context) {
		KEY_BT_ADDRESS = context.getString(R.string.pref_key_bt_address);
		KEY_HOST_NAME = context.getString(R.string.pref_key_aterm_host_name);
		_context = context;
		_info = new Info(context);
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
		_setRouter(pref.getString(KEY_PRODUCT, ""));
		
		// initialize host name
		if (_getHostname().length() == 0) {
			pref = PreferenceManager.getDefaultSharedPreferences(_context);
			pref.edit()
			.putString(KEY_HOST_NAME, Const.ATERM_DEFAULT_HOST_NAME)
			.commit();
		}
	}
	
	private String _getHostname() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
		return pref.getString(KEY_HOST_NAME, "");
	}
	
	private void _setRouter(String product) {
		switch (Product.toProduct(product)) {
		case WM3800R:
			_router = new AtermWM3800R();
			break;
		default:
			// unsupported product
			_router = new AtermUnsupported();
			break;
		}
	}

	/**
	 * update an Aterm information with HTTP. (executing on AsyncTask is recommended.)
	 * if connecting to router is succeeded, a new object 'Info' is stored router's information.
	 * when any errors are occurred, information is not update.
	 * e.g. HTTP connection error, information parsing error and so on.
	 */
	public void updateInfoAsync() {
		Info info = null;
		DefaultHttpClient httpClient = _newClient();
		// access to Aterm information page
		try {
			HttpGet request = new HttpGet(_makeCommand(Const.ATERM_CMD_GET_INFO));
			info = httpClient.execute(request, new ResponseHandler<Info>(){
				@Override
				public Info handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					Info info = null;
					switch (response.getStatusLine().getStatusCode()) {
					case HttpStatus.SC_OK:
						try {
							HttpEntity entity = response.getEntity();
							// Jsoup.parse closes InputStream after parsing
							Document doc = Jsoup.parse(entity.getContent(), "euc-jp", "http://aterm.me/index.cgi");
							MyLog.d(doc.title());
							String product = doc.select(".product span").text();
							MyLog.d(product);
							if (product != null && product.length() > 0) {
								SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_context);
								pref.edit()
								.putString(KEY_PRODUCT, product)
								.commit();
							}
							_setRouter(product);
							info = _router.parseDocument(doc, _context);
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							// Jsoup.parse closes InputStream after parsing
							// so calling close() is not need. (should not close by myself)
//							if (entity != null)
//								entity.getContent().close();
						}
					}
					return info;
				}
			});
			
			// update only when parsing is succeeded.
			if (info != null) {
				_info = info;
				MyLog.v("Aterm's information is updated.");
			}
		} catch (IOException e) {
			MyLog.e(e.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	public void updateInfo(final Context context) {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				updateInfoAsync();
				Intent broadcast = new Intent();
				broadcast.setAction(ACTION_GET_INFO);
				context.sendBroadcast(broadcast);
			}
		})).start();
	}
	
	public Info getInfo() { return _info; }

	protected DefaultHttpClient _newClient() {
		HttpParams params = new BasicHttpParams();

		// Turn off stale checking.  Our connections break all the time anyway,
		// and it's not worth it to pay the penalty of checking every time.
		HttpConnectionParams.setStaleCheckingEnabled(params, false);

		HttpConnectionParams.setConnectionTimeout(params, SOCKET_OPERATION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, SOCKET_OPERATION_TIMEOUT);
		HttpConnectionParams.setSocketBufferSize(params, 8192);

		// Don't handle redirects -- return them to the caller.  Our code
		// often wants to re-POST after a redirect, which we must do ourselves.
		HttpClientParams.setRedirecting(params, false);

		// Set the specified user agent and register standard protocols.
		HttpProtocolParams.setUserAgent(params, Const.USER_AGENT);

		// create HTTP client has above params with basic authentication
		DefaultHttpClient client = new DefaultHttpClient(params);
		Credentials credentials = new UsernamePasswordCredentials(
				Const.ATERM_BASIC_USERNAME,
				Const.ATERM_BASIC_PASSWORD
				);
		AuthScope scope = new AuthScope(_getHostname(), Const.ATERM_PORT);
		client.getCredentialsProvider().setCredentials(scope, credentials);
		
		return client;
	}
	
	public boolean wakeUp() {
		String address = _info.getBtAddress();
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			MyLog.w("invalid bluetooth address: " + address);
			return false;
		}
		Intent intent = new Intent(_context, WakeUpService.class);
		intent.putExtra(KEY_BT_ADDRESS, address);
		_context.startService(intent);
		
		return true;
	}
	
	public static class WakeUpService extends Service {
		
		private BluetoothHelper _bt;
		private String _address;

		@Override
		public IBinder onBind(Intent intent) {
			// TODO 自動生成されたメソッド・スタブ
			return null;
		}
		
		private final BroadcastReceiver _receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();
				
				if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
					final int state
					= intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
					switch (state) {
					case BluetoothAdapter.STATE_OFF:
						break;
					case BluetoothAdapter.STATE_ON:
						_wakeUp();
						break;
					}
				}
			}
		};
		@Override
		public void onCreate() {
			super.onCreate();
			
			MyLog.v("create wake up service.");
			
			_bt = new BluetoothHelper();
			IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
			this.registerReceiver(_receiver, filter);
		}
		
		@Override
		public void onDestroy() {
			super.onDestroy();
			
			MyLog.v("destroy wake up service.");
			
			this.unregisterReceiver(_receiver);
		}
		
		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			_address = intent.getStringExtra(KEY_BT_ADDRESS);
			if (!_bt.isEnabled()) {
				(new Thread(new Runnable() {
					@Override
					public void run() { _bt.enable(); }
				})).start();
			} else {
				_wakeUp();
			}
			
			return Service.START_STICKY;
		}
		
		private void _wakeUp() {
			(new Thread(new Runnable() {
				@Override
				public void run() {
					MyLog.i("does wake up.");
					
					// wake up Aterm
					_bt.connect(_address, 1000);
					
					// after treatment
					if (_bt.needsEnableControl())
						_bt.disable();
				}
			})).start();

			this.stopSelf();
		}
	}
	
	public boolean hasConnection(Context context) {
		ConnectivityManager cm
		= (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return info.isConnectedOrConnecting();
	}
	
	private String _makeCommand(String cmd) {
		return new Uri.Builder()
		.scheme("http")
		.authority(_getHostname())
		.path("index.cgi")
		.appendPath(cmd)
		.build()
		.toString();
	}
	
	private void _command(String cmd) {
		DefaultHttpClient httpClient = _newClient();
		// access to Aterm information page
		try {
			HttpGet request = new HttpGet(_makeCommand(cmd));
			// just access, router becomes to standby.
			httpClient.execute(request);

		} catch (IOException e) {
			MyLog.e(e.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	public void standby() {
		_command(_router.getStandbyCommand());
	}
	
	public void reboot() {
		_command(_router.getRebootCommand());
	}
}
