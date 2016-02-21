package kz.taxi.komandir;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

public class MainActivity extends ActionBarActivity implements OnLongClickListener {
	static Database db;
	AQuery aq;
	static CustomVideoView video;
	ImageView banner;
	Handler mHandler;
	LinearLayout rp_layout;
    boolean stopUI = false;
    // to access playBtn state from FragmentFullVid
    static Button playBtn;
	static boolean dimmed = false;
	static boolean undimmed = false;
	static int screen_width;
	static int screen_height;
	static boolean first_popup = false;
	static boolean download_complete = false;
	// current fragment tag
	static String cur_tag = "";
	boolean videos_changed = false;
	// run small banner
	static Runnable r;
	// run full banner
	static Runnable run_banner;
	// banner counter k - small, l - large
	static int k = 0;
	static int l = 0;
	// large video pos
	static int full_vid_pos = 0;
	// small video pos
	static int curr_pos = 0;
	static int small_vid_duration = 0;
	
	//Variable to store brightness value
	private int brightness;
	//Content resolver used as a handle to the system's settings
	private ContentResolver cResolver;
	//Window object, that will store a reference to the current window
	private Window window;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d("nursultan", "car onCreateCalled");
		db = new Database(this);
		aq = new AQuery(this);
		video = (CustomVideoView) findViewById(R.id.video);
		rp_layout = (LinearLayout) findViewById(R.id.right_panel);
		banner = (ImageView) findViewById(R.id.banner);
		//loadAdmin();
		loadLang();
		loadDate();
		listenButtons();
		loadCurrencies();
		loadWeather();
		loadTracker();
		downloadBigData();
		
		downloadMenu();
		
		// get dimens of screen
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		screen_width = size.x;
		screen_height = size.y;
		// checkin status and register resetStatusTimer if onCreate called twice 
		checkStatus();
		// brightness of screen
		//Get the content resolver
		cResolver = getContentResolver();

		//Get the current window
		window = getWindow();
		// some changes
		    try
		            {
		               // To handle the auto
		                Settings.System.putInt(cResolver,
		                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
		 //Get the current system brightness
		                brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
		            } 
		            catch (SettingNotFoundException e) 
		            {
		                //Throw an error case it couldn't be retrieved
		                Log.e("Error", "Cannot access system brightness");
		                e.printStackTrace();
		            }
		// handler 
		mHandler = new Handler();
		video.resizeVideo(screen_width - (screen_width * 4/10), screen_height - 51);
		stopBannerTimer();
		
		Log.d("nursultan", "video url: " + db.get("video1"));
		
	}
	
	// checkin car status
	void checkStatus(){
		Log.d("nursultan", "car checkin status");
		String url = API.base + "getstatus.php?car_id=" + db.get("car_id");
		
		if(db.get("video").length()!=0){
			download_complete = true;
		}
		aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {
			@Override
			public void callback(String url, JSONObject json, AjaxStatus status) {
				if (json != null) {
					try {
						String prev = db.get("status");
						db.update("status", json.getString("status"));
						if(json.getString("status").equals("1") && !undimmed && download_complete){
							Log.d("nursultan", "udiming display");
							unDimDisplay();
							setVolume(15/2);
							undimmed = true;
							dimmed = false;
						}
						if(json.getString("status").equals("0") && !dimmed){
							Log.d("nursultan", "dimming display");
							dimDisplay();
							setVolume(0);
							dimmed = true;
							undimmed = false;
						}
					} catch (JSONException e) {
					}
				} else {
					Log.d("nurzhik", "json error: getstatus");
				}
			}
		});
		
