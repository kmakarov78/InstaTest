package com.testwork.instagramloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class SimplePictureLoader {

	LoadCallback callback;

	private String userId = "";

	public ArrayList<InstaPictureInfo> pictureList;

	private Context context;
	
	private String packageName = "";

	public SimplePictureLoader(Context context) {
		this.context = context;
		pictureList = new ArrayList<InstaPictureInfo>();
	}

	public void getUserId(String userName) {

		try {
			URL url = new URL("https://api.instagram.com/v1/users/search?q="
					+ userName + "&client_id=788a8b525cd34c499f51489b435b18a5");

			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);
			urlConnection.connect();

			String response = streamToString(urlConnection.getInputStream());
			System.out.println(response);
			getIdFromJson(response);

		} catch (Exception ex) {
			try {
				Looper.prepare();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
			ex.printStackTrace();
		}

	}

	public void getPictureList(String userId) {

		try {

			boolean hasNextUrl = true;

			URL url = new URL(
					"https://api.instagram.com/v1/users/"
							+ userId
							+ "/media/recent/?client_id=788a8b525cd34c499f51489b435b18a5");

			pictureList = new ArrayList<InstaPictureInfo>();

			while (hasNextUrl && pictureList.size() < 200) {

				HttpURLConnection urlConnection = (HttpURLConnection) url
						.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.connect();

				String response = streamToString(urlConnection.getInputStream());
				//System.out.println(response);

				String nextUrl = getNextUrlFromJson(response);
				if(nextUrl.length()>0){
					hasNextUrl = true;
					url = new URL(nextUrl);
				} else {
					hasNextUrl = false;
				}
				
				processImageListJSON(response);
				
				ReportProgress(context.getResources().getString(R.string.getting_list, ""+pictureList.size()));

			}
			
			sortList();

		} catch (Exception ex) {
			try {
				Looper.prepare();
				Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG)
						.show();
			} catch (Exception e) {
				e.printStackTrace();
			}
			ex.printStackTrace();
		}

	}
	
	private void ReportProgress(String progress){
		if(callback!=null){
			try{
				callback.SetText(progress);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	private String streamToString(InputStream is) throws IOException {
		String str = "";

		if (is != null) {

			StringBuilder sb = new StringBuilder();
			String line;

			try {

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));

				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}

				reader.close();

			} finally {
				is.close();
			}

			str = sb.toString();

		}

		return str;
	}

	public void loadImages(final String userName, final LoadCallback cb) {

		callback = cb;
		
		new Thread() {

			@Override
			public void run() {

				try {

					getUserId(userName);

					getPictureList(userId);
					
					downloadPictures();
					
					//cb.SetText(userId);

				} catch (Exception e) {
					e.printStackTrace();
					try {
						Looper.prepare();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG)
							.show();
				}
				finally{
					cb.OnComplete();
				}

			}
		}.start();

	}
	
	private void downloadPictures(){
		int i = 0;
		int count = 50;
		while(i<count && i<pictureList.size()){
			InstaPictureInfo info = pictureList.get(i);
			info.filePath = LoadPicture(info.url); 
			ReportProgress(context.getResources().getString(R.string.getting_pics, ""+i));
			i++;
		}
	}
	
	public String LoadPicture(String urlStr) {

		if (urlStr.startsWith("http:")) {
			return loadImageFromHttp(urlStr);
		}

		if (urlStr.startsWith("https:")) {
			return loadImageFromHttps(urlStr);
		}
		return "";
	}
	
	public String loadImageFromHttp(String urlStr) {
		File SDCardRoot = Environment.getExternalStorageDirectory();

		String path = "";
		String fname = "";

		try {

			URL url = new URL(urlStr);


			path = getPathForPics();

			fname = md5(urlStr);

			File file = new File(path, fname + ".jpg");

			if (file.exists()) {
				return path + fname + ".jpg";
			}

			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();

			urlConnection.setRequestMethod("GET");

			urlConnection.connect();

			File parent = file.getParentFile();

			if (!parent.exists()) {
				if (!parent.mkdirs()) {
					throw new IllegalStateException("Couldn't create dir: "
							+ parent);
				}
			}

			File nomedia = new File(path, ".nomedia");

			if (!nomedia.exists()) {
				nomedia.createNewFile();
			}

			FileOutputStream fileOutput = new FileOutputStream(file);

			InputStream inputStream = urlConnection.getInputStream();

			int totalSize = urlConnection.getContentLength();
			int downloadedSize = 0;

			byte[] buffer = new byte[1024];
			int bufferLength = 0;

			while ((bufferLength = inputStream.read(buffer)) > 0) {
				fileOutput.write(buffer, 0, bufferLength);
				downloadedSize += bufferLength;
			}
			fileOutput.close();

			return path + fname + ".jpg";

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}

	public String loadImageFromHttps(String urlStr) {
		File SDCardRoot = Environment.getExternalStorageDirectory();

		String path = "";
		String fname = "";

		try {

			URL url = new URL(urlStr);

			path = getPathForPics();

			fname = md5(urlStr);

			File file = new File(path, fname + ".jpg");

			if (file.exists()) {
				return path + fname + ".jpg";
			}

			HttpsURLConnection urlConnection = (HttpsURLConnection) url
					.openConnection();

			urlConnection.setRequestMethod("GET");
			
			urlConnection.connect();

			File parent = file.getParentFile();

			if (!parent.exists()) {
				if (!parent.mkdirs()) {
					throw new IllegalStateException("Couldn't create dir: "
							+ parent);
				}
			}

			File nomedia = new File(path, ".nomedia");

			if (!nomedia.exists()) {
				nomedia.createNewFile();
			}

			FileOutputStream fileOutput = new FileOutputStream(file);

			InputStream inputStream = urlConnection.getInputStream();

			int totalSize = urlConnection.getContentLength();
			int downloadedSize = 0;

			byte[] buffer = new byte[1024];
			int bufferLength = 0;
			
			while ((bufferLength = inputStream.read(buffer)) > 0) {
				fileOutput.write(buffer, 0, bufferLength);
				downloadedSize += bufferLength;
			}
			fileOutput.close();


			return path + fname + ".jpg";

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}


	private String md5(String in) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.reset();
			digest.update(in.getBytes());
			byte[] a = digest.digest();
			int len = a.length;
			StringBuilder sb = new StringBuilder(len << 1);
			for (int i = 0; i < len; i++) {
				sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
				sb.append(Character.forDigit(a[i] & 0x0f, 16));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getPathForPics() {
		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/Android/data/"
				+ getPackageName()
				+ "/cache/pics/";
		return path;
	}

	private String getPackageName() {

		if (packageName.length() != 0) {
			return packageName;
		}

		if (context != null) {
			packageName = context.getPackageName();
		}

		return packageName;

	}

	public String getNextUrlFromJson(String json) {
		
		String ret = "";

		try {

			JSONObject jsonObj = new JSONObject(json);
			if(jsonObj.has("pagination")){
				JSONObject pagination = jsonObj.getJSONObject("pagination");
				if(pagination.has("next_url")){
					ret = pagination.getString("next_url");
				}
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return ret;
	}
	
	public String getIdFromJson(String json) {

		try {

			JSONObject jsonObj = new JSONObject(json);
			JSONObject user = jsonObj.getJSONArray("data").getJSONObject(0);
			userId = user.getString("id");

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return userId;
	}
	
	

	private void processImageListJSON(String json_str) {
		try {

			JSONObject jsonObj = new JSONObject(json_str);
			JSONArray data = jsonObj.getJSONArray("data");

			int i = 0, count = data.length();

			while (i < count) {

				JSONObject img_json = data.getJSONObject(i);
				processImageItem(img_json);
				i++;

			}

			for (InstaPictureInfo info : pictureList) {
				System.out.println("likes: " + info.likes + ", width: "
						+ info.width + ", height: " + info.height + ", url: "
						+ info.url);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void sortList() {
		Collections.sort(pictureList, new Comparator<InstaPictureInfo>() {

			@Override
			public int compare(InstaPictureInfo lhs, InstaPictureInfo rhs) {
				// TODO Auto-generated method stub
				return rhs.likes.compareTo(lhs.likes);
			}

		});

	}

	private void processImageItem(JSONObject obj) {

		if (obj.has("type")) {

			String type = "";
			try {
				type = obj.getString("type");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			if (!type.equalsIgnoreCase("image")) {
				return;
			}

		} else {
			return;
		}

		if (obj.has("images")) {

			JSONObject images = null;

			try {
				images = obj.getJSONObject("images");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (images != null && images.has("low_resolution")) {
				InstaPictureInfo info = new InstaPictureInfo();
				try {
					JSONObject lowres = images.getJSONObject("low_resolution");
					if (lowres.has("url")) {
						info.url = lowres.getString("url");
					}
					if (lowres.has("width")) {
						info.width = lowres.getInt("width");
					}
					if (lowres.has("height")) {
						info.height = lowres.getInt("height");
					}

					if (obj.has("likes")) {
						JSONObject likes = obj.getJSONObject("likes");
						if (likes.has("count")) {
							info.likes = likes.getInt("count");
						}
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}

				pictureList.add(info);
			}
		}

	}

	public interface LoadCallback {
		public void SetText(String str);

		public void OnComplete();
		
		public void OnFail();
	}

}

