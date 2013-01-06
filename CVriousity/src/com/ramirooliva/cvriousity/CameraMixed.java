package com.ramirooliva.cvriousity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import com.ramirooliva.cvriousity.R;

public class CameraMixed extends Activity {
	private static final String TAG = "Sample::Activity";

	private MenuItem mItemPreviewFBRIEF;
	private MenuItem mItemPreviewORB;
	private MenuItem mItemPreviewFFREAK;
	private MenuItem mItemPreviewMFREAK;
	private MenuItem mItemPreviewGFREAK;
	private CameraView mView;
	private static SensorManager mySensorManager;
	private boolean sensorrunning;
	private float y;

	public CameraMixed() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
		if (null != mView)
			mView.releaseCamera();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
		if ((null != mView) && !mView.openCamera()) {
			AlertDialog ad = new AlertDialog.Builder(this).create();
			ad.setCancelable(false); // This blocks the 'BACK' button
			ad.setMessage("Fatal error: can't open camera!");
			ad.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}
			});
			ad.show();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Create and set View
		mView = new CameraView(CameraMixed.this);
		setContentView(mView);

		// Check native OpenCV camera
		if (!mView.openCamera()) {
			AlertDialog ad = new AlertDialog.Builder(CameraMixed.this).create();
			ad.setCancelable(false); // This blocks the 'BACK' button
			ad.setMessage("Fatal error: can't open camera!");
			ad.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}
			});
			ad.show();
		}
		/*
		 * mySensorManager = (SensorManager)
		 * getSystemService(Context.SENSOR_SERVICE); List<Sensor> mySensors =
		 * mySensorManager .getSensorList(Sensor.TYPE_ORIENTATION);
		 * 
		 * if (mySensors.size() > 0) {
		 * mySensorManager.registerListener(mySensorEventListener,
		 * mySensors.get(0), SensorManager.SENSOR_DELAY_NORMAL); sensorrunning =
		 * true; Toast.makeText(this, "Start ORIENTATION Sensor",
		 * Toast.LENGTH_LONG) .show(); } else { Toast.makeText(this,
		 * "No ORIENTATION Sensor", Toast.LENGTH_LONG) .show(); sensorrunning =
		 * false; finish(); }
		 */
	}

	private SensorEventListener mySensorEventListener = new SensorEventListener() {

		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub

			y = event.values[1];
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	};

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		if (sensorrunning) {
			mySensorManager.unregisterListener(mySensorEventListener);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "onCreateOptionsMenu");
		mItemPreviewFBRIEF = menu.add("FAST+BRIEF");
		mItemPreviewFFREAK = menu.add("FAST+FREAK");
		mItemPreviewORB = menu.add("ORB");
		mItemPreviewMFREAK = menu.add("MSER+FREAK");
		mItemPreviewGFREAK = menu.add("GFREAK+FREAK");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "Menu Item selected " + item);
		if (item == mItemPreviewORB) {
			mView.setViewMode(CameraView.VIEW_MODE_ORB);
		} else if (item == mItemPreviewMFREAK) {
			mView.setViewMode(CameraView.VIEW_MODE_MFREAK);
		} else if (item == mItemPreviewFFREAK) {
			mView.setViewMode(CameraView.VIEW_MODE_FFREAK);
		} else if (item == mItemPreviewGFREAK) {
			mView.setViewMode(CameraView.VIEW_MODE_GFREAK);
		} else if (item == mItemPreviewFBRIEF) {
			mView.setViewMode(CameraView.VIEW_MODE_FBRIEF);
		}
		trainCatalogImages();
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_UP:
			mView.doCameraFocus();
			break;
		}
		return true;
	}

	private void trainCatalogImages() {
		new AsyncTask<Integer, Integer, Boolean>() {
			ProgressDialog progressDialog;
			private int viewMode;
			DisplayMetrics metrics;

			@Override
			protected void onPreExecute() {
				/*
				 * This is executed on UI thread before doInBackground(). It is
				 * the perfect place to show the progress dialog.
				 */
				progressDialog = ProgressDialog.show(CameraMixed.this, "",
						"Training...");
				viewMode = mView.getViewMode();
				metrics = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(metrics);
			}

			@Override
			protected Boolean doInBackground(Integer... params) {
				if (params == null) {
					return false;
				}
				try {
					// index images of catalog
					Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

					Intro.getCatalogInstance().TrainAll(viewMode,
							metrics.heightPixels);
				} catch (Exception e) {
					Log.e("tag", e.getMessage());
					return false;
				}
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				progressDialog.dismiss();
			}
		}.execute();

	}
}
