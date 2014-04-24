package com.tan_ce.tecingamechat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.CookieManager;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents a connection to the TEC chat server. Will eventually handle authentication,
 * once I've figured out how I want to do it.
 * 
 * @author tan-ce
 *
 */
public class ChatServer {
	public static final String default_api_gateway = "https://minecraft.tan-ce.com:9443/chat/api.php";
	
	protected String api_gateway;
	
	/**
	 * Generates a URL for the API gateway
	 * 
	 * @param query
	 * @param params
	 * @return
	 * @throws MalformedURLException 
	 * @throws UnsupportedEncodingException 
	 */
	protected URL urlBuilder(String query, Map<String, String> params) throws MalformedURLException, UnsupportedEncodingException {
		String url = api_gateway + "?q=" + URLEncoder.encode(query, "UTF-8");
		for (Map.Entry<String, String> e : params.entrySet()) {
			url = url + "&" + URLEncoder.encode(e.getKey(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
		}
		return new URL(url);
	}
	
	/**
	 * Generates a URL for the API gateway
	 * 
	 * @param query
	 * @return
	 * @throws UnsupportedEncodingException 
	 * @throws MalformedURLException 
	 */
	protected URL urlBuilder(String query) throws MalformedURLException, UnsupportedEncodingException {
		return urlBuilder(query, new HashMap<String, String>());
	}
	
	ChatServer() {
		api_gateway = default_api_gateway;
		
		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);
	}
	
	/**
	 * Retrieves chat message history
	 * 
	 * @return
	 * @throws Exception 
	 */
	public List<ChatMessage> getHistory() throws Exception {
		URL url = urlBuilder("history");
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
		if (result.getInt("error") != 0) {
			throw new Exception("Server reports error");
		}
		
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
}
