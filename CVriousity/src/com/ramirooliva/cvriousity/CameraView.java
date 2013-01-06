package com.ramirooliva.cvriousity;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.os.AsyncTask;
import android.util.Log;

class CameraView extends CameraViewBase {

	public static final int VIEW_MODE_FBRIEF = 0;
	public static final int VIEW_MODE_ORB = 1;
	public static final int VIEW_MODE_FFREAK = 2;
	public static final int VIEW_MODE_MFREAK = 3;
	public static final int VIEW_MODE_GFREAK = 4;

	public static final int ANALYSIS_WANTED = -1;
	public static final int ANALYSIS_INACTIVE = 0;
	public static final int ANALYSIS_JUST_STARTED = 1;
	public static final int ANALYSIS_ON_GOING = 2;

	private Mat mYuv;
	private Mat mRgba;
	private Mat mGraySubmat;
	private Mat mIntermediateMat;
	private Mat mRecGraySubmat;

	private int mViewMode = VIEW_MODE_FBRIEF;
	private Bitmap mBitmap;

	int[] matchPolygon = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private int[] targetPolygon = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private int[] polygon = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private int targetIndex = -1;
	private int polygonAlpha;

	private static final int MAXSAMPLES = 10;
	int tickindex = 0;
	int ticksum = 0;
	long[] ticklist = new long[MAXSAMPLES];
	long ticklast = 0;
	int gcCount = 0;
	boolean beat;

	private int[] scores = new int[5]; // one for each method
	int recognizeFrozenImageState;
	int showScores, highScore;
	String[] progressRetro = { ".", "..", "...", "...." };
	int method;

	public CameraView(Context context) {
		super(context);
	}

	@Override
	protected void onPreviewStarted(int previewWidth, int previewHeight) {
		// initialize Mats before usage
		mYuv = new Mat(getFrameHeight() + getFrameHeight() / 2,
				getFrameWidth(), CvType.CV_8UC1);
		mGraySubmat = mYuv.submat(0, getFrameHeight(), 0, getFrameWidth());

		mRgba = new Mat();
		mIntermediateMat = new Mat();

		mBitmap = Bitmap.createBitmap(previewWidth, previewHeight,
				Bitmap.Config.ARGB_8888);

		centerPolygon();
	}

	@Override
	protected void onPreviewStopped() {

		if (mBitmap != null) {
			mBitmap.recycle();
			mBitmap = null;
		}

		// Explicitly deallocate Mats
		if (mYuv != null)
			mYuv.release();
		if (mRgba != null)
			mRgba.release();
		if (mGraySubmat != null)
			mGraySubmat.release();
		if (mIntermediateMat != null)
			mIntermediateMat.release();

		mYuv = null;
		mRgba = null;
		mGraySubmat = null;
		mIntermediateMat = null;
	}

	private static boolean recogBusy = false;

	private void doAsyncRecognition() {

		if (recogBusy) {
			return;
		}
		recogBusy = true;

		new AsyncTask<Integer, Integer, Boolean>() {

			@Override
			protected void onPreExecute() {
			}

			@Override
			protected Boolean doInBackground(Integer... params) {
				if (params == null) {
					return false;
				}
				try {
					Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
					matchPolygon[8] = -1;
					FindFeatures(mRecGraySubmat.getNativeObjAddr(), mViewMode,
							matchPolygon);
					if (matchPolygon[8] >= 0) {
						targetPolygon = matchPolygon.clone();
						targetIndex = matchPolygon[8];
						polygonAlpha = 100;
					}
					beat = true;
				} catch (Exception e) {
					Log.e("tag", e.getMessage());
					return false;
				}
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				// Log.i("tag", "Exiting..." + result);
				recogBusy = false;
			}
		}.execute();

	}

	private void doAsyncRecognitionFrozen() {

		if (recogBusy) {
			return;
		}
		recogBusy = true;

		new AsyncTask<Integer, Integer, Boolean>() {

			@Override
			protected void onPreExecute() {
			}

			@Override
			protected Boolean doInBackground(Integer... params) {
				if (params == null) {
					return false;
				}
				try {
					Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
					for (method = 0; method < 5; method++) {
						matchPolygon[8] = -1;
						FindFeatures(mRecGraySubmat.getNativeObjAddr(), method,
								matchPolygon);
						if (matchPolygon[8] >= 0) {
							targetPolygon = matchPolygon.clone();
							targetIndex = matchPolygon[8];
							polygonAlpha = 100;
							scores[method]++;
							if (scores[method] > highScore) {
								highScore = scores[method];
							}
						}
					}
					beat = true;
				} catch (Exception e) {
					Log.e("tag", e.getMessage());
					return false;
				}
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				// Log.i("tag", "Exiting..." + result);
				recognizeFrozenImageState = ANALYSIS_INACTIVE;
				showScores = 200;
				recogBusy = false;
			}
		}.execute();

	}

