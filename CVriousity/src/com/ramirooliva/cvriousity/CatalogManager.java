package com.ramirooliva.cvriousity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import com.ramirooliva.cvriousity.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class CatalogManager {

	private static final String TAG = "Cvriousity::CatalogManager";

	ArrayList<CatalogBean> catalogBeanArrayList = new ArrayList<CatalogBean>();

	CatalogManager() {
		loadData();
	}

	private String getAppFilesPath() {
		String path = Environment.getExternalStorageDirectory()
				+ "/Android/data/org.cvriousity.testapp/files";
		File file = new File(path);
		file.mkdirs();
		return path;
	}

	@SuppressWarnings("resource")
	public Object loadClassFile(File f) {
		try {
			ObjectInputStream ois = new ObjectInputStream(
					new FileInputStream(f));
			Object o = ois.readObject();
			return o;
		} catch (Exception ex) {
			Log.v("Catalog", ex.getMessage());
			ex.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void loadData() {
		File file = new File(getAppFilesPath(), "cvriousity_catalog");
		if (file.exists()) {
			ArrayList<CatalogIndex> catalogIndexArrayList = (ArrayList<CatalogIndex>) loadClassFile(file);
			catalogBeanArrayList = new ArrayList<CatalogBean>();
			for (int i = 0; i < catalogIndexArrayList.size(); i++) {
				CatalogBean b = new CatalogBean();
				CatalogIndex e = catalogIndexArrayList.get(i);
				b.filename = e.filename;
				String metadata[] = e.metadata.split(";");
				b.artTitle = metadata[0];
				catalogBeanArrayList.add(b);
			}
		}
	}

	private void saveData() {
		try {
			ArrayList<CatalogIndex> catalogIndexArrayList = new ArrayList<CatalogIndex>();
			for (int i = 0; i < catalogBeanArrayList.size(); i++) {
				CatalogBean b = catalogBeanArrayList.get(i);
				CatalogIndex e = new CatalogIndex();
				e.filename = b.filename;
				String metadata = b.artTitle + ";";
				e.metadata = metadata;
				catalogIndexArrayList.add(e);
			}

			File file = new File(getAppFilesPath(), "cvriousity_catalog");
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(file)); // Select where you wish to
													// save the file...
			oos.writeObject(catalogIndexArrayList); // write the class as an
													// 'object'
			oos.flush(); // flush the stream to insure all of the information
							// was written to 'save.bin'
			oos.close();// close the stream
		} catch (Exception ex) {
			Log.v("Catalog", ex.getMessage());
			ex.printStackTrace();
		}
	}

	public void addEntry(CatalogBean entry) {
		catalogBeanArrayList.add(entry);
		saveData();
	}

	public void removeEntry(int position) {
		catalogBeanArrayList.remove(position);
		saveData();
	}

	public File getRandomImageFile() {
		Calendar cal = Calendar.getInstance();
		String fileName = "cvriousity_catalog_entry_" + cal.getTimeInMillis()
				+ ".jpg";
		return new File(getAppFilesPath(), fileName);
	}

	public String getLastImageFile() {
		return getRandomImageFile().getAbsolutePath();
	}

	public void addImageFromResource(int resourceId, String title) {
		InputStream ins = App.getContext().getResources()
				.openRawResource(resourceId);
		File file = getRandomImageFile();
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
			int size = 0;
			// Read the entire resource into a local byte buffer.
			byte[] buffer = new byte[1024];
			while ((size = ins.read(buffer, 0, 1024)) >= 0) {
				fos.write(buffer, 0, size);
			}
			ins.close();
			fos.close();
			CatalogBean b = new CatalogBean();
			b.artTitle = title;
			b.filename = file.getName();
			addEntry(b);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void addImage(Bitmap bmp, String title) {
		Calendar cal = Calendar.getInstance();
		String filename = "cvriousity_catalog_entry_" + cal.getTimeInMillis()
				+ ".jpg";
		File file = new File(getAppFilesPath(), filename);
		FileOutputStream out;
		try {
			out = new FileOutputStream(file);
			bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		CatalogBean b = new CatalogBean();
		b.filename = filename;
		b.artTitle = title;
		addEntry(b);
	}

	public int size() {
		return catalogBeanArrayList.size();
	}

	// public Bitmap getImage(int index) {
	// Log.i(TAG, "getImage called!");
	// String filename = catalogBeanArrayList.get(index).filename;
	// File file = new File(getAppFilesPath(), filename);
	// FileInputStream in;
	// try {
	// in = new FileInputStream(file);
	// final BitmapFactory.Options options = new BitmapFactory.Options();
	// options.inPurgeable = true;
	// options.inSampleSize = 4;
	// return BitmapFactory.decodeStream(in, null, options);
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// }
	// return null;
	// }
	//

	public Bitmap getImage(int index) {
		Log.i(TAG, "getImageThumbnail called!");
		CatalogBean b = catalogBeanArrayList.get(index);

		// if bitmap is not cached, then temporary create a thumbnail. Will
		// always delay storage of large bitmap as much as possible
		File file = new File(getAppFilesPath(), b.filename);
		FileInputStream in;
		try {
			in = new FileInputStream(file);
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPurgeable = true;
			options.inSampleSize = 4;
			return BitmapFactory.decodeStream(in, null, options);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;

	}

	public String getImageFilename(int index) {
		CatalogBean b = catalogBeanArrayList.get(index);
		File file = new File(getAppFilesPath(), b.filename);
		return file.getPath();
	}

	public String getImageTitle(int index) {
		CatalogBean b = catalogBeanArrayList.get(index);
		return b.artTitle;
	}

	public void setTitle(int index, String title) {
		CatalogBean b = catalogBeanArrayList.get(index);
		b.artTitle = title;
		saveData();
	}

	public void TrainAll(int viewMode, int screenHeight) {

		ResetTrain();
		CatalogManager catalogManager = Intro.getCatalogInstance();
		for (int i = 0; i < catalogManager.size(); i++) {
			Log.i(TAG,
					"Training image " + (i + 1) + "="
							+ catalogManager.getImageFilename(i) + "...");
			/*
			 * DisplayMetrics metrics = new DisplayMetrics();
			 * App.getContext().getApplicationContext
			 * ().getWindowManager().getDefaultDisplay().getMetrics(metrics);
			 * Log.i(TAG, "Alto: " + metrics.heightPixels);
			 */
			CatalogBean b = catalogBeanArrayList.get(i);
			File file = new File(getAppFilesPath(), b.filename);
			TrainImage(viewMode,  file.getAbsolutePath(), screenHeight);
		}
	}

	public native void TrainImage(int viewMode, String filename, int height);

	public native void ResetTrain();

}
