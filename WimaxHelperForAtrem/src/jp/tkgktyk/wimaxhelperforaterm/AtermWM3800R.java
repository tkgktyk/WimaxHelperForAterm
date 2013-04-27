package jp.tkgktyk.wimaxhelperforaterm;

import jp.tkgktyk.wimaxhelperforaterm.AtermHelper.Info;
import jp.tkgktyk.wimaxhelperforaterm.AtermHelper.Router;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;

public class AtermWM3800R implements Router {
	
	public enum Key {
		FIRMWARE_VERSION("�t�@�[���E�F�A�o�[�W����"),
		FIRMWARE_UPDATE("�t�@�[���E�F�A�X�V�ʒm"),
		BATTERY("�d�r�c��"),
		RSSI("RSSI"),
		CINR("CINR"),
		WAN_TOGETHER("WiFi WAN���A��"),
		BLUETOOTH_NAME("Bluetooth��"),
		BLUETOOTH_ADDRESS("MAC�A�h���X(Bluetooth)"),
		CONNECTION_STATUS("�ڑ����"),
		ANTENNA_LEVEL("�d�g���"),
		IP_ADDRESS("IP�A�h���X�^�l�b�g�}�X�N"),
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
					info.charging = v.contains("�[�d��");
					if (info.charging) {
						int start = v.indexOf('��');
						info.setBattery((start != -1)? (v.lastIndexOf('��') - start + 1) * 10: 0);
					} else {
						int start = v.indexOf('�i');
						info.setBattery(Integer.parseInt(v.substring(start + 1, v.indexOf('��'))));
					}
					break;
				case RSSI:
					info.rssi = Integer.parseInt(v.substring(0, v.indexOf(' ')));
					break;
				case CINR:
					info.cinr = Integer.parseInt(v.substring(0, v.indexOf(' ')));
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
					info.antenna = Integer.parseInt(v.substring(v.indexOf("�F") + 1));
					break;
				case IP_ADDRESS:
					// IP address matches some data
					// in this case only first data is stored
					if (info.ipAddress == null || info.ipAddress.length() == 0)
						info.ipAddress = v.substring(0, v.indexOf('/'));
					break;
				}
			} catch (Exception e) {
				MyLog.e("parse error: " + v);
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
}
