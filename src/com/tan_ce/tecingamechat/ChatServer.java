package com.tan_ce.tecingamechat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

/**
 * Represents a connection to the TEC chat server. Will eventually handle authentication,
 * once I've figured out how I want to do it.
 * 
 * @author tan-ce
 *
 */
public class ChatServer {
	public static final String API_GATEWAY = "https://minecraft.tan-ce.com:9443/chat/api.php?";
	public static final String SENDER_ID = "37149530282";
	public static final String PREF_VERSION = "version";
	public static final String PREF_AUTHKEY = "auth_key";
	public static final String PREF_GCMID = "gcm_id";
	protected SharedPreferences pref;
	protected String authKey;

	protected static void addURLKeyValue(StringBuilder url, String key, String value) {
		try {
			url.append("&");
			url.append(URLEncoder.encode(key, "UTF-8"));
			url.append("=");
			url.append(URLEncoder.encode(value, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Log.e("ChatServer", "addGetKeyValue: No support for UTF-8???");
		}
	}

	/**
	 * Generates a URL for the API gateway
	 * 
	 * @param me		If not null, uses the authKey from this object
	 * @param query		The query to perform
	 * @param params	Any additional parameters to put in the $_GET string
	 * @return			URL object
	 * @throws 			MalformedURLException
	 */
	protected static URL urlBuilder(ChatServer me, String query, Map<String, String> params) throws MalformedURLException {
		StringBuilder url = new StringBuilder(API_GATEWAY.length() + 64);
		url.append(API_GATEWAY);

		// Append the query
		addURLKeyValue(url, "q", query);

		// Append the authKey, if applicable
		if (me != null) addURLKeyValue(url, "auth_key", me.authKey);

		// Append the rest of the query
		for (Map.Entry<String, String> e : params.entrySet()) {
			addURLKeyValue(url, e.getKey(), e.getValue());
		}

		return new URL(url.toString());
	}

	/**
	 * Generates a URL for the API gateway
	 * 
	 * @param query		The query to perform
	 * @param params	Any additional parameters to put in the $_GET string
	 * @return			URL object
	 * @throws 			MalformedURLException
	 */
	protected URL urlBuilder(String query, Map<String, String> params) throws MalformedURLException {
		return urlBuilder(this, query, params);
	}

	/**
	 * Generates a URL for the API gateway
	 * 
	 * @param query		The query to perform
	 * @return			URL object
	 * @throws 			MalformedURLException
	 */
	protected URL urlBuilder(String query) throws MalformedURLException, UnsupportedEncodingException {
		return urlBuilder(this, query, new HashMap<String, String>());
	}

	protected static String postEncoder(Map<String, String> data) {
		StringBuilder encoded = new StringBuilder(64);
		for (Map.Entry<String, String> e : data.entrySet()) {
			try {
				encoded.append(	URLEncoder.encode(e.getKey(), "utf-8") + "=" +
						URLEncoder.encode(e.getValue(), "utf-8") + "&");
			} catch (UnsupportedEncodingException e1) {
				// Should Never Happen (tm)
				return "Encoding error?";
			}

		}

		return encoded.toString();
	}

	protected static void checkError(JSONObject result) throws Exception {
		if (result.getInt("error") != 0) {
			try {
				if (result.getInt("auth_failed") == 1) {
					throw new NeedRegistrationException();
				}
			} catch (JSONException e) {
				// Silently ignore - this means it was not an authentication error
			}

			String errMsg;
			try {
				errMsg = result.getString("error_msg");
			} catch (Exception e) {
				errMsg = "Unknown error";
			}
			throw new Exception(errMsg);
		}
	}

	public static void registerUser(Context ctx, String user, String password) throws Exception {
		// Get the GCM registration ID
		String gcmId = getGCMId(ctx);

		if (gcmId.isEmpty()) {
			// Generate a new GCM registration ID
			try {
				GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(ctx);
				gcmId = gcm.register(SENDER_ID);
			} catch (Exception e) {
				throw new Exception("Error registering with GCM: " + e.getMessage());
			}

			saveGCMId(ctx, gcmId);
		}

		// Post the registration info
		HttpURLConnection conn = (HttpURLConnection)
				urlBuilder(null, "register", new HashMap<String, String>())
				.openConnection();

		String json_str = null;

		HashMap<String, String> postData = new HashMap<String, String>();
		postData.put("user", user);
		postData.put("pwd", password);
		postData.put("gcm_id", gcmId);

		// Send and receive
		try {
			conn.setDoOutput(true);

			OutputStream os = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(postEncoder(postData));
			writer.close();

			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
			json_str = reader.readLine();
		} finally {
			conn.disconnect();
		}

		// Check the returned data for errors
		JSONObject result = new JSONObject(json_str);
		checkError(result);

		String authKey = result.getString("auth_key");
		saveAuthKey(ctx, authKey);
		Log.i("ChatServer", "Registration successful");
	}

	/**
	 * Constructor
	 * 
	 * @param ctx
	 * @throws NeedRegistrationException
	 */
	ChatServer(Context ctx) throws NeedRegistrationException {
		// Try to retrieve the authentication key
		authKey = getAuthKey(ctx);
		if (authKey.isEmpty()) {
			throw new NeedRegistrationException();
		}

		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);
	}

	/**
	 * Handles the result of a history request
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	protected List<ChatMessage> historyRequest(URL url) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		String json_str;

		try {
			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
			json_str = reader.readLine();
		} finally {
			conn.disconnect();
		}

		JSONObject result = new JSONObject(json_str);
		checkError(result);

		JSONArray jhist = result.getJSONArray("history");
		ArrayList<ChatMessage> hist = new ArrayList<ChatMessage>(jhist.length());
		for (int i = 0; i < jhist.length(); i++) {
			JSONObject jcm = jhist.getJSONObject(i);

			int idx = jcm.getInt("id");
			String user = jcm.getString("user");
			String msg = jcm.getString("msg");
			// Java uses milliseconds since the epoch:
			long date = jcm.getLong("ts") * 1000;

			hist.add(new ChatMessage(idx, user, msg, date));
		}

		return hist;
	}

	public void sendMsg(String msg) throws Exception {
		HashMap<String, String> postData = new HashMap<String, String>();
		postData.put("msg", msg);

		URL url = urlBuilder("say");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		String json_str;
		try {
			conn.setDoOutput(true);

			OutputStream os = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(postEncoder(postData));
			writer.close();

			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
			json_str = reader.readLine();
		} finally {
			conn.disconnect();
		}

		// Check the returned data for errors
		JSONObject result = new JSONObject(json_str);
		checkError(result);
	}

	/**
	 * Retrieves most recent chat message history
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<ChatMessage> getHistory() throws Exception {
		URL url = urlBuilder("history");
		return historyRequest(url);
	}

	/**
	 * Retrieves chat message history
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<ChatMessage> getHistory(int startIdx, int count) throws Exception {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("id", Integer.toString(startIdx));
		params.put("count", Integer.toString(count));

		URL url = urlBuilder("history", params);
		return historyRequest(url);
	}

	protected static String getGCMId(Context ctx) {
		SharedPreferences pref = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);

		String registrationId = pref.getString(PREF_GCMID, "");
		if (registrationId.isEmpty()) {
			Log.i("ChatServer", "Registration not found.");
			return "";
		}

		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = pref.getInt(PREF_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(ctx);
		if (registeredVersion != currentVersion) {
			Log.i("ChatServer", "App version changed.");
			return "";
		}

		return registrationId;
	}

	protected static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	protected static void saveGCMId(Context ctx, String gcmId) {
		SharedPreferences pref = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		editor.remove(PREF_AUTHKEY);
		editor.putString(PREF_GCMID, gcmId);
		editor.putInt(PREF_VERSION, getAppVersion(ctx));
		editor.commit();
	}

	protected static String getAuthKey(Context ctx) {
		SharedPreferences pref = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);

		String authKey = pref.getString(PREF_AUTHKEY, "");
		if (authKey.isEmpty()) {
			Log.i("ChatServer", "Auth key not found.");
			return "";
		}

		// Check if app was updated; if so, it must clear the auth key
		// since the existing auth key is not guaranteed to work with the new
		// app version.
		int registeredVersion = pref.getInt(PREF_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(ctx);
		if (registeredVersion != currentVersion) {
			Log.i("ChatServer", "App version changed.");
			return "";
		}

		return authKey;
	}

	protected static void saveAuthKey(Context ctx, String authKey) {
		SharedPreferences pref = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		editor.putString(PREF_AUTHKEY, authKey);
		editor.putInt(PREF_VERSION, getAppVersion(ctx));
		editor.commit();
	}
}
