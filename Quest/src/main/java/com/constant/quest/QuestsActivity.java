package com.constant.quest;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    static String syncableChallengeID;
    static String visibleExp;
    static String visibleDist;
    static String visibleBear;
    static String photo;
    float visBear;
    final float[] results= new float[3];
    float[] mGravity;
    float[] mGeomagnetic;
    Uri selectedImage;


    Bitmap bmpOriginal;
    Bitmap bmpOriginal2;

    SimpleCursorAdapter dataAdapter;

    private static String friendsURL = "http://caching.elasticbeanstalk.com:80";
    private AmazonS3Client s3Client = new AmazonS3Client(
            new BasicAWSCredentials(Constants.ACCESS_KEY_ID,
                    Constants.SECRET_KEY));


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
        Button btnSync = (Button)v.findViewById(R.id.btnSync);
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
//        syncRemLoc();
        startUpdateListView();
        // Start Listener for Log-out button
        btnLogout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(getActivity(),
                LogOutActivity.class);
                startActivity(i);
            }
        });
        btnSync.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                syncRemLoc();
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
        // TODO highlight selected challenge
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
                // Get the cursor, positioned to the corresponding row in the result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);
                // Get the challenges ID from this row in the database.
                selectChallenge = cursor.getString(cursor.getColumnIndexOrThrow("challenge_id"));
                String startDate = cursor.getString(cursor.getColumnIndexOrThrow("accepted_at"));
                // get accept_reject.xml view
                LayoutInflater li = LayoutInflater.from(getActivity());
                View decisionView = li.inflate(R.layout.accept_reject, null);
                View decisionView2 = li.inflate(R.layout.reject, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getActivity());
                // set accept_reject.xml to alert dialog builder
                if (startDate != null && startDate .equals("")) {
                    alertDialogBuilder.setView(decisionView);
                }
                else {
                    alertDialogBuilder.setView(decisionView2);
                }
                if (startDate != null && startDate .equals("")) {
                    // set dialog message
                    alertDialogBuilder
                            .setCancelable(true)
                            .setPositiveButton("Humbly Accept",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
                                            DatabaseHandler db3 = DatabaseHandler.getInstance(getActivity());
                                            db3.acceptChallenge(uid, selectChallenge, timeStamp, "24");
                                            dialog.cancel();
                                        }
                                    }
                            )
                            .setNegativeButton("Vehemently Reject",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            DatabaseHandler db9 = DatabaseHandler.getInstance(getActivity());
                                            db9.flagChallenges(uid, selectChallenge, "YES");
                                            dialog.cancel();
                                        }
                                    }
                            );
                }
                else {
                    // set dialog message
                    alertDialogBuilder
                            .setCancelable(true)
                            .setPositiveButton("Remove",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            DatabaseHandler db3 = DatabaseHandler.getInstance(getActivity());
                                            db3.flagChallenges(uid, selectChallenge, "YES");
                                            dialog.cancel();
                                        }
                                    }
                            )
                            .setNegativeButton("Cancel",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,int id) {
                                            dialog.cancel();
                                        }
                                    }
                            );
                }
                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                // show it
                alertDialog.show();
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
        if (isOnline()) {
            AsyncTask<Void, String, Void> task4 = new syncAsyncTask();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                task4.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                task4.execute();
        }
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
            // get list of current challenges
            String selected_id = "";
            DatabaseHandler db5 = DatabaseHandler.getInstance(getActivity());
            Cursor cursor = db5.getChallengeIDs(uid);
            int count = cursor.getCount();
            for (int i = 0;
                    i < count;
                    i++) {
                selected_id += (cursor.getString(cursor.getColumnIndex(DatabaseHandler.KEY_CHALLENGE_ID))) + ", ";
                cursor.moveToNext();
            }

            if (selected_id.startsWith("5")) {
                selected_id = selected_id.substring(0,selected_id.length()-2);
            }

            List<NameValuePair> params2 = new ArrayList<NameValuePair>();
            params2.add(new BasicNameValuePair("tag", "syncAdd_challenges"));
            params2.add(new BasicNameValuePair("user", uid));
            params2.add(new BasicNameValuePair("ID", selected_id));
            // Pass the list on to the server
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
            String deletion_id = "";
            DatabaseHandler db3 = DatabaseHandler.getInstance(getActivity());
            Cursor cursor2 = db3.getChallengeIDsForDeletion(uid);
            int count2 = cursor2.getCount();
            for (int i = 0;
                 i < count2;
                 i++) {
                deletion_id += (cursor2.getString(cursor2.getColumnIndex(DatabaseHandler.KEY_CHALLENGE_ID))) + ", ";
                cursor2.moveToNext();
            }

            if (deletion_id.startsWith("5")) {
                deletion_id = deletion_id.substring(0,deletion_id.length()-2);
            }


            List<NameValuePair> params3 = new ArrayList<NameValuePair>();
            params3.add(new BasicNameValuePair("tag", "syncRem_challenges"));
            params3.add(new BasicNameValuePair("user", uid));
            params3.add(new BasicNameValuePair("ID", deletion_id));
            // Pass the list on to the server
            JSONObject json3 = jsonParser.getJSONFromUrl(friendsURL, params3);
            // check for response
            try {
                if (json3.getString(KEY_SUCCESS) != null) {
                    String res = json3.getString(KEY_SUCCESS);
                    if(Integer.parseInt(res) == 1){
                        // Delete list of challenges pending deletion in SQLite Database
                        DatabaseHandler db4 = DatabaseHandler.getInstance(getActivity());
                        db4.deleteChallenges(uid);
                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            DatabaseHandler db6 = DatabaseHandler.getInstance(getActivity());
            Cursor cursor6 = db6.getHeldChallenges(uid);
            int count6 = cursor6.getCount();
            for (int i = 0;
                 i < count6;
                 i++) {
                    String tag = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_TAG))) + ", ";
                    String name = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_NAME))) + ", ";
                    String created_by = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_CREATED_BY))) + ", ";
                    String challenged = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_CHALLENGED))) + ", ";
                    String text = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_TEXT))) + ", ";
                    photo = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_PHOTO))) + ", ";
                    String photoURI = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_PHOTO_URI))) + ", ";
                    selectedImage = Uri.parse(photoURI);
                    String video = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_VIDEO))) + ", ";
                    String longitude = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_LONGITUDE))) + ", ";
                    String latitude = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_LATITUDE))) + ", ";
                    String expires_in = (cursor6.getString(cursor6.getColumnIndex(DatabaseHandler.KEY_EXPIRES_IN))) + ", ";
                    List<NameValuePair> params6 = new ArrayList<NameValuePair>();
                    params6.add(new BasicNameValuePair("tag", tag));
                    params6.add(new BasicNameValuePair("name", name));
                    params6.add(new BasicNameValuePair("created_by", created_by));
                    params6.add(new BasicNameValuePair("challenged", challenged));
                    params6.add(new BasicNameValuePair("text", text));
                    params6.add(new BasicNameValuePair("photo", photo));
                    params6.add(new BasicNameValuePair("video", video));
                    params6.add(new BasicNameValuePair("longitude", longitude));
                    params6.add(new BasicNameValuePair("latitude", latitude));
                    params6.add(new BasicNameValuePair("expires", expires_in));
                    startS3upload();
                    // getting JSON Object
                    JSONObject json4 = jsonParser.getJSONFromUrl(friendsURL, params6);
                    // check for challenge response
                    try {
                        if (json4.getString(KEY_SUCCESS) != null) {
                            String res = json4.getString(KEY_SUCCESS);
                            if(Integer.parseInt(res) == 1){
                                DatabaseHandler db7 = DatabaseHandler.getInstance(getActivity());
                                db7.deleteHeldChallenge(created_by, name);
                                Toast.makeText(getActivity(),
                                        "Quest created successfully",
                                        Toast.LENGTH_LONG).show();
                            }
                            else{
                                // Error in creating challenge
                                Toast.makeText(getActivity(),
                                        "Error creating Quest",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                 cursor6.moveToNext();
                 }
            return null;
        }
    }

    public void startUpdateListView() {
        AsyncTask<Void, String, Void> task3 = new UpdateAsyncTask();
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
                        Thread.sleep(100);
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
                                        // TODO create reward script
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
    public void startS3upload() {
        AsyncTask<Uri, Void, S3TaskResult> task6 = new S3PutObjectTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task6.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (selectedImage));
        else
            task6.execute(selectedImage);
    }
    private class S3PutObjectTask extends AsyncTask<Uri, Void, S3TaskResult> {


        ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getActivity()
                    .getString(R.string.uploading));
            dialog.setCancelable(false);
            dialog.show();
        }

        protected S3TaskResult doInBackground(Uri... uris) {

            if (uris == null || uris.length != 1) {
                return null;
            }

            // The file location of the image selected.
            Uri selectedImage = uris[0];

            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();

            S3TaskResult result = new S3TaskResult();

            // Put the image data into S3.
            try {
                // s3Client.createBucket(Constants.getPictureBucket());

                // Content type is determined by file extension.
                PutObjectRequest por = new PutObjectRequest(
                        Constants.getPictureBucket(), photo,
                        new java.io.File(filePath));
                s3Client.putObject(por);
            }
            catch (Exception exception) {
            }
            return result;
        }

        protected void onPostExecute(S3TaskResult result) {
            dialog.dismiss();
        }
    }

    private class S3TaskResult {
    }

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