		resetStatusTimer();
	}
	
	
	
	@Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (db.get("status").equals("0")) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }
	
	
	protected void onResume(){
		Log.d("nursultan", "activity state: onResume");
		super.onResume();
		if(db.get("video1").length()!=0 && curr_pos !=0){
			video.seekTo(curr_pos);
			video.start();
		}
//		// setting val to to false /
		if(checkFrag("full_vid")<0){
			undimmed = false;
			dimmed = false;
		}
		
		if(db.get("status").equals("0"))
			setVolume(0);
		if(checkFrag("full_vid")>0){
			FragmentVideofull.vv.seekTo(full_vid_pos);
			FragmentVideofull.vv.start();
		}
		resetSmallBanner();
	}
	
	private void resetSmallBanner(){
		banner.removeCallbacks(r);
		playBanners();
	}
	
	protected void onPause(){
		Log.d("nursultan", "activity state: onPause");
		super.onPause();
		// if video is currently playing take pos, if not, pos = 0
		if(!playBtn.isClickable()){
			curr_pos = video.getCurrentPosition();
		}
		banner.removeCallbacks(r);
		stopBannerTimer();
		if(checkFrag("full_vid")>0){
			full_vid_pos = FragmentVideofull.vv.getCurrentPosition();
		}
	}
	
	
	// set volume func
	private void setVolume(int volume){
		AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
	}
	
	// dim display
	
	void dimDisplay(){
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness =0.01f;// 100 / 100.0f;
		getWindow().setAttributes(lp);
		// in case status 0 is called from serv, and full video is not finished playin
		Fragment f = new FragmentVideofull();
		FragmentManager fm = getFragmentManager();
		f = fm.findFragmentByTag("full_vid");
		if(f!=null){
			fm.beginTransaction().remove(f).commit();
		}
		Log.d("nursultan", "car DIM DISPLAY");
	}
	
	void unDimDisplay(){
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness =100.0f;// 100 / 100.0f;
		getWindow().setAttributes(lp);
		video.setVisibility(View.INVISIBLE);
		video.pause();
		if(checkFrag("full_vid") < 0){
			FragmentVideofull f = new FragmentVideofull();
			FragmentManager fm = getFragmentManager();
			fm.beginTransaction().add(R.id.frame_container, f, "full_vid")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
		}
        
        Log.d("nursultan", "car DIM UN display");
        
        // checkin videos
        
		//startActivity(new Intent(this,FullActivity.class));
	}
	
	
	protected void onDestroy(){
		super.onDestroy();
		Settings.System.putInt(this.getContentResolver(),
		        Settings.System.SCREEN_BRIGHTNESS, 255);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness =100.0f;// 100 / 100.0f;
		getWindow().setAttributes(lp);
	}
	

	/*DevicePolicyManager deviceManger;
	ActivityManager activityManager;
	boolean active;

	private void loadAdmin() {
		deviceManger = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ComponentName compName = new ComponentName(this, MyAdmin.class);
		active = deviceManger.isAdminActive(compName);
		Log.d("nurzhik", "active = " + active);
		if (!active) {
			Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "To turn off screen.");
			startActivityForResult(intent, 1);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case 1:
				if (resultCode == Activity.RESULT_OK) {
					Log.i("nurzhik", "Admin enabled!");
				} else {
					Log.i("nurzhik", "Admin enable FAILED!");
				}
				return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}*/
	private void listenButtons() {
		TextView rus = (TextView) findViewById(R.id.rus);
		TextView eng = (TextView) findViewById(R.id.eng);
		playBtn = (Button) findViewById(R.id.playBtn);
		playBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(db.get("video1").length()!=0){
					Log.d("nursultan", "car play btn pressed");
					video.setVideoURI(Uri.parse(db.get("video1")));
					video.start();
					playBtn.setVisibility(View.INVISIBLE);
					playBtn.setClickable(false);
					// getting dufration
					MediaPlayer mp = MediaPlayer.create(getApplicationContext(), Uri.parse(db.get("video1")));
					small_vid_duration = mp.getDuration();
					mp.release();
					Log.d("nursultan", "pos duration: " + small_vid_duration);
				}
			}
			
		});
		video.setOnCompletionListener(new OnCompletionListener(){

			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				playBtn.setVisibility(View.VISIBLE);
				playBtn.setClickable(true);
				curr_pos = 0;
				Log.d("nursultan", "position: " + curr_pos);
			}
			
		});
