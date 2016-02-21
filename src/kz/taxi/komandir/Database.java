package kz.taxi.komandir;

import android.content.Context;
import android.content.SharedPreferences;

public class Database {
	SharedPreferences sp;
	SharedPreferences.Editor editor;

	public Database(Context context) {
		sp = context.getSharedPreferences("kz.taxi.komandir", 0);
		editor = sp.edit();
	}

	public String get(String key) {
		return sp.getString(key, "");
	}

	public void update(String key, String value) {
		editor.putString(key, value);
		editor.commit();
	}
}
