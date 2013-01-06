package com.ramirooliva.cvriousity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import com.ramirooliva.cvriousity.R;

public class CropBitmap {

	public static Bitmap getCroppedImage(Bitmap source, double tolerance,
			int deviceWidth, int deviceHeight) {

		// scale down to device's screen dimensions
		int width = source.getWidth();
		int height = source.getHeight();
		int scaledHeight = deviceHeight*1;
		int scaledWidth = (int)(width*((float)scaledHeight/(float)height));
		Rect srcRect = new Rect(0, 0, width, height);
		Rect dstRect = new Rect(0, 0, scaledWidth, scaledHeight);
		Bitmap scaledDownBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true);
//		Bitmap scaledDownBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight,
//				Bitmap.Config.ARGB_8888);
//		Canvas canvas = new Canvas(scaledDownBitmap);
//		canvas.drawBitmap(source, srcRect, dstRect, null);
		
		// Get our top-left pixel color as our "baseline" for cropping
		int baseColor = Color.WHITE; // scaledDownBitmap.getPixel(0, 0);

		// now crop...
		int topY = Integer.MAX_VALUE, topX = Integer.MAX_VALUE;
		int bottomY = -1, bottomX = -1;
		for (int y = 5; y < scaledHeight-5; y++) {
			for (int x = 5; x < scaledWidth-5; x++) {
				if (colorWithinTolerance(baseColor, scaledDownBitmap.getPixel(x, y),
						tolerance)) {
					if (x < topX)
						topX = x;
					if (y < topY)
						topY = y;
					if (x > bottomX)
						bottomX = x;
					if (y > bottomY)
						bottomY = y;
				}
			}
		}

		// extract and return cropped region
		topX= Math.max(topX-10, 0);
		topY= Math.max(topY-10,0);
		bottomX=Math.min(bottomX+10, width);
		bottomY=Math.min(bottomY+10, height);
		
		int srcHeight = bottomY - topY + 1;
		int srcWidth = bottomX - topX + 1;
		srcRect = new Rect(topX, topY, bottomX, bottomY);
		
		int dstHeight, dstWidth;
//		if ( srcHeight>srcWidth) {
//			dstHeight = deviceHeight;
//			dstWidth = (int)(srcWidth * ((float)deviceHeight/(float)srcHeight));
//		} else {
//			dstWidth = deviceWidth;
//			dstHeight = (int)(srcHeight * ((float)deviceWidth/(float)srcWidth));
//		}
		if (srcWidth * (float)deviceHeight/(float)srcHeight > deviceWidth) {
			dstWidth = deviceWidth;
			dstHeight = (int)(srcHeight * ((float)deviceWidth/(float)srcWidth));
		} else {
			dstHeight = deviceHeight;
			dstWidth = (int)(srcWidth * ((float)deviceHeight/(float)srcHeight));
		}
		
