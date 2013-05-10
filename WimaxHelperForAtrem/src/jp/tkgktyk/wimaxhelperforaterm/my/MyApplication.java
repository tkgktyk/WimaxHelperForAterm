package jp.tkgktyk.wimaxhelperforaterm.my;

import jp.tkgktyk.wimaxhelperforaterm.AtermHelper;
import android.app.Application;

/**
 * An application class provides AtermHelper object to activities and services.
 */
public class MyApplication extends Application {
	private AtermHelper _aterm;
	
	public AtermHelper getAterm() {
		if (_aterm == null)
			_aterm = new AtermHelper(this);
		return _aterm;
	}
	
	@Override
	public void onLowMemory() {
		_aterm = null;
	}
}
