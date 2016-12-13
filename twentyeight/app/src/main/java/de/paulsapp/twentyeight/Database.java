package de.paulsapp.twentyeight;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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

    @SuppressWarnings("resource")
    public void exportDB() {
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source;
        FileChannel destination;
        String dbname = context.getResources().getString(R.string.dbname);
        String currentDBPath = "/data/" + "de.paulsapp.twentyeight"
                + "/databases/" + dbname;
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, dbname);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(context, "DB Exported!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteDB() {
        String dbname = context.getResources().getString(R.string.dbname);
        boolean result = context.deleteDatabase(dbname);
        if (result) {
            Toast.makeText(context, "DB Deleted!", Toast.LENGTH_LONG).show();
        }
    }
}
