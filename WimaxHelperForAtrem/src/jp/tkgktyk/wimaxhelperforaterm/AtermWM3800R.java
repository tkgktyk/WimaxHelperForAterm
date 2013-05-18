package jp.tkgktyk.wimaxhelperforaterm;

import jp.tkgktyk.wimaxhelperforaterm.AtermHelper.Info;
import jp.tkgktyk.wimaxhelperforaterm.AtermHelper.Product;
import jp.tkgktyk.wimaxhelperforaterm.AtermHelper.Router;
import jp.tkgktyk.wimaxhelperforaterm.my.MyLog;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;

/**
 * A Router class for Aterm WM3800R.
 */
public class AtermWM3800R implements Router {
	
	/**
	 * To use String in switch statement during parse document. Document has a
	 * information table that have two column, 'key' and 'value'. This enum
	 * corresponds to key column.
	 */
	public enum Key {
		FIRMWARE_VERSION("ファームウェアバージョン"),
		FIRMWARE_UPDATE("ファームウェア更新通知"),
		BATTERY("電池残量"),
		RSSI("RSSI"),
		CINR("CINR"),
		SSID("ネットワーク名(SSID)"),
		WAN_TOGETHER("WiFi WAN側連動"),
		BLUETOOTH_NAME("Bluetooth名"),
		BLUETOOTH_ADDRESS("MACアドレス(Bluetooth)"),
		CONNECTION_STATUS("接続状態"),
		ANTENNA_LEVEL("電波状態"),
		IP_ADDRESS("IPアドレス／ネットマスク"),
		UNKNOWN("");

	    private final String _key;

	    private Key(String key) {
	        _key = key;
	    }

	    @Override
	    public String toString() {
	        return _key;
	    }

	    public static Key toEnum(String fullkey) {
	    	if (fullkey == null)
	    		return UNKNOWN;
	        for (Key key : values()) {
	            if (fullkey.contains(key.toString()))
	            	return key;
	        }
	        return UNKNOWN;
	    }

	}

	@Override
	public Info parseDocument(Document doc, Context context) {
		Info info = new AtermHelper.Info(context);
		Elements trs = doc.select(".table_common .small_item_info_tr");
		for (Element tr : trs) {
			Element key;
			Element value;
			Elements tds = tr.select(".small_item_td");
			if (tds.isEmpty() || (key = tds.first()) == null)
				continue;
			tds = tr.select(".small_item_td2");
			if (tds.isEmpty() || (value = tds.first()) == null)
				continue;

			String v = value.text().trim();
			try {
				switch (Key.toEnum(key.text())) {
				case FIRMWARE_VERSION:
					info.version = v;
					break;
				case FIRMWARE_UPDATE:
					// implement is still nothing
					break;
				case BATTERY:
					info.charging = v.contains("充電中");
					if (info.charging) {
						int start = v.indexOf('■');
						info.battery = (start != -1)? (v.lastIndexOf('■') - start + 1) * 10: 0;
					} else {
						int start = v.indexOf('（');
						info.battery = Integer.parseInt(v.substring(start + 1, v.indexOf('％')));
					}
					break;
				case RSSI:
					info.rssi = Integer.parseInt(v.substring(0, v.indexOf(' ')));
					break;
				case CINR:
					info.cinr = Integer.parseInt(v.substring(0, v.indexOf(' ')));
					break;
				case SSID:
					info.addSsid(v);
					break;
				case WAN_TOGETHER:
					info.wanTogether = v.equals("enable");
					break;
				case BLUETOOTH_NAME:
					info.btName = v;
					break;
				case BLUETOOTH_ADDRESS:
					info.setBtAddress(v);
					break;
				case CONNECTION_STATUS:
					info.status = v;
					break;
				case ANTENNA_LEVEL:
					info.antenna = Integer.parseInt(v.substring(v.indexOf("：") + 1));
					break;
				case IP_ADDRESS:
					// remove sub net mask.
					info.ipAddress.add(v.substring(0, v.indexOf('/')));
					break;
				default:
					// do nothing
				}
			} catch (Exception e) {
				MyLog.e("parse error: " + v + "@" + key.text());
				MyLog.e(e.toString());
			}
		}

		return info;
	}

	@Override
	public String getStandbyCommand() {
		return Const.ATERM_CMD_STANDBY_BT;
	}

	@Override
	public String getRebootCommand() {
		return Const.ATERM_CMD_REBOOT;
	}

	@Override
	public Product toProduct() { return Product.WM3800R; }
}
