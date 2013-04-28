package jp.tkgktyk.wimaxhelperforaterm.my;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;

public class MyAsync extends AsyncTask<Object, Integer, Object> {
	public interface Task {
		public boolean run();
		public void onSucceeded();
		public void onFailed();
	}

	private final Activity _activity;
	private final Task _task;
	private final ProgressDialog _dialog;

	public MyAsync(Activity activity, String message, Task task) {
		MyLog.d();
		_activity = activity;
		_task = task;
		_dialog = new ProgressDialog(activity);

		_dialog.setMessage(message);
		_dialog.setCancelable(false);
	}
	public MyAsync(Activity activity, int message, Task task) {
		this(activity, activity.getString(message), task);
	}

	@Override
	protected void onPreExecute() {
		MyLog.d();
		// fix orientation
		_activity.setRequestedOrientation(_activity.getResources().getConfiguration().orientation);
		_dialog.show();
	}
	@Override
	protected Object doInBackground(Object... args) {
		MyLog.d();

		if (!_task.run()) {
			// on failed
			this.cancel(true);
			return null;
		}
		return null;
	}
	@Override
	protected void onPostExecute(Object result) {
		MyLog.d();
		if (this.isCancelled()) {
			this.onCancelled();
			return;
		}
		_task.onSucceeded();
		_onFinished();
	}
	@Override
	protected void onCancelled() {
		MyLog.d();
		_task.onFailed();
		_onFinished();
	}
	
	private void _onFinished() {
		_activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		_dialog.dismiss();
	}
}
