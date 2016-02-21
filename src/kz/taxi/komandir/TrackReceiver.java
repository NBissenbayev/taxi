package kz.taxi.komandir;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

public class TrackReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, Intent intent) {
		AQuery aq = new AQuery(context);
		final Database db = new Database(context);
		String url = API.base + "getstatus.php?car_id=" + db.get("car_id");
		aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {
			@Override
			public void callback(String url, JSONObject json, AjaxStatus status) {
				if (json != null) {
					try {
						String prev = db.get("status");
						db.update("status", json.getString("status"));
						// if 0 na servake, lochim screen  // po statusu
						if (prev.equals("1") && db.get("status").equals("0"))
							lock_screen(context);
						// unlochim screen
						if (prev.equals("0") && db.get("status").equals("1"))
							unlock_screen(context);
					} catch (JSONException e) {
					}
				} else {
					Log.d("nurzhik", "json error: getstatus");
				}
			}
		});
	}

	protected void lock_screen(Context context) {
		Log.d("nurzhik", "turn off");
		//DevicePolicyManager mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		//mDPM.lockNow();
	}

	@SuppressWarnings("deprecation")
	protected void unlock_screen(Context context) {
		Log.d("nurzhik", "turn on & full");
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
		wakeLock.acquire();
		Intent i = new Intent();
		i.setClassName("kz.taxi.komandir", "kz.taxi.komandir.FullActivity");
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
}