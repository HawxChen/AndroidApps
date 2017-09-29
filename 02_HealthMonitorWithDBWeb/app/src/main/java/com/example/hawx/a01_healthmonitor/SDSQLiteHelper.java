package com.example.hawx.a01_healthmonitor;
import android.os.Environment;
import java.lang.String;
import java.io.File;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
/**
 * Created by Hawx on 29/09/2017.
 */

//Reference: https://stackoverflow.com/questions/7229450/sqliteopenhelper-creating-database-on-sd-card

public class SDSQLiteHelper {
    private static final String TAG  = "DatabaseHelper";
    public static final File DB_FILE = Environment.getExternalStorageDirectory();
    public static final String DB_FILE_PATH = DB_FILE.getAbsolutePath();
    public static final String DB_NAME = "Group25.db";
    private String mTableNameLastOpen; //Take convenience of this project.
    private SQLiteDatabase db;

    private static class SDSQLiteSchema {
        public static final String INCREASE_ID = "0";
        public static final String TS_FIELD = "TIMESTAMP";
        public static final String X_FIELD = "X";
        public static final String Y_FIELD = "Y";
        public static final String Z_FIELD = "Z";
    }

    public SDSQLiteHelper()
    {
        try
        {
            deleteDB();
            db = SQLiteDatabase.openDatabase(DB_FILE_PATH
                    + File.separator + DB_NAME, null,SQLiteDatabase.OPEN_READWRITE);
        }
        catch (SQLiteException ex)
        {
            Log.e(TAG, "error -- " + ex.getMessage(), ex);
            // error means tables does not exits
        }
        finally
        {
            SQLiteDatabase.deleteDatabase(new File(DB_FILE_PATH
                    + File.separator + DB_NAME));
        }
    }


    private void createTables(String tableName)
    {
        //https://sqlite.org/autoinc.html
        //https://www.techonthenet.com/sql/tables/create_table.php
        mTableNameLastOpen = tableName;
        final String CMD = "CREATE TABLE " + tableName + " ( "
                + SDSQLiteSchema.INCREASE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SDSQLiteSchema.TS_FIELD + " INTEGER NOT NULL, "
                + SDSQLiteSchema.X_FIELD + " REAL NOT NULL, "
                + SDSQLiteSchema.Y_FIELD + " REAL NOT NULL, "
                + SDSQLiteSchema.Z_FIELD + " REAL NOT NULL, " + "); ";

        db.execSQL(CMD);
    }

    public void closeDB()
    {
        db.close();
    }

    public void deleteDB()
    {
        SQLiteDatabase.deleteDatabase(new File(DB_FILE_PATH
                + File.separator + DB_NAME));
    }

    public SQLiteDatabase getWritableDatabase(String tableName)
    {
        mTableNameLastOpen = tableName;
        db = SQLiteDatabase.openDatabase(DB_FILE_PATH
                        + File.separator + tableName, null,
                SQLiteDatabase.OPEN_READWRITE);
        return db;
    }



}
