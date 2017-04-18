package com.example.videomaps;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by YGG on 4/18/2017.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String INT_TYPE = " INTEGER";
    private static final String TEXT_TYPE = " TEXT";
    private static final String VARCHAR_TYPE = " VARCHAR(99)";
    private static final String FLOAT_TYPE = " VARCHAR(9)"; //" FLOAT";
    private static final String DATE_TYPE = " DATETIME";
    private static final String COMMA_SEP = ", ";

    public static final class Place implements BaseColumns {
        public static final String NAME = "name";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String DESCRIPTION = "description";
        public static final String TABLE_NAME = "place";
        public static final String _CREATE =
                "CREATE TABLE IF NOT EXISTS " + Place.TABLE_NAME + " (" +
                        Place._ID + INT_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                        Place.NAME + TEXT_TYPE + COMMA_SEP +
                        Place.LATITUDE + FLOAT_TYPE + COMMA_SEP +//+ " (8,6)" + COMMA_SEP +
                        Place.LONGITUDE + FLOAT_TYPE + COMMA_SEP +//+ " (9,6)" + COMMA_SEP +
                        Place.DESCRIPTION + TEXT_TYPE + " )";
    }

    public static final class Media implements BaseColumns {
        public static final String DATE = "date";
        public static final String PLACE_ID = "place_id";
        public static final String TABLE_NAME = "media";
        public static final String _CREATE =
                "CREATE TABLE IF NOT EXISTS " + Media.TABLE_NAME + " (" +
                        Media._ID + INT_TYPE + COMMA_SEP +
                        Media.DATE + TEXT_TYPE + COMMA_SEP +
                        Media.PLACE_ID + INT_TYPE + " NOT NULL" + COMMA_SEP +
                        " PRIMARY KEY (" + _ID + COMMA_SEP + PLACE_ID + " )" + " )";
    }

    public static final class Place_Media implements BaseColumns {
        public static final String TAG_NAME = "tag_name";
        public static final String REVIEW_ID = "review_id";
        public static final String PLACE_ID = "place_id";
        public static final String TABLE_NAME = "place_media";
        public static final String _CREATE =
                "CREATE TABLE IF NOT EXISTS " + Place_Media.TABLE_NAME + " (" +
                        Place_Media.TAG_NAME + INT_TYPE + " NOT NULL" + COMMA_SEP +
                        Place_Media.REVIEW_ID + INT_TYPE + " NOT NULL" + COMMA_SEP +
                        Place_Media.PLACE_ID + INT_TYPE + " NOT NULL" + COMMA_SEP +
                        " PRIMARY KEY (" + TAG_NAME + COMMA_SEP + REVIEW_ID + COMMA_SEP + PLACE_ID + " )" + " )";
    }

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "mvm.db";
    private Context ctx;

    /*
    private static final String SQL_CREATE_ENTRIES =
    Place._CREATE + "; " + Review._CREATE + "; "  + Hashtag._CREATE + "; " +
    Media._CREATE  + "; " + Place_Media._CREATE;
    */
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Place.TABLE_NAME  + " " + Media.TABLE_NAME + " " + Place_Media.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Place._CREATE);
        db.execSQL(Media._CREATE);
        db.execSQL(Place_Media._CREATE);
        // db.insert();
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Currently do nothing

        // Downward upgrade policy is to simply to discard the data and start over

        // db.execSQL(SQL_DELETE_ENTRIES);
        // onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Currently do nothing

        // onUpgrade(db, oldVersion, newVersion);
    }

}