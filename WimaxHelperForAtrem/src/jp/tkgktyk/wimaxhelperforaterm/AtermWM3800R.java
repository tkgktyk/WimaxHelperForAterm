package jp.tkgktyk.wimaxhelperforaterm;

import jp.tkgktyk.wimaxhelperforaterm.AtermHelper.Info;
import jp.tkgktyk.wimaxhelperforaterm.AtermHelper.Product;
import jp.tkgktyk.wimaxhelperforaterm.AtermHelper.Router;
import jp.tkgktyk.wimaxhelperforaterm.my.MyLog;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A Router class for Aterm WM3800R.
 */
public class AtermWM3800R extends Router {
	/**
	 * To use String in switch statement during parse document. Document has a
	 * information table that have two column, 'key' and 'value'. This enum
	 * corresponds to key column.
	 */
	public enum Key {
		FIRMWARE_VERSION("�t�@�[���E�F�A�o�[�W����"),
		FIRMWARE_UPDATE("�t�@�[���E�F�A�X�V�ʒm"),
		BATTERY("�d�r�c��"),
		SSID("�l�b�g���[�N��(SSID)"),
		BLUETOOTH_NAME("Bluetooth��"),
		BLUETOOTH_ADDRESS("MAC�A�h���X(Bluetooth)"),
		ANTENNA_LEVEL("�d�g���"),
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
	public Info parseDocument(Document doc) {
		Info info = new AtermHelper.Info();
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
					info.updateNotified = v.contains("�V�t�@�[���E�F�A�֍X�V�\");
					break;
				case BATTERY:
					info.charging = v.contains("�[�d��");
					if (info.charging) {
						int start = v.indexOf('��');
						info.battery = (start != -1)? (v.lastIndexOf('��') - start + 1) * 10: 0;
					} else {
						int start = v.indexOf('�i');
						info.battery = Integer.parseInt(v.substring(start + 1, v.indexOf('��')));
					}
					break;
				case SSID:
					info.addSsid(v);
					break;
				case BLUETOOTH_NAME:
					info.btName = v;
					break;
				case BLUETOOTH_ADDRESS:
					info.setBtAddress(v);
					break;
				case ANTENNA_LEVEL:
					info.antenna = Integer.parseInt(v.substring(v.indexOf("�F") + 1));
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
	public Product toProduct() { return Product.WM3800R; }
}
