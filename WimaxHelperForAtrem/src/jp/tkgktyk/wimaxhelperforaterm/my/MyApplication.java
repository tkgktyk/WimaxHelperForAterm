package jp.tkgktyk.wimaxhelperforaterm.my;

import jp.tkgktyk.wimaxhelperforaterm.AtermHelper;
import android.app.Application;

/**
 * An application class provides AtermHelper object to activities and services.
 */
public class MyApplication extends Application {
	private AtermHelper _aterm;

	@Override
	public void onCreate() {
		super.onCreate();
		
		_aterm = new AtermHelper(this);
	}
	
	public AtermHelper getAterm() { return _aterm; }
}
