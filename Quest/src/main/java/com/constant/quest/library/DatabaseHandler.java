package com.constant.quest.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;

public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "android_api";

    // Table nameS
    private static final String TABLE_LOGIN = "login";
    private static final String TABLE_FRIENDS = "friends";
    private static final String TABLE_CHALLENGE = "challenge";

    // Table Column names
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_UID = "uid";
    public static final String KEY_USERID = "userid";
    public static final String KEY_HID = "uid";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_FID = "_id";
    public static final String KEY_CID = "_id";
    public static final String KEY_CHALLENGE_ID = "challenge_id";
    public static final String KEY_CREATED_BY = "created_by";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_ACCEPTED_AT = "accepted_at";
    public static final String KEY_TIME_TO_EXPIRE = "time_to_expire";
    public static final String KEY_SYNC_STATUS = "sync_status";
    public static final String KEY_EXPIRES_IN = "expires_in";
    public static final String KEY_DISTANCE_FROM = "distance_from";
    public static final String KEY_BEARING_TO = "bearing_to";
    public static final String KEY_PENDING_DELETION = "synced";
    private static DatabaseHandler sInstance = null;

    public static DatabaseHandler getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHandler(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Constructor should be private to prevent direct instantiation.
     * make call to static factory method "getInstance()" instead.
     */
    private DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    // public DatabaseHandler(Context context) {

    //     super(context, DATABASE_NAME, null, DATABASE_VERSION);
    // }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LOGIN_TABLE =
                "CREATE TABLE " + TABLE_LOGIN + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_EMAIL + " TEXT UNIQUE,"
                + KEY_UID + " TEXT,"
                + KEY_CREATED_AT + " TEXT" + ")";
        db.execSQL(CREATE_LOGIN_TABLE);
        String CREATE_FRIEND_TABLE =
                "CREATE TABLE " + TABLE_FRIENDS + "("
                + KEY_FID + " INTEGER PRIMARY KEY autoincrement,"
                + KEY_USERID + " TEXT,"
                + KEY_NAME + " TEXT,"
                + KEY_EMAIL + " TEXT,"
                + KEY_UID + " TEXT" + ")";
        db.execSQL(CREATE_FRIEND_TABLE);
        String CREATE_CHALLENGE_TABLE =
                "CREATE TABLE " + TABLE_CHALLENGE + "("
                + KEY_CID + " INTEGER PRIMARY KEY autoincrement,"
                + KEY_USERID + " TEXT,"
                + KEY_CHALLENGE_ID + " TEXT,"
                + KEY_NAME + " TEXT,"
                + KEY_CREATED_BY + " TEXT,"
                + KEY_LONGITUDE + " TEXT,"
                + KEY_LATITUDE + " TEXT,"
                + KEY_ACCEPTED_AT + " TEXT,"
                + KEY_TIME_TO_EXPIRE + " TEXT,"
                + KEY_SYNC_STATUS + " TEXT,"
                + KEY_EXPIRES_IN + " TEXT,"
                + KEY_DISTANCE_FROM + " TEXT,"
                + KEY_BEARING_TO + " TEXT,"
                + KEY_PENDING_DELETION + " TEXT" + ")";
        db.execSQL(CREATE_CHALLENGE_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGIN);

        // Create tables again
        onCreate(db);
    }

    /**
     * Storing user details in database
     * */
    public void addUser(String name, String email, String uid, String created_at) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name); // Name
        values.put(KEY_EMAIL, email); // Email
        values.put(KEY_UID, uid); // Email
        values.put(KEY_CREATED_AT, created_at); // Created At

        // Inserting Row
        if (db!=null){
            db.insert(TABLE_LOGIN, null, values);

        }
    }

    /**
     * Storing friend details in database
     * */
    public void addFriend(String name, String email, String unique_id, String user_id) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_USERID, user_id); // Logged in User ID
        values.put(KEY_NAME, name); // Name
        values.put(KEY_EMAIL, email); // Email
        values.put(KEY_UID, unique_id); // UID

        // Inserting Row
        if (db!=null){
            db.insert(TABLE_FRIENDS, null, values);

        }
    }

    /**
     * Syncing friend details between databases
     * */
    public void updateFriends(String values, String user_id) {
        // Clear current friend list from local database
        String friendRemoveQuery = "DELETE FROM " + TABLE_FRIENDS + " WHERE " + KEY_USERID + " = '" + user_id + "'";
        SQLiteDatabase db = this.getWritableDatabase();
        if (db!=null){
            db.execSQL(friendRemoveQuery);

        }
        String friendUpdateQuery = "INSERT INTO " + TABLE_FRIENDS + "(" + KEY_USERID + ", " + KEY_NAME + ", " + KEY_EMAIL + ", " + KEY_UID + ") VALUES" + values;
        SQLiteDatabase db2 = this.getWritableDatabase();
        if (db2!=null){
            db2.execSQL(friendUpdateQuery);

        }
    }

    /**
     * Syncing challenge details between databases
     * */
    public void updateChallenges(String values, String user_id) {
                // Update current challenge list from local database
//        String challengeRemoveQuery = "DELETE FROM " + TABLE_CHALLENGE + " WHERE " + KEY_USERID + " = '" + user_id + "'";
//        SQLiteDatabase db = this.getWritableDatabase();
//        if (db!=null){
//            db.execSQL(challengeRemoveQuery);
//        }
        String challengeUpdateQuery = "INSERT INTO " + TABLE_CHALLENGE + "(" + KEY_USERID + ", " + KEY_CHALLENGE_ID +", " + KEY_NAME + ", " + KEY_CREATED_BY + ", " + KEY_LONGITUDE + ", " + KEY_LATITUDE + ", " + KEY_ACCEPTED_AT + "," + KEY_TIME_TO_EXPIRE + ") VALUES" + values;
        SQLiteDatabase db2 = this.getWritableDatabase();
        if (db2!=null){
            db2.execSQL(challengeUpdateQuery);
        }
    }

    public Cursor getChallengeIDs(String user_id) {

        String challengeQuery = "SELECT " + KEY_CHALLENGE_ID + " FROM " + TABLE_CHALLENGE + " WHERE " + KEY_USERID + " = '" + user_id + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery(challengeQuery, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }

        return mCursor;

    }
    public void syncChallenges(String challenge_id, String user_id) {
        String challengeUpdateQuery = "UPDATE " + TABLE_CHALLENGE + " SET " + KEY_SYNC_STATUS + " = '1' ' WHERE " + KEY_USERID + " = '" + user_id + "' AND " + KEY_CHALLENGE_ID + " = '" + challenge_id + "'";
        SQLiteDatabase db2 = this.getWritableDatabase();
        if (db2!=null){
            db2.execSQL(challengeUpdateQuery);
        }
    }

    public void updateDistanceFrom(String user_id, String challenge_id, String distance_to_point, String bearing_to_point, String expiration_time) {
        // Update current Distance from goal in local database
        String updateChallengeDistance = "UPDATE " + TABLE_CHALLENGE + " SET " + KEY_DISTANCE_FROM + " = '" + distance_to_point + "', " + KEY_BEARING_TO + " = '" + bearing_to_point + "', " + KEY_EXPIRES_IN + " = '" + expiration_time + "' WHERE " + KEY_USERID + " = '" + user_id + "' AND " + KEY_CHALLENGE_ID + " = '" + challenge_id + "'";
        SQLiteDatabase db = this.getWritableDatabase();
        if (db!=null){
            db.execSQL(updateChallengeDistance);

        }
    }

    public void acceptChallenge(String user_id, String challenge_id, String accepted_at, String expires) {
        // Accept current challenge in local database
        String updateChallengeAcceptance = "UPDATE " + TABLE_CHALLENGE + " SET " + KEY_ACCEPTED_AT + " = '" + accepted_at + "', " + KEY_TIME_TO_EXPIRE + " = '" + expires + "' WHERE " + KEY_USERID + " = '" + user_id + "' AND " + KEY_CHALLENGE_ID + " = '" + challenge_id + "'";
        SQLiteDatabase db = this.getWritableDatabase();
        if (db!=null){
            db.execSQL(updateChallengeAcceptance);

        }
    }
    public HashMap<String, String> fetchSyncableChallenges(String user_id) {
        HashMap<String,String> CID = new HashMap<String,String>();
        // Clear a challenge from local database
        SQLiteDatabase db = this.getReadableDatabase();
        String getSyncable = "SELECT " + KEY_CHALLENGE_ID + " WHERE " + KEY_USERID + " = '" + user_id + "' AND " + KEY_PENDING_DELETION + " = 'true'";
        if (db!=null){
            Cursor cursor = db.rawQuery(getSyncable, null);
            // Move to first row
            cursor.moveToFirst();
            if(cursor.getCount() > 0){
                CID.put("cid", cursor.getString(0));
            }
        }
        // return CID
        return CID;
    }

    public void deleteChallenge(String user_id, String challenge_id) {
        // Clear a challenge from local database
        SQLiteDatabase db = this.getWritableDatabase();
        if (db!=null){
            db.delete(TABLE_CHALLENGE, KEY_USERID+"="+user_id+" and "+KEY_CHALLENGE_ID+"="+challenge_id, null);
        }
    }

    /**
     * Getting user data from database
     * */
    public HashMap<String, String> getUserDetails(){
        HashMap<String,String> user = new HashMap<String,String>();
        String selectQuery = "SELECT  * FROM " + TABLE_LOGIN;

        SQLiteDatabase db = this.getReadableDatabase();
        if (db!=null){
            Cursor cursor = db.rawQuery(selectQuery, null);
            // Move to first row
            cursor.moveToFirst();
            if(cursor.getCount() > 0){
                user.put("name", cursor.getString(1));
                user.put("email", cursor.getString(2));
                user.put("uid", cursor.getString(3));
                user.put("created_at", cursor.getString(4));
            }

        }
        // return user
        return user;
    }

    /**
     * Getting UID for logged in user from database
     * */
    public HashMap<String, String> getUID(){
        HashMap<String,String> UID = new HashMap<String,String>();
        String selectQuery = "SELECT  * FROM " + TABLE_LOGIN;
        SQLiteDatabase db8 = this.getReadableDatabase();
        Cursor cursor = db8.rawQuery(selectQuery, null);
        // Move to first row
        cursor.moveToFirst();
        if(cursor.getCount() > 0){
            UID.put("uid", cursor.getString(3));
        }
        // return UID
        return UID;
    }
    /**
     * Getting user login status
     * return true if rows are there in table
     * */
    public int getRowCount() {
        String countQuery = "SELECT  * FROM " + TABLE_LOGIN;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int rowCount = cursor.getCount();


        // return row count
        return rowCount;
    }

    public int getChallengeRowCount() {
        String countQuery = "SELECT  * FROM " + TABLE_CHALLENGE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int rowCount = cursor.getCount();


        // return row count
        return rowCount;
    }

    /**
     * Re create database
     * Delete all tables and create them again
     * */
    public void resetTables(){
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        if (db!=null){
        db.delete(TABLE_LOGIN, null, null);

        }
    }
    public Cursor fetchAllFriends(String user_id) {

        String friendQuery = "SELECT * FROM " + TABLE_FRIENDS + " WHERE " + KEY_USERID + " = '" + user_id + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery(friendQuery, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }

        return mCursor;
    }
    public Cursor fetchSingleFriend(String friend) {

        String friendQuery = "SELECT " + KEY_UID + " FROM " + TABLE_FRIENDS + " WHERE " + KEY_NAME + " = '" + friend + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery(friendQuery, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }

        return mCursor;
    }
    public Cursor fetchAllChallenges(String user_id) {

        String challengeQuery = "SELECT * FROM " + TABLE_CHALLENGE + " WHERE " + KEY_USERID + " = '" + user_id + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery(challengeQuery, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }

        return mCursor;

    }

}
