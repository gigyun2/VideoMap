package com.example.videomaps;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by YGG on 4/18/2017.
 */

class DatabaseHelper extends SQLiteOpenHelper {
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
                        Place.NAME + VARCHAR_TYPE + COMMA_SEP +
                        Place.LATITUDE + FLOAT_TYPE + COMMA_SEP +//+ " (8,6)" + COMMA_SEP +
                        Place.LONGITUDE + FLOAT_TYPE + COMMA_SEP +//+ " (9,6)" + COMMA_SEP +
                        Place.DESCRIPTION + TEXT_TYPE + " )";
    }

    public static final class Media implements BaseColumns {
        public static final String PATH = "path";
        public static final String DATE = "date";
        public static final String PLACE_ID = "place_id";
        public static final String TABLE_NAME = "media";
        public static final String _CREATE =
                "CREATE TABLE IF NOT EXISTS " + Media.TABLE_NAME + " (" +
                        Media._ID + INT_TYPE + COMMA_SEP +
                        Media.PATH + TEXT_TYPE + COMMA_SEP +
                        Media.DATE + DATE_TYPE + COMMA_SEP + " DEFAULT CURRENT_TIMESTAMP" +
                        Media.PLACE_ID + INT_TYPE + " NOT NULL" + COMMA_SEP +
                        " PRIMARY KEY (" + _ID + COMMA_SEP + PLACE_ID + " )" + " )";
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
            "DROP TABLE IF EXISTS " + Place.TABLE_NAME  + " " + Media.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Place._CREATE);
        db.execSQL(Media._CREATE);
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

    static public boolean addMedia(SQLiteDatabase db, String path, int place_id) {

        ContentValues placeValue = new ContentValues();
        placeValue.put(Media.PLACE_ID, Integer.toString(place_id));
        placeValue.put(Media.DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        placeValue.put(Media.PATH, path);
        if (db.insert(DatabaseHelper.Place.TABLE_NAME, null, placeValue) == -1)
            return false;
        return true;
    }

    static public Cursor queryMedia(SQLiteDatabase db, int place_id) {

        String[] projection = {
                Media.DATE,
                Media.PATH
        };
        String selection = Media.PLACE_ID + " = ?";
        String[] selectionArgs = {Integer.toString(place_id)};
        String orderBy =  Media.DATE + " DESC";
        return db.query(Media.TABLE_NAME, projection, selection, selectionArgs, null, null, orderBy);
    }

    static public boolean addPlace(SQLiteDatabase db, String name, double latitude, double longitude, String desc) {

        ContentValues placeValue = new ContentValues();
        placeValue.put(Place.NAME, name);
        placeValue.put(Place.LATITUDE, Double.toString(latitude));
        placeValue.put(Place.LONGITUDE, Double.toString(longitude));
        placeValue.put(Place.DESCRIPTION, desc);
        if (db.insert(DatabaseHelper.Place.TABLE_NAME, null, placeValue) == -1)
            return false;
        return true;
    }

    static public Cursor queryPlaceAll(SQLiteDatabase db) {

        return db.query(Place.TABLE_NAME, null, null, null, null, null, null);
    }
    /*
    public Cursor queryPlace(double latitude, double longitude) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                Place.,
                Place.PATH
        };
        String selection = Media.PLACE_ID + " = ?";
        String[] selectionArgs = {Integer.toString(place_id)};
        String orderBy =  Media.DATE + " DESC";
        Cursor c = db.query(Place.TABLE_NAME, projection, selection, selectionArgs, null, null, orderBy);
    }
    */
}