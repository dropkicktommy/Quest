package com.constant.quest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.constant.quest.library.DatabaseHandler;
import com.constant.quest.library.JSONParser;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class FriendsActivity extends Fragment {

    TextView friendsErrorMsg;
    ListView listView;

    // JSON Response node names
    private static String KEY_SUCCESS = "success";
    private static String KEY_ERROR = "error";
    private static String KEY_ERROR_MSG = "error_msg";
    private static String KEY_NAME = "name";
    private static String KEY_EMAIL = "email";
    private static String KEY_UNIQUE_ID = "unique_id";
    private static String new_friend_tag = "new_friend";
    private static String friendsURL = "http://caching.elasticbeanstalk.com:80";
    static String uid = "";
    static String uid2 = "";

    private DatabaseHandler dbHelper;
    private SimpleCursorAdapter dataAdapter;
    private JSONParser jsonParser;

    public FriendsActivity(){
        jsonParser = new JSONParser();
    }

    public static final FriendsActivity newInstance()
    {
        FriendsActivity f = new FriendsActivity();
        return f;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.friends, container, false);
        Button btnAddFriends = (Button)v.findViewById(R.id.btnAddFriends);
        friendsErrorMsg = (TextView)v.findViewById(R.id.friends_error);
        listView = (ListView)v.findViewById(R.id.friendListView);
        displayListView();
        btnAddFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // get add_friends.xml view
                LayoutInflater li = LayoutInflater.from(getActivity());
                View addFriendsView = li.inflate(R.layout.add_friends, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getActivity());
                // set add_friends.xml to alertdialog builder
                alertDialogBuilder.setView(addFriendsView);
                final EditText newEmail = (EditText) addFriendsView
                        .findViewById(R.id.newEmail);
                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        // get user input and set it to result
                                        DatabaseHandler db = DatabaseHandler.getInstance(getActivity());
                                        // DatabaseHandler db = new DatabaseHandler(getActivity());
                                        String friends_email = newEmail.getText().toString();
                                        StringBuilder sb = new StringBuilder(); {
                                            String UID = db.getUID().toString();
                                            sb.append(UID);
                                            sb.delete(0, 5);
                                            sb.deleteCharAt(sb.length()-1);
                                            uid = sb.toString();
                                        }
                                        List<NameValuePair> params = new ArrayList<NameValuePair>();
                                        params.add(new BasicNameValuePair("tag", new_friend_tag));
                                        params.add(new BasicNameValuePair("user", uid));
                                        params.add(new BasicNameValuePair("email", friends_email));
                                        // getting JSON Object
                                        JSONObject json = jsonParser.getJSONFromUrl(friendsURL, params);
                                        // check for challenge response
                                        try {
                                            if (json.getString(KEY_SUCCESS) != null) {
                                                friendsErrorMsg.setText("");
                                                String res = json.getString(KEY_SUCCESS);
                                                if(Integer.parseInt(res) == 1){
                                                    friendsErrorMsg.setText("Journeyman added Successfully!!!");
                                                    // Friend successfully added
                                                    // Store friend details in SQLite Database
                                                    DatabaseHandler db2 = DatabaseHandler.getInstance(getActivity());
                                                    // DatabaseHandler db2 = new DatabaseHandler(getActivity());
                                                    JSONObject json_friend = json.getJSONObject("friend");
                                                    db2.addFriend(json_friend.getString(KEY_NAME), json_friend.getString(KEY_EMAIL), json_friend.getString(KEY_UNIQUE_ID), uid);
                                                    displayListView();
                                                }
                                                else {
                                                    // Error in adding friend
                                                    friendsErrorMsg.setText("Error occured adding Journeyman");
                                                }
                                            }
                                        }
                                        catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });
                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                // show it
                alertDialog.show();
            }
        });
        return v;
    };
    private void displayListView() {
        DatabaseHandler db3 = DatabaseHandler.getInstance(getActivity());
        // DatabaseHandler db3 = new DatabaseHandler(getActivity());
        StringBuilder sb2 = new StringBuilder(); {
            String UID = db3.getUID().toString();
            sb2.append(UID);
            sb2.delete(0, 5);
            sb2.deleteCharAt(sb2.length()-1);
            uid2 = sb2.toString();
        }
        DatabaseHandler db12 = DatabaseHandler.getInstance(getActivity());
        Cursor cursor = db12.fetchAllFriends(uid2);
        // The desired columns to be bound
        String[] columns = new String[] {
                DatabaseHandler.KEY_NAME,
                DatabaseHandler.KEY_EMAIL,
                DatabaseHandler.KEY_FID
        };
        // the XML defined views which the data will be bound to
        int[] to = new int[] {
                R.id.name,
                R.id.email,
                R.id.id,
        };
        // create the adapter using the cursor pointing to the desired data
        //as well as the layout information
        dataAdapter = new SimpleCursorAdapter(
                getActivity(), R.layout.friend_info,
                cursor,
                columns,
                to);
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);
    }
}