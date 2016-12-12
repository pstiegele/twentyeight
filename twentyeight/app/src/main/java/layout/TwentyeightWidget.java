package layout;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.GregorianCalendar;

import de.paulsapp.twentyeight.LocalDBHandler;
import de.paulsapp.twentyeight.MainActivity;
import de.paulsapp.twentyeight.R;
import de.paulsapp.twentyeight.Server;
import de.paulsapp.twentyeight.WidgetService;

/**
 * Implementation of App Widget functionality.
 */
public class TwentyeightWidget extends AppWidgetProvider {




    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

//        Log.d("Widget", "updateAppWidget gestartet");
//        String cred[]=loadCredentials(context);
//        Server server= new Server(cred[0],cred[1],cred[2],context);
//        Log.d("Widget", "Server erstellt");
//        final Context finalcontext = context;
//        final AppWidgetManager finalAppWidgetManager = appWidgetManager;
//        final int finalappWidgetId = appWidgetId;
//
//        server.setSelectListener(new Server.SelectListener() {
//
//            @Override
//            public void selectIsReady(boolean result) { // aktuelle Temp geladen
//                Log.d("Widget", "Temperatur wurde geladen, listener aufgerufen");
//                updateData(finalcontext,finalAppWidgetManager,finalappWidgetId);
//
//            }
//        });
//        server.refreshData();
//        Log.d("Widget", "updateAppWidget fertig");
/*
        // Create an Intent to launch ExampleActivity
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.twentyeight_widget);
        views.setOnClickPendingIntent(R.id.widget_reload, pendingIntent);

*/

    }

    public static void updateData(Context context, AppWidgetManager appWidgetManager, int appWidgetId){
      //  Log.d("Widget", "updateData wurde vom Listener aufgerufen");
     //   SharedPreferences temp = context.getSharedPreferences("temp", 0);

     //   String widgetText = context.getResources().getString(R.string.CHART_DEGREE, temp.getFloat("temp_aussen", 0)).replace(",", ".");

        // Construct the RemoteViews object
     //   RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.twentyeight_widget);
      //  views.setTextViewText(R.id.appwidget_text, widgetText);

        // Instruct the widget manager to update the widget
     //   appWidgetManager.updateAppWidget(appWidgetId, views);
     //   Log.d("Widget", "updateData fertig");



    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
      //  Toast.makeText(context,"onUpdate",Toast.LENGTH_LONG).show();
     //   for (int appWidgetId : appWidgetIds) {
//            Intent intent = new Intent(context, TwentyeightWidget.class);
//            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
//            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//            RemoteViews rv = new RemoteViews(context.getPackageName(),R.id.updateWidgetButton);
//            rv.setOnClickPendingIntent(R.layout.twentyeight_widget,pendingIntent);
//            updateAppWidget(context, appWidgetManager, appWidgetId);
        final int N = appWidgetIds.length;
    //    for (int i=0; i<N; i++) {

       //     int appWidgetId = appWidgetIds[i];

            context.startService(new Intent(context, WidgetService.class));




         //   appWidgetManager.updateAppWidget(appWidgetId, views);
   //     }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }



//    @Override
//    public void onReceive(Context context, Intent intent){
//        super.onReceive(context, intent);
//        Toast.makeText(context, "onReceive", Toast.LENGTH_LONG).show();
//        Log.d("widget","onReceive");
//        Bundle extras = intent.getExtras();
//        if(extras!=null) {
//            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
//            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), TwentyeightWidget.class.getName());
//            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
//
//            onUpdate(context, appWidgetManager, appWidgetIds);
//        }
//
//    }
}

