package de.paulsapp.twentyeight;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A login screen that offers login via server/user/password.
 */
public class LoginActivity extends Activity {

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLogin mAuthTask = null;

	// UI references.
	private EditText mUrlView;
	private AutoCompleteTextView mUserView;
	private EditText mPasswordView;
	private View mProgressView;
	private View mLoginFormView;
	private Context context;
	SQLiteOpenHelper database;
	SQLiteDatabase connection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
	//	setupActionBar();

		context = getApplicationContext();
		// Set up the login form.
		
		
		mUrlView = (EditText) findViewById(R.id.url);
		mUrlView.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int id, KeyEvent event) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		mUserView = (AutoCompleteTextView) findViewById(R.id.user);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		Button mUserSignInButton = (Button) findViewById(R.id.sign_in_button);
		mUserSignInButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});

		mLoginFormView = findViewById(R.id.login_form);
		mProgressView = findViewById(R.id.login_progress);
		
		
		openDB(context);
		Cursor credentialscursor = connection.rawQuery(
				"SELECT url,user,password FROM savedUsers WHERE selected = 1",
				null);

		if (credentialscursor.getCount() > 0) {
						String url = "";
			String user = "";
			String password = "";
			try {
				credentialscursor.moveToFirst();
				url = credentialscursor.getString(credentialscursor
						.getColumnIndex("url"));
				user = credentialscursor.getString(credentialscursor
						.getColumnIndex("user"));
				password = credentialscursor.getString(credentialscursor
						.getColumnIndex("password"));
			} catch (Exception e) {
				Toast.makeText(context, "Ups, Error!", Toast.LENGTH_LONG).show();
			}
			mUrlView.setText(url);
			mUserView.setText(user);
			mPasswordView.setText(password);
			
		}
		
		
		
	}

	public SQLiteDatabase openDB(Context context) {
		database = new LocalDBHandler(context);
		connection = database.getReadableDatabase();
		return connection;
	}

	public void closeDB() {
		connection.close();
		database.close();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mUrlView.setError(null);
		mUserView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		String url = mUrlView.getText().toString();
		if (!url.startsWith("http://www.")) {
			if (!url.startsWith("http://")) {
				url = "http://" + url;
			} else {
				url = "http://" + url.substring(7);
			}

		} else {
			url = "http://" + url.substring(11);
		}
		String user = mUserView.getText().toString();
		String password = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid url.
		if (TextUtils.isEmpty(url)) {
			mUrlView.setError(getString(R.string.error_field_required));
			focusView = mUrlView;
			cancel = true;

		} else if (!isUrlValid(url)) {
			mUrlView.setError(getString(R.string.error_invalid_url));
			focusView = mUrlView;
			cancel = true;
		}

		// Check for a valid password.
		if (TextUtils.isEmpty(password)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;

		} else if (!isPasswordValid(password)) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid user.
		if (TextUtils.isEmpty(user)) {
			mUserView.setError(getString(R.string.error_field_required));
			focusView = mUserView;
			cancel = true;
		} else if (!isUserValid(user)) {
			mUserView.setError(getString(R.string.error_invalid_email));
			focusView = mUserView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			showProgress(true);
			mAuthTask = new UserLogin(url, user, password);
			mAuthTask.dologin();
		}
	}

	private boolean isUrlValid(String url) {
		return true;
	}

	private boolean isUserValid(String user) {
		return user.length() > 0;
	}

	private boolean isPasswordValid(String password) {
		return password.length() > 0;
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});

			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mProgressView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mProgressView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */

	public class UserLogin {
		private final String mUrl;
		private final String mUser;
		private final String mPassword;

		UserLogin(String url, String user, String password) {
			mUrl = url;
			mUser = user;
			mPassword = password;
		}

		public void dologin() {
			Server server = new Server(mUrl, mUser, mPassword, context);

			server.setConnectListener(new Server.ConnectListener() {

				@Override
				public void connectIsReady(boolean result) {
					mAuthTask = null;
					showProgress(false);

					if (result) {

						SQLiteOpenHelper database = new LocalDBHandler(context);
						SQLiteDatabase connection = database
								.getWritableDatabase();
						String sqlquery = "SELECT * FROM savedUsers WHERE url = '"
								+ mUrl
								+ "' AND user= '"
								+ mUser
								+ "' AND password = '" + mPassword + "'";
						Cursor cursor = connection.rawQuery(sqlquery, null);
						if (cursor.getCount() <= 0) {
							String sqlquery2 = "UPDATE savedUsers SET selected=0";
							connection.execSQL(sqlquery2);

							SimpleDateFormat s = new SimpleDateFormat(
									"yyyyMMddHHmmss", Locale.GERMANY);
							String datetime = s.format(new Date());

							connection
									.execSQL("INSERT INTO savedUsers (url,user,password,datetime,selected) VALUES ('"
											+ mUrl
											+ "','"
											+ mUser
											+ "','"
											+ mPassword
											+ "','"
											+ datetime
											+ "',1)");
						}
						connection.close();
						database.close();
						Intent mainActivityIntent = new Intent(context, MainActivity.class);
						startActivity(mainActivityIntent);
						finish();
					} else {
						mPasswordView
								.setError(getString(R.string.error_wrong_credentials));
						mPasswordView.requestFocus();
					}

				}

			});

			server.connect();

		}
	}

}
