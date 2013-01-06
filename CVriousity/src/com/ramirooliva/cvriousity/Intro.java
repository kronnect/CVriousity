package com.ramirooliva.cvriousity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import com.ramirooliva.cvriousity.R;

public class Intro extends Activity {
	private static final String TAG = "Cvriousity::Intro";
	private static CatalogManager mCatalogManager = new CatalogManager();

	String[] titles = { "\"Allegory\", AACHEN, Hans von",
			"\"Bacchus, Ceres and Cupid\", AACHEN, Hans von",
			"\"Joking Couple\", AACHEN, Hans von",
			"\"The Archangel Michael\", ABADIA, Juan de la",
			"\"Albarello\", ABAQUESNE, Masséot",
			"\"Ceramic Floor\", ABAQUESNE, Masséot",
			"\"Ceramic Floor\", ABAQUESNE, Masséot",
			"\"The Flood\", ABAQUESNE, Masséot",
			"\"Chimney breast\", ABBATE, Niccolò dell'",
			"\"Chimney breast\", ABBATE, Niccolò dell'" };

	public Intro() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	public static CatalogManager getCatalogInstance() {
		return mCatalogManager;
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.intro);

		final Button go_button = (Button) findViewById(R.id.btn_continue);
		go_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// First time run? Preload 10 sample images...
				if (mCatalogManager.size() == 0) {
					for (int i = 1; i <= 10; i++) {
						String name = String.format("image%07d", i);
						Log.i(TAG, name);
						int id = getResources().getIdentifier(name, "drawable",
								getPackageName());
						Log.i(TAG, String.valueOf(id));
						mCatalogManager.addImageFromResource(id, titles[i - 1]);
					}
				}
				Intent intent = new Intent(Intro.this, Catalog.class);
				Intro.this.startActivity(intent);
			}
		});

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "onCreateOptionsMenu");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "Menu Item selected " + item);
		return true;
	}

}
