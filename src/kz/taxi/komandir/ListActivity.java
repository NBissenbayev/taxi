package kz.taxi.komandir;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

public class ListActivity extends ActionBarActivity implements OnChildClickListener {
	String id;
	ExpandableListView list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);
		setTitle(getIntent().getStringExtra("title_ru"));
		id = getIntent().getStringExtra("id");
		list = (ExpandableListView) findViewById(R.id.list_items);
		loadList();
		list.setOnChildClickListener(this);
	}

	private void loadList() {
		final AQuery aq = new AQuery(this);
		aq.ajax(API.base + "menu.php?menu_id=" + id, JSONArray.class, 24 * 60 * 60 * 1000L, new AjaxCallback<JSONArray>() {
			@Override
			public void callback(String url, JSONArray array, AjaxStatus status) {
				if (array != null) {
					try {
						ListAdapter adapter = new ListAdapter(aq.getContext(), array);
						list.setAdapter(adapter);
					} catch (Exception e) {
					}
				} else {
					Toast.makeText(aq.getContext(), "Error:" + status.getCode(), Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	JSONArray arr;

	public class ListAdapter extends BaseExpandableListAdapter {
		Context context;
		JSONArray array;

		public ListAdapter() {
		}

		public ListAdapter(Context context, JSONArray array) {
			this.context = context;
			this.array = array;
			arr = array;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View root, ViewGroup parent) {
			if (root == null)
				root = LayoutInflater.from(context).inflate(R.layout.item_child, parent, false);
			TextView name = (TextView) root.findViewById(R.id.child_name);
			AQuery aq = new AQuery(root);
			try {
				JSONObject item = array.getJSONObject(groupPosition).getJSONArray("category_items").getJSONObject(childPosition);
				name.setText(item.getString("item_title_ru"));
				aq.id(R.id.child_cover).image(API.base + item.getString("cover"), true, true);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return root;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			try {
				return array.getJSONObject(groupPosition).getJSONArray("category_items").length();
			} catch (JSONException e) {
				return 0;
			}
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public int getGroupCount() {
			return array.length();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View root, ViewGroup parent) {
			if (root == null)
				root = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
			TextView name = (TextView) root.findViewById(R.id.group_name);
			try {
				name.setText(array.getJSONObject(groupPosition).getString("category_title_ru"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return root;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int arg0, int arg1) {
			return true;
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		try {
			JSONArray gallery = arr.getJSONObject(groupPosition).getJSONArray("category_items").getJSONObject(childPosition).getJSONArray("gallery");
			fragmentManager.beginTransaction().replace(R.id.item_container, new ListFragment(gallery)).commit();
		} catch (Exception e) {
		}
		return false;
	}

	public static class ListFragment extends Fragment {
		JSONArray arr;

		public ListFragment() {
		}

		public ListFragment(JSONArray arr) {
			this.arr = arr;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_list, container, false);
			LinearLayout gallery = (LinearLayout) rootView.findViewById(R.id.gallery);
			for (int i = 0; i < arr.length(); i++) {
				View item = inflater.inflate(R.layout.item_gallery, container, false);
				AQuery aq = new AQuery(item);
				try {
					aq.id(R.id.gallery_image).image(API.base + arr.getJSONObject(i).getString("img_ru"), true, true);
					TextView text = (TextView) item.findViewById(R.id.gallery_text);
					text.setText(Html.fromHtml(arr.getJSONObject(i).getString("text_ru")));
					//aq.id(R.id.gallery_text).text(arr.getJSONObject(i).getString("text_ru"));
					gallery.addView(item);
				} catch (Exception e) {
				}
			}
			return rootView;
		}
	}
}
