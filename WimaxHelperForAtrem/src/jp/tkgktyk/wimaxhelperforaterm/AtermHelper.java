package jp.tkgktyk.wimaxhelperforaterm;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jp.tkgktyk.wimaxhelperforaterm.my.MyFunc;
import jp.tkgktyk.wimaxhelperforaterm.my.MyLog;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;

/**
 * A helper class to access to Aterm router.
 * This saves Aterm's host name and product name to DefaultSharedPreferences.
 * A bluetooth MAC address is also saved to it by internal Info class.
 */
public class AtermHelper {
    // Default connection and socket timeout of 60 seconds.  Tweak to taste.
    private static final int SOCKET_OPERATION_TIMEOUT = 60 * 1000;

    public static final String ACTION_GET_INFO = "ACTION_GET_INFO";
	private static String KEY_BT_ADDRESS = "";
	
	/**
	 * An interface(not java's interface) to access to Aterm's information.
	 * This class uses default shared preferences to save some information.
	 */
	public static class Info implements Serializable {
		public static int INVALID_BATTERY_VALUE = 999;
		
		public long timeInMillis = Calendar.getInstance().getTimeInMillis();
		public String version = "";
		public boolean updateNotified = false;
		public int battery = INVALID_BATTERY_VALUE;
		public boolean charging = false;
		public int rssi = -999;
		public int cinr = -1;
		private Set<String> _ssidSet = new HashSet<String>();
		public boolean wanTogether = false;
		public String btName = "";
		private String _btAddress = "";
		public String status = "";
		public int antenna = -1;
		public List<String> ipAddress = new ArrayList<String>();
		
		/**
		 * Default parameters of member is set in define statement.
		 * @param context
		 */
		public Info() {
		}
		
		/**
		 * Load preferences for initialization.
		 */
		public void loadPreference() {
			_ssidSet = MyFunc.getSetPreference(R.string.pref_key_aterm_ssid);
			_btAddress = MyFunc.getStringPreference(R.string.pref_key_bt_address);
		}
		
		public boolean isValid() { return (battery != INVALID_BATTERY_VALUE); }
		
		/**
		 * check whether this is fresh.
		 * @return
		 * returns true when 10 minutes pass or invalid.
		 */
		public boolean isOld() {
			return !isValid() ||
					(Calendar.getInstance().getTimeInMillis() - timeInMillis) >= (10*60*1000);
		}

		/**
		 * Getter of router's ssids
		 * @return
		 * set of router's ssid
		 */
		public Set<String> getSsidSet() { return _ssidSet; }
		
		/**
		 * Wrapper of add function of SSID's Set Object
		 * @param ssid
		 */
		public void addSsid(String ssid) { _ssidSet.add(ssid); }
		
		/**
		 * Getter of Bluetooth MAC address.
		 * @return
		 * Bluetooth MAC address.
		 */
		public String getBtAddress() { return _btAddress; }
		
		/**
		 * Setter of Bluetooth MAC address.
		 * @param address
		 * Set MAC address.
		 */
		public void setBtAddress(String address) {
			_btAddress = address.toUpperCase(Locale.US);
		}
	}

	/**
	 * An interface to talk with WiMAX router.
	 * 
	 */
	public interface Router {
		/**
		 * Parse router information page(HTML) with {@link Document}.
		 * @param doc
		 * Document of Jsoup liked to router's information.
		 * @return
		 * a new information. Null is returned only when it fails.
		 */
		public Info parseDocument(Document doc);
		/**
		 * @return
		 * Router's standby command.
		 */
		public String getStandbyCommand();
		/**
		 * @return
		 * Router's reboot command.
		 */
		public String getRebootCommand();
		/**
		 * @return
		 * Translate to Product that represents this router's product name.
		 */
		public Product toProduct();
	}
	
	/**
	 * An extra class of Router.
	 * This represents that this application is unsupported the router.
	 */
	protected class AtermUnsupported implements Router {
		@Override
		public Info parseDocument(Document doc) {
			// return empty info
			return new Info();
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

		@Override
		public Product toProduct() { return Product.UNSUPPORTED; }
	}
	
