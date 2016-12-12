package de.paulsapp.twentyeight;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by pstiegele on 12.12.2016.
 */

public class Database {
    private SQLiteOpenHelper database;
    private SQLiteDatabase connection;
    private boolean databaseIsOpen = false;
    private Context context;
    public Database(Context context){
        this.context=context;
    }

    private SQLiteDatabase openDB() {
        if (!databaseIsOpen) {
            database = new LocalDBHandler(context);
            connection = database.getReadableDatabase();
            databaseIsOpen = true;
        }
        return connection;
    }

    private void closeDB() {
        if (databaseIsOpen) {
            connection.close();
            database.close();
            databaseIsOpen = false;
        }
    }
    public Cursor getRawQuery(String query){
        if (!databaseIsOpen){
            openDB();
        }
        Cursor cursor = connection.rawQuery(query,null);
        cursor.moveToFirst();
        closeDB();
        return cursor;
    }

    public void execSQLString(String sqlstring){
        if (!databaseIsOpen){
            openDB();
        }
        connection.execSQL(sqlstring);
        closeDB();
    }
}
