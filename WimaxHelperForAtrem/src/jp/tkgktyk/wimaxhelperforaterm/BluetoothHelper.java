package jp.tkgktyk.wimaxhelperforaterm;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import jp.tkgktyk.wimaxhelperforaterm.my.MyLog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;

/**
 * A helper class for Bluetooth.
 */
public class BluetoothHelper {

	private final BluetoothAdapter _adapter;
	/** Does this class need to control enable of bluetooth? */
	private boolean _needsEnableControl;
	
	public BluetoothHelper() {
		_adapter = BluetoothAdapter.getDefaultAdapter();
		_needsEnableControl = hasDevice()? !_adapter.isEnabled(): false;
	}
	
	/**
	 * Enable bluetooth if need.
	 */
	public void enable() {
		if (!isEnabled()) {
			MyLog.i("enable Bluetooth.");
			_needsEnableControl = true;
			_adapter.enable(); // need a permission BLUETOOTH_ADMIN
		}
	}
	
	/**
	 * Disable bluetooth if need.
	 */
	public void disable() {
		if (isEnabled()) {
			MyLog.i("disable Bluetooth.");
			_adapter.disable(); // need a permission BLUETOOTH_ADMIN
		}
	}
	
	/**
	 * Try to connect to bluetooth specified address.
	 * @param address
	 * Specify bluetooth MAC address.
	 * @param timeout
	 * When passes timeout[msec] from start of a trial to connect, force stop the trial.
	 */
	public void connect(String address, final long timeout) {
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			MyLog.e("invalid bluetooth address: " + address);
			return;
		}
		BluetoothDevice device = _adapter.getRemoteDevice(address);
		try {
			// create unpaired RFCOMM socket
			BluetoothSocket socket = null;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
				try {
					Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
					socket = (BluetoothSocket) m.invoke(device, 1);
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				if (socket == null)
					return;
			} else {
				socket = device.createInsecureRfcommSocketToServiceRecord(Const.BLUETOOTH_UUID);
			}
			// normally bluetooth socket's timeout is 12 seconds.
			// more quickly, uses close thread.
			final BluetoothSocket finalSocket = socket;
			Timer timer = new Timer(false);
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						finalSocket.close();
					} catch (IOException e) {
						// do nothing
					}
				}
			}, timeout);
			socket.connect();
		} catch (IOException e) {
			// always reaches here
		} finally {
			MyLog.v("tryed wake up.");
		}
	}

	/**
	 * @return
	 * Return true if bluetooth is enabled.
	 */
	public boolean isEnabled() { return _adapter.isEnabled(); }

	/**
	 * @return
	 * Return true if Android has bluetooth devices.
	 */
	public boolean hasDevice() { return (_adapter != null); }
	
	/**
	 * @return
	 * Return true if BluetoothHelper needs to control of bluetooth enable.
	 */
	public boolean needsEnableControl() { return _needsEnableControl; }
}
