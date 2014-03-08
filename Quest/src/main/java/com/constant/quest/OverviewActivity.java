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
}
