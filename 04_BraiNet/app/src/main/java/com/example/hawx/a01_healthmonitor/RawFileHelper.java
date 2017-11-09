package com.example.hawx.a01_healthmonitor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.util.jar.Pack200;
import java.lang.CharSequence;
import android.util.Log;
/**
 * Created by Hawx on 09/11/2017.
 */

/*
    We will use this file to do feature extraction an comparator.
 */
public class RawFileHelper {
    static final String TAG = "RawFileHelper";
    private String rawfile_path;
    private String sample_rawfile_NAME      = "G25_00.edf";
    private String testing_rawfile_NAME      = "G25_00.edf";
    static final File raw_file      = Environment.getExternalStorageDirectory();
    static final String RAW_FILE_DIR = raw_file.getAbsolutePath() + File.separator + "CSE535_BriaNet";

    public RawFileHelper() {
        /*
         Check the directory exist or not, if not, just create one.
        */
        File raw_file_dir = new File((RAW_FILE_DIR));
        if((!raw_file_dir.exists()) || !raw_file_dir.isDirectory()) {
            raw_file_dir.mkdirs();
            Log.e(TAG, "The working directory is built!");
        }
    }

    /*
        Read one Raw file in
        The API is wating to be defined.
         public readInOnefile() {

            }
     */


    /*
       Pass the content to feature extraction.jar
     */


    /*
       Comparator
     */
}
