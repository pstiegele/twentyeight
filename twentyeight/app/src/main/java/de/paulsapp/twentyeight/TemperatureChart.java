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
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by pstiegele on 13.12.2016. Hell yeah!
 */

public class TemperatureChart {


    private Temperature temperature;
    private Database db;
    private Activity activity;
    private Server server;
    final Runnable Runnable_Chart = new Runnable() {
        public void run() {
            final LineChart chart = (LineChart) activity.findViewById(R.id.chart);
            chart.invalidate();
        }
    };
    private boolean isInitalized = false;
    private Handler handler = new Handler();

    public TemperatureChart(Temperature temperature, Database db, Activity activity, Server server) {
        this.temperature = temperature;
        this.db = db;
        this.activity = activity;
        this.server = server;
    }

    public void refreshTempCharts(boolean animate) {
        if (!isInitalized) {
            initalizeTempChart();
        }
        if(server.nodata()){
            return;
        }
        Object[] aussen = importTempChartAussen(); //in aussen[0] ist das LineDataSet gespeichert, in aussen[1] die xValues
        LineDataSet innen = importTemperatureInnen();
        setLineDatasInChart((LineDataSet)aussen[0],innen,(ArrayList<String>) aussen[1]);
        LineChart chart = (LineChart) activity.findViewById(R.id.chart);
        if(animate){
            chart.animateY(1500);
        }


    }

    private Object[] importTempChartAussen() {
        //setChartData1(chart);
        //chart.animateY(1500);

        //xValues: beinhaltet die Beschriftungen der X-Skalierung
        ArrayList<String> xValues = new ArrayList<>();
        //values: beinhaltet die x und y Werte als Entry
        ArrayList<Entry> values = new ArrayList<>();

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd' '00:00:00",
                Locale.GERMANY);
        Date d = new Date();
        d.setTime(d.getTime() - 604800000); //604800000ms=7d
        GregorianCalendar datenow = new GregorianCalendar();

        //ermittle die maximal Werte der letzten 7 Tage
        String query = "SELECT datetime, Max(value) AS value FROM temperature where sensorid=1 AND datetime> '"
                + parser.format(d)
                + "' GROUP BY SUBSTR(datetime,0,11) ORDER BY datetime";
        Cursor cr1 = db.getRawQuery(query);

        Date date = null;
        String day_of_week;
        SimpleDateFormat sdfDayMonth = new SimpleDateFormat("dd.MM", Locale.GERMANY);
        SimpleDateFormat sdfYearMonthDateHourMinuteSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
        for (int i = 0; i < cr1.getCount(); i++) {
            //lese jeden Temperaturwert als Date ein und konvertiers dann zu einem GregorianCalendar

            try {
                date = sdfYearMonthDateHourMinuteSecond.parse(cr1.getString(cr1.getColumnIndex("datetime")));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            GregorianCalendar g = new GregorianCalendar();
            g.setTime(date);

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

                day_of_week = sdfDayMonth.format(g.getTime());

            } else {
                day_of_week = g.getDisplayName(GregorianCalendar.DAY_OF_WEEK,
                        GregorianCalendar.SHORT, Locale.GERMANY);
            }
            xValues.add(day_of_week);
            values.add(new Entry(i,cr1.getFloat(cr1.getColumnIndex("value"))));
            cr1.moveToNext();
        }
        cr1.close();


        //erstellt ein LineDataSet mit den Objekten und entsprechenden Anpassungen
        LineDataSet set1 = new LineDataSet(values, "Aussen");
        // set1.setDrawCubic(true);
        set1.setCubicIntensity(0.2f);
        set1.setDrawCircles(false);
        set1.setLineWidth(2f);
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        // set1.setColor(Color.rgb(104, 241, 175));
        set1.setColor(activity.getSharedPreferences("myprefs", 0).getInt(
                "trendcurveColorAussen", Color.WHITE));
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        Object[] result = new Object[2];
        result[0]=set1;
        result[1]=xValues;
        return result;
    }

    private LineDataSet importTemperatureInnen() {

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd' '00:00:00",
                Locale.GERMANY);
        Date d = new Date();
        d.setTime(d.getTime() - 604800000); //604800000ms=7d
        ArrayList<Entry> values = new ArrayList<>();
        String query2 = "SELECT datetime, Max(value) AS value FROM temperature where sensorid=2 AND datetime> '"
                + parser.format(d)
                + "' GROUP BY SUBSTR(datetime,0,11) ORDER BY datetime";
        Cursor cr = db.getRawQuery(query2);
        for (int i = 0; i < cr.getCount(); i++) {
            values.add(new Entry(i,cr.getFloat(cr.getColumnIndex("value"))));
            cr.moveToNext();
        }
        cr.close();

        LineDataSet set2 = new LineDataSet(values, "Innen");
        //  set2.setDrawCubic(true);
        set2.setCubicIntensity(0.2f);
        set2.setDrawCircles(false);
        set2.setLineWidth(2f);
        set2.setCircleSize(2f);
        set2.setHighLightColor(Color.rgb(244, 117, 117));
        // set2.setColor(Color.rgb(104, 241, 175));
        set2.setColor(activity.getSharedPreferences("myprefs", 0).getInt(
                "trendcurveColorInnen", Color.GRAY));
        set2.setFillColor(ColorTemplate.getHoloBlue());
        set2.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        return set2;
    }


    private void initalizeTempChart() {

        LineChart chart = (LineChart) activity.findViewById(R.id.chart);

        chart.setLogEnabled(false);
        Description description = new Description();
        description.setText("");
        chart.setDescription(description);
        chart.setNoDataText("Keine Daten verfügbar");
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setMaxVisibleValueCount(14);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setScaleYEnabled(false);
        chart.setScaleXEnabled(false);
        chart.setExtraTopOffset(5f);
        //chart.setHighlightEnabled(false);

        chart.getXAxis().setDrawAxisLine(false);
        chart.getXAxis().setEnabled(true);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setTypeface(
                Typeface.create("sans-serif-light", Typeface.NORMAL));
        chart.getXAxis().setTextSize(13f);

        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setEnabled(true);
        IAxisValueFormatter iAxisValueFormatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                DecimalFormat mFormat = new DecimalFormat("###,###,##");
                return mFormat.format(value) + "°";
            }
        };
        chart.getAxisLeft().setValueFormatter(iAxisValueFormatter);
        chart.getAxisLeft().setTypeface(
                Typeface.create("sans-serif-light", Typeface.NORMAL));
        chart.getAxisLeft().setTextSize(13f);

        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setEnabled(false);
        isInitalized=true;
    }


    private void setLineDatasInChart(LineDataSet set1, LineDataSet set2, final ArrayList<String> xValues) {
        LineChart chart = (LineChart) activity.findViewById(R.id.chart);
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        dataSets.add(set2);
        IValueFormatter iValueFormatter = new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                DecimalFormat mFormat = new DecimalFormat("###,###,##");
                return mFormat.format(value) + "°";
            }
        };

        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return xValues.get((int)value);
            }
        };

        chart.getXAxis().setValueFormatter(formatter);

        LineData data = new LineData(dataSets);
        data.setValueTextColor(activity.getSharedPreferences("myprefs", 0).getInt(
                "chartValueColor", Color.BLACK));
        data.setValueFormatter(iValueFormatter);
        data.setValueTextSize(15f);
        data.setValueTypeface(Typeface.create("sans-serif-thin",
                Typeface.NORMAL));
        data.setDrawValues(true);
        chart.setData(data);

    }

    public void updateTempCharts() {
        if(server.nodata()){
            return;
        }
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
