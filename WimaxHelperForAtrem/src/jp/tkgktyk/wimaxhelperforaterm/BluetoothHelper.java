package jp.tkgktyk.wimaxhelperforaterm;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class BluetoothHelper {

	private final BluetoothAdapter _adapter;
	private boolean _needsEnableControl;
	private BluetoothSocket _socket;
	
	public BluetoothHelper() {
		_adapter = BluetoothAdapter.getDefaultAdapter();
		_needsEnableControl = hasDevice()? !_adapter.isEnabled(): false;
		_socket = null;
	}
	
	public void enable() {
		if (!isEnabled()) {
			MyLog.i("enable Bluetooth.");
			_needsEnableControl = true;
			_adapter.enable(); // need a permission BLUETOOTH_ADMIN
		}
	}
	
	public void disable() {
		if (isEnabled()) {
			MyLog.i("disable Bluetooth.");
			_adapter.disable(); // need a permission BLUETOOTH_ADMIN
		}
	}
	
	public void connect(String address, final long timeout) {
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			MyLog.e("invalid bluetooth address: " + address);
			return;
		}
		BluetoothDevice device = _adapter.getRemoteDevice(address);
		// normally bluetooth socket's timeout is 12 seconds.
		// more quickly, uses close thread. 
		Thread closeThread = new Thread(new Runnable() {
			@Override
			public void run() {
				if (_socket != null) {
					try {
						Thread.sleep(timeout);
					} catch (InterruptedException e) {
						// do nothing
					}
					try {
						_socket.close();
					} catch (IOException e) {
						// do nothing
					} finally {
						_socket = null;
					}
				}
			}
		});
		try {
			_socket = device.createRfcommSocketToServiceRecord(Const.BLUETOOTH_UUID);
			closeThread.start();
			_socket.connect();
		} catch (IOException e) {
			// always reaches here
		} finally {
			if (closeThread.isAlive())
				// interrupt thread and close socket
				closeThread.interrupt();
			MyLog.v("tryed wake up.");
		}
	}

	public boolean isEnabled() { return _adapter.isEnabled(); }
	
	public boolean hasDevice() { return (_adapter != null); }
	
	public boolean needsEnableControl() { return _needsEnableControl; }
}
