package com.example.hawx.a01_healthmonitor;
import android.database.Cursor;
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
    private String mTableNameLastOpen; //Take convenience of this project.
    private String db_path;
    private SQLiteDatabase db;

    public static class SDSQLiteSchema {
        public static final String INCREASE_ID = "ID";
        public static final String TS_FIELD = "TIMESTAMP";
        public static final String X_FIELD = "X";
        public static final String Y_FIELD = "Y";
        public static final String Z_FIELD = "Z";
    }

    public SDSQLiteHelper()
    {

        File   DB_FILE      = Environment.getExternalStorageDirectory();
        String DB_FILE_PATH = DB_FILE.getAbsolutePath();
        String DB_NAME      = "Group25.db";

        db_path = DB_FILE_PATH  + File.separator + DB_NAME;

        try
        {
            // Open the database (create if it does not exist)
            Log.e(TAG,"DB Path: " + db_path);
            db = SQLiteDatabase.openDatabase(db_path, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
            Log.e(TAG, "Successful DB: " + db.toString());
        }
        catch (SQLiteException ex)
        {
            Log.e(TAG, "error -- " + ex.getMessage(), ex);
            // error means tables does not exits
        }
        finally
        {
            //SQLiteDatabase.deleteDatabase(new File(DB_FILE_PATH
            //        + File.separator + DB_NAME));
        }
    }


    public void createTables(String tableName)
    {
        //https://sqlite.org/autoinc.html
        //https://www.techonthenet.com/sql/tables/create_table.php
        mTableNameLastOpen = tableName;

        String CMD = "CREATE TABLE IF NOT EXISTS \"" + tableName + "\" ( "
                + SDSQLiteSchema.INCREASE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SDSQLiteSchema.TS_FIELD + " INTEGER NOT NULL, "
                + SDSQLiteSchema.X_FIELD + " REAL NOT NULL, "
                + SDSQLiteSchema.Y_FIELD + " REAL NOT NULL, "
                + SDSQLiteSchema.Z_FIELD + " REAL NOT NULL " + "); ";

        Log.e(TAG, "Creating Table in datbase " + db.toString());
        Log.e(TAG, "SQL Query: " + CMD);
        db.execSQL(CMD);
        Log.e(TAG, "Success");
    }

    public void closeDB()
    {
        db.close();
    }

    public static void deleteDB()
    {
        //SQLiteDatabase.deleteDatabase(new File(db_path));
    }

    public SQLiteDatabase getWritableDatabase(String tableName)
    {
        return db;
    }



}