	/**
	 * To use String in switch statement.
	 */
	public enum Product {
		/**
		 * Product name is normalized by {@link MyFunc#normalize(String)}
		 */
		WM3800R("Aterm WM3800R"),
		UNSUPPORTED("");

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
	        return UNSUPPORTED;
	    }

	}
	
	private Context _context;
	private Router _router;
	private Info _info;
	private boolean _isRouterDocked;
	private Info _lastValidInfo;
	
	public AtermHelper(Context context) {
		KEY_BT_ADDRESS = context.getString(R.string.pref_key_bt_address);
		_context = context;
		_info = new Info();
		_info.loadPreference();
		_isRouterDocked = false;
		_lastValidInfo = new Info();
		
		_setRouter(MyFunc.getStringPreference(R.string.pref_key_aterm_product));
		
		// set default host name
		if (_getHostName().length() == 0) {
			MyFunc.setStringPreference(
					R.string.pref_key_aterm_host_name,
					Const.ATERM_DEFAULT_HOST_NAME
					);
		}
	}
	
	/**
	 * Getter of host name. Get from DefaultSharedPreferences.
	 * @return
	 * Router's host name.
	 */
	private String _getHostName() {
		return MyFunc.getStringPreference(R.string.pref_key_aterm_host_name);
	}
	
	/**
	 * Getter of router class.
	 * @return
	 * connecting router.
	 */
	public Router getRouter() { return _router; }
	
	/**
	 * Setter of router class.
	 * @param product
	 * Select router by this.
	 */
	private void _setRouter(String product) {
		Product p = Product.toProduct(product);
		if (_router != null && _router.toProduct() == p) {
			// already set same router.
			return;
		}
		if (p != Product.UNSUPPORTED) {
			// save product to DefaultSharedPreferences
			MyFunc.setStringPreference(R.string.pref_key_aterm_product, product);
		}
		// select Router
		switch (p) {
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
	 * Check
	 * @return
	 * returns true if router is probably docking.
	 */
	public boolean isRouterDocked() { return _isRouterDocked; }

	/**
	 * update an Aterm information with HTTP. (executing on AsyncTask is recommended.)
	 * if connecting to router is succeeded, a new object 'Info' is stored a router's information.
	 * If any error occurred, information is replaced empty.
	 * e.g. HTTP connection error, information parsing error and so on.
	 */
	public void updateInfoForAsync() {
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
						try { // get connection and parse document.
							HttpEntity entity = response.getEntity();
							// Jsoup.parse closes InputStream after parsing
							Document doc = Jsoup.parse(entity.getContent(), "euc-jp", "http://aterm.me/index.cgi");
							String product = MyFunc.normalize(doc.select(".product span").text());
							MyLog.d(product);
							_setRouter(product);
							info = _router.parseDocument(doc);
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
		} catch (IOException e) {
			MyLog.e(e.toString());
		} finally {
			// update info object.
			if (info != null) {
				if (info.isValid()) {
					if (info.charging) {
						_isRouterDocked = true;
					} else if (info.battery > _lastValidInfo.battery) {
						_isRouterDocked = true;
					} else if (_lastValidInfo.isOld()){
						_isRouterDocked = (info.battery == _lastValidInfo.battery);
					}
					if (!info.getBtAddress().equals(_lastValidInfo.getBtAddress())) {
						MyFunc.setStringPreference(R.string.pref_key_bt_address, info.getBtAddress());
						MyFunc.setSetPreference(R.string.pref_key_aterm_ssid, info.getSsidSet());
					}
					_lastValidInfo = info;
				}
				_info = info;
				MyLog.d("Aterm's information is updated.");
			} else {
				// keep _isRouterDocked's value.
				_info = new Info();
				MyLog.w("Information update is failed.");
			}
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	/**
	 * perform {@link #forceToUpdateInfo()}.
	 */
	public void updateInfo() {
		if (_lastValidInfo.isOld())
			forceToUpdateInfo();
	}

	/**
	 * Start an update information thread implemented by {@link #updateInfoAsync()}.
	 */
	public void forceToUpdateInfo() {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				updateInfoForAsync();
				Intent broadcast = new Intent();
				broadcast.setAction(ACTION_GET_INFO);
				_context.sendBroadcast(broadcast);
			}
		})).start();
	}
	
