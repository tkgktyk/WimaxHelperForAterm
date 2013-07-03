package jp.tkgktyk.wimaxhelperforaterm;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jp.tkgktyk.wimaxhelperforaterm.my.MyFunc;
import jp.tkgktyk.wimaxhelperforaterm.my.MyLog;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

/**
 * A helper class to access to Aterm router.
 * This saves Aterm's host name and product name to DefaultSharedPreferences.
 * A bluetooth MAC address is also saved to it by internal Info class.
 */
public class AtermHelper {

    public static final String ACTION_GET_INFO = Const.PACKAGE_NAME + ".AtermHelper.ACTION_GET_INFO";
	
	/**
	 * A HTTP client class for accessing to AtermRouter.
	 */
	private static class HttpClient extends DefaultHttpClient {
	    // Default connection and socket timeout of 60 seconds.  Tweak to taste.
	    private static final int SOCKET_OPERATION_TIMEOUT = 60 * 1000;
	    
		public HttpClient(String hostName) {
			// create HTTP client has above params with basic authentication
			super(_makeDefaultParams());
			Credentials credentials = new UsernamePasswordCredentials(
					Const.ATERM_BASIC_USERNAME,
					Const.ATERM_BASIC_PASSWORD
					);
			AuthScope scope = new AuthScope(hostName, Const.ATERM_PORT);
			this.getCredentialsProvider().setCredentials(scope, credentials);
		}
		
		private static HttpParams _makeDefaultParams() {
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

			return params;
		}
	}
	
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
		private Set<String> _ssidSet = new HashSet<String>();
		public String btName = "";
		private String _btAddress = "";
		public int antenna = -1;
		
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
					(MyFunc.elapsedTimeInMillis(timeInMillis) >= Const.UPDATE_INTERVAL_IN_MILLIS);
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
	public static abstract class Router {
		/**
		 * Parse router information page(HTML) with {@link Document}.
		 * @param doc
		 * Document of Jsoup liked to router's information.
		 * @return
		 * a new information. Null is returned only when it fails.
		 */
		public abstract Info parseDocument(Document doc);
		/**
		 * @return
		 * Router's standby command.
		 */
		public String getStandbyCommand() { return Const.ATERM_CMD_STANDBY_BT; }
		/**
		 * @return
		 * Router's reboot command.
		 */
		public String getRebootCommand() { return Const.ATERM_CMD_REBOOT; }
		/**
		 * @return
		 * Translate to Product that represents this router's product name.
		 */
		public abstract Product toProduct();
	}
	
	/**
	 * An extra class of Router.
	 * This represents that this application is unsupported the router.
	 */
	protected class AtermUnsupported extends Router {
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
		UNSUPPORTED("–¢‘Î‰ž");

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
	private boolean _updateInfoLocked;
	
