package com.ramirooliva.cvriousity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.ramirooliva.cvriousity.R;

class CatalogView extends View {

	private int mWidth;
	private int mHeight;

	Paint mPaint = new Paint();

	public CatalogView(Context context) {
		super(context);
	}

	public CatalogView(Context context, AttributeSet attribs) {
		super(context, attribs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
		mHeight = View.MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(mWidth, mHeight);
	}

}
