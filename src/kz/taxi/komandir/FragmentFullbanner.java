package kz.taxi.komandir;

import org.json.JSONArray;
import org.json.JSONException;

import com.androidquery.AQuery;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

public class FragmentFullbanner extends Fragment{
	Button cbtn;
	ImageView img;
	Database db;
	static Runnable r;
	static int k = 0;
	AQuery aq;
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.fragment_fullbanner, container, false);
        return rootview;
    }
	
	public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        img = (ImageView) view.findViewById(R.id.img);
        db = new Database(view.getContext());
        aq = new AQuery(view);
        cbtn = (Button) view.findViewById(R.id.close_btn);
        cbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	FragmentManager fm = getActivity().getFragmentManager();
            	Fragment f = fm.findFragmentByTag("full_banner");
            	fm.beginTransaction().remove(f)
            	.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
            	.commit();
            	img.removeCallbacks(r);
            }
        });
        try {
			final JSONArray array = new JSONArray(db.get("banners"));
			img = (ImageView) view.findViewById(R.id.banner_img);
			r = null;
			k = 0;
			r = new Runnable() {
				@Override
				public void run() {
					try {
						//final String url = API.base + array.getJSONObject(k).getString("first_image");
						final String urlBig = API.base + array.getJSONObject(k).getString("second_image");
						aq.id(R.id.banner_img).image(urlBig, true, true);
						img.postDelayed(r, 5000);
						k++;
						if (k == array.length())
							k = 0;
					} catch (JSONException e) {
						
					}
				}
			};
			img.postDelayed(r, 1000/2);
			
		} catch (Exception e) {
		}
        
    }
	
	public void onPause(){
		super.onPause();
		img.removeCallbacks(r);
	}
	
	
}
