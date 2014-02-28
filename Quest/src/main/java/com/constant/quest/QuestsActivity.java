package com.constant.quest;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.constant.quest.library.DatabaseHandler;
import com.constant.quest.library.JSONParser;
import com.constant.quest.library.UserFunctions;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class QuestsActivity extends Fragment {

    ListView listView;
    TextView bearing;
    TextView textViewClock;
    TextView textViewDistance;
    ImageView imgViewBearing;
    ImageView imgViewNeedle;

    Display display;
    int displayCorrection;

    protected ConnectivityManager cm;

    private static String KEY_SUCCESS = "success";
    public String distance_to_point = "";
    static String uid = "";
    static String expiration_time = "";
    static String selectChallenge;
    static String visibleExp;
    static String visibleDist;
    static String visibleBear;
    float visBear;
    final float[] results= new float[3];
    float[] mGravity;
    float[] mGeomagnetic;

    Bitmap bmpOriginal;
    Bitmap bmpOriginal2;

    SimpleCursorAdapter dataAdapter;

    public AsyncTask<Void, String, Void> task3 = new UpdateAsyncTask();
    public AsyncTask<Void, String, Void> task4 = new UpdateAsyncTask();

    private static String friendsURL = "http://caching.elasticbeanstalk.com:80";

    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in Milliseconds

    private JSONParser jsonParser;
    private boolean mRunning = false;

    protected LocationManager locationManager;
    protected SensorManager mSensorManager;
    protected WindowManager windowManager;

    public QuestsActivity(){
        jsonParser = new JSONParser();
    }

    UserFunctions userFunctions;

    public static QuestsActivity newInstance()
    {
        QuestsActivity f = new QuestsActivity();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        // Set up views
        View v = inflater.inflate(R.layout.welcome, container, false);
        listView = (ListView)v.findViewById(R.id.challengeListView);
        bearing = (TextView)v.findViewById(R.id.bearing);
        textViewClock = (TextView)v.findViewById(R.id.textViewClock);
        textViewDistance = (TextView)v.findViewById(R.id.textViewDistance);
        imgViewBearing = (ImageView)v.findViewById(R.id.imgViewBearing);
        imgViewNeedle = (ImageView)v.findViewById(R.id.imgViewNeedle);
        Button btnLogout = (Button)v.findViewById(R.id.btnLogout);
        // Set up Resources
        userFunctions = new UserFunctions();
        bmpOriginal = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.bearing3);
        bmpOriginal2 = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.needle);
        // Set up Listeners and Managers
        locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
        cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        windowManager = (WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE);
        // Compensation values for screen rotation
        display = windowManager.getDefaultDisplay();
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                displayCorrection = 0;
                break;
            case Surface.ROTATION_90:
                displayCorrection = 90;
                break;
            case Surface.ROTATION_180:
                displayCorrection = 180;
                break;
            case Surface.ROTATION_270:
                displayCorrection = 270;
                break;
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MINIMUM_TIME_BETWEEN_UPDATES,
                MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
                new MyLocationListener()
        );
        syncRemLoc();
        startUpdateListView();
        // Start Listener for Log-out button
        btnLogout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(getActivity(),
                LogOutActivity.class);
                startActivity(i);
            }
        });
        return v;
    }
    // Update information in the Challenge List
    private void updateDataAdapter() {
        DatabaseHandler db4 = DatabaseHandler.getInstance(getActivity());
        StringBuilder sb = new StringBuilder(); {
            String UID = db4.getUID().toString();
            sb.append(UID);
            sb.delete(0, 5);
            sb.deleteCharAt(sb.length()-1);
            uid = sb.toString();
        }
        DatabaseHandler db5 = DatabaseHandler.getInstance(getActivity());
        Cursor cursor = db5.fetchAllChallenges(uid);
        // The desired columns to be bound
        String[] columns = new String[] {
            DatabaseHandler.KEY_NAME,
            DatabaseHandler.KEY_CREATED_BY,
            DatabaseHandler.KEY_DISTANCE_FROM
        };
        // the XML defined views which the data will be bound to
        int[] to = new int[] {
            R.id.name,
            R.id.created_by,
            R.id.distance,
        };
            // create the adapter using the cursor pointing to the desired data
            //as well as the layout information
        dataAdapter = new SimpleCursorAdapter(
            getActivity(), R.layout.challenge_info,
            cursor,
            columns,
            to);
            // Assign adapter to ListView
        Parcelable state = listView.onSaveInstanceState();
        listView.setAdapter(dataAdapter);
        listView.onRestoreInstanceState(state);

        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
                // Get the cursor, positioned to the corresponding row in the result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);
                // Get the challenges ID from this row in the database.
                selectChallenge = cursor.getString(cursor.getColumnIndexOrThrow("challenge_id"));
                String startDate = cursor.getString(cursor.getColumnIndexOrThrow("accepted_at"));
                if(isOnline()) {
                    if (startDate != null && startDate .equals("")) {
                        List<NameValuePair> params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair("tag", "accept_challenge"));
                        params.add(new BasicNameValuePair("user", uid));
                        params.add(new BasicNameValuePair("challengeID", selectChallenge));
                        // getting JSON Object
                        JSONObject json = jsonParser.getJSONFromUrl(friendsURL, params);
                        // check for response
                        try {
                            if (json.getString(KEY_SUCCESS) != null) {
                                String res = json.getString(KEY_SUCCESS);
                                if(Integer.parseInt(res) == 1){
                                    // challenge successfully accepted
                                    // Store details in SQLite Database
                                    DatabaseHandler db3 = DatabaseHandler.getInstance(getActivity());
                                    JSONObject json_accept = json.getJSONObject("challenge");
                                    db3.acceptChallenge(uid, selectChallenge, json_accept.getString("accepted_at"), json_accept.getString("expires"));
                                }
                            }
                        }
                        catch (JSONException e) {
                        e.printStackTrace();
                        }
                    }
                }
                else {
                    Toast.makeText(getActivity(),
                    "No internet Connection available", Toast.LENGTH_SHORT).show();
                }
            }

        });
    }

    private class MyLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {
        }
        public void onStatusChanged(String s, int i, Bundle b) {
        }
        public void onProviderDisabled(String s) {
             Toast.makeText(getActivity(),
                     "Provider disabled by the user. GPS turned off",
                     Toast.LENGTH_LONG).show();
        }
        public void onProviderEnabled(String s) {
             Toast.makeText(getActivity(),
                     "Provider enabled by the user. GPS turned on",
                     Toast.LENGTH_LONG).show();
        }
    }
    public void syncRemLoc() {
        new syncAsyncTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task4.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task4.execute();
    }
    private class syncAsyncTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            DatabaseHandler db = DatabaseHandler.getInstance(getActivity());
            // DatabaseHandler db = new DatabaseHandler(getApplicationContext());
            StringBuilder sb = new StringBuilder(); {
                String UID = db.getUID().toString();
                sb.append(UID);
                sb.delete(0, 5);
                sb.deleteCharAt(sb.length()-1);
                uid = sb.toString();
            }
            List<NameValuePair> params2 = new ArrayList<NameValuePair>();
            params2.add(new BasicNameValuePair("tag", "syncAdd_challenges"));
            params2.add(new BasicNameValuePair("user", uid));
            // getting JSON Object
            JSONObject json2 = jsonParser.getJSONFromUrl(friendsURL, params2);
            // check for response
            try {
                if (json2.getString(KEY_SUCCESS) != null) {
                    String res = json2.getString(KEY_SUCCESS);
                    if(Integer.parseInt(res) == 1){
                        // Update list of challenges in SQLite Database
                        DatabaseHandler db2 = DatabaseHandler.getInstance(getActivity());
                        JSONObject json_listChallenges = json2.getJSONObject("challenge_list");
                        db2.updateChallenges(json_listChallenges.getString("values"), uid);
                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            DatabaseHandler db3 = DatabaseHandler.getInstance(getActivity());
            String cid = db3.fetchSyncableChallanges(uid).toString();
            List<NameValuePair> params3 = new ArrayList<NameValuePair>();
            params3.add(new BasicNameValuePair("tag", "syncRem_challenges"));
            params3.add(new BasicNameValuePair("user", uid));
            params3.add(new BasicNameValuePair("cid", cid));
            // getting JSON Object
            JSONObject json3 = jsonParser.getJSONFromUrl(friendsURL, params3);
            // check for response
            try {
                if (json3.getString(KEY_SUCCESS) != null) {
                    String res = json3.getString(KEY_SUCCESS);
                    if(Integer.parseInt(res) == 1){
                        // Update list of challenges in local Database to reflect deletions
                        DatabaseHandler db4 = DatabaseHandler.getInstance(getActivity());
                        db4.deleteChallenge(uid, cid);
                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    public void startUpdateListView() {
        new UpdateAsyncTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task3.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task3.execute();
    }
    private class UpdateAsyncTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            mRunning = true;
            while (mRunning) {
                while (!isCancelled()) {
                    try {
                        Thread.sleep(10);
                        DatabaseHandler db0 = DatabaseHandler.getInstance(getActivity());
                        StringBuilder sb = new StringBuilder(); {
                            String UID = db0.getUID().toString();
                            sb.append(UID);
                            sb.delete(0, 5);
                            sb.deleteCharAt(sb.length()-1);
                            uid = sb.toString();
                        }
                        DatabaseHandler db1 = DatabaseHandler.getInstance(getActivity());
                        int count = db1.getChallengeRowCount();
                        if(count > 0){
                            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                String current_longitude = String.format(
                                        "%1$s",
                                        location.getLongitude()
                                );
                                double current_long = Double.parseDouble(current_longitude);
                                String current_latitude = String.format(
                                        "%1$s",
                                        location.getLatitude()
                                );
                                double current_lat = Double.parseDouble(current_latitude);
                                DatabaseHandler db = DatabaseHandler.getInstance(getActivity());
                                Cursor cursor = db.fetchAllChallenges(uid);
                                cursor.moveToFirst();
                                while(!cursor.isAfterLast()) {
                                    String challenge_id = cursor.getString(cursor.getColumnIndex(DatabaseHandler.KEY_CHALLENGE_ID));
                                    String destination_longitude = cursor.getString(cursor.getColumnIndex(DatabaseHandler.KEY_LONGITUDE)); //add the item
                                    double dest_long = Double.parseDouble(destination_longitude);
                                    String destination_latitude = cursor.getString(cursor.getColumnIndex(DatabaseHandler.KEY_LATITUDE)); //add the item
                                    double dest_lat = Double.parseDouble(destination_latitude);
                                    Location.distanceBetween(current_lat, current_long, dest_lat, dest_long, results);
                                    double accuracy = location.getAccuracy();
                                    if (results[0] <= accuracy) {
                                        distance_to_point = "0 ft";
                                    }
                                    else {
                                        double corrected_results = results[0] - accuracy;
                                        double feet_to_point = corrected_results*3.2808;
                                        if (feet_to_point <= 530) {
                                            distance_to_point = String.format("%.0f", feet_to_point) + " ft";
                                        }
                                        else {
                                            double miles_to_point = feet_to_point/5280.0;
                                            distance_to_point = String.format("%.1f", miles_to_point) + " mi";
                                        }
                                    }
                                    String bearing_to_point = Float.toString(((results[1]) + 360) % 360);
                                    String accepted_at = cursor.getString(cursor.getColumnIndex(DatabaseHandler.KEY_ACCEPTED_AT));
                                    String _time_to_expire = cursor.getString(cursor.getColumnIndex(DatabaseHandler.KEY_TIME_TO_EXPIRE));
                                    int time_to_expire = 0;
                                    try {
                                        time_to_expire = Integer.parseInt(_time_to_expire);
                                    }
                                    catch(NumberFormatException nfe) {
                                    }
                                    if (accepted_at != null && accepted_at .equals("")) {
                                        distance_to_point = "Begin Quest";
                                        expiration_time = "";
                                    }

                                    else {
                                        long now = System.currentTimeMillis();
                                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
                                        format.setTimeZone(TimeZone.getTimeZone("UTC"));
                                        Date d1 = null;
                                        Date d2 = null;
                                        try {
                                            d1 = new Date (now);
                                            d2 = format.parse(accepted_at);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.setTime(d2);
                                        calendar.add(Calendar.HOUR, time_to_expire);

                                        long difference = calendar.getTimeInMillis() - d1.getTime();
                                        long seconds = difference / 1000;
                                        long minutes = seconds / 60;
                                        long hours = minutes / 60;
                                        long days = hours / 24;

                                        long tMinutes = minutes - hours * 60;
                                        long tHours = hours - days * 24;
                                        if (days > 0) {
                                            expiration_time = Long.toString(days) + " days, " +
                                                    Long.toString(tHours) + " hrs, " +
                                                    Long.toString(tMinutes) + " mins";
                                        }
                                        else if (tHours > 0) {
                                                expiration_time = Long.toString(tHours) + " hrs, " +
                                                        Long.toString(tMinutes) + " mins";
                                        }
                                        else {
                                        expiration_time = Long.toString(tMinutes) + " mins";
                                        }
                                        if (difference <=0) {
                                            distance_to_point = "Quest Failed";
                                            expiration_time = "";
                                        }
                                    }
                                    if (selectChallenge != null && challenge_id != null && challenge_id .equals(selectChallenge)) {
                                        visibleExp = expiration_time;
                                        visibleDist = distance_to_point;
                                        visibleBear = bearing_to_point;
                                        visBear = ((results[1]) + 360) % 360;
                                    }
                                    DatabaseHandler db2 = DatabaseHandler.getInstance(getActivity());
                                    db2.updateDistanceFrom(uid, challenge_id, distance_to_point, bearing_to_point, expiration_time);
                                    cursor.moveToNext();
                                }
                            }
                        }
                        publishProgress();
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(String... result) {
            super.onProgressUpdate(result);
            updateDataAdapter();
            updateInfo();
        }
    }
    public void updateInfo() {
        textViewClock.setText(visibleExp);
        textViewDistance.setText(visibleDist);
        bearing.setText("Bearing: " + visibleBear);
    }
    /**
     * Function get Online status
     * */
    public boolean isOnline() {
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }
    private final SensorEventListener mSensorListener = new SensorEventListener(){
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Get Accelerometer and Magnetometer values
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
                        mGeomagnetic);
                if (success) {
                    // Calculate Compass rotation
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    float azimuthInRadians = orientation[0];
                    float azimuthInDegrees = (((float)Math.toDegrees(azimuthInRadians)+360) + displayCorrection) %360;
                    // Draw Compass ring
                    Bitmap bmResult = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas tempCanvas = new Canvas(bmResult);
                    tempCanvas.rotate(-azimuthInDegrees, bmpOriginal.getWidth()/2, bmpOriginal.getHeight()/2);
                    tempCanvas.drawBitmap(bmpOriginal, 0, 0, null);
                    imgViewBearing.setImageBitmap(bmResult);
                    // Draw Compass needle
                    if (visBear != 0) {
                        Bitmap bmResult2 = Bitmap.createBitmap(bmpOriginal2.getWidth(), bmpOriginal2.getHeight(), Bitmap.Config.ARGB_8888);
                        Canvas tempCanvas2 = new Canvas(bmResult2);
                        tempCanvas2.rotate(-azimuthInDegrees + visBear, bmpOriginal2.getWidth()/2, bmpOriginal2.getHeight()/2);
                        tempCanvas2.drawBitmap(bmpOriginal2, 0, 0, null);
                        imgViewNeedle.setImageBitmap(bmResult2);
                    }
                }
            }
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSensorListener);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mRunning = false;
    }
}
