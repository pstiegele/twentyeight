package de.paulsapp.twentyeight;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

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
	private void initColors(){
		initBackgroundColor();
		initTextColors();
		initImageColors();
		initLineChartColors();
	}

	private void initTextColors(){
		final TextView tv_aussen = (TextView) activity.findViewById(R.id.temperature_now_outside);
		final TextView tv_innen = (TextView) activity.findViewById(R.id.temperature_now_inside);
		final TextView tv_refresh = (TextView) activity.findViewById(R.id.lastrefresh_tv);

		SharedPreferences colorSettings = activity.getSharedPreferences("myprefs", 0);
		tv_aussen.setTextColor(colorSettings.getInt("tv_colorAussen",
				Color.WHITE));
		tv_innen.setTextColor(colorSettings
				.getInt("tv_colorInnen", Color.WHITE));

		tv_aussen.setText(activity.getString(R.string.CHART_DEGREE, getnewesttempentry(1)).replace(",", "."));
		tv_innen.setText(activity.getString(R.string.CHART_DEGREE, getnewesttempentry(2)).replace(",", "."));
		tv_refresh.setTextColor(colorSettings.getInt("refreshColor",
				Color.WHITE));
	}

	private void initImageColors(){
		final ImageView sanduhr = (ImageView) activity.findViewById(R.id.lastrefresh_iv);
		final ImageView house = (ImageView) activity.findViewById(R.id.house_image);

		SharedPreferences colorSettings = activity.getSharedPreferences("myprefs", 0);
		if (colorSettings.getInt("sanduhrColor", Color.WHITE) == Color.WHITE) {
			sanduhr.setImageDrawable(ResourcesCompat.getDrawable(
					activity.getResources(),R.drawable.sanduhr,null));
		} else {
			sanduhr.getDrawable().setColorFilter(Color.DKGRAY,
					PorterDuff.Mode.MULTIPLY);
		}

		house.getDrawable().setColorFilter(
				colorSettings.getInt("houseColor", Color.GRAY),
				PorterDuff.Mode.MULTIPLY);


	}

	private void initLineChartColors(){
		final LineChart chart = (LineChart) activity.findViewById(R.id.chart);
		final XAxis xaxis = chart.getXAxis();
		final YAxis axisLeft = chart.getAxisLeft();

		SharedPreferences colorSettings = activity.getSharedPreferences("myprefs", 0);
		xaxis.setTextColor(colorSettings.getInt("chartColor", Color.WHITE));
		axisLeft.setTextColor(colorSettings.getInt("chartColor", Color.WHITE));
	}

	public void initTemperature(){
		initColors();
		saveTemperatureAsInitalTemperature();
	}

	private void saveTemperatureAsInitalTemperature(){
		SharedPreferences settings = activity.getSharedPreferences("temp", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putFloat("temp_aussen_start",
				(float) (Math.round(getnewesttempentry(1) * 10) / 10.0));
		editor.putFloat("temp_innen_start",
				(float) (Math.round(getnewesttempentry(2) * 10) / 10.0));
		editor.apply();
	}

}
