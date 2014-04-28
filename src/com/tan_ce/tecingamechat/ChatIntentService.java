package com.tan_ce.tecingamechat;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class ChatIntentService extends IntentService {
	final public static int NOTIFICATION_ID = 1;
	
	public ChatIntentService() {
		super("ChatIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);
		
		Log.w("Test", "has intent");
		
		if (!extras.isEmpty()) {
			// Filter messages based on message type
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				Log.w("Test", "has notification");
				sendNotification(extras.getString("msg"));
			}
		}
		
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}
	
	protected void sendNotification(String msg) {
		NotificationManager nm = (NotificationManager)
				this.getSystemService(Context.NOTIFICATION_SERVICE);
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, 
				new Intent(this, ChatActivity.class), 0);
		
		NotificationCompat.Builder builder =
				new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle("TEC Minecraft Chat")
			.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
			.setContentText(msg);
		
		builder.setContentIntent(contentIntent);
		nm.notify(NOTIFICATION_ID, builder.build());
	}

}