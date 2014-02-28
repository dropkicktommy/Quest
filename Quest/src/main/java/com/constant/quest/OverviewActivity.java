package com.constant.quest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;

import com.constant.quest.library.DatabaseHandler;
import com.constant.quest.library.JSONParser;
import com.constant.quest.library.UserFunctions;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OverviewActivity extends Activity {

    protected ConnectivityManager cm;

    private static String KEY_SUCCESS = "success";
    static String uid = "";



    private static String friendsURL = "http://caching.elasticbeanstalk.com:80";

    private JSONParser jsonParser;
    UserFunctions userFunctions;

    public OverviewActivity(){
        jsonParser = new JSONParser();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        /**
         * Dashboard Screen for the application
         * */
        // Check login status in database
        userFunctions = new UserFunctions();
        if(userFunctions.isUserLoggedIn(getApplicationContext())) {
            if(isOnline()) {
                updateLists();
            }
            Intent PageView = new Intent(getApplicationContext(), PageViewActivity.class);
            PageView.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(PageView);
            // Closing dashboard screen
            finish();
        }
        else{
            // user is not logged in show login screen
            Intent login = new Intent(getApplicationContext(), LoginActivity.class);
            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(login);
            // Closing dashboard screen
            finish();
        }
    }
    private void updateLists() {
        DatabaseHandler db = DatabaseHandler.getInstance(getApplicationContext());
        // DatabaseHandler db = new DatabaseHandler(getApplicationContext());
        StringBuilder sb = new StringBuilder(); {
            String UID = db.getUID().toString();
            sb.append(UID);
            sb.delete(0, 5);
            sb.deleteCharAt(sb.length()-1);
            uid = sb.toString();
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag", "list_friends"));
        params.add(new BasicNameValuePair("user", uid));
        // getting JSON Object
        JSONObject json = jsonParser.getJSONFromUrl(friendsURL, params);
        // check for challenge response
        try {
            if (json.getString(KEY_SUCCESS) != null) {
                String res = json.getString(KEY_SUCCESS);
                if(Integer.parseInt(res) == 1){

                    // Update list of friend details in SQLite Database
                    DatabaseHandler db2 = DatabaseHandler.getInstance(getApplicationContext());
                    // DatabaseHandler db2 = new DatabaseHandler(getApplicationContext());
                    JSONObject json_listFriends = json.getJSONObject("friend_list");
                    db2.updateFriends(json_listFriends.getString("values"), uid);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        List<NameValuePair> params2 = new ArrayList<NameValuePair>();
        params2.add(new BasicNameValuePair("tag", "list_challenges"));
        params2.add(new BasicNameValuePair("user", uid));
        // getting JSON Object
        JSONObject json2 = jsonParser.getJSONFromUrl(friendsURL, params2);
        // check for challenge response
        try {
            if (json2.getString(KEY_SUCCESS) != null) {
                String res = json2.getString(KEY_SUCCESS);
                if(Integer.parseInt(res) == 1){

                    // Update list of friend details in SQLite Database
                    DatabaseHandler db2 = DatabaseHandler.getInstance(getApplicationContext());
                    // DatabaseHandler db2 = new DatabaseHandler(getApplicationContext());
                    JSONObject json_listChallenges = json2.getJSONObject("challenge_list");
                    db2.updateChallenges(json_listChallenges.getString("values"), uid);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * Function get Online status
     * */
    public boolean isOnline() {

        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }
}
