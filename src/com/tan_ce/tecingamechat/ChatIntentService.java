package com.tan_ce.tecingamechat;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);

		// Ignore if the user doesn't want notifications
		if (prefs.getBoolean("notifications_new_message", true) && !extras.isEmpty()) {
			// Filter messages based on message type
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				int lastIdx = prefs.getInt("lastIdx", 0);
				ChatMessage cm;

				try {
					cm = new ChatMessage(extras);

					// Only show a notification if the UI hasn't processed this
					// message in the foreground
					if (lastIdx < cm.getIdx()) {
						sendNotification(cm.getUser() + ": " + cm.getMessage());
					}
				} catch (Exception e) {
					Log.w("ChatActivity", "Server pushed invalid message: " + e.getMessage());
				}
			}
		}

		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	protected void sendNotification(String msg) {
		NotificationManager nm = (NotificationManager)
				this.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent notificationIntent = new Intent(this, ChatActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

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