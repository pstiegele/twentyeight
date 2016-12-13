package de.paulsapp.twentyeight;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by pstiegele on 13.12.2016. Hell yeah!
 */

public class TemperatureChart{


    private Temperature temperature;
    private Database db;
    private Activity activity;
    private Handler handler = new Handler();
    final Runnable Runnable_Chart = new Runnable() {
        public void run() {
            final LineChart chart = (LineChart) activity.findViewById(R.id.chart);
            chart.invalidate();
        }
    };

    public TemperatureChart(Temperature temperature, Database db, Activity activity){
        this.temperature=temperature;
        this.db=db;
        this.activity=activity;
    }

    public void setInitalTempCharts() {
        final TextView tv_aussen = (TextView) activity.findViewById(R.id.temperature_now_outside);
        final TextView tv_innen = (TextView) activity.findViewById(R.id.temperature_now_inside);
        final TextView tv_refresh = (TextView) activity.findViewById(R.id.lastrefresh_tv);
        final LineChart chart = (LineChart) activity.findViewById(R.id.chart);
        final XAxis xaxis = chart.getXAxis();
        final YAxis axisLeft = chart.getAxisLeft();
        final ImageView sanduhr = (ImageView) activity.findViewById(R.id.lastrefresh_iv);
        final ImageView house = (ImageView) activity.findViewById(R.id.house_image);

        SharedPreferences colorSettings = activity.getSharedPreferences("myprefs", 0);

        tv_aussen.setTextColor(colorSettings.getInt("tv_colorAussen",
                Color.WHITE));
        tv_innen.setTextColor(colorSettings
                .getInt("tv_colorInnen", Color.WHITE));

        // tv_aussen.setText(Math.round(getnewesttempentry(1) * 10) / 10.0 + "°");
        // tv_innen.setText(Math.round(getnewesttempentry(2) * 10) / 10.0 + "°");


        tv_aussen.setText(activity.getString(R.string.CHART_DEGREE, temperature.getnewesttempentry(1)).replace(",", "."));
        tv_innen.setText(activity.getString(R.string.CHART_DEGREE, temperature.getnewesttempentry(2)).replace(",", "."));


        tv_refresh.setTextColor(colorSettings.getInt("refreshColor",
                Color.WHITE));

        xaxis.setTextColor(colorSettings.getInt("chartColor", Color.WHITE));
        axisLeft.setTextColor(colorSettings.getInt("chartColor", Color.WHITE));

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

        SharedPreferences settings = activity.getSharedPreferences("temp", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("temp_aussen_start",
                (float) (Math.round(temperature.getnewesttempentry(1) * 10) / 10.0));
        editor.putFloat("temp_innen_start",
                (float) (Math.round(temperature.getnewesttempentry(2) * 10) / 10.0));
        editor.apply();
    }

    public void setTempChart() {
        // unteres Chart
        LineChart chart = (LineChart) activity.findViewById(R.id.chart);

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

    private void setChartData1(LineChart chart) {
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
        set1.setColor(activity.getSharedPreferences("myprefs", 0).getInt(
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
        set2.setColor(activity.getSharedPreferences("myprefs", 0).getInt(
                "trendcurveColorInnen", Color.GRAY));
        set2.setFillColor(ColorTemplate.getHoloBlue());
        // TEMP INNEN ENDE

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        dataSets.add(set2);

        LineData data = new LineData(xValues1, dataSets);
        data.setValueTextColor(activity.getSharedPreferences("myprefs", 0).getInt(
                "chartValueColor", Color.BLACK));
        data.setValueFormatter(new MyValueFormatter());
        data.setValueTextSize(15f);
        data.setValueTypeface(Typeface.create("sans-serif-thin",
                Typeface.NORMAL));
        data.setDrawValues(true);
        chart.setData(data);

    }

    public void updateTempCharts() {
        final TextView tv_aussen = (TextView) activity.findViewById(R.id.temperature_now_outside);
        final TextView tv_innen = (TextView) activity.findViewById(R.id.temperature_now_inside);
        final TextView tv_refresh = (TextView) activity.findViewById(R.id.lastrefresh_tv);
        final LineChart chart = (LineChart) activity.findViewById(R.id.chart);
        final XAxis xaxis = chart.getXAxis();
        final YAxis axisLeft = chart.getAxisLeft();
        final ImageView sanduhr = (ImageView) activity.findViewById(R.id.lastrefresh_iv);
        final ImageView house = (ImageView) activity.findViewById(R.id.house_image);

        SharedPreferences colorSettings = activity.getSharedPreferences("myprefs", 0);

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

        SharedPreferences temp = activity.getSharedPreferences("temp", 0);
        float aussen_alt = temp.getFloat("temp_aussen_start", 0);
        float innen_alt = temp.getFloat("temp_innen_start", 0);

        ValueAnimator animator = new ValueAnimator();
        animator.setFloatValues(aussen_alt, (float) temperature.getnewesttempentry(1));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                tv_aussen.setText(activity.getString(R.string.CHART_DEGREE_STRING, String.valueOf(animation.getAnimatedValue())));
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
        animator2.setFloatValues(innen_alt, (float) temperature.getnewesttempentry(2));
        animator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                tv_innen.setText(activity.getString(R.string.CHART_DEGREE_STRING, String.valueOf(animation.getAnimatedValue())));
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


        SharedPreferences settings = activity.getSharedPreferences("temp", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("temp_aussen_start",
                (float) (Math.round(temperature.getnewesttempentry(1) * 10) / 10.0));
        editor.putFloat("temp_innen_start",
                (float) (Math.round(temperature.getnewesttempentry(2) * 10) / 10.0));
        editor.apply();

    }

    private void updateChart() {
        handler.post(Runnable_Chart);
    }

}
