package de.paulsapp.twentyeight;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {


    /////////////////////
    //// Variablen /////
    /////////////////////
    final Runnable Runnable_Chart = new Runnable() {
        public void run() {
            final LineChart chart = (LineChart) findViewById(R.id.chart);
            chart.invalidate();
        }
    };
    public static Server server;
    private Handler handler = new Handler();
    final Runnable Runnable_Refresh_Time = new Runnable() {
        public void run() {
            setRefreshTime();

        }
    };
    private Timer timer15s;
    private Context context;
    public static Database db;
    private Drawer drawer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_root);
        context = getApplicationContext();
        init(savedInstanceState);
    }

    public void init(Bundle savedInstanceState) {
        ////////////////////////
        ///// Init Database ////
        ////////////////////////
        db = new Database(context);

        /////////////////
        //// Drawer /////
        /////////////////
        drawer = new Drawer(context,this);
        drawer.loadDrawerTitleAndStatus();
        drawer.setDrawerPrefs(savedInstanceState);


        ////////////////////////
        /// Background Color ///
        ////////////////////////
        initBackgroundColor();


        ////////////////////////
        /// load Credentials ///
        /// and init Server  ///
        ////////////////////////
        server=new Server();
        server.loadCredentials(db,this,context);

        ////////////////////////
        ///   init TempChart ///
        ////////////////////////
        setInitalTempCharts();
        setTempChart();


        /////////////////////////
        /// updateDosenStatus ///
        /////////////////////////
        updateDosenStatus();

        /////////////////////////
        ///    init Timer to   //
        /// update RefreshTime //
        /////////////////////////
        initRefreshTimeTimer();


        /////////////////////////////////
        /// set RefreshButtonListener ///
        /////////////////////////////////
        setRefreshButtonListener();

    }


    public void initBackgroundColor() {
        SharedPreferences settings;
        settings = getSharedPreferences("myprefs", 0);
        if (settings.contains("rl_color")) {
            int rl_color = settings.getInt("rl_color", 0);
            refreshbgcolor(rl_color, false);
        }
    }




    public void setRefreshTime() {
        TextView tv = (TextView) findViewById(R.id.lastrefresh_tv);
        SharedPreferences temp = getSharedPreferences("temp", 0);
        long last_refresh = temp.getLong("last_refresh", 0);
        GregorianCalendar gc_now = new GregorianCalendar();
        long timediff = gc_now.getTimeInMillis() - last_refresh;
        int tvtimediff = (int) (timediff / 1000 / 60);
        if (tvtimediff < 60) {
            tv.setText(getString(R.string.REFRESH_TIME_MIN, (tvtimediff + 1)));
        } else if (tvtimediff < 1440) {
            tv.setText(getString(R.string.REFRESH_TIME_HOUR, ((tvtimediff / 60) + 1)));
        } else {
            tv.setText(getString(R.string.REFRESH_TIME_DAY, ((tvtimediff / 60 / 24) + 1)));
        }

        if (timediff / 1000 / 60 > 5) {
            if (server != null) {
                updateData(server);
            }

        }
    }


    public void initServer(String url, String user, String password) {
        server = new Server(url, user, password, context);
    }

    public void initRefreshTimeTimer() {
        timer15s = new Timer();
        timer15s.schedule(new TimerTask() {
            @Override
            public void run() {
                updateRefreshTime();
            }
        }, 0, 15 * 1000);

    }

    public void setRefreshButtonListener() {
        OnClickListener refreshListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                ProgressBar pb = (ProgressBar) findViewById(R.id.load_progressbar);
                pb.setVisibility(android.view.View.VISIBLE);


                drawer.updateDose();
                updateData(server);

            }
        };

        OnLongClickListener menuListener = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {

                openOptionsMenu();
                updateguibarcharts();
                return false;
            }
        };


        ImageView refreshiv = (ImageView) findViewById(R.id.lastrefresh_iv);
        refreshiv.setOnClickListener(refreshListener);
        refreshiv.setOnLongClickListener(menuListener);

        TextView refreshtv = (TextView) findViewById(R.id.lastrefresh_tv);
        refreshtv.setOnClickListener(refreshListener);
        refreshtv.setOnLongClickListener(menuListener);


    }

    public void updateDosenStatus() {
        if (server != null) {
           drawer.updateDose();
        }

    }


    public void updateData(final Server server) {

        server.setSelectListener(new Server.SelectListener() {

            @Override
            public void selectIsReady(boolean result) { // aktuelle Temp geladen


            }
        });

        server.setConnectListener(new Server.ConnectListener() { // server ist
            // erreichbar
            @Override
            public void connectIsReady(boolean result) {

                if (result) {
                    server.refreshData();
                    updateguibarcharts();
                    ProgressBar pb = (ProgressBar) findViewById(R.id.load_progressbar);
                    pb.setVisibility(android.view.View.INVISIBLE);
                    SharedPreferences settings = getSharedPreferences("temp", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    GregorianCalendar gc = new GregorianCalendar();
                    editor.putLong("last_refresh", gc.getTimeInMillis());
                    editor.apply();
                    updateRefreshTime();

                }
                // guiSetOnlineStatus(server);
            }
        });

        server.connect();

    }

    private void updateRefreshTime() {
        handler.post(Runnable_Refresh_Time);
    }

    private void updateChart() {
        handler.post(Runnable_Chart);
    }

    public void updateguibarcharts() { // aktuelle Temperatur

        refreshtemp();
        updateTempCharts();
    }


    @SuppressWarnings("deprecation")
    public void setInitalTempCharts() {
        final TextView tv_aussen = (TextView) findViewById(R.id.temperature_now_outside);
        final TextView tv_innen = (TextView) findViewById(R.id.temperature_now_inside);
        final TextView tv_refresh = (TextView) findViewById(R.id.lastrefresh_tv);
        final LineChart chart = (LineChart) findViewById(R.id.chart);
        final XAxis xaxis = chart.getXAxis();
        final YAxis axisLeft = chart.getAxisLeft();
        final ImageView sanduhr = (ImageView) findViewById(R.id.lastrefresh_iv);
        final ImageView house = (ImageView) findViewById(R.id.house_image);

        SharedPreferences colorSettings = getSharedPreferences("myprefs", 0);

        tv_aussen.setTextColor(colorSettings.getInt("tv_colorAussen",
                Color.WHITE));
        tv_innen.setTextColor(colorSettings
                .getInt("tv_colorInnen", Color.WHITE));

        // tv_aussen.setText(Math.round(getnewesttempentry(1) * 10) / 10.0 + "°");
        // tv_innen.setText(Math.round(getnewesttempentry(2) * 10) / 10.0 + "°");


        tv_aussen.setText(getString(R.string.CHART_DEGREE, getnewesttempentry(1)).replace(",", "."));
        tv_innen.setText(getString(R.string.CHART_DEGREE, getnewesttempentry(2)).replace(",", "."));


        tv_refresh.setTextColor(colorSettings.getInt("refreshColor",
                Color.WHITE));

        xaxis.setTextColor(colorSettings.getInt("chartColor", Color.WHITE));
        axisLeft.setTextColor(colorSettings.getInt("chartColor", Color.WHITE));

        if (colorSettings.getInt("sanduhrColor", Color.WHITE) == Color.WHITE) {
            sanduhr.setImageDrawable(getResources().getDrawable(
                    R.drawable.sanduhr));
        } else {
            sanduhr.getDrawable().setColorFilter(Color.DKGRAY,
                    PorterDuff.Mode.MULTIPLY);
        }

        house.getDrawable().setColorFilter(
                colorSettings.getInt("houseColor", Color.GRAY),
                PorterDuff.Mode.MULTIPLY);

        SharedPreferences settings = getSharedPreferences("temp", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("temp_aussen_start",
                (float) (Math.round(getnewesttempentry(1) * 10) / 10.0));
        editor.putFloat("temp_innen_start",
                (float) (Math.round(getnewesttempentry(2) * 10) / 10.0));
        editor.apply();
    }

    public void updateTempCharts() {
        final TextView tv_aussen = (TextView) findViewById(R.id.temperature_now_outside);
        final TextView tv_innen = (TextView) findViewById(R.id.temperature_now_inside);
        final TextView tv_refresh = (TextView) findViewById(R.id.lastrefresh_tv);
        final LineChart chart = (LineChart) findViewById(R.id.chart);
        final XAxis xaxis = chart.getXAxis();
        final YAxis axisLeft = chart.getAxisLeft();
        final ImageView sanduhr = (ImageView) findViewById(R.id.lastrefresh_iv);
        final ImageView house = (ImageView) findViewById(R.id.house_image);

        SharedPreferences colorSettings = getSharedPreferences("myprefs", 0);

        final ObjectAnimator textColorAnimatorAussen = ObjectAnimator.ofObject(
                tv_aussen, "TextColor", new ArgbEvaluator(), tv_aussen
                        .getTextColors().getDefaultColor(), colorSettings
                        .getInt("tv_colorAussen", Color.WHITE));
        textColorAnimatorAussen.setDuration(2000);

        final ObjectAnimator textColorAnimatorInnen = ObjectAnimator.ofObject(
                tv_innen, "TextColor", new ArgbEvaluator(), tv_innen
                        .getTextColors().getDefaultColor(), colorSettings
                        .getInt("tv_colorInnen", Color.WHITE));
        textColorAnimatorInnen.setDuration(2000);

        final ObjectAnimator textColorAnimatorRefresh = ObjectAnimator
                .ofObject(tv_refresh, "TextColor", new ArgbEvaluator(),
                        tv_refresh.getTextColors().getDefaultColor(),
                        colorSettings.getInt("refreshColor", Color.WHITE));
        textColorAnimatorRefresh.setDuration(2000);

        final ObjectAnimator textColorAnimatorXAxis = ObjectAnimator.ofObject(
                xaxis, "TextColor", new ArgbEvaluator(), xaxis.getTextColor(),
                colorSettings.getInt("chartColor", Color.WHITE));
        textColorAnimatorXAxis.setDuration(2000);

        final ObjectAnimator textColorAnimatorYAxis = ObjectAnimator.ofObject(
                axisLeft, "TextColor", new ArgbEvaluator(),
                axisLeft.getTextColor(),
                colorSettings.getInt("chartColor", Color.WHITE));
        textColorAnimatorYAxis.setDuration(2000);

        final ObjectAnimator trendcurveInnenAnimator = ObjectAnimator.ofObject(
                chart.getData().getDataSetByLabel("Innen", true), "Color",
                new ArgbEvaluator(),
                chart.getData().getDataSetByLabel("Innen", true).getColor(),
                colorSettings.getInt("trendcurveColorInnen", Color.GRAY));
        trendcurveInnenAnimator.setDuration(2000);

        chart.getData().setValueTextColor(
                colorSettings.getInt("chartValueColor", Color.BLACK));

        if (colorSettings.getInt("sanduhrColor", Color.WHITE) == Color.WHITE) {
            sanduhr.getDrawable().clearColorFilter();
        } else {
            sanduhr.getDrawable().setColorFilter(Color.DKGRAY,
                    PorterDuff.Mode.MULTIPLY);
        }

        house.getDrawable().setColorFilter(colorSettings.getInt("houseColor", Color.BLACK),
                PorterDuff.Mode.MULTIPLY);

        // house.getDrawable().setColorFilter(colorSettings.getInt("houseColor",
        // Color.GRAY),PorterDuff.Mode.MULTIPLY);

        SharedPreferences temp = getSharedPreferences("temp", 0);
        float aussen_alt = temp.getFloat("temp_aussen_start", 0);
        float innen_alt = temp.getFloat("temp_innen_start", 0);

        ValueAnimator animator = new ValueAnimator();
        animator.setFloatValues(aussen_alt, (float) getnewesttempentry(1));
        animator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                tv_aussen.setText(getString(R.string.CHART_DEGREE_STRING, String.valueOf(animation.getAnimatedValue())));
            }
        });
        animator.setEvaluator(new TypeEvaluator<Float>() {
            public Float evaluate(float fraction, Float startValue,
                                  Float endValue) {
                return Float.valueOf(String.valueOf(Math
                        .round((startValue - ((startValue - endValue) * fraction)) * 10) / 10.0));
            }
        });
        animator.setDuration(2000);

        ValueAnimator animator2 = new ValueAnimator();
        animator2.setFloatValues(innen_alt, (float) getnewesttempentry(2));
        animator2.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                tv_innen.setText(getString(R.string.CHART_DEGREE_STRING, String.valueOf(animation.getAnimatedValue())));
            }
        });
        animator2.setEvaluator(new TypeEvaluator<Float>() {
            public Float evaluate(float fraction, Float startValue,
                                  Float endValue) {
                return Float.valueOf(String.valueOf(Math
                        .round((startValue - ((startValue - endValue) * fraction)) * 10) / 10.0));
            }
        });
        animator2.setDuration(2000);
        animator2.start();
        textColorAnimatorAussen.start();
        textColorAnimatorInnen.start();
        animator.start();
        textColorAnimatorRefresh.start();
        textColorAnimatorYAxis.start();
        textColorAnimatorXAxis.start();
        trendcurveInnenAnimator.start();

        final Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            int i = 0;

            @Override
            public void run() {
                i++;
                updateChart();
                if (i >= 42) {
                    timer.cancel();
                }
            }
        }, 0, 50);


        SharedPreferences settings = getSharedPreferences("temp", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("temp_aussen_start",
                (float) (Math.round(getnewesttempentry(1) * 10) / 10.0));
        editor.putFloat("temp_innen_start",
                (float) (Math.round(getnewesttempentry(2) * 10) / 10.0));
        editor.apply();

    }

    public void refreshbgcolor(int color, boolean withanimation) {
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.main_rl);
        Window window = this.getWindow();
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


    public void refreshtemp() {
        SharedPreferences temp = getSharedPreferences("temp", 0);
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

        SharedPreferences settings = getSharedPreferences("myprefs", 0);
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

    public void setTempChart() {
        // unteres Chart
        LineChart chart = (LineChart) findViewById(R.id.chart);

        chart.setLogEnabled(false);
        chart.setDescription("");
        chart.setNoDataText("Keine Daten verfügbar");
        chart.setTouchEnabled(true);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setMaxVisibleValueCount(10);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setScaleYEnabled(false);
        chart.setScaleXEnabled(true);
        chart.setHighlightEnabled(false);

        chart.getXAxis().setDrawAxisLine(false);
        chart.getXAxis().setEnabled(true);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setTypeface(
                Typeface.create("sans-serif-light", Typeface.NORMAL));
        chart.getXAxis().setTextSize(13f);

        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setEnabled(true);
        chart.getAxisLeft().setValueFormatter(new MyValueFormatter());
        chart.getAxisLeft().setTypeface(
                Typeface.create("sans-serif-light", Typeface.NORMAL));
        chart.getAxisLeft().setTextSize(13f);

        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        setChartData1(chart);
        chart.animateY(1500);

    }

    public void setChartData1(LineChart chart) {
        // TEMP AUSSEN ANFANG
        ArrayList<String> xValues1 = new ArrayList<>();
        ArrayList<Entry> values1 = new ArrayList<>();
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd' '00:00:00",
                Locale.GERMANY);
        Date d = new Date();
        d.setTime(d.getTime() - 604800000);
        GregorianCalendar datenow = new GregorianCalendar();
        // String query =
        // "SELECT datetime, Max(value) AS value FROM temperature where sensorid=1 GROUP BY SUBSTR(datetime,0,11) ORDER BY datetime";
        String query = "SELECT datetime, Max(value) AS value FROM temperature where sensorid=1 AND datetime> '"
                + parser.format(d)
                + "' GROUP BY SUBSTR(datetime,0,11) ORDER BY datetime";
        Cursor cr1 = db.getRawQuery(query);
        int year;
        int month;
        int day;
        int hour;
        int minute;
        int second;
        String date;
        String day_of_week;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.GERMANY);
        for (int i = 0; i < cr1.getCount(); i++) {
            // Datetime
            date = cr1.getString(cr1.getColumnIndex("datetime"));
            year = Integer.parseInt(date.substring(0, 4));
            month = Integer.parseInt(date.substring(5, 7)) - 1;
            day = Integer.parseInt(date.substring(8, 10));
            hour = Integer.parseInt(date.substring(11, 13));
            minute = Integer.parseInt(date.substring(14, 16));
            second = Integer.parseInt(date.substring(17, 19));
            GregorianCalendar g = new GregorianCalendar(year, month, day, hour,
                    minute, second);


            if (datenow.get(GregorianCalendar.YEAR) == g
                    .get(GregorianCalendar.YEAR)
                    && datenow.get(GregorianCalendar.DAY_OF_YEAR) == g
                    .get(GregorianCalendar.DAY_OF_YEAR)) {
                day_of_week = "Heute        "; // unsichtbare Zeichen dahinter
                // damits ins Layout passt
            } else if (datenow.get(GregorianCalendar.YEAR) == g
                    .get(GregorianCalendar.YEAR) && (datenow.get(GregorianCalendar.DAY_OF_YEAR)) == g
                    .get(GregorianCalendar.DAY_OF_YEAR) + 1) {

                day_of_week = "Gestern";

            } else if (datenow.get(GregorianCalendar.YEAR) >= g
                    .get(GregorianCalendar.YEAR)
                    && datenow.get(GregorianCalendar.DAY_OF_YEAR) - 8 >= g
                    .get(GregorianCalendar.DAY_OF_YEAR)) {

                day_of_week = sdf.format(g.getTime());

            } else {
                day_of_week = g.getDisplayName(GregorianCalendar.DAY_OF_WEEK,
                        GregorianCalendar.SHORT, Locale.GERMANY);
            }

            xValues1.add(day_of_week);
            values1.add(new Entry(cr1.getFloat(cr1.getColumnIndex("value")), i));
            cr1.moveToNext();
        }
        cr1.close();

        LineDataSet set1 = new LineDataSet(values1, "Aussen");
        set1.setDrawCubic(true);
        set1.setCubicIntensity(0.2f);
        set1.setDrawCircles(false);
        set1.setLineWidth(2f);
        set1.setCircleSize(2f);
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        // set1.setColor(Color.rgb(104, 241, 175));
        set1.setColor(getSharedPreferences("myprefs", 0).getInt(
                "trendcurveColorAussen", Color.WHITE));
        set1.setFillColor(ColorTemplate.getHoloBlue());

        // TEMP AUSSEN ENDE
        // TEMP INNEN ANFANG
        //ArrayList<String> xValues2 = new ArrayList<>();
        ArrayList<Entry> values2 = new ArrayList<>();
        String query2 = "SELECT datetime, Max(value) AS value FROM temperature where sensorid=2 AND datetime> '"
                + parser.format(d)
                + "' GROUP BY SUBSTR(datetime,0,11) ORDER BY datetime";
        Cursor cr2 = db.getRawQuery(query2);
        //  date = "";
        for (int i = 0; i < cr2.getCount(); i++) {
            // Datetime
            //       date = cr2.getString(cr2.getColumnIndex("datetime"));
            //      date = date.substring(8, 10) + "." + date.substring(5, 7) + "."
            //               + date.substring(0, 4);
            //  xValues2.add(date);
            values2.add(new Entry(cr2.getFloat(cr2.getColumnIndex("value")), i));
            cr2.moveToNext();
        }
        cr2.close();

        LineDataSet set2 = new LineDataSet(values2, "Innen");
        set2.setDrawCubic(true);
        set2.setCubicIntensity(0.2f);
        set2.setDrawCircles(false);
        set2.setLineWidth(2f);
        set2.setCircleSize(2f);
        set2.setHighLightColor(Color.rgb(244, 117, 117));
        // set2.setColor(Color.rgb(104, 241, 175));
        set2.setColor(getSharedPreferences("myprefs", 0).getInt(
                "trendcurveColorInnen", Color.GRAY));
        set2.setFillColor(ColorTemplate.getHoloBlue());
        // TEMP INNEN ENDE

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        dataSets.add(set2);

        LineData data = new LineData(xValues1, dataSets);
        data.setValueTextColor(getSharedPreferences("myprefs", 0).getInt(
                "chartValueColor", Color.BLACK));
        data.setValueFormatter(new MyValueFormatter());
        data.setValueTextSize(15f);
        data.setValueTypeface(Typeface.create("sans-serif-thin",
                Typeface.NORMAL));
        data.setDrawValues(true);
        chart.setData(data);

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

    public void toast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT)
                .show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (drawer.mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (id == R.id.login_activity) {
            Intent loginActivityIntent = new Intent(this, LoginActivity.class);
            startActivity(loginActivityIntent);
        }
        if (id == R.id.export_db) {
            exportDB();
        }
        if (id == R.id.delete_db) {
            deleteDB();
        }
        if (id == R.id.refresh) {
            ProgressBar pb = (ProgressBar) findViewById(R.id.load_progressbar);
            pb.setVisibility(android.view.View.VISIBLE);

            updateData(server);

        }
        if (id == R.id.coldRefresh) {
            //TODO: Kaltstart einfügen
            AsyncGetAllElementsColdRefresh runner = new AsyncGetAllElementsColdRefresh();
            runner.execute(server);

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onResume() {
        super.onResume(); // Always call the superclass method first

        SharedPreferences temp = getSharedPreferences("temp", 0);
        long last_refresh = temp.getLong("last_refresh", 0);
        GregorianCalendar gc_now = new GregorianCalendar();
        long timediff = gc_now.getTimeInMillis() - last_refresh;
        if (timediff / 1000 / 60 > 2) {
            ProgressBar pb = (ProgressBar) findViewById(R.id.load_progressbar);
            pb.setVisibility(android.view.View.VISIBLE);

            updateData(server);
            drawer.updateDose();
        }
        if (timer15s == null) {
            timer15s = new Timer();
            timer15s.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateRefreshTime();
                }
            }, 0, 15 * 1000);
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        if (timer15s != null) {
            timer15s.cancel();
            timer15s = null;
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawer.mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawer.mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @SuppressWarnings("resource")
    private void exportDB() {
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source;
        FileChannel destination;
        String dbname = context.getResources().getString(R.string.dbname);
        String currentDBPath = "/data/" + "de.paulsapp.twentyeight"
                + "/databases/" + dbname;
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, dbname);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteDB() {
        String dbname = context.getResources().getString(R.string.dbname);
        boolean result = this.deleteDatabase(dbname);
        if (result) {
            Toast.makeText(this, "DB Deleted!", Toast.LENGTH_LONG).show();
        }
    }

    /* The click listner for ListView in the navigation drawer */
//    private class DrawerItemClickListener implements ListView.OnItemClickListener {
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            selectItem(position);
//        }
//    }




    //Allgemeine Infos Anfang
    @SuppressWarnings("deprecation")
    private class AsyncGetAllElementsColdRefresh extends AsyncTask<Server, String, Boolean> {
        @Override
        protected Boolean doInBackground(Server... params) {
            Server server = params[0];
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(server.address);
            JSONObject json = new JSONObject();
            String strresponse;
            boolean statusChanges = false;

            try {
                // JSON data:
                json.put("type", "coldRefresh");
                json.put("user", server.user);
                json.put("password", server.password);

                JSONArray postjson = new JSONArray();
                postjson.put(json);

                // Post the data:
                httppost.setHeader("json", json.toString());
                httppost.getParams().setParameter("jsonpost", postjson);

                // Execute HTTP Post Request

                HttpResponse response = httpclient.execute(httppost);

                // for JSON:
                if (response != null) {
                    InputStream is = response.getEntity().getContent();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();

                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    strresponse = sb.toString();
                    JSONObject responseJSON = new JSONObject(strresponse);
                    if (responseJSON.getString("credentials").equals("true")) {

                        // in lokale DB schreiben

                        int count = responseJSON.getInt("countDose");
                        String name;
                        DoseStatus status;
                        //    String icon;
                        //   int shortTimeLimit;
                        //   Date timeSetShort = null;
                        //  Date lastTimeSet = null;

                        for (int i = 0; i < count; i++) {
                            JSONObject job = responseJSON.getJSONArray("Dose")
                                    .getJSONObject(i);
                            name = job.getString("name");
                            if (job.getInt("status") == 0) {
                                status = DoseStatus.AUS;
                            } else {
                                status = DoseStatus.AN;
                            }

                            String sqlstring = "INSERT INTO doseElements (name,status) VALUES ('"
                                    + name
                                    + "','"
                                    + status.getAsInt()
                                    + "')";
                            Cursor cr = db.getRawQuery(
                                    "SELECT name FROM doseElements WHERE name = '" + name + "'");
                            if (cr.getCount() == 0) {
                                db.execSQLString(sqlstring);
                            } else {
                                db.execSQLString("UPDATE \"doseElements\" SET \"status\"='" + status.getAsInt() + "' WHERE \"name\" = '" + name + "'");
                            }
                            cr.close();
                            if (drawer.mStatus.length >= i) {
                                drawer.mStatus[i] = status.getAsBoolean();
                                statusChanges = true;
                            }


                        }

                        //TODO: Name, permission, description



                    }

                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            return statusChanges;
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                drawer.mDrawerAdapter.notifyDataSetChanged();
            }


        }

        protected void onPreExecute() {
            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog
        }
    }
    //Allgemeine Infos Ende
}
