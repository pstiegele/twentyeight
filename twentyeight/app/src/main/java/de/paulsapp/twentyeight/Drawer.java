package de.paulsapp.twentyeight;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Paul Stiegele on 12.12.2016. Hell yeah!
 */

public class Drawer {
    public RecyclerView.Adapter mDrawerAdapter;
    public ActionBarDrawerToggle mDrawerToggle;
    public boolean[] mStatus;
    public int[] mValue;
    private Server server;
    private Context context;
    private String[] mDrawerItemsTitles;
    private int[] mDrawerItemsIcons = {R.drawable.schreibtischlampe, R.drawable.fernseher, R.drawable.bluetoothspeaker, R.drawable.lightstripe};
    private DrawerLayout mDrawerLayout;
    private RecyclerView mDrawerRecyclerView;
    private Database db;
    private Activity activity;

    public Drawer(Context context, Database db, Server server, Activity activity) {
        this.context = context;
        this.db = db;
        this.server = server;
        this.activity = activity;
    }


    public void loadDrawerTitleAndStatus() {
        Cursor cr = db.getRawQuery("SELECT name,status,value FROM doseElements");
        mDrawerItemsTitles = new String[cr.getCount()];
        mStatus = new boolean[cr.getCount()];
        mValue = new int[cr.getCount()];
        for (int i = 0; i < cr.getCount(); i++) {
            mDrawerItemsTitles[i] = cr.getString(cr.getColumnIndex("name"));
            mStatus[i] = cr.getInt(cr.getColumnIndex("status")) == 0;
            mValue[i] = cr.getInt(cr.getColumnIndex("value"));
            cr.moveToNext();
        }
        cr.close();
    }

    public void setDrawerPrefs(Bundle savedInstanceState) {
//        String NAME = "Paul Stiegele";
//        String EMAIL = "paul@stiegele.name";
//        int PROFILEPICTURE = R.drawable.profilepicture;
        String NAME = "";
        String EMAIL = "";
        int PROFILEPICTURE = R.drawable.ic_account_circle_white_48dp;
        RecyclerView.LayoutManager mDrawerLayoutManager;

        mDrawerLayout = (DrawerLayout) activity.findViewById(R.id.layout_root);
        mDrawerRecyclerView = (RecyclerView) activity.findViewById(R.id.left_drawer);

        mDrawerRecyclerView.setHasFixedSize(true);
        mDrawerAdapter = new MyDrawerAdapter(mDrawerItemsTitles, mDrawerItemsIcons, mStatus, mValue, NAME, EMAIL, PROFILEPICTURE, context, server, db);
        mDrawerRecyclerView.setAdapter(mDrawerAdapter);
        mDrawerLayoutManager = new LinearLayoutManager(activity);
        mDrawerRecyclerView.setLayoutManager(mDrawerLayoutManager);

        mDrawerToggle = new ActionBarDrawerToggle(activity, mDrawerLayout, R.string.openDrawer, R.string.closeDrawer) {
            public void onDrawerClosed(View view) {
                activity.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                activity.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };


        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if (savedInstanceState == null) {
            selectItem(0);
        }


        final GestureDetector mGestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

        });


        mDrawerRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                View child = recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());


                if (child != null && mGestureDetector.onTouchEvent(motionEvent)) {
                    // mDrawerLayout.closeDrawers();
                    // Toast.makeText(MainActivity.this, "The Item Clicked is: " + recyclerView.getChildPosition(child), Toast.LENGTH_SHORT).show();

                    mDrawerAdapter.notifyDataSetChanged();
                    int a = recyclerView.getChildAdapterPosition(child);
                    int b = recyclerView.getChildLayoutPosition(child);
                    int c = recyclerView.getChildCount();
                    View d = recyclerView.getChildAt(0);
                    AsyncSetStatusRunner runner = new AsyncSetStatusRunner();
                    int newStatus;
                    if (mStatus[recyclerView.getChildLayoutPosition(child) - 1]) {
                        newStatus = 0;    //invertiert, weil Status ja geändert werden soll
                        mStatus[recyclerView.getChildLayoutPosition(child) - 1] = false;
                    } else {
                        newStatus = 1;
                        mStatus[recyclerView.getChildLayoutPosition(child) - 1] = true;
                    }
                    Dosen.SammelParamsStatic sp = new Dosen.SammelParamsStatic(mDrawerItemsTitles[recyclerView.getChildLayoutPosition(child) - 1], String.valueOf(newStatus), server);
                    mDrawerAdapter.notifyDataSetChanged();
                    runner.execute(sp);
                    db.execSQLString("UPDATE \"doseElements\" SET \"status\"='" + newStatus + "' WHERE \"id\" = " + recyclerView.getChildLayoutPosition(child));
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
            }
        });


    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        //hier den code einfügen um status eines geräts zu verändern
        if (position == 4122) {
            Log.d("Drawer", "krass, selectItem wurde aufgerufen");
        }
        // update selected item and title, then close the drawer
        //  mDrawerRecyclerView.setItemChecked(position, true);
        // setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerRecyclerView);
    }

    private void updateDose() {
        AsyncGetElementsRunner runner = new AsyncGetElementsRunner();
        Dosen.SammelParamsStatic sps = new Dosen.SammelParamsStatic(server);
        runner.execute(sps);
    }

    public void updateDosenStatus() {
        if (server != null && server.isInitialized) {
            updateDose();
        }

    }

    private class AsyncSetStatusRunner extends AsyncTask<Dosen.SammelParamsStatic, String, Boolean> {
        @Override
        protected Boolean doInBackground(Dosen.SammelParamsStatic... params) {

            String name = params[0].name;
            String status = params[0].status;
            Server server = params[0].server;
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(server.address);
            JSONObject json = new JSONObject();

            try {
                // JSON data:
                json.put("type", "doseSetStatus");
                json.put("user", server.user);
                json.put("password", server.password);
                json.put("doseName", name);
                json.put("doseStatus", status);

                JSONArray postjson = new JSONArray();
                postjson.put(json);

                // Post the data:
                httppost.setHeader("json", json.toString());
                httppost.getParams().setParameter("jsonpost", postjson);

                // Execute HTTP Post Request

                httpclient.execute(httppost);


            } catch (IOException e) {
                Log.i("paul", e.getLocalizedMessage());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return true;
        }


        protected void onPreExecute() {
            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog
        }
    }

    @SuppressWarnings("deprecation")
    private class AsyncGetElementsRunner extends AsyncTask<Dosen.SammelParamsStatic, String, Boolean> {
        @Override
        protected Boolean doInBackground(Dosen.SammelParamsStatic... params) {
            Server server = params[0].server;
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(server.address);
            JSONObject json = new JSONObject();
            String strresponse;
            boolean statusChanges = false;

            try {
                // JSON data:
                json.put("type", "doseGetElements");
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

                        int count = responseJSON.getInt("count");
                        String name;
                        DoseStatus status;
                        //    String icon;
                        //   int shortTimeLimit;
                        //   Date timeSetShort = null;
                        //  Date lastTimeSet = null;
                        int value;


                        if (count != mStatus.length) {
                            mStatus = new boolean[count];
                        }

                        for (int i = 0; i < count; i++) {
                            JSONObject job = responseJSON.getJSONArray("content")
                                    .getJSONObject(i);
                            name = job.getString("name");
                            if (job.getInt("status") == 0) {
                                status = DoseStatus.AUS;
                            } else {
                                status = DoseStatus.AN;
                            }
                            if (job.isNull("value")) {
                                value = -1;
                            } else {
                                value = job.getInt("value");
                            }


                            String sqlstring = "INSERT INTO doseElements (name,status,value) VALUES ('"
                                    + name
                                    + "','"
                                    + status.getAsInt()
                                    + "','"
                                    + value
                                    + "')";
                            Cursor cr = db.getRawQuery("SELECT name FROM doseElements WHERE name = '" + name + "'");
                            if (cr.getCount() == 0) {
                                db.execSQLString(sqlstring);
                            } else {
                                db.execSQLString("UPDATE \"doseElements\" SET \"status\"='" + status.getAsInt() + "' WHERE \"name\" = '" + name + "'");
                                db.execSQLString("UPDATE \"doseElements\" SET \"value\"='" + value + "' WHERE \"name\" = '" + name + "'");
                            }
                            cr.close();
                            if (mStatus.length >= i) {
                                mStatus[i] = status.getAsBoolean();
                                if (mValue.length - 1 >= i) {
                                    mValue[i] = value;
                                }

                                statusChanges = true;
                            }


                        }


                    }

                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            return statusChanges;
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                mDrawerAdapter.notifyDataSetChanged();
            }


        }

        protected void onPreExecute() {
            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog
        }
    }

}
