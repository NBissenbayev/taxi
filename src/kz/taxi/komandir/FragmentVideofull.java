package kz.taxi.komandir;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class FragmentVideofull extends Fragment {

	Button btnUp;
	Button btnDown;
	static CustomVideoView vv;
	Database db;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_videofull, container, false);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new Database(view.getContext());
        vv = (CustomVideoView) view.findViewById(R.id.fullVideo);
        
        vv.resizeVideo(MainActivity.screen_width, MainActivity.screen_height);
        if(db.get("video").length()!=0){
        	vv.setVideoURI(Uri.parse(db.get("video")));
        	vv.start();
        }
        
        // volume btns
        btnUp = (Button) view.findViewById(R.id.btnUp);
		btnDown = (Button) view.findViewById(R.id.btnDown);
		
		btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                volumeUp();
            }
        });
		
		btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                volumeDown();
            }
        });
        
        final MainActivity ma =(MainActivity) getActivity();
        vv.setOnCompletionListener(new OnCompletionListener(){
        
			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.d("nursultan", "car video full is ended");
				// TODO Auto-generated method stub
				FragmentManager fm = ma.getFragmentManager();
				FragmentVideofull f = (FragmentVideofull)fm.findFragmentByTag("full_vid");
				fm.beginTransaction().remove(f).commit();
				MainActivity.video.setVisibility(View.VISIBLE);
				if(!MainActivity.playBtn.isClickable()){
					MainActivity.playBtn.setVisibility(View.VISIBLE);
					MainActivity.playBtn.setClickable(true);
					if(db.get("video1").length() !=0){
						MainActivity.video.setVideoURI(Uri.parse(db.get("video1")));
						MainActivity.video.stopPlayback();
					}
				}
				
//				if(db.get("video1").length()!=0){
//					MainActivity.video.setVideoURI(Uri.parse(db.get("video1")));
////					MainActivity.video.pause();
//				}
				Fragment flist = fm.findFragmentByTag(MainActivity.cur_tag);
				if(flist!=null){
					Log.d("nursultan", "car REMOVING FRAGMENT LIST FRAG");
					fm.beginTransaction().remove(flist)
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
					.commit();
				}
				
			}
        	
        });
    }
    
    void volumeUp(){
		AudioManager audioManager = (AudioManager)getView().getContext().getSystemService(Context.AUDIO_SERVICE);
		int cur_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		if(cur_volume<15){
			cur_volume ++;
		}
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, cur_volume, 0);
	}
	
	void volumeDown(){
		AudioManager audioManager = (AudioManager)getView().getContext().getSystemService(Context.AUDIO_SERVICE);
		int cur_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		if(cur_volume>0){
			cur_volume --;
		}
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, cur_volume, 0);
	}
}

