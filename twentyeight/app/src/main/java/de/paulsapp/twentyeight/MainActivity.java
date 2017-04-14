package de.paulsapp.twentyeight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.splunk.mint.Mint;

import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {


    public Server server;
    public Database db;
    /////////////////////
    //// Variablen /////
    /////////////////////

    private Handler handler = new Handler();
    private Timer timer15s;
    private Context context;
    private Drawer drawer;
    private Temperature temperature;
    private TemperatureChart temperatureChart;
    final Runnable Runnable_Refresh_Time = new Runnable() {
        public void run() {
            setRefreshTime();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mint.setApplicationEnvironment(Mint.appEnvironmentStaging);
        Mint.initAndStartSession(this.getApplication(), "55f2ef32");
        setContentView(R.layout.layout_root);
        context = getApplicationContext();
        init(savedInstanceState);
    }

    public void init(Bundle savedInstanceState) {
        ////////////////////////
        ///// Init Database ////
        ////////////////////////
        db = new Database(context);

        ////////////////////////
        /// load Credentials ///
        /// and init Server  ///
        ////////////////////////
        server = new Server();
        if (!server.loadCredentials(db, this, context)) {
            return;
        }

        /////////////////
        //// Drawer /////
        /////////////////
        drawer = new Drawer(context,db,server,this);
        drawer.loadDrawerTitleAndStatus();
        drawer.setDrawerPrefs(savedInstanceState);


        ////////////////////////
        /// Background Color ///
        ////////////////////////
        temperature = new Temperature(context,this);
        temperatureChart = new TemperatureChart(temperature, db,this,server);
        temperature.initTemperature();


        ////////////////////////
        ///   init TempChart ///
        ////////////////////////
       temperatureChart.refreshTempCharts(true);



        /////////////////////////
        /// updateDosenStatus ///
        /////////////////////////
        drawer.updateDosenStatus();

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
                drawer.updateDosenStatus();
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


    public void updateData(final Server server) {

        server.setSelectListener(new Server.SelectListener() {

            @Override
            public void selectIsReady(boolean result) { // aktuelle Temp geladen
                if(temperatureChart!=null){
                    try {
                        temperatureChart.refreshTempCharts(false);
                        LineChart chart = (LineChart) findViewById(R.id.chart);
                        chart.invalidate();
                    }catch (Exception e){

                    }

                }


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



    public void updateguibarcharts() { // aktuelle Temperatur
if(temperature!=null){
    temperature.refreshtemp();
    temperatureChart.updateTempCharts();
}

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
            db.exportDB();
        }
        if (id == R.id.delete_db) {
            db.deleteDB();
        }
        if (id == R.id.refresh) {
            ProgressBar pb = (ProgressBar) findViewById(R.id.load_progressbar);
            pb.setVisibility(android.view.View.VISIBLE);

            updateData(server);

        }
        if (id == R.id.coldRefresh) {
            server.doColdRefresh(drawer);
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
            drawer.updateDosenStatus();
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
}
