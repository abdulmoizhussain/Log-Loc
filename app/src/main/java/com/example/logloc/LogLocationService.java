package com.example.logloc;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class LogLocationService extends Service {
	MainActivity mainActivity;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		this.mainActivity.stopLogger(null);
		Toast.makeText(this, "Logger Stopped", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.mainActivity = new MainActivity();
	}
}
