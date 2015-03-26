package com.testwork.instagramloader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;

public class DrawCollage {
	
	private Context context;
	
	public ArrayList<String> imagePaths;
	
	public int rows;
	public int cols;
	
	private int bmWidth;
	private int bmHeight;
	
	public Bitmap collageBitmap;
	
	private Paint paint;
	private Canvas canvas;
	private Bitmap currentBitmap;
	
	private int IMAGE_MAX_SIZE = 600;
	
	public DrawCollage(Context context){
		imagePaths = new ArrayList<String>();
		rows = 6;
		cols = 4;
		bmWidth = 1200;
		bmHeight = 1800;
		
		
		
		this.context = context;
		
	}
	
	public void makeCollageBitmap(){
		
		
		int i = 0;
		int size = rows*cols;
		
		if(size>imagePaths.size()){
			size = imagePaths.size();
		}
		
		collageBitmap = Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.ARGB_8888);
		
		canvas = new Canvas(collageBitmap);
		paint = new Paint();
		paint.setFilterBitmap(true);
		paint.setAntiAlias(true);
		
		
		while(i<size){
			int row = i/cols;
			int col = i%cols;
			
			currentBitmap = loadBitmapByIndex(i);
			
//			){
				drawOneBitmap(col, row);
//			}
			
			currentBitmap.recycle();
			currentBitmap = null;
			
			System.out.println("image number "+i+" row "+row+" col "+col);
			
			i++;
		}
		
	}
	
	private Bitmap loadBitmapByIndex(int i) {
		
		String path = imagePaths.get(i);
		
		Bitmap bm = DrawCollage.decodeFile(path, IMAGE_MAX_SIZE);

		return bm;
	}
	
	public static Bitmap decodeFile(String sPath, int image_max_size) {


		Bitmap b = null;
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;


			if (sPath == null || sPath.length() == 0) {
				return null;
			}

			InputStream fis;

			fis = new FileInputStream(sPath);

			BitmapFactory.decodeStream(fis, null, o);
			if (fis != null) {
				fis.close();
			}

			int scale = 1;
			if (o.outHeight > image_max_size 
					|| o.outWidth > image_max_size) {
				scale = (int) Math.pow(
						2,
						(int) Math.round(Math.log((image_max_size)
								/ (double) Math.max(o.outHeight, o.outWidth))
								/ Math.log(0.5)));
			}


			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;

			fis = new FileInputStream(sPath);

			b = BitmapFactory.decodeStream(fis, null, o2);
			if (fis != null) {
				fis.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return b;
	}


	private void drawOneBitmap(int col, int row){

		float sizeX = (float)bmWidth/(float)cols;
		float sizeY = (float)bmHeight/(float)rows;
		
		float scaleX = sizeX/(float)currentBitmap.getWidth();
		float scaleY = sizeY/(float)currentBitmap.getHeight();
		
		float dx = sizeX*(float)col;
		float dy = sizeY*(float)row;
				
		Matrix drawMatrix = new Matrix();
		drawMatrix.preTranslate(dx, dy);
		drawMatrix.postScale(scaleX, scaleY);
				
		canvas.drawBitmap(currentBitmap, drawMatrix, paint);
		
	}
	
}
