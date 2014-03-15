package com.constant.quest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.constant.quest.library.DatabaseHandler;
import com.constant.quest.library.JSONParser;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class CreateActivity extends Fragment {


    CheckBox checkBoxPhoto;
    CheckBox checkBoxVideo;
    EditText inputChallengeName;
    EditText inputChallengeText;
    TextView latView;
    TextView lonView;
    TextView createErrorMsg;
    ListView listView2;

    protected ConnectivityManager cm;


    static String photoID;
    static String photoURI;
    static String longitude;
    static String longitudeC;
    static String longExp;
    static String latitude;
    static String latitudeC;
    static String latExp;
    static String uid = "";
    static String uid2 = "";

    Uri selectedImage;


    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in Milliseconds

    private AmazonS3Client s3Client = new AmazonS3Client(
            new BasicAWSCredentials(Constants.ACCESS_KEY_ID,
                    Constants.SECRET_KEY));

    private JSONParser jsonParser;
    private boolean mRunning = false;

    protected LocationManager locationManager;

    public CreateActivity(){
        jsonParser = new JSONParser();
    }

    public static final CreateActivity newInstance()
    {
        CreateActivity f = new CreateActivity();
        return f;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        View v = inflater.inflate(R.layout.create_challenge, container, false);
        Button btnCreate = (Button)v.findViewById(R.id.btnCreate);
        inputChallengeName = (EditText)v.findViewById(R.id.challengeName);
        inputChallengeText = (EditText)v.findViewById(R.id.challengeText);
        createErrorMsg = (TextView)v.findViewById(R.id.create_error);
        latView = (TextView)v.findViewById(R.id.latitude);
        lonView = (TextView)v.findViewById(R.id.longitude);
        listView2 = (ListView)v.findViewById(R.id.friendListView2);

        s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));

        displayListView();

        checkBoxPhoto = (CheckBox)v.findViewById(R.id.chkPhoto);
        checkBoxPhoto.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if (checkBoxPhoto.isChecked())
                {
                    openImageIntent();
                }
            }
        });
        checkBoxVideo = (CheckBox)v.findViewById(R.id.chkVideo);

        cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MINIMUM_TIME_BETWEEN_UPDATES,
                MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
                new MyLocationListener()
        );

        updateLocation();

        btnCreate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MINIMUM_TIME_BETWEEN_UPDATES,
                        MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
                        new MyLocationListener()
                );

                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(location != null) {
                    DatabaseHandler db = DatabaseHandler.getInstance(getActivity());
                    String name = inputChallengeName.getText().toString();
                    String text = inputChallengeText.getText().toString();
                    StringBuilder sb = new StringBuilder(); {
                        String UID = db.getUID().toString();
                        sb.append(UID);
                        sb.delete(0, 5);
                        sb.deleteCharAt(sb.length()-1);
                        uid = sb.toString();
                    }
                    String selected = "";
                    int cntChoice = listView2.getCount();
                    SparseBooleanArray sparseBooleanArray = listView2.getCheckedItemPositions();
                    for(int i = 0; i < cntChoice; i++){
                        if(sparseBooleanArray.get(i)) {
                            String selected_friend = listView2.getItemAtPosition(i).toString();
                            DatabaseHandler db8 = DatabaseHandler.getInstance(getActivity());
                            Cursor cursor2 = db8.fetchSingleFriend(selected_friend);
                            selected += (cursor2.getString(cursor2.getColumnIndex(DatabaseHandler.KEY_UID))) + ", ";
                        }
                    }
                    selected = selected.substring(0, selected.length() - 2);
                    if (checkBoxPhoto.isChecked()) {
                        long now = System.currentTimeMillis();
                        photoID = uid + "/" + now;
                        photoURI = selectedImage.toString();
                    }
                    else {
                        photoID = "";
                    }
                    String video = "";
                    if (checkBoxVideo.isChecked()) {
                        video = "true";
                    }
                    String longitude = String.format(
                        "%1$s",
                        location.getLongitude()
                    );
                    String latitude = String.format(
                        "%1$s",
                        location.getLatitude()
                    );
                    String expires = "24";
                    DatabaseHandler db12 = DatabaseHandler.getInstance(getActivity());
                    db12.holdChallenge(name, uid, selected, text, photoID, photoURI, video, longitude, latitude, expires);
                }
                else {
                    Toast.makeText(getActivity(),
                        "No GPS location available", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return v;
    }
    private class MyLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {
            String message = String.format(
                    "New Location \n Longitude: %1$s \n Latitude: %2$s",
                    location.getLongitude(), location.getLatitude()
            );
        }

        public void onStatusChanged(String s, int i, Bundle b) {
             Toast.makeText(getActivity(),
                     "Provider status changed",
                     Toast.LENGTH_LONG).show();
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
    public void displayListView() {
        DatabaseHandler db3 = DatabaseHandler.getInstance(getActivity());
        StringBuilder sb2 = new StringBuilder(); {
            String UID = db3.getUID().toString();
            sb2.append(UID);
            sb2.delete(0, 5);
            sb2.deleteCharAt(sb2.length()-1);
            uid2 = sb2.toString();
        }
        DatabaseHandler db6 = DatabaseHandler.getInstance(getActivity());
        Cursor cursor = db6.fetchAllFriends(uid2);
        ArrayList<String> names = new ArrayList<String>();
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            names.add(cursor.getString(cursor.getColumnIndex(DatabaseHandler.KEY_NAME))); //add the item
            cursor.moveToNext();
        }
        ArrayAdapter<String> adapter
                = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_list_item_multiple_choice,
                names);
        // Assign adapter to ListView
        listView2.setAdapter(adapter);
        listView2.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }
    private Uri outputFileUri;
    private static final int YOUR_SELECT_PICTURE_REQUEST_CODE = 232;

    private void openImageIntent() {

    // Determine Uri of camera image to save.
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "MyDir" + File.separator);
        root.mkdirs();
        final String fname = System.currentTimeMillis()+"";
        final File sdImageMainDirectory = new File(root, fname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera.
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getActivity().getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

        startActivityForResult(chooserIntent, YOUR_SELECT_PICTURE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == Activity.RESULT_OK)
        {
            if(requestCode == YOUR_SELECT_PICTURE_REQUEST_CODE)
            {
                final boolean isCamera;
                if(data == null)
                {
                    isCamera = true;
                }
                else
                {
                    final String action = data.getAction();
                    if(action == null)
                    {
                        isCamera = false;
                    }
                    else
                    {
                        isCamera = action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                    }
                }

                if(isCamera)
                {
                    selectedImage = outputFileUri;
                }
                else
                {
                    selectedImage = data == null ? null : data.getData();
                }
            }
        }
    }
    public void updateLocation() {
        AsyncTask<Void, String, Void> my_task = new UpdateAsyncTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            my_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            my_task.execute();
    }

    private class UpdateAsyncTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mRunning = true;
            while (mRunning) {


                while (!isCancelled()) {
                    try {
                        Thread.sleep(10);
                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            longitude = location.convert(location.getLongitude(), location.FORMAT_SECONDS);
                            longitudeC = longitude.replace(':', ' ');
                            StringBuilder longitudeTMP = new StringBuilder(); {
                                longitudeTMP.append(longitudeC);
                                if (String.valueOf(longitudeTMP.charAt(0)).equals("-")) {
                                    longitudeTMP.deleteCharAt(0);
                                    longitudeTMP.append(" W");
                                }
                                else {
                                    longitudeTMP.append(" E");
                                }
                                if (String.valueOf(longitudeTMP.charAt(1)).equals(" ")) {
                                    longitudeTMP.insert(0, ' ');
                                }
                                longitudeTMP.insert(2, '\u00B0');
                                if (String.valueOf(longitudeTMP.charAt(5)).equals(" ")) {
                                    longitudeTMP.insert(4, ' ');
                                }
                                longitudeTMP.insert(6, '\u2032');
                                if (String.valueOf(longitudeTMP.charAt(9)).equals(".")) {
                                    longitudeTMP.insert(7, ' ');
                                }
                                longitudeTMP.insert(10, '\u2033');
                                longitudeTMP.delete(11, longitudeTMP.length()-2);
                                longExp = longitudeTMP.toString();
                            }


                            latitude = location.convert(location.getLatitude(), location.FORMAT_SECONDS);
                            latitudeC = latitude.replace(':', ' ');
                            StringBuilder latitudeTMP = new StringBuilder(); {
                                latitudeTMP.append(latitudeC);
                                if (String.valueOf(latitudeTMP.charAt(0)).equals("-")) {
                                    latitudeTMP.deleteCharAt(0);
                                    latitudeTMP.append(" S");
                                }
                                else {
                                    latitudeTMP.append(" N");
                                }
                                if (String.valueOf(latitudeTMP.charAt(1)).equals(" ")) {
                                    latitudeTMP.insert(0, ' ');
                                }
                                latitudeTMP.insert(2, '\u00B0');
                                if (String.valueOf(latitudeTMP.charAt(5)).equals(" ")) {
                                    latitudeTMP.insert(4, ' ');
                                }
                                latitudeTMP.insert(6, '\u2032');
                                if (String.valueOf(latitudeTMP.charAt(9)).equals(".")) {
                                    latitudeTMP.insert(7, ' ');
                                }
                                latitudeTMP.insert(10, '\u2033');
                                latitudeTMP.delete(11, latitudeTMP.length()-2);
                                latExp = latitudeTMP.toString();
                            }
                        }
                    }

                    catch (InterruptedException e) {

                    }
                    publishProgress();
                }

            }
            return null;

        }
        @Override
        protected void onProgressUpdate(String... result) {
            super.onProgressUpdate(result);
            updateLocationResult();
        }
    }

    public void close() {
        mRunning = false;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
    }
    public void updateLocationResult(){
        lonView.setText(longExp);
        latView.setText(latExp);

    }
}