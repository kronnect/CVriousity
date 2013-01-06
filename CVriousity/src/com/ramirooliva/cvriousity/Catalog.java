package com.ramirooliva.cvriousity;

import java.io.File;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class Catalog extends Activity implements
		AdapterView.OnItemSelectedListener, ViewSwitcher.ViewFactory {
	private static final int CAMERA_REQUEST = 1888;
//	private static final String TAG = "Cvriousity::Catalog";
	private Gallery gallery;
	private CatalogManager mCatalogManager;
	private String mLastFilename;

	private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
//				Log.i(TAG, "OpenCV loaded successfully");
				// Load native library after(!) OpenCV initialization
				System.loadLibrary("mixed_sample");
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public Catalog() {
//		Log.i(TAG, "Instantiated new " + this.getClass());
		mCatalogManager = Intro.getCatalogInstance();
	}

	@Override
	protected void onPause() {
//		Log.i(TAG, "onPause");
		super.onPause();
	}

	@Override
	protected void onResume() {
//		Log.i(TAG, "onResume");
		super.onResume();
	}

	/** Called when the activity is first created. */
	static Uri capturedImageUri = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Log.i(TAG, "onCreate");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.catalog);

//		Log.i(TAG, "Trying to load OpenCV library");
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this,
				mOpenCVCallBack)) {
//			Log.e(TAG, "Cannot connect to OpenCV Manager");
		}

		final ImageButton photo_button = (ImageButton) findViewById(R.id.btn_photo);
		photo_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(
						android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
				File file = mCatalogManager.getRandomImageFile();
				mLastFilename = file.getAbsolutePath();
				Uri outputFileUri = Uri.fromFile(file);
				i.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
				startActivityForResult(i, CAMERA_REQUEST);
			}
		});

		final Button go_button = (Button) findViewById(R.id.btn_go);
		go_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				new AsyncTask<Integer, Integer, Boolean>() {
					ProgressDialog progressDialog;
					DisplayMetrics metrics;

					@Override
					protected void onPreExecute() {
						progressDialog = ProgressDialog.show(Catalog.this, "",
								"Training...");
						metrics = new DisplayMetrics();
						getWindowManager().getDefaultDisplay().getMetrics(
								metrics);
						mCatalogManager.ResetTrain();
					}

					@Override
					protected void onProgressUpdate(Integer... values) {
						super.onProgressUpdate(values);
						int mode = values[0];
						switch(mode) {
						case CameraView.VIEW_MODE_FBRIEF:
							progressDialog.setMessage("Training (FAST+BRIEF)...");
							break;
						case CameraView.VIEW_MODE_ORB:
							progressDialog.setMessage("Training (ORB)...");
							break;
						case CameraView.VIEW_MODE_FFREAK:
							progressDialog.setMessage("Training (FAST+FREAK)...");
							break;
						case CameraView.VIEW_MODE_MFREAK:
							progressDialog.setMessage("Training (MSER+FREAK)...");
							break;
						case CameraView.VIEW_MODE_GFREAK:
							progressDialog.setMessage("Training (GFTT+FREAK)...");
							break;
						}
					}

					@Override
					protected Boolean doInBackground(Integer... params) {
						if (params == null) {
							return false;
						}
						try {
							// index images of catalog
							Thread.currentThread().setPriority(
									Thread.MAX_PRIORITY);
							for (int trainViewMode = 0; trainViewMode < 5; trainViewMode++) {
								publishProgress(trainViewMode);
								Intro.getCatalogInstance().TrainAll(
										trainViewMode, metrics.heightPixels);
							}

						} catch (Exception e) {
							Log.e("tag", e.getMessage());
							return false;
						}
						return true;
					}

					@Override
					protected void onPostExecute(Boolean result) {
						progressDialog.dismiss();
						Intent intent = new Intent(Catalog.this,
								CameraMixed.class);
						Catalog.this.startActivity(intent);
					}
				}.execute();
			}
		});

		final Button delete_button = (Button) findViewById(R.id.btn_delete);
		delete_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int currentPosition = gallery.getSelectedItemPosition();
				if (currentPosition >= 0) {
					mCatalogManager.removeEntry(currentPosition);
					((BaseAdapter) gallery.getAdapter()).notifyDataSetChanged();
				}
			}
		});

		final Button edit_button = (Button) findViewById(R.id.btn_label);
		edit_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int currentPosition = gallery.getSelectedItemPosition();
				if (currentPosition >= 0) {
					AlertDialog.Builder alert = new AlertDialog.Builder(
							Catalog.this);

					alert.setTitle("Image's title");
					alert.setMessage("Enter a new title for this image:");

					// Set an EditText view to get user input
					final EditText input = new EditText(Catalog.this);
					String artTitle = mCatalogManager
							.getImageTitle(currentPosition);
					input.setText(artTitle.toCharArray(), 0, artTitle.length());
					alert.setView(input);

					alert.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									String value = input.getText().toString();
									int currentPosition = gallery
											.getSelectedItemPosition();
									mCatalogManager.setTitle(currentPosition,
											value);
									showImageTitle(currentPosition);
								}
							});

					alert.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Canceled.
								}
							});

					alert.show();

				}
			}
		});

		mSwitcher = (ImageSwitcher) findViewById(R.id.switcher);
		mSwitcher.setFactory(this);
		mSwitcher.setInAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_in));
		mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_out));

		gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setAdapter(new ImageAdapter(this));
		gallery.setOnItemSelectedListener(this);

	}

	private Bitmap decodeSampledBitmapFromPath(String path) {

		// First decode with inJustDecodeBounds=true to check dimensions
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		options.inSampleSize = Math.round((float) options.outHeight
				/ (float) 1024);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(path);
	}

	private int getLastImageId() {
		final String[] imageColumns = { BaseColumns._ID,
				MediaColumns.DATA };
		final String imageOrderBy = BaseColumns._ID + " DESC";
		Cursor imageCursor = managedQuery(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageColumns,
				null, null, imageOrderBy);
		if (imageCursor.moveToFirst()) {
			int id = imageCursor.getInt(imageCursor
					.getColumnIndex(BaseColumns._ID));
			String fullPath = imageCursor.getString(imageCursor
					.getColumnIndex(MediaColumns.DATA));
//			Log.d(TAG, "getLastImageId::id " + id);
//			Log.d(TAG, "getLastImageId::path " + fullPath);
			imageCursor.close();
			return id;
		} else {
			return 0;
		}
	}

	private void removeImage(int id) {
		ContentResolver cr = getContentResolver();
		cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				BaseColumns._ID + "=?",
				new String[] { Long.toString(id) });
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAMERA_REQUEST) {
			// Bitmap photo = (Bitmap) data.getExtras().get("data");
			// imageView.setImageBitmap(photo);
			try {
//				Log.i(TAG, "Getting image...");
				// remove last image from camera roll
				removeImage(getLastImageId());
				// load a reduced size version of captured image
				Bitmap bitmap = decodeSampledBitmapFromPath(mLastFilename);
				// delete temp image
				new File(mLastFilename).delete();
				// crop image
				DisplayMetrics metrics = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(metrics);
				Bitmap croppedBitmap = CropBitmap.getCroppedImage(bitmap, 0.7,
						metrics.widthPixels, metrics.heightPixels);
				bitmap.recycle();
				System.gc();
//				Log.i(TAG, "Adding image...");
				mCatalogManager.addImage(croppedBitmap, "Untitled image");
				((BaseAdapter) gallery.getAdapter()).notifyDataSetChanged();
//				Log.i(TAG, "Refreshed...");
				croppedBitmap.recycle();
				System.gc();
				
				gallery.setSelection(gallery.getCount()-1, true);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] projx = { MediaColumns.DATA };
		Cursor cursor = managedQuery(contentUri, projx, null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaColumns.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	private void showImageTitle(int position) {
		Drawable d = new BitmapDrawable(getResources(),
				mCatalogManager.getImage(position));
		mSwitcher.setImageDrawable(d);
		Toast.makeText(getBaseContext(),
				"You have selected " + mCatalogManager.getImageTitle(position),
				Toast.LENGTH_SHORT).show();
	}

	public void onItemSelected(AdapterView<?> parent, View v, int position,
			long id) {
		showImageTitle(position);
	}

	public void onNothingSelected(AdapterView<?> parent) {
	}

	public View makeView() {
		ImageView i = new ImageView(this);
		i.setBackgroundColor(0xFF000000);
		i.setScaleType(ImageView.ScaleType.FIT_CENTER);
		i.setLayoutParams(new ImageSwitcher.LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT));
		return i;
	}

	private ImageSwitcher mSwitcher;

	public class ImageAdapter extends BaseAdapter {
		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			return mCatalogManager.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView i = new ImageView(mContext);

			i.setImageBitmap(mCatalogManager.getImage(position));
			i.setAdjustViewBounds(true);
			i.setLayoutParams(new Gallery.LayoutParams(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
			// i.setBackgroundResource(R.drawable.btn_blue);
			i.setBackgroundColor(Color.WHITE);
			i.setPadding(1, 1, 1, 1);
			return i;
		}

		private Context mContext;

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_POINTER_UP: {
			break;
		}
		}
		/*
		 * Intent intent = new Intent(Catalog.this, Sample4Mixed.class);
		 * Catalog.this.startActivity(intent);
		 */
		return true;
	}

}
