package com.draper;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;

public class MessageController {
	private static String scriptureToView;
	Context context;

	public MessageController(Context context) {
		this.context = context;
	}

	public void send150CharactersUntilFinished(String messageToSend,
			List<Person> people) {

		for (Person person : people) {
			String tempMessage = messageToSend;
			while (tempMessage.length() > 0) {
				if (tempMessage.length() > 160) {
					String tempScripture = tempMessage.substring(0, 160);
					tempMessage = tempMessage.substring(160);
					new SMSText(context, person.getPhone(), tempScripture);
				} else {
					new SMSText(context, person.getPhone(), tempMessage);
					tempMessage = "";
				}
			}
		}
	}

	public void sendRandomScriptureVerses(DatabaseHelper dbConn,
			List<Boolean> prefs, List<Person> people) {
		boolean sendMessage = prefs.get(0);
		boolean useBible = prefs.get(1);
		boolean useBookOfMormon = prefs.get(2);
		if (dbConn == null) {
			dbConn = new DatabaseHelper(context);
		}
		List<Integer> minMaxValues = setStartAndEnd(useBible, useBookOfMormon);
		int minValue = minMaxValues.get(0);
		int maxValue = minMaxValues.get(1);
		int randomNumber = minValue
				+ (int) (Math.random() * ((maxValue - minValue) + 1));
		Cursor cursor = dbConn.getRandomScripture(randomNumber);
		cursor.moveToFirst();
		String rawScripture = cursor.getString(0) + "- " + cursor.getString(1);
		cursor.close();
		dbConn.close();
		scriptureToView = rawScripture;
		String scripture = rawScripture;
		if (sendMessage == false) {
			return;
		}
		send150CharactersUntilFinished(scripture, people);
	}

	private List<Integer> setStartAndEnd(boolean useBible,
			boolean useBookOfMormon) {
		List<Integer> startEnd = new ArrayList<Integer>();
		int start = 0;
		int end = 0;
		if (useBible && useBookOfMormon == false) {
			end = 31103;
		}
		if (useBible && useBookOfMormon) {
			end = 37706;
		}
		if (useBible == false && useBookOfMormon) {
			start = 31103;
			end = 37706;
		}
		startEnd.add(start);
		startEnd.add(end);
		return startEnd;
	}

	public void sendNotification() {
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		int icon = R.drawable.alien;
		CharSequence text = "text";
		CharSequence contentTitle = "Scripture";
		CharSequence contentText = "Sample notification scripture.";
		long when = System.currentTimeMillis();

		Intent intent = new Intent(context, NotificationViewer.class);
		intent.putExtra("Scripture", scriptureToView);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				intent, 0);
		Notification notification = new Notification(icon, text, when);

		long[] vibrate = { 0, 100, 200, 300 };
		notification.vibrate = vibrate;
		notification.ledARGB = Color.RED;
		notification.ledOffMS = 300;
		notification.ledOnMS = 300;
		notification.defaults |= Notification.DEFAULT_LIGHTS;

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		notificationManager.notify(10001, notification);
	}
}
