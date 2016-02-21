package kz.taxi.komandir;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BootService extends Service {
	@Override
	public void onStart(Intent intent, int startId) {
		Intent dialogIntent = new Intent(this, MainActivity.class);
		dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(dialogIntent);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}