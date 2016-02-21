package kz.taxi.komandir;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

public class BootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Database db = new Database(context);
			AQuery aq = new AQuery(context);
			String url = API.base + "getstatus.php?car_id=" + db.get("car_id");
			aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {
				@Override
				public void callback(String url, JSONObject json, AjaxStatus status) {
					if (json != null) {
						Intent serviceIntent = new Intent(context, BootService.class);
						context.startService(serviceIntent);
					} else {
						Log.d("nurzhik", "json error: getstatus on bootreceive");
					}
				}
			});
		}
	}
}