	private void centerPolygon() {
		polygon[0] = polygon[2] = polygon[4] = polygon[6] = mRgba.cols() / 2;
		polygon[1] = polygon[3] = polygon[5] = polygon[7] = mRgba.rows() / 2;
		polygon[8] = -1;
	}

	double CalcAverageTick(long newtick) {
		ticksum -= ticklist[tickindex]; /* subtract value falling off */
		ticksum += newtick; /* add new value */
		ticklist[tickindex] = newtick; /*
										 * save new value so it can be
										 * subtracted later
										 */
		if (++tickindex == MAXSAMPLES) /* inc buffer index */
			tickindex = 0;

		/* return average */
		return ((double) ticksum / MAXSAMPLES);
	}

	private String getModeName(int viewMode) {
		String mode;
		switch (viewMode) {
		case VIEW_MODE_ORB:
			mode = "ORB";
			break;
		case VIEW_MODE_FFREAK:
			mode = "FAST+FREAK";
			break;
		case VIEW_MODE_MFREAK:
			mode = "MSER+FREAK";
			break;
		case VIEW_MODE_GFREAK:
			mode = "GFTT+FREAK";
			break;
		default:
			mode = "FAST+BRIEF";
			break;
		}
		return mode;
	}

	@Override
	protected Bitmap processFrame(byte[] data) {

		Bitmap bmp = mBitmap;
		if (++gcCount > 100) {
			gcCount = 0;
			System.gc();
		}

		if (recognizeFrozenImageState > ANALYSIS_INACTIVE) {
			if (recognizeFrozenImageState == ANALYSIS_JUST_STARTED) {
				recognizeFrozenImageState = ANALYSIS_ON_GOING;
				mYuv.put(0, 0, data);
				Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4);
				showScores = 20000;
				gcCount = 0;
			}
			doAsyncRecognitionFrozen();
		} else {
			mYuv.put(0, 0, data);
			Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4);
			mRecGraySubmat = mGraySubmat.clone();
			doAsyncRecognition();
		}
		try {
			Utils.matToBitmap(mRgba, bmp);
		} catch (Exception e) {
			Log.e("org.cvriousity", "Utils.matToBitmap() throws an exception: "
					+ e.getMessage());
			bmp.recycle();
			return null;
		}

		// HUD information

		// draw current View mode on top/right corner
		String mode = getModeName(mViewMode);
		Canvas canvas = new Canvas(bmp);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.WHITE);
		paint.setTextSize(25);
		Rect textSize = new Rect();
		paint.getTextBounds(mode, 0, mode.length(), textSize);
		int x = (int) (mRgba.cols() - textSize.width() - 10);
		canvas.drawText(mode, x, 25, paint);

		// draw FPS or status indicator on top/left corner
		long time = System.currentTimeMillis();
		String fps;
		if (recognizeFrozenImageState > ANALYSIS_INACTIVE) {
			fps = "Analyzing image" + progressRetro[gcCount % 4];
			int y = mRgba.rows() * gcCount / 100;
			paint.setColor(Color.GREEN);
			paint.setAlpha(128);
			paint.setStrokeWidth(3);
			canvas.drawLine(0, y, mRgba.cols(), y, paint);
			paint.setColor(Color.WHITE);
			paint.setStrokeWidth(1);
			paint.setAlpha(255);
		} else {
			fps = " FPS: "
					+ String.format("%.2f", 1 / (CalcAverageTick(time
							- ticklast) / 1000.0));
			if (beat) {
				fps += " *";
				beat = false;
			}
		}
		ticklast = time;
		canvas.drawText(fps, 10, 25, paint);

		// show current scores
		if (--showScores > 0) {
			paint.setTextSize(25);
			paint.setStyle(Style.FILL);
			paint.setColor(Color.DKGRAY);
			paint.setAlpha(128);
			x = mRgba.cols() / 2;
			RectF rectF = new RectF(0, mRgba.rows() - 160,
					240 + mRgba.cols() / 2, mRgba.rows() + 20);
			canvas.drawRoundRect(rectF, 10, 10, paint);
			paint.setAlpha(255);
			paint.setColor(Color.WHITE);

			for (int k = 0; k < 5; k++) {
				String name = getModeName(k);
				canvas.drawText(name, 20, mRgba.rows() - k * 30 - 10, paint);
				paint.getTextBounds(name, 0, name.length(), textSize);
				paint.setStyle(Style.FILL);
				x = mRgba.cols() / 2;
				Rect rect = new Rect(200, mRgba.rows() - k * 30 - 10, 200 + x,
						mRgba.rows() - k * 30 - 10 - textSize.height());
				paint.setColor(Color.BLUE);
				canvas.drawRect(rect, paint);
				paint.setColor(Color.WHITE);
				if (method > k) {
					if (highScore > 0) {
						x = (int) (((float) scores[k] / (float) highScore)
								* mRgba.cols() / 2);
						rect = new Rect(200, mRgba.rows() - k * 30 - 10,
								200 + x, mRgba.rows() - k * 30 - 10
										- textSize.height());
						canvas.drawRect(rect, paint);
						canvas.drawText(String.valueOf(scores[k]),
								200 + x + 10, mRgba.rows() - k * 30 - 10, paint);
					}
				} else {
					canvas.drawText("Working...", 200 + 10, mRgba.rows() - k
							* 30 - 10, paint);
				}
			}
		}

		// show recognized area
		if (recognizeFrozenImageState == ANALYSIS_INACTIVE) {
			// Overlay polygon
			polygonAlpha -= (5 - mViewMode);
			if (polygonAlpha > 0) {
				polygon[0] += (targetPolygon[0] - polygon[0]) / 2;
				polygon[1] += (targetPolygon[1] - polygon[1]) / 2;
				polygon[2] += (targetPolygon[2] - polygon[2]) / 2;
				polygon[3] += (targetPolygon[3] - polygon[3]) / 2;
				polygon[4] += (targetPolygon[4] - polygon[4]) / 2;
				polygon[5] += (targetPolygon[5] - polygon[5]) / 2;
				polygon[6] += (targetPolygon[6] - polygon[6]) / 2;
				polygon[7] += (targetPolygon[7] - polygon[7]) / 2;
				paint.setColor(Color.GREEN);
				paint.setStyle(Style.FILL);
				paint.setAlpha(polygonAlpha);
				Path path = new Path();
				path.reset();
				path.moveTo(polygon[0], polygon[1]); // used for first point
				path.lineTo(polygon[2], polygon[3]); // used for first point
				path.lineTo(polygon[4], polygon[5]); // used for first point
				path.lineTo(polygon[6], polygon[7]); // used for first point
				path.lineTo(polygon[0], polygon[1]); // used for first point
				canvas.drawPath(path, paint);
				paint.setColor(Color.WHITE);
				paint.setStrokeWidth(4);
				paint.setStyle(Style.STROKE);
				paint.setAlpha(polygonAlpha);
				canvas.drawPath(path, paint);
			} else {
				centerPolygon();
			}

			// show art name
			if (targetIndex>=0 && polygonAlpha + 200 > 0) {
				CatalogManager catalogManager = Intro.getCatalogInstance();
				String title = catalogManager.getImageTitle(targetIndex);
				paint.setTextSize(20);
				paint.getTextBounds(title, 0, title.length(), textSize);
				RectF rectf = new RectF(mRgba.cols() / 2 - textSize.width() / 2
						- 20, 100 - textSize.height() * 2 - 20, mRgba.cols()
						/ 2 + textSize.width() / 2 + 20, 100 - 20);
				int y = 100 - 20 - textSize.height();
				paint.setAlpha(255);
				paint.setColor(Color.DKGRAY);
				paint.setStyle(Style.FILL);
				canvas.drawRoundRect(rectf, 10, 10, paint);
				paint.setColor(Color.LTGRAY);
				paint.setStrokeWidth(2);
				paint.setStyle(Style.STROKE);
				canvas.drawRoundRect(rectf, 10, 10, paint);
				paint.getTextBounds(title, 0, title.length(), textSize);
				x = mRgba.cols() / 2 - textSize.width() / 2;
				y += textSize.height() / 2;
				paint.setStyle(Style.FILL);
				paint.setColor(Color.WHITE);
				canvas.drawText(title, x, y, paint);
			}
		}

		return bmp;

	}

	public void setViewMode(int viewMode) {
		mViewMode = viewMode;
	}

	public int getViewMode() {
		return mViewMode;
	}

	public void doCameraFocus() {
		AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback() {

			public void onAutoFocus(boolean success, Camera camera) {
				// TODO Auto-generated method stub
				if (success) {
					// start recognizing frozen image
					if (recognizeFrozenImageState == ANALYSIS_WANTED) {
						recognizeFrozenImageState = ANALYSIS_JUST_STARTED;
					}
				}
			}
		};
		mCamera.autoFocus(myAutoFocusCallback);
	}

	public void setWantCompleteAnalysis() {
		recognizeFrozenImageState = ANALYSIS_WANTED;
	}

	public native void FindFeatures(long matAddrGr, int viewMode, int[] polygon);

}