//		rus.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				updLang("ru");
//			}
//		});
//		eng.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				updLang("en");
//			}
//		});
	}

	private void loadLang() {
		if (db.get("lang").length() > 0)
			return;
		updLang("ru");
	}
	//restart app, clear backstack of activities
	private void restartApp() {
		download_complete = false;
		Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
	}

	private void updLang(String lang) {
		db.update("lang", lang);
		Resources res = getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		android.content.res.Configuration conf = res.getConfiguration();
		conf.locale = new Locale(lang);
		res.updateConfiguration(conf, dm);
		restartApp();
	}

	private void downloadBigData() {
		loadCurrencies();
		removeVideos();
		downloadVideos();
		loadBanners();
	}

	private void loadTracker() {
		if (db.get("track").equals("1"))
			return;
		// update track
		db.update("track", "1");
		// car id be default
		db.update("car_id", "319AL");
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this, TrackReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		long ten = 10 * 1000; //10 sec
		long interval = 60 * 1000; //1 min
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ten, interval, pendingIntent);
	}
	
	private void playVideo () {
		if (db.get("video1").length() == 0) {
			Log.d("nursultan", "car successcfull request");
			return;
		}
		Log.d("nursultan", "car video link: " + db.get("video1"));
//		String path = db.get("video1");
//		video.setVideoURI(Uri.parse(path));
//		video.stopPlayback();
//		video.setOnPreparedListener(new OnPreparedListener(){
//			@Override
//			public void onPrepared(MediaPlayer mp) {
//				// TODO Auto-generated method stub
//				mp.setLooping(true);
//			}
//			
//		});
	}
	
	// check status for every ** time fram
	// video task
    private Handler checkHandler = new Handler() {
        public void handleMessage(Message msg) {
            // whatever message
        }
    };

    private Runnable statusUpdateCallback = new Runnable() {
        @Override
        public void run() {
            // Perform any required operation on disconnect
            updateStatus();
        }
    };
    
    private Runnable bannerCallback = new Runnable(){
    	
    	public void run(){
    		displayFullBanner();
    	}
    };
    // reset banner 5 minutes
    public void resetBannerTimer(){
    	checkHandler.removeCallbacks(bannerCallback);
    	checkHandler.postDelayed(bannerCallback, 300*1000L);
    }
    

    //  check status every 15 sec
    public void resetStatusTimer() {
        checkHandler.removeCallbacks(statusUpdateCallback);
        checkHandler.postDelayed(statusUpdateCallback, 15*1000L);
    }

    public void stopStatusTimer() {
        checkHandler.removeCallbacks(statusUpdateCallback);
    }
    
    public void stopBannerTimer(){
    	checkHandler.removeCallbacks(bannerCallback);
    }

    void forceRestart(Context context){
    	Intent mStartActivity = new Intent(context, MainActivity.class);
    	int mPendingIntentId = 123456;
    	PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
    	AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    	mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
    	System.exit(0);
    }
    
    //
    void updateStatus(){
        String url = API.base + "getstatus.php?car_id=" + db.get("car_id");
    	mHandler.postDelayed(new Runnable (){
    		public void run(){
    	    	aq.ajax(API.base + "getstatus.php?car_id=" + db.get("car_id"), JSONObject.class, new AjaxCallback<JSONObject>() {
    				@Override
    				public void callback(String url, JSONObject json, AjaxStatus status) {
    					if (json != null) {
    						try {
    							Log.d("nursultan", "car UPDATE STATUS in handler CALLED");
    							Log.d("nursultan", "car VIDEOCHANGE: " + checkVideos());
    							Log.d("nursultan", "car link video: " + db.get("video"));
    							Log.d("nursultan", "car link video1: " + db.get("video1"));
    							
    							FragmentManager fm = getFragmentManager();
    							
    							if(checkVideos()){
    								restartApp();
    							}
    							
    							db.update("status", json.getString("status"));
    							if(json.getString("status").equals("1") && !undimmed && download_complete && !checkVideos()){
    								Log.d("nursultan", "undiming display");
    								unDimDisplay();
    								setVolume(15/2);
    								undimmed = true;
    								dimmed = false;
    							}
    							if(json.getString("status").equals("0") && !dimmed){
    								Log.d("nursultan", "dimming display");
    								dimDisplay();
    								setVolume(0);
    								dimmed = true;
    								undimmed = false;
    								// full banner is playin
    								Fragment fbanner = fm.findFragmentByTag("full_banner");
    								if(fbanner!=null){
    									fm.beginTransaction().remove(fbanner).commit();
    								}
    								stopBannerTimer();
    							}
    						} catch (JSONException e) {
    							
    						}
    					} else {
    						Log.d("nurzhik", "json error: getstatus");
    					}
    				}
    			});
    	    	// check for video to laod
    	    	if(db.get("video").length()!=0){
    	    		download_complete = true;
    	    	}
    	    	Log.d("nursultan", "car status is: " + db.get("status") + " car_id is " + db.get("car_id"));
    	    	mHandler.postDelayed(this, 15*1000L);
    		}
    	}, 15*1000L); // every 15 seconds
    	
    }
    
    
	//
	

	private void downloadVideos() {
		Log.d("nursultan", "car DOWNLOADING VIDEOS");
		db.update("video", "");
		db.update("video1", "");
		aq.ajax(API.base + "video.php", JSONObject.class, -1, new AjaxCallback<JSONObject>() {
			@Override
			public void callback(String url, final JSONObject json, AjaxStatus status) {
				if (json != null) {
					try {
						String video = json.getString("video");
						String video1 = json.getString("video1");
						db.update("prev_vid", json.getString("video"));
						db.update("prev_vid1", json.getString("video1"));
//						db.update("video", video);
//						db.update("video1", video1);
						downloadVideo("video", video);
						downloadVideo("video1", video1);
					} catch (Exception e) {
						Log.d("nurzhik", "json error: video.php");
					}
				} else {
					Log.d("nurzhik", "json error: video.php json is null");
				}
			}
		});
	}

	protected void removeVideos() {
		Log.d("nursultan","car REMOVING ALL VIDEOS");
		File downloads = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS);
		File list[] = downloads.listFiles();
		for (int i = 0; i < list.length; i++) {
			new File(list[i].getPath()).delete();
		}
		// null the ref on videos
		db.update("video", "");
		db.update("video1", "");
	}

	private void downloadVideo(final String key, final String name) {
		Log.d("nursultan", "car DOWNLOAD VIDEO IS CALLED");
		final String url = API.base + name;
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
		request.setTitle(name);
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
		request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
		final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		final long enqueue = manager.enqueue(request);
		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
					Query query = new Query();
					query.setFilterById(enqueue);
					Cursor c = manager.query(query);
					if (c.moveToFirst()) {
						int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
						if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
							String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
							db.update(key, uriString);
							Log.d("nursultan", "car link" + db.get(key));
							playVideo();
						}
					}
				}
			}
		};
		
		registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	private void loadBanners() {
		db.update("banners", "");
		aq.ajax(API.base + "banner.php", JSONArray.class, -1, new AjaxCallback<JSONArray>() {
			@Override
			public void callback(String url, final JSONArray array, AjaxStatus status) {
				if (array != null) {
					db.update("banners", array.toString());
					resetSmallBanner();
				} else {
					Log.d("nurzhik", "json error: banner.php null");
				}
			}
		});
	}

	

	void playBanners() {
		if (db.get("banners").length() == 0) {
			loadBanners();
			return;
		}
		try {
			final JSONArray array = new JSONArray(db.get("banners"));
			r = null;
			k = 0;
			r = new Runnable() {
				@Override
				public void run() {
					try {
						final String url = API.base + array.getJSONObject(k).getString("first_image");
						//final String urlBig = API.base + array.getJSONObject(k).getString("second_image");
						aq.id(R.id.banner).image(url, true, true);
						banner.postDelayed(r, 5000);
						k++;
						if (k == array.length())
							k = 0;
					} catch (JSONException e) {
					}
				}
			};
			banner.postDelayed(r, 1000/2);
			
		} catch (Exception e) {
			
		}
	}

	private void loadWeather() {
		final AQuery aq = new AQuery(this);
		final TextView today = (TextView) findViewById(R.id.today);
		final TextView tomorrow = (TextView) findViewById(R.id.tomorrow);
		aq.ajax("http://www.gismeteo.kz/city/daily/5205/", String.class, 2 * 60 * 60 * 1000L, new AjaxCallback<String>() {
			@Override
			public void callback(String url, String html, AjaxStatus status) {
				Document doc = Jsoup.parse(html);
				loadDay(R.string.today, R.id.bugin, today, doc.select("div[id=tab_wdaily1]").get(0));
				loadDay(R.string.tomorrow, R.id.erten, tomorrow, doc.select("div[id=tab_wdaily2]").get(0));
			}
		});
	}

	protected void loadDay(int str, int img, TextView today, Element doc) {
		String url = doc.select("img").get(0).attr("src");
		String temp = doc.select("span[class=value m_temp c]").get(0).text();
		today.setText(getString(str) + "\n" + temp + " °C");
		AQuery aq = new AQuery(this);
		aq.id(img).image(url, true, true);
	}

	private void loadDate() {
		TextView date = (TextView) findViewById(R.id.date);
		TextView time = (TextView) findViewById(R.id.time);
		SimpleDateFormat df = new SimpleDateFormat("d ");
		SimpleDateFormat tf = new SimpleDateFormat("hh:mm");
		Calendar cal = Calendar.getInstance();
		int month = cal.get(Calendar.MONTH);
		String months[] = getResources().getStringArray(R.array.months);
		date.setText(df.format(new Date()) + months[month]);
		time.setText(tf.format(new Date()));
		date.setOnLongClickListener(this);
	}

	private void loadCurrencies() {
		Log.d("nursultan", "Currency is loading...");
		final int[] draw = { R.drawable.usd, R.drawable.eur, R.drawable.rub, R.drawable.gbp, R.drawable.chf, R.drawable.xau, R.drawable.xag };
		final LinearLayout currencies = (LinearLayout) findViewById(R.id.layout_currencies);
		aq.ajax("http://www.halykbank.kz/ru/currency-rates", String.class, 60 * 60 * 1000L, new AjaxCallback<String>() {
			@Override
			public void callback(String url, String html, AjaxStatus status) {
				Document doc = Jsoup.parse(html);
				Elements tds = doc.select("td");
				int k = 0;
				for (int i = 0; i < tds.size(); i++) {
					Element td = tds.get(i);
					if (td.text().length() == 6) {
						String country = td.text().substring(3);
						View root = LayoutInflater.from(aq.getContext()).inflate(R.layout.item_flag, null, false);
						ImageView flag = (ImageView) root.findViewById(R.id.flag);
						flag.setImageResource(draw[k++]);
						TextView label = (TextView) root.findViewById(R.id.label);
						label.setText(country);
						TextView rate = (TextView) root.findViewById(R.id.rate);
						rate.setText(tds.get(i + 2).text());
						currencies.addView(root);
						i += 2;
						if (k == 7)
							return;
					}
				}
			}
		});
	}

	private void downloadMenu() {
		Log.d("nurzhik", "download menu");
		db.update("menu", "");
		aq.ajax(API.base + "menu.php", JSONArray.class, 2 * 60 * 60 * 1000L, new AjaxCallback<JSONArray>() {
			@Override
			public void callback(String url, JSONArray array, AjaxStatus status) {
				if (array != null) {
					db.update("menu", array.toString());
					Log.d("nurzhik", "menu: " + array.toString());
					loadMenu();
				} else {
					Log.d("nurzhik", "json error: menu.php");
				}
			}
		});
	}

	protected void loadMenu() {
		Log.d("nurzhik", db.get("menu"));
		try {
			LinearLayout menu = (LinearLayout) findViewById(R.id.layout_menu);
			JSONArray array = new JSONArray(db.get("menu"));
			Log.d("nurzhik", "menu ok");
			for (int i = 0; i < array.length(); i++) {
				final JSONObject obj = array.getJSONObject(i);
				View root = LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_menu, null, false);
				(new AQuery(root)).id(R.id.image_menu).image(API.base + obj.getString("icon_ru"));
				ImageView image = (ImageView) root.findViewById(R.id.image_menu);
				changeImageSize(image, 80, 80);
				menu.addView(root);
				root.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						try {
							if(checkFrag(cur_tag)<0 && checkFrag("full_vid")<0 && checkFrag("full_banner") < 0){
								Log.d("nursultan", "check frag cur tag: " + checkFrag(cur_tag) + " " + checkFrag("full_vid"));
								Bundle args = new Bundle();
								ListFragment f = new ListFragment();
								FragmentManager fm = getFragmentManager();
								args.putString("id", obj.getString("id"));
								args.putString("id_name", obj.getString("title_ru"));
								
								f.setArguments(args);
								fm.beginTransaction().add(R.id.frame_container, f, "list_frag")
								.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
								.commit();
								video.pause();
								if(!playBtn.isClickable()){
									curr_pos = video.getCurrentPosition();
								}
								cur_tag = f.getTag();
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
			}
		} catch (Exception e) {
			Log.d("nurzhik", "menu error");
		}
	}
	
	int checkFrag(String cur_tag){
		FragmentManager fm = getFragmentManager();
		Fragment f = fm.findFragmentByTag(cur_tag);
		if(f!=null){
			return 1;
		}
		return -1;
	}
	//ajusting layout
	public void changeImageSize(View view, int width, int height) {
		view.setMinimumWidth(width);
		view.setMinimumHeight(height);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, height);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		view.setLayoutParams(lp);
		view.requestLayout();
		view.invalidate();
	}

	long previous_time = -1;
	
	@Override
	public boolean onLongClick(View v) {
		long current_time = System.currentTimeMillis();
		if (previous_time != -1 && current_time - previous_time < 5 * 1000) {// 5 sec
			previous_time = current_time;
//			Intent intent = new Intent(this, SettingsActivity.class);
//			startActivity(intent);
			displayChangeIdDialog();
			return true;
		}
		previous_time = current_time;
		return false;
	}
	
	
	void displayChangeIdDialog() {
        final Dialog d = new Dialog(MainActivity.this);
        d.setContentView(R.layout.dialog_id);
        d.setCancelable(false);
        d.setTitle("Change car ID");
        Button cbtn = (Button) d.findViewById(R.id.okBtn);
        cbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	EditText edit = (EditText) d.findViewById(R.id.et_carId);
        		String car_id = edit.getText().toString();
        		db.update("car_id", car_id);
                d.dismiss();
            }
        });
        //d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.show();
    }
	
	
	@Override
    public void onUserInteraction() {
        resetBannerTimer();
    }
	
	boolean checkVideos(){
		
		//check links on serv
		aq.ajax(API.base + "video.php", JSONObject.class, -1, new AjaxCallback<JSONObject>() {
			@Override
			public void callback(String url, final JSONObject json, AjaxStatus status) {
				if (json != null) {
					try {
						String video = json.getString("video");
						String video1 = json.getString("video1");
						if(db.get("prev_vid").equals(video) && db.get("prev_vid1").equals(video1))
							videos_changed =  false;
						else {
							videos_changed = true;
						}
					} catch (Exception e) {
						Log.d("nurzhik", "json error: video.php");
					}
				} else {
					Log.d("nurzhik", "json error: video.php json is null");
				}
			}
		});
		return videos_changed;
	}
	
	
	// full banner fragment
	void displayFullBanner(){
		// check for existing full_vid fragment //
		FragmentManager fm = getFragmentManager();
		if(checkFrag("full_vid") < 0 && checkFrag("full_banner")<0){
			FragmentFullbanner fb = new FragmentFullbanner();
			fm.beginTransaction().replace(R.id.frame_container, fb, "full_banner")
			.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
			.commit();
		}
		if(checkFrag("full_vid") > 0)
			Log.d("nursultan", "car video is playin!");
	}
	
}


	
