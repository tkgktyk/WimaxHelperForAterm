package jp.tkgktyk.wimaxhelperforaterm.my;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;

public class MyAsyncLoader<T> extends AsyncTask<Object, Integer, T> {
	public interface Task<T> {
		public T run();
		public void onSucceeded(T result);
		public void onFailed();
		public void onCancelled();
	}

	private final Activity _activity;
	private final Task<T> _task;
	private final ProgressDialog _dialog;

	public MyAsyncLoader(Activity activity, String message, Task<T> task, boolean cancelable) {
		MyLog.d();
		_activity = activity;
		_task = task;
		_dialog = new ProgressDialog(activity);

		_dialog.setMessage(message);
		_dialog.setCancelable(cancelable);
	}
	public MyAsyncLoader(Activity activity, int message, Task<T> task, boolean cancelable) {
		this(activity, activity.getString(message), task, cancelable);
	}

	@Override
	protected void onPreExecute() {
		MyLog.d();
		// fix orientation
		_activity.setRequestedOrientation(_activity.getResources().getConfiguration().orientation);
		_dialog.show();
	}
	@Override
	protected T doInBackground(Object... args) {
		MyLog.d();

		if (this.isCancelled()) {
			// on canceled
			this.cancel(true);
			return null;
		}
		return _task.run();
	}
	@Override
	protected void onPostExecute(T result) {
		MyLog.d();
		if (this.isCancelled()) {
			this.onCancelled();
			return;
		}
		if (result != null)
			_task.onSucceeded(result);
		else
			_task.onFailed();
		_onFinished();
	}
	@Override
	protected void onCancelled() {
		MyLog.d();
		_task.onCancelled();
		_onFinished();
	}
	
	private void _onFinished() {
		_activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		_dialog.dismiss();
	}
}
