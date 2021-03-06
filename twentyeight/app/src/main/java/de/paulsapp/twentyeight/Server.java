package de.paulsapp.twentyeight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class Server {

	String address = "";
	OnlineStatus onlineStatus;
	public boolean isInitialized = false;
	String user = "";
	String password = "";
	String strresponse = "";
	ConnectListener mConnectListener;
	Context context;
	SelectListener mSelectListener;
	private Database db;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Server(String address, String user, String password, Context context) {
		this.context = context;
		this.address = address;
		this.user = user;
		this.password = password;
		this.isInitialized=true;
		onlineStatus = new OnlineStatus();
		if (ping() == true) {
			onlineStatus.setStatusAsPingAble();
		} else {
			onlineStatus.setStatusAsOffline();
		}

	}

	public Server(){
		this.isInitialized=false;
		onlineStatus = new OnlineStatus();
		onlineStatus.setStatusAsOffline();
	}

	public interface ConnectListener {
		public void connectIsReady(boolean result);
	}

	public interface SelectListener {
		public void selectIsReady(boolean result);
	}

	public void setConnectListener(ConnectListener listener) {
		mConnectListener = listener;
	}

	public void setSelectListener(SelectListener listener) {
		mSelectListener = listener;
	}

	public boolean ping() {

		return false;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public OnlineStatusValue getOnlineStatus() {
		return onlineStatus.getStatus();
	}

	public void connect() {
		AsyncTaskRunner runner = new AsyncTaskRunner();
		String query[] = new String[1];
		query[0] = "connect";
		runner.execute(query);

	}

	
	public Object[] postConnectData() throws JSONException {
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(address);
		JSONObject json = new JSONObject();

		Object ret[] = new Object[4];

		try {
			// JSON data:
			json.put("type", "connect");
			json.put("user", user);
			json.put("password", password);

			JSONArray postjson = new JSONArray();
			postjson.put(json);

			// Post the data:
			httppost.setHeader("json", json.toString());
			httppost.getParams().setParameter("jsonpost", postjson);

			// Execute HTTP Post Request
			System.out.print(json);
			HttpResponse response = httpclient.execute(httppost);

			// for JSON:
			if (response != null) {
				InputStream is = response.getEntity().getContent();

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();

				String line = null;
				try {
					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
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
				if (responseJSON.getString("response").equals("true")) {

					ret[0] = true; // Anfrage erfolgreich
					ret[1] = responseJSON.getJSONObject("temp").getDouble("1"); // Temp
																				// von
																				// sensorid=1
																				// (aussen)
					ret[2] = responseJSON.getJSONObject("temp").getDouble("2"); // Temp
																				// von
																				// sensorid=2
																				// (innen)
					ret[3] = responseJSON.getJSONObject("temp").getString(
							"datetime"); // Datetime von sensorid=2 (eigentlich
											// gleich wie sensorid=1)

					return ret;
				} else {

					ret[0] = false;
					return ret;
				}

			}

		} catch (ClientProtocolException e) {
			Log.i("paul", e.getLocalizedMessage());
		} catch (IOException e) {
			Log.i("paul", e.getLocalizedMessage());
		}
		ret[0] = false;
		return ret;
	}

	
	public boolean[] postSelectData() throws JSONException {
		// Create a new HttpClient and Post Header
		boolean ret[] = new boolean[2];
		ret[0] = false;
		ret[1] = true;
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(address);
		JSONObject json = new JSONObject();

		SQLiteOpenHelper database = new LocalDBHandler(this.context);
		SQLiteDatabase connection = database.getWritableDatabase();
		Cursor cur = connection.rawQuery(
				"SELECT MAX(datetime) FROM temperature", null);
		cur.moveToFirst();
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(GregorianCalendar.DAY_OF_MONTH,-8);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String lastDateTime = simpleDateFormat.format(cal.getTime());
		Log.d("Server","postSelectData: lastDateTime="+lastDateTime);
		if (cur.getCount() > 0) {
			lastDateTime = cur.getString(0);
		}

		try {
			// JSON data:
			json.put("type", "select");
			json.put("user", user);
			json.put("password", password);
			json.put("timestamp", lastDateTime);

			JSONArray postjson = new JSONArray();
			postjson.put(json);

			// Post the data:
			httppost.setHeader("json", json.toString());
			httppost.getParams().setParameter("jsonpost", postjson);

			// Execute HTTP Post Request
			System.out.print(json);
			HttpResponse response = httpclient.execute(httppost);

			// for JSON:
			if (response != null) {
				InputStream is = response.getEntity().getContent();

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();

				String line = null;
				try {
					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
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
					double value = 0.0;
					String datetime = "";
					int sensorid = 0;
					for (int i = 0; i < count; i++) {
						JSONObject job = responseJSON.getJSONArray("content")
								.getJSONObject(i); 
						value = job.getDouble("value");
						datetime = job.getString("datetime");
						sensorid = job.getInt("sensorid");
						connection
								.execSQL("INSERT INTO temperature (value,datetime,sensorid) VALUES ('"
										+ value
										+ "','"
										+ datetime
										+ "','"
										+ sensorid + "')");
					}

					connection.close();
					database.close();

					// erstes boolean ist ob alles korrekt verlief, zweites ob
					// nochmal aktualisiert werden muss (jedes mal nur max. 30
					// Tupel)

					if (count == 300) {
						ret[1] = false; // wenn aktualisiert werden muss = false
					} else {
						ret[1] = true;
					}

					ret[0] = true;
					return ret;
				} else {

					connection.close();
					database.close();
					ret[0] = true;
					ret[1] = true;
					return ret;
				}

			}

		} catch (ClientProtocolException e) {
			Log.i("paul", e.getLocalizedMessage());
		} catch (IOException e) {
			Log.i("paul", e.getLocalizedMessage());
		}
		return ret;
	}

	public boolean dbInsert() {

		return true;
	}

	public boolean refreshData() {
		AsyncTaskRunner runner = new AsyncTaskRunner();
		String query[] = new String[1];
		query[0] = "select";
		runner.execute(query);

		return true;
	}

	private class AsyncTaskRunner extends AsyncTask<String, String, Object[]> {
		@Override
		protected Object[] doInBackground(String... params) {


			Object ret[] = new Object[5]; // ret[0]=Art der Anfrage
											// ret[1]=Anfrage fehlgeschlagen /
											// erfolgreich
											// ret[2]=bei connect: Temperaturen
											// Object

			switch (params[0]) {
			// Select: ret[0]=true
			// connect: ret[0] =false
			case "select":
				ret[0] = "select";
				try {
					boolean boo[] = new boolean[2];
					boo = postSelectData();
					while (boo[0] == true && boo[1] == false) {
						boo = postSelectData();
					}
					ret[1] = boo[0];
					return ret;
				} catch (Exception e) {
					Log.i("paul", e.getLocalizedMessage());
				}
				break;

			case "connect":
				ret[0] = "connect";
				try {
					Object res[] = postConnectData();
					ret[1] = res[0]; // status abfrage (erfolgreich, fehlerhaft)
					ret[2] = res[1]; // Temp sensorid=1 (aussen)
					ret[3] = res[2]; // Temp sensorid=2 (innen)
					ret[4] = res[3]; // datetime
					return ret;
				} catch (Exception e) {
					Log.i("paul", e.getLocalizedMessage());
				}
				break;

			}
			ret[0] = false;
			ret[1] = false;
			return ret;
		}

		@Override
		protected void onPostExecute(Object[] result) {
			// execution of result of Long time consuming operation

			if (result[0].equals("select")) { // ist true bei select, bei
												// connect false
				mSelectListener.selectIsReady((boolean) result[1]);
			} else {
				if ((boolean) result[1]) {
					onlineStatus.setStatusAsOnline();
					SharedPreferences temp = context.getSharedPreferences(
							"temp", 0);
					SharedPreferences.Editor editor = temp.edit();
					double ta = (double) result[2];
					double ti = (double) result[3];
					editor.putFloat("temp_aussen",(float) ta);
				//	editor.putFloat("temp_aussen",(float) 10);	//DEBUG
					editor.putFloat("temp_innen", (float) ti);
			//		editor.putFloat("temp_innen", (float) 10);	//DEBUG
					editor.putString("timestamp", (String) result[4]);
					editor.commit();

				} else {
					onlineStatus.setStatusAsOffline();
				}
				mConnectListener.connectIsReady((boolean) result[1]);
			}

		}

		protected void onPreExecute() {
			// Things to be done before execution of long running operation. For
			// example showing ProgessDialog
		}
	}

	public boolean loadCredentials(Database db, Activity activity, Context context) {
		this.db=db;
		Cursor crs = db.getRawQuery("SELECT url,user,password FROM savedUsers WHERE selected = 1");
		if (crs.getCount() <= 0) {
			Intent loginActivityIntent = new Intent(activity, LoginActivity.class);
			activity.startActivity(loginActivityIntent);
			activity.finish();
			return false;
		} else {
			try {
				address = crs.getString(crs
						.getColumnIndex("url"));
				user = crs.getString(crs
						.getColumnIndex("user"));
				password = crs.getString(crs
						.getColumnIndex("password"));
				this.context=context;
				crs.close();
				isInitialized=true;
				return true;
			} catch (Exception e) {
				Log.e("Server","could not load credentials");
			}
			crs.close();

			return false;

		}
	}

	//Allgemeine Infos Anfang
	@SuppressWarnings("deprecation")
	public class AsyncGetAllElementsColdRefresh extends AsyncTask<Drawer, String, Boolean> {
		@Override
		protected Boolean doInBackground(Drawer... params) {
			Drawer drawer = params[0];
			// Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(address);
			JSONObject json = new JSONObject();
			String strresponse;
			boolean statusChanges = false;

			try {
				// JSON data:
				json.put("type", "coldRefresh");
				json.put("user", user);
				json.put("password", password);

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
			if(statusChanges){
				drawer.mDrawerAdapter.notifyDataSetChanged();
			}
			return statusChanges;
		}

		protected void onPostExecute(Boolean result) {

		}

		protected void onPreExecute() {
			// Things to be done before execution of long running operation. For
			// example showing ProgessDialog
		}
	}

	public void doColdRefresh(Drawer drawer){
		AsyncGetAllElementsColdRefresh runner = new AsyncGetAllElementsColdRefresh();
		runner.execute(drawer);

	}

	public boolean nodata(){
        String sqlString = "SELECT * FROM temperature";
        Cursor cr = db.getRawQuery(sqlString);
        if (cr.getCount()<=4){
            return true;
        }else{
            return false;
        }

	}

}
