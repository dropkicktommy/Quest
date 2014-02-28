package com.constant.quest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.constant.quest.library.UserFunctions;


/**
 * Created with IntelliJ IDEA.
 * User: tony
 * Date: 5/18/13
 * Time: 10:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class LogOutActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);{
            try {
                // QuestsActivity.task.cancel(true);
                Thread.sleep(500);
                UserFunctions userFunction = new UserFunctions();
                userFunction.logoutUser(getApplicationContext());
                Intent login = new Intent(getApplicationContext(), LoginActivity.class);
                login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(login);
                // Closing dashboard screen
                finish();
            }
            catch (InterruptedException e) {
            }
        }
    }
}