		int dx = deviceWidth/2-dstWidth/2;
		int dy = deviceHeight/2-dstHeight/2;
		dstRect = new Rect(dx, dy, dx+dstWidth-1, dy+dstHeight-1);
		Bitmap destination = Bitmap.createBitmap(deviceWidth, deviceHeight,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(destination);
		canvas.drawColor(Color.BLACK);
		canvas.drawBitmap(scaledDownBitmap, srcRect, dstRect, null);
//		Paint paint = new Paint();
//		paint.setColor(Color.YELLOW);
//		paint.setTextScaleX(3);
//		canvas.drawText("Bitmap: " + width + " , " + height, 10, 10, paint);
//		canvas.drawText("Crop: " + topX + "," + topY + "-" + bottomX +"," + bottomY, 10, 40, paint);
//		canvas.drawText("Dest: " + dstRect.left + "," + dstRect.top + "-" + dstRect.right +"," + dstRect.bottom, 10, 80, paint);
//		canvas.drawText("Device: " + deviceWidth + "," + deviceHeight, 10, 120, paint);
		scaledDownBitmap.recycle();
		System.gc();
		return destination;
	}

//	public static Bitmap getCroppedImage(Bitmap source, double tolerance,
//			int deviceWidth, int deviceHeight) {
//
//		// scale down to device's screen dimensions
//		int width = source.getWidth();
//		int height = source.getHeight();
//		int tarWidth = (int)(width * ( (float)deviceHeight/(float)height));
//		Rect srcRect = new Rect(0, 0, width, height);
//		Rect dstRect = new Rect(0, 0, tarWidth-1, deviceHeight-1);
//		Bitmap scaledDownBitmap = Bitmap.createBitmap(tarWidth, deviceHeight,
//				Bitmap.Config.ARGB_8888);
//		Canvas canvas = new Canvas(scaledDownBitmap);
//		canvas.drawBitmap(source, srcRect, dstRect, null);
//
//		// Get our top-left pixel color as our "baseline" for cropping
//		int baseColor = Color.WHITE; // scaledDownBitmap.getPixel(0, 0);
//
//		// now crop...
//		int topY = Integer.MAX_VALUE, topX = Integer.MAX_VALUE;
//		int bottomY = -1, bottomX = -1;
//		for (int y = 5; y < deviceHeight-5; y++) {
//			for (int x = 5; x < tarWidth-5; x++) {
//				if (colorWithinTolerance(baseColor, scaledDownBitmap.getPixel(x, y),
//						tolerance)) {
//					if (x < topX)
//						topX = x;
//					if (y < topY)
//						topY = y;
//					if (x > bottomX)
//						bottomX = x;
//					if (y > bottomY)
//						bottomY = y;
//				}
////				double dist = colorDistance(baseColor, scaledDownBitmap.getPixel(x, y));
////				if (dist>tolerance) {
////					int color = Color.MAGENTA;
////					scaledDownBitmap.setPixel(x, y, color);
////				}
//			}
//		}
//
//		// extract and return cropped region
//		topX= Math.max(topX-10, 0);
//		topY= Math.max(topY-10,0);
//		bottomX=Math.min(bottomX+10, tarWidth);
//		bottomY=Math.min(bottomY+10, deviceHeight);
//		
//		int srcHeight = bottomY - topY + 1;
//		int srcWidth = bottomX - topX + 1;
//		srcRect = new Rect(topX, topY, bottomX, bottomY);
//		dstRect = new Rect(0, 0, srcWidth-1, srcHeight);
//		Bitmap destination = Bitmap.createBitmap(srcWidth, srcHeight,
//				Bitmap.Config.ARGB_8888);
//		canvas = new Canvas(destination);
//		canvas.drawBitmap(scaledDownBitmap, srcRect, dstRect, null);
//		Paint paint = new Paint();
//		paint.setColor(Color.BLACK);
//		paint.setTextScaleX(3);
//		canvas.drawText("Bitmap: " + width + " , " + height, 10, 10, paint);
//		canvas.drawText("ScaledDown: " + tarWidth + " , " + deviceHeight, 10, 40, paint);
//		canvas.drawText("Crop: " + topX + "," + topY + "-" + bottomX +"," + bottomY, 10, 80, paint);
//		canvas.drawText("Device: " + deviceWidth + "," + deviceHeight, 10, 120, paint);
//		//scaledDownBitmap.recycle();
//		//System.gc();
//		return scaledDownBitmap;
//	}

	private static boolean colorWithinTolerance(int a, int b, double tolerance) {
		int aAlpha = (int) ((a & 0xFF000000) >>> 24); // Alpha level
		int aRed = (int) ((a & 0x00FF0000) >>> 16); // Red level
		int aGreen = (int) ((a & 0x0000FF00) >>> 8); // Green level
		int aBlue = (int) (a & 0x000000FF); // Blue level

		int bAlpha = (int) ((b & 0xFF000000) >>> 24); // Alpha level
		int bRed = (int) ((b & 0x00FF0000) >>> 16); // Red level
		int bGreen = (int) ((b & 0x0000FF00) >>> 8); // Green level
		int bBlue = (int) (b & 0x000000FF); // Blue level

		double distance = Math.sqrt((aAlpha - bAlpha) * (aAlpha - bAlpha)
				+ (aRed - bRed) * (aRed - bRed) + (aGreen - bGreen)
				* (aGreen - bGreen) + (aBlue - bBlue) * (aBlue - bBlue));

		// 510.0 is the maximum distance between two colors
		// (0,0,0,0 -> 255,255,255,255)
		double percentAway = distance / 510.0d;

		return (percentAway > tolerance);
	}
}
