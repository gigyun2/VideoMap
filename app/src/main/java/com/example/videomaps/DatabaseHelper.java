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
                        Media._ID + INT_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                        Media.PATH + TEXT_TYPE + COMMA_SEP +
                        Media.DATE + DATE_TYPE + " DEFAULT CURRENT_TIMESTAMP" + COMMA_SEP +
                        Media.PLACE_ID + INT_TYPE + " NOT NULL" + COMMA_SEP +
                        " PRIMARY KEY (" + _ID + COMMA_SEP + PLACE_ID + " )" + " )";
    }

/*
    public static final class Hashtag implements BaseColumns {
        public static final String NAME = "name";
        public static final String TABLE_NAME = "hashtag";
        public static final String _CREATE =
                "CREATE TABLE IF NOT EXISTS " + Hashtag.TABLE_NAME + " (" +
                        Hashtag.NAME + VARCHAR_TYPE + " PRIMARY KEY" + " )";
    }

    public static final class Tag_Review implements BaseColumns {
        public static final String TAG_NAME = "tag_name";
        public static final String REVIEW_ID = "review_id";
        public static final String PLACE_ID = "place_id";
        public static final String TABLE_NAME = "tag_review";
        public static final String _CREATE =
                "CREATE TABLE IF NOT EXISTS " + Tag_Review.TABLE_NAME + " (" +
                        Tag_Review.TAG_NAME + INT_TYPE + " NOT NULL" + COMMA_SEP +
                        Tag_Review.REVIEW_ID + INT_TYPE + " NOT NULL" + COMMA_SEP +
                        Tag_Review.PLACE_ID + INT_TYPE + " NOT NULL" + COMMA_SEP +
                        " PRIMARY KEY (" + TAG_NAME + COMMA_SEP + REVIEW_ID + COMMA_SEP + PLACE_ID + " )" +
                " )";
    }
*/

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

        ContentValues mediaValue = new ContentValues();
        mediaValue.put(Media.PLACE_ID, Integer.toString(place_id));
        mediaValue.put(Media.DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        mediaValue.put(Media.PATH, path);
        if (db.insert(DatabaseHelper.Media.TABLE_NAME, null, mediaValue) == -1)
            return false;
        return true;
    }

    static public int deleteMedia(SQLiteDatabase db, String path) {

        String[] projection = {
                Media._ID
        };
        String selection = Media.PATH + " = ?";
        String[] selectionArgs = {path};
        Cursor c = db.query(Media.TABLE_NAME, projection, selection, selectionArgs, null, null, null);

        if (!c.moveToFirst())
            return -1;

        int mid = c.getInt(c.getColumnIndex(Media._ID));

        String where = Media.PATH + " = ?";
        String[] whereArgs = {path};
        return db.delete(DatabaseHelper.Media.TABLE_NAME, where, whereArgs);
    }

    static public Cursor queryMedia(SQLiteDatabase db, int place_id) {

        String[] projection = {
                Media._ID,
                Media.DATE,
                Media.PATH
        };
        String selection = Media.PLACE_ID + " = ?";
        String[] selectionArgs = {Integer.toString(place_id)};
        String orderBy =  Media.DATE + " DESC";
        return db.query(Media.TABLE_NAME, projection, selection, selectionArgs, null, null, orderBy);
    }

    static public long addPlace(SQLiteDatabase db, String name, double latitude, double longitude, String desc) {

        ContentValues placeValue = new ContentValues();
        placeValue.put(Place.NAME, name);
        placeValue.put(Place.LATITUDE, Double.toString(latitude));
        placeValue.put(Place.LONGITUDE, Double.toString(longitude));
        placeValue.put(Place.DESCRIPTION, desc);
        return db.insert(DatabaseHelper.Place.TABLE_NAME, null, placeValue);
    }

    static public Cursor queryPlaceAll(SQLiteDatabase db) {

        return db.query(Place.TABLE_NAME, null, null, null, null, null, null);
    }

    static public Cursor queryPlace(SQLiteDatabase db, double latitude, double longitude) {

        String[] projection = {
                Place._ID,
                Place.NAME,
                Place.DESCRIPTION
        };
        String selection = Place.LATITUDE + " = ? AND " + Place.LONGITUDE + " = ?";
        String[] selectionArgs = {Double.toString(latitude), Double.toString(longitude)};
        return db.query(Place.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
    }
}