	public AtermHelper(Context context) {
		_context = context;
		_info = new Info();
		_info.loadPreference();
		_isRouterDocked = false;
		_lastValidInfo = new Info();
		_updateInfoLocked = false;
		
		_setRouter(MyFunc.getStringPreference(R.string.pref_key_aterm_product));
		
		// set default host name
		if (getHostName().length() == 0) {
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
	public String getHostName() {
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
		MyLog.d(_router.toProduct().toString());
	}
	
	/**
	 * Check
	 * @return
	 * returns true if router is probably docking.
	 */
	public boolean isRouterDocked() { return _isRouterDocked; }

	/** Getter of Info */
	public Info getInfo() { return _info; }

	/**
	 * Start wake up service implemented by {@link WakeUpService}
	 */
	public void wakeUp() {
		String address = _info.getBtAddress();
		Intent intent = new Intent(_context, WakeUpService.class);
		intent.putExtra(WakeUpService.KEY_BT_ADDRESS, address);
		_context.startService(intent);
		
		// reset _isRouterDocked and _lastValidInfo
		_isRouterDocked = false;
		_lastValidInfo.battery = Info.INVALID_BATTERY_VALUE;
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
		.authority(getHostName())
		.path("index.cgi")
		.appendPath(cmd)
		.build()
		.toString();
	}
	
	/**
	 * An enum to select commands.
	 */
	private enum Command {
		UPDATE_INFO,
		STANDBY,
		REBOOT;
	}

	/**
	 * Execute Aterm command.
	 * @param cmd
	 * Specify Aterm's command name. Usually it is a last path segment of full command URI.
	 * @return
	 * returns false if cannot execute command. Reasons of 'cannot' are
	 * WiFi is not connected or now other command is executing.
	 */
	private boolean _command(final Command cmd) {
		if (!this.isWifiConnected())
			return false;
		synchronized (this) {
			if (_updateInfoLocked) {
				return false;
			} else {
				_updateInfoLocked = true;
				(new Thread(new Runnable() {
					@Override
					public void run() {
						HttpClient client = new HttpClient(getHostName());
						try {
							// update information
							// need to create connection when execute other commands.
							Map<String, String> hiddens = _updateInfoForAsync(client);
							Intent broadcast = new Intent();
							broadcast.setAction(ACTION_GET_INFO);
							_context.sendBroadcast(broadcast);

							// select command
							String cmdStr = "";
							switch (cmd) {
							case STANDBY:
								cmdStr = _makeCommand(_router.getStandbyCommand());
								break;
							case REBOOT:
								cmdStr = _makeCommand(_router.getRebootCommand());
								break;
							}
							// execute command
							if (cmdStr.length() != 0) {
								try {
									HttpPost method = new HttpPost(cmdStr);
									List<NameValuePair> params = new ArrayList<NameValuePair>();
									for (String key: hiddens.keySet()) {
										params.add(new BasicNameValuePair(key, hiddens.get(key)));
									}
									method.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

									// just access, router executes command.
									client.execute(method);
								} catch (IOException e) {
									// reach here when command succeeded.
								}
							}
						} finally {
							client.getConnectionManager().shutdown();
						}
						_updateInfoLocked = false;
					}
				})).start();
				return true; // to execute command is success.
			}
		}
	}

	/**
	 * update an Aterm information with HTTP. (executing on AsyncTask is recommended.)
	 * if connecting to router is succeeded, a new object 'Info' is stored a router's information.
	 * If any error occurred, information is replaced empty.
	 * e.g. HTTP connection error, information parsing error and so on.
	 */
	private Map<String, String> _updateInfoForAsync(DefaultHttpClient client) {
		Info info = null;
		final Map<String, String> hiddenMap = new HashMap<String, String>();
		// access to Aterm information page
		try {
			HttpGet request = new HttpGet(_makeCommand(Const.ATERM_CMD_GET_INFO));
			info = client.execute(request, new ResponseHandler<Info>(){
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
							_setRouter(product);
							info = _router.parseDocument(doc);
							// parse hidden values
							Elements hiddens = doc.select("input[type=hidden]");
							if (hiddens != null) {
								for (Element e : hiddens) {
									String name = MyFunc.normalize(e.attr("name"));
									String value = MyFunc.normalize(e.attr("value"));
									hiddenMap.put(name, value);
								}
							}
						} catch (IOException e) {
							MyLog.e(e.toString());
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
		}
		return hiddenMap;
	}
	
	/**
	 * perform {@link #forceToUpdateInfo()} if need.
	 * @return
	 * returns false if cannot execute command. Reasons of 'cannot' are
	 * WiFi is not connected or now other command is executing.
	 */
	public boolean updateInfo() {
		if (_lastValidInfo.isOld())
			return forceUpdateInfo();
		else
			return false;
	}

	/**
	 * Start an update information thread implemented by {@link #updateInfoAsync()}.
	 * @return
	 * returns false if cannot execute command. Reasons of 'cannot' are
	 * WiFi is not connected or now other command is executing.
	 */
	public boolean forceUpdateInfo() { return _command(Command.UPDATE_INFO); }
	
	/**
	 * execute standby command 
	 * @return
	 * returns false if cannot execute command. Reasons of 'cannot' are
	 * WiFi is not connected or now other command is executing.
	 */
	public boolean standby() { return _command(Command.STANDBY); }

	/** execute reboot command
	 * @return
	 * returns false if cannot execute command. Reasons of 'cannot' are
	 * WiFi is not connected or now other command is executing.
	 */
	public boolean reboot() { return _command(Command.REBOOT); }
}
