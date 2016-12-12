package de.paulsapp.twentyeight;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import layout.TwentyeightWidget;

public class WidgetService extends Service {

    static boolean databaseIsOpen = false;
    static private SQLiteOpenHelper database;
    static private SQLiteDatabase connection;

    public WidgetService() {
    }

    public static String[] loadCredentials(Context context) {
        String[] cred = new String[3];
        openDB(context);
        Cursor credentialscursor = connection.rawQuery(
                "SELECT url,user,password FROM savedUsers WHERE selected = 1",
                null);

        if (credentialscursor.getCount() <= 0) {


        } else {


            try {
                credentialscursor.moveToFirst();
                cred[0] = credentialscursor.getString(credentialscursor
                        .getColumnIndex("url"));
                cred[1] = credentialscursor.getString(credentialscursor
                        .getColumnIndex("user"));
                cred[2] = credentialscursor.getString(credentialscursor
                        .getColumnIndex("password"));
            } catch (Exception e) {

            }
            credentialscursor.close();


        }
        closeDB();
        return cred;
    }

    public static SQLiteDatabase openDB(Context context) {
        if (!databaseIsOpen) {
            database = new LocalDBHandler(context);
            connection = database.getReadableDatabase();
            databaseIsOpen = true;
        }
        return connection;
    }

    public static void closeDB() {
        if (databaseIsOpen) {
            connection.close();
            database.close();
            databaseIsOpen = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {

        final Context context = this;

        //update Temperature
        String cred[] = loadCredentials(context);
        Server server = new Server(cred[0], cred[1], cred[2], context);

        server.setConnectListener(new Server.ConnectListener() { // server ist
            // erreichbar
            @Override
            public void connectIsReady(boolean result) {

                if (result) {
                    //Log.d("Widget", "Temperatur wurde geladen, listener aufgerufen");
                    //set Listener for onClick
                    Intent intent = new Intent(context, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.twentyeight_widget);
                    views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);

                    SharedPreferences temp = context.getSharedPreferences("temp", 0);
                    String widgetText = context.getResources().getString(R.string.CHART_DEGREE, temp.getFloat("temp_aussen", 0)).replace(",", ".");
                    views.setTextViewText(R.id.appwidget_text, widgetText);


                    //get max temp last week
                    openDB(context);
                    SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd' '00:00:00",
                            Locale.GERMANY);
                    Date d = new Date();
                    d.setTime(d.getTime() - 604800000);
                    String query = "SELECT Max(value) AS value FROM temperature where sensorid=1 AND datetime> '"
                            + parser.format(d)
                            + "' ";
                    Cursor cr1 = connection.rawQuery(query, null);
                    cr1.moveToFirst();
                    float maxValue = cr1.getFloat(cr1.getColumnIndex("value"));
                    cr1.close();
                    closeDB();
                    //Log.d("Widget","maxValue: "+maxValue);
                    //get max temp end


                    Path circlePercentage = new Path();
                    Path fullCircle = new Path();
                    Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                    Bitmap bmp = Bitmap.createBitmap(200, 200, conf);
                    RectF box = new RectF(10, 10, bmp.getWidth() - 10, bmp.getHeight() - 10);
                    //  float sweep = (float) (360 * temp.getFloat("temp_aussen", 0)/40.0*100.0 * 0.01f);
                    float sweep = (float) (360 * temp.getFloat("temp_aussen", 0) / maxValue * 100.0 * 0.01f);
                    float fullcircleSweep = 360 * 100 * 0.01f;
                    circlePercentage.addArc(box, -90, sweep);
                    fullCircle.addArc(box, 0, fullcircleSweep);

                    Paint mPaintWhite = new Paint();
                    mPaintWhite.setDither(true);
                    mPaintWhite.setColor(Color.WHITE);
                    mPaintWhite.setStyle(Paint.Style.STROKE);
                    mPaintWhite.setStrokeJoin(Paint.Join.ROUND);
                    mPaintWhite.setStrokeCap(Paint.Cap.ROUND);
                    mPaintWhite.setStrokeWidth(13);
                    mPaintWhite.setAntiAlias(true);

                    double h = (-0.11) * ((temp.getFloat("temp_aussen", 0) + 20) * (temp.getFloat("temp_aussen", 0) + 20)) + 250;

                    // in HSV float wert einfügen
                    float[] hsvfl = {(float) h, 0.78f, 0.96f};
                    int hsv = Color.HSVToColor(hsvfl);

                    Paint mPaintPercentage = new Paint();
                    mPaintPercentage.setDither(true);
                    mPaintPercentage.setColor(hsv);
                    mPaintPercentage.setStyle(Paint.Style.STROKE);
                    mPaintPercentage.setStrokeJoin(Paint.Join.ROUND);
                    mPaintPercentage.setStrokeCap(Paint.Cap.ROUND);
                    mPaintPercentage.setStrokeWidth(14);
                    mPaintPercentage.setAntiAlias(true);

                    Paint mPaintBlack = new Paint();
                    mPaintBlack.setDither(true);
                    mPaintBlack.setColor(Color.BLACK);
                    mPaintBlack.setStyle(Paint.Style.FILL);
                    mPaintBlack.setAntiAlias(true);
                    mPaintBlack.setAlpha(60);


                    Canvas canvas = new Canvas(bmp);
                    canvas.drawPath(fullCircle, mPaintBlack);
                    canvas.drawPath(fullCircle, mPaintWhite);
                    canvas.drawPath(circlePercentage, mPaintPercentage);


                    views.setImageViewBitmap(R.id.widgetImageView, bmp);


                    ComponentName thisWidget = new ComponentName(context, TwentyeightWidget.class);
                    AppWidgetManager manager = AppWidgetManager.getInstance(context);
                    manager.updateAppWidget(thisWidget, views);

                }

            }
        });
        server.connect();

    }
}
