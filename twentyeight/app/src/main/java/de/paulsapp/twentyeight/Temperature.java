package de.paulsapp.twentyeight;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class Temperature{


	private Context context;
	private Activity activity;

	public Temperature(Context context,Activity activity){
		this.context=context;
		this.activity=activity;
		
	}

	public double getnewesttempentry(int sensorid) {

		SharedPreferences temp = context.getSharedPreferences("temp", 0);
		if (sensorid == 1) {
			return temp.getFloat("temp_aussen", 0);
		}
		if (sensorid == 2) {
			return temp.getFloat("temp_innen", 0);
		}
		return 0;

	}


	public void refreshtemp() {
		SharedPreferences temp = activity.getSharedPreferences("temp", 0);
		double aussen = temp.getFloat("temp_aussen", 0);

		// Formel um Farbe für Hintergrund zu berechnen
		double h = (-0.11) * ((aussen + 20) * (aussen + 20)) + 250;

		// in HSV float wert einfügen
		float[] hsvfl = {(float) h, 0.78f, 0.96f};
		int hsv = Color.HSVToColor(hsvfl);

		// Überschrift Text Farbe ermitteln (schwarz / weiß)
		double tvtc = (299 * Color.red(hsv) + 587 * Color.green(hsv) + 114 * Color
				.blue(hsv)) / 1000;
		int tvColorAussen;
		int tvColorInnen;
		int refreshColor;
		int chartColor;
		int sanduhrColor;
		int houseColor;
		int trendcurveColorInnen;
		int trendcurveColorAussen;
		int chartValueColor;
		if (tvtc >= 128) { // wenn Hell
			tvColorAussen = Color.WHITE;
			tvColorInnen = Color.GRAY;
			chartColor = Color.DKGRAY;
			refreshColor = Color.DKGRAY;
			sanduhrColor = Color.DKGRAY;
			houseColor = Color.GRAY;
			trendcurveColorInnen = Color.GRAY;
			trendcurveColorAussen = Color.WHITE;
			chartValueColor = Color.DKGRAY;
		} else { // wenn Dunkel
			tvColorAussen = Color.WHITE;
			tvColorInnen = Color.BLACK;
			chartColor = Color.WHITE;
			refreshColor = Color.WHITE;
			sanduhrColor = Color.WHITE;
			houseColor = Color.BLACK;
			trendcurveColorInnen = Color.BLACK;
			trendcurveColorAussen = Color.WHITE;
			chartValueColor = Color.WHITE;
		}

		refreshbgcolor(hsv, true);

		// int tvbgc_alt = tv.getTextColors().getDefaultColor();
		// final ObjectAnimator textviewColorAnimator = ObjectAnimator.ofObject(
		// // Animation
		// textview
		// tv, "textColor", new ArgbEvaluator(), tvbgc_alt, itvtc);
		// textviewColorAnimator.setDuration(2000);
		// textviewColorAnimator.start();

		SharedPreferences settings = activity.getSharedPreferences("myprefs", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("tv_colorAussen", tvColorAussen);
		editor.putInt("tv_colorInnen", tvColorInnen);
		editor.putInt("chartColor", chartColor);
		editor.putInt("refreshColor", refreshColor);
		editor.putInt("sanduhrColor", sanduhrColor);
		editor.putInt("houseColor", houseColor);
		editor.putInt("trendcurveColorInnen", trendcurveColorInnen);
		editor.putInt("trendcurveColorAussen", trendcurveColorAussen);
		editor.putInt("chartValueColor", chartValueColor);
		editor.putInt("rl_color", hsv);
		editor.apply();

	}

	public void refreshbgcolor(int color, boolean withanimation) {
		RelativeLayout rl = (RelativeLayout) activity.findViewById(R.id.main_rl);
		Window window = activity.getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

		if (withanimation) {

			// aktuelle Hintergrund Farbe auslesen
			int cr = ((ColorDrawable) rl.getBackground()).getColor();

			final ObjectAnimator statusBarColorAnimator = ObjectAnimator
					.ofObject(window, "statusBarColor", new ArgbEvaluator(),
							cr, color);
			window.setStatusBarColor(color);

			final ObjectAnimator navigationBarColorAnimator = ObjectAnimator
					.ofObject(window, "navigationBarColor",
							new ArgbEvaluator(), cr, color);
			window.setStatusBarColor(color);

			final ObjectAnimator backgroundColorAnimator = ObjectAnimator
					.ofObject(rl, "backgroundColor", new ArgbEvaluator(), cr,
							color);
			backgroundColorAnimator.setDuration(2000);
			statusBarColorAnimator.setDuration(2000);
			navigationBarColorAnimator.setDuration(2000);
			navigationBarColorAnimator.start();
			statusBarColorAnimator.start();
			backgroundColorAnimator.start();

		} else {
			rl.setBackgroundColor(color);
			window.setStatusBarColor(color);
			window.setNavigationBarColor(color);
		}
		//mDrawerRecyclerView.setBackgroundColor(color);

	}
	public void initBackgroundColor() {
		SharedPreferences settings;
		settings = activity.getSharedPreferences("myprefs", 0);
		if (settings.contains("rl_color")) {
			int rl_color = settings.getInt("rl_color", 0);
			refreshbgcolor(rl_color, false);
		}
	}
}
