package org.openvoice;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

public class OutboundCallDownloadTask extends AsyncTask<String, Void, Boolean> {

	private org.openvoice.OutboundCallActivity mMain;
  private Context mContext;
  private SharedPreferences mPrefs;
  private List<Map<String, String>> mCallData;
  
  public OutboundCallDownloadTask(Context context, org.openvoice.OutboundCallActivity main) {
    mContext = context;
    mMain = main;
    mPrefs = context.getSharedPreferences(org.openvoice.MessagingsActivity.PREFERENCES_NAME, Context.MODE_WORLD_READABLE);    
    mCallData = new ArrayList<Map<String, String>>();
  }  

  @Override
  protected Boolean doInBackground(String... params) {
    return new Boolean(downloadStatus());
  }

  @Override
  protected void onPostExecute(Boolean result) {
    if(result) {
      mMain.showCalls(mCallData);
    }
  }

  public boolean downloadStatus() {
    DefaultHttpClient client = new DefaultHttpClient();
    try {
      String user_id = mPrefs.getString(org.openvoice.MessagingsActivity.PREF_USER_ID, "");
      String token = mPrefs.getString(org.openvoice.MessagingsActivity.PREF_TOKEN, "");
      String addr = "/users/" + user_id + "/outgoing_calls?format=json&token=" + token; 
      URI uri = new URI(SettingsActivity.getServerUrl(mContext) + addr);
      HttpGet method = new HttpGet(uri);
      ResponseHandler<String> responseHandler = new BasicResponseHandler();
      String responseBody = client.execute(method, responseHandler);
      if( responseBody != null && responseBody != "") {
        try {
          JSONArray jsons = new JSONArray(responseBody);
          for(int i=0; i<jsons.length(); i++) {
            JSONObject json = jsons.getJSONObject(i);            
            JSONObject message = json.getJSONObject("outgoing_call");
            extract_status(i, message);
          }
        } catch(JSONException jsone) {
          try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray ar = json.toJSONArray(json.names());
            JSONObject elem = ar.getJSONObject(0);
            extract_status(0, elem);
          } catch(JSONException e) {
            Log.e(getClass().getName(), e.getMessage());
          }
        }
        return true;
      }
    } catch (Exception e) {
      //Log.e(getClass().getName(), e.getMessage());
    } finally {
      // TODO do something here
    }
    return false;
  }

  private void extract_status(int i, JSONObject elem)
  throws JSONException {
    HashMap<String, String> md = new HashMap<String, String>();
    String to = elem.getString("callee_number");
  	md.put("caller_id", to);
    md.put("caller_name", ContactManager.getInstance(mContext).getContactNameByPhoneNumber(to));
    md.put("time", elem.getString("created_at"));
    mCallData.add(md);
  }
}