package de.paulsapp.twentyeight;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalDBHandler extends SQLiteOpenHelper {
	
	private Context context;
	
	public LocalDBHandler(Context context){
	    super(
	        context,
	        context.getResources().getString(R.string.dbname),
	        null,
	        Integer.parseInt(context.getResources().getString(R.string.dbversion)));
	    this.context=context;
	  }

	  @Override
	  public void onCreate(SQLiteDatabase db) {
	    for(String sql : context.getResources().getStringArray(R.array.create))
	      db.execSQL(sql);

	  }

	  @Override
	  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	  }
	  //lösche alte db, lade neue db
	  
	}