	/** Getter of Info */
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
		AuthScope scope = new AuthScope(_getHostName(), Const.ATERM_PORT);
		client.getCredentialsProvider().setCredentials(scope, credentials);
		
		return client;
	}
	
	/**
	 * Start wake up service implemented by {@link WakeUpService}
	 * @return
	 * if return false, bluetooth address is invalid. otherwise return true.
	 */
	public boolean wakeUp() {
		String address = _info.getBtAddress();
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			MyLog.w("invalid bluetooth address: " + address);
			return false;
		}
		Intent intent = new Intent(_context, WakeUpService.class);
		intent.putExtra(KEY_BT_ADDRESS, address);
		_context.startService(intent);
		
		// reset _isRouterDocked and _lastValidInfo
		_isRouterDocked = false;
		_lastValidInfo.battery = Info.INVALID_BATTERY_VALUE;
		
		return true;
	}
	
	/**
	 * A class for waking up Aterm with bluetooth.
	 *
	 */
	public static class WakeUpService extends Service {
		private BluetoothHelper _bt;
		private String _address;
		private boolean _needsEnableControl;

		@Override
		public IBinder onBind(Intent intent) {
			// TODO 自動生成されたメソッド・スタブ
			return null;
		}
		
		/**
		 * A broadcast receiver to catch bluetooth enable event for trigger starting wake up sequence.
		 */
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
			
			MyLog.d("create wake up service.");
			
			_bt = new BluetoothHelper();
			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			this.registerReceiver(_receiver, filter);
		}
		
		@Override
		public void onDestroy() {
			super.onDestroy();
			
			MyLog.d("destroy wake up service.");
			
			this.unregisterReceiver(_receiver);
		}
	
		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			_address = intent.getStringExtra(KEY_BT_ADDRESS);
			
			// if bluetooth is not enable, enable it and wait for BluetoothAdapter.STATE_ON.
			if (!_bt.isEnabled()) {
				_needsEnableControl = true;
				(new Thread(new Runnable() {
					@Override
					public void run() { _bt.enable(); }
				})).start();
			} else {
				_needsEnableControl = false;
				_wakeUp();
			}
			
			return Service.START_STICKY;
		}
		
		/**
		 * Start wake up thread and stop service.
		 */
		private void _wakeUp() {
			(new Thread(new Runnable() {
				@Override
				public void run() {
					MyLog.i("wake up.");
					
					// wake up Aterm
					_bt.connect(
							_address,
							MyFunc.getLongPreference(R.string.pref_key_bt_connect_timeout)
							);
					
					// after treatment
					if (_needsEnableControl)
						_bt.disable();
				}
			})).start();
			
			this.stopSelf();
		}
	}
	
	/**
	 * Check whether wifi is connected.
	 * If check whether catch the router's radio, scanning time of wifi (5-15sec)
	 * is not reasonable after wifi sleep.
	 * @return
	 * return true only when wifi is connected.
	 */
	public boolean isWifiConnected() {
		ConnectivityManager cm
		= (ConnectivityManager)_context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return info.isConnected();
	}
	
	/**
	 * Make URI string of Aterm command.
	 * @param cmd
	 * Specify Aterm's command name. Usually it is a last path segment of full command URI.
	 * @return
	 * command URI string.
	 */
	private String _makeCommand(String cmd) {
		return new Uri.Builder()
		.scheme("http")
		.authority(_getHostName())
		.path("index.cgi")
		.appendPath(cmd)
		.build()
		.toString();
	}
	
	/**
	 * Execute Aterm command.
	 * @param cmd
	 * Specify Aterm's command name. Usually it is a last path segment of full command URI.
	 */
	private void _command(String cmd) {
		DefaultHttpClient httpClient = _newClient();
		try {
			HttpGet request = new HttpGet(_makeCommand(cmd));
			// just access, router executes command.
			httpClient.execute(request);
		} catch (IOException e) {
			MyLog.e(e.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	/** execute standby command */
	public void standby() {
		_command(_router.getStandbyCommand());
	}

	/** execute reboot command */
	public void reboot() {
		_command(_router.getRebootCommand());
	}
}
