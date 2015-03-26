package com.testwork.instagramloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.testwork.instagramloader.SimplePictureLoader.LoadCallback;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.print.PrintHelper;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends FragmentActivity {

	static MainActivity instance;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		instance = this;

		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new FirstScreenFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	private static void switchToSelectImages(ArrayList<String> _imageUrls) {
		instance.getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.container,
						new SelectImagesScreenFragment(_imageUrls))
				.addToBackStack(null).commit();
	}

	public static class SelectImagesScreenFragment extends Fragment {

		ImageAdapter imageAdapter;
		ArrayList<String> imageUrls;
		Set<String> selectedImages;

		GridView gridView;
		Button btn;

		public SelectImagesScreenFragment(ArrayList<String> _imageUrls) {

			imageUrls = _imageUrls;
			selectedImages = Collections.synchronizedSet(new HashSet<String>());

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_selectimages,
					container, false);

			btn = (Button) rootView.findViewById(R.id.print_btn);

			gridView = (GridView) rootView.findViewById(R.id.img_grid);

			imageAdapter = new ImageAdapter();

			gridView.setAdapter(imageAdapter);

			gridView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					String imgPath = imageUrls.get(position);
					if (selectedImages.contains(imgPath)) {
						selectedImages.remove(imgPath);
					} else {
						selectedImages.add(imgPath);
					}

					imageAdapter.notifyDataSetChanged();

				}

			});

			btn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					
					if(selectedImages.size()==0){
						selectedImages.addAll(imageUrls);
					}
					
					
					final DrawCollage dc = new DrawCollage(getActivity());

					for (String imgStr : selectedImages) {
						dc.imagePaths.add(imgStr);
					}

					selectedImages.clear();
					imageAdapter.notifyDataSetChanged();
					
					dc.makeCollageBitmap();

					PrintHelper photoPrinter = new PrintHelper(getActivity());
					photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
					photoPrinter.printBitmap("instagram collage",
							dc.collageBitmap);

				}

			});

			Toast.makeText(getActivity(), R.string.select_images, Toast.LENGTH_LONG);
			
			return rootView;

		}

		public class ImageAdapter extends BaseAdapter {

			private LayoutInflater inflater;

			ImageAdapter() {
				inflater = LayoutInflater.from(getActivity());
			}

			@Override
			public int getCount() {
				return imageUrls.size();
			}

			@Override
			public Object getItem(int arg0) {
				return null;
			}

			@Override
			public long getItemId(int arg0) {
				return 0;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				ViewHolder holder;
				View view = convertView;

				if (view == null) {
					view = inflater.inflate(R.layout.item_grid_image, parent,
							false);
					holder = new ViewHolder();
					assert view != null;
					view.setTag(holder);
				} else {
					holder = (ViewHolder) view.getTag();
				}

				holder.imageView = (ImageView) view.findViewById(R.id.image);
				holder.markView = (ImageView) view.findViewById(R.id.markimage);
				holder.picturePath = imageUrls.get(position);
				holder.selected = selectedImages.contains(holder.picturePath);
				if (holder.selected) {
					holder.markView.setVisibility(View.VISIBLE);
				} else {
					holder.markView.setVisibility(View.INVISIBLE);
				}
				holder.link();

				return view;
			}

		}

		public class ViewHolder {

			public ImageView markView;
			public ImageView imageView;
			public String picturePath;
			public Context context;
			public Boolean selected;

			public void link() {
				// load picture in thread and show it
				Bitmap bm = DrawCollage.decodeFile(picturePath,
						imageView.getWidth());
				imageView.setImageBitmap(bm);
			}

		}

	}

	public static class FirstScreenFragment extends Fragment {

		Button btn;
		TextView text;
		EditText username;
		ImageView imgView;

		SimplePictureLoader loader;

		ArrayList<String> imageUrls;

		public FirstScreenFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);

			btn = (Button) rootView.findViewById(R.id.download_btn);
			text = (TextView) rootView.findViewById(R.id.text);
			username = (EditText) rootView.findViewById(R.id.username);

			// imgView = (ImageView) rootView.findViewById(R.id.collage_img);
			//

			loader = new SimplePictureLoader(getActivity());

			btn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					
					if(username.getText().toString().length()==0){
						return;
					}
					
					text.setText(R.string.loading);
					
					btn.setEnabled(false);

					loader = new SimplePictureLoader(getActivity());

					loader.loadImages(username.getText().toString(),
							new LoadCallback() {

								@Override
								public void SetText(final String str) {

									Runnable r = new Runnable() {
										@Override
										public void run() {
											text.setText(str);
										}
									};

									getActivity().runOnUiThread(r);
								}

								@Override
								public void OnComplete() {
									

									Runnable r = new Runnable() {
										@Override
										public void run() {
											btn.setEnabled(true);
										}
									};

									getActivity().runOnUiThread(r);
									

									imageUrls = new ArrayList<String>();

									int i = 0;

									while (i < loader.pictureList.size()) {
										String filePath = loader.pictureList
												.get(i).filePath;
										if (filePath == null
												|| filePath.length() == 0) {
											i++;
											continue;
										}

										imageUrls.add(filePath);
										i++;
									}

									if(imageUrls.size()>0){
										switchToSelectImages(imageUrls);
									} else {
										
										Runnable r2 = new Runnable() {
											@Override
											public void run() {
												text.setText(R.string.error);
											}
										};

										getActivity().runOnUiThread(r2);
									}
								}
								
								@Override
								public void OnFail(){
									
									Runnable r = new Runnable() {
										@Override
										public void run() {
											btn.setEnabled(true);
										}
									};
									
								}
								
							});

				}
			});

			return rootView;
		}

	}

}
