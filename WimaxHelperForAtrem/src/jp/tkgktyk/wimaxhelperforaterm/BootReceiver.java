package jp.tkgktyk.wimaxhelperforaterm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A receiver for BOOT_COMPLETED to start {@link MainService} on startup.
 * Requires intent filter and uses-permission.
 */
public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(context, MainService.class);
		context.startService(service);
	}

}
