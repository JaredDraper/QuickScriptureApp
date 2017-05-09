package com.draper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;

public class Alarm extends BroadcastReceiver {

	// this constructor is called by the alarm manager.
	public Alarm() {
	}

	// you can use this constructor to create the alarm.
	// Just pass in the main activity as the context,
	// any extras you'd like to get later when triggered
	// and the timeout
	public Alarm(Context context, Bundle extras, int hoursUntilNext) {
		AlarmManager alarmMgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, Alarm.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		if (extras.getBoolean("alarmSet") == false) {
			alarmMgr.cancel(pendingIntent);
		}
		if (extras.getBoolean("sendToRecipients")
				|| extras.getBoolean("sendNotification")) {
			alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
					extras.getLong("time"), hoursUntilNext * 3600000,
					pendingIntent);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Controller controller = new Controller(context);
			AlarmManager alarmMgr = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			Intent bootIntent = new Intent(context, Alarm.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
					0, bootIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			if ((controller.sendMessage || controller.sendNotification)
					&& controller.alarmSet) {
				alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, controller.time,
						24 * 3600000, pendingIntent);
			}
		} else {
			Controller controller = new Controller(context);
			controller.send();
		}
	}

	public class Controller {
		DatabaseHelper dbConn;
		String oldPerson = "";
		String oldPhone = "";
		String rawScripture = "";
		String messageToSend = "";
		boolean sendMessage = false;
		boolean sendNotification = false;
		boolean alarmSet = false;
		private boolean useBookOfMormon;
		private boolean useBible;
		Long time;
		List<Person> people = new ArrayList<Person>();
		String scriptureToView;
		Context context;

		public Controller(Context context) {
			this.context = context;
			loadPreferences();
			dbConn = new DatabaseHelper(context);
		}

		public void send() {
			sendRandomScriptureVerses();
			if (sendNotification) {
				sendNotification();
			}
		}

		private void loadPreferences() {
			SharedPreferences sharedPrefs = PreferenceManager
					.getDefaultSharedPreferences(context);
			Map<String, ?> items = sharedPrefs.getAll();
			if (items.isEmpty()) {
				Editor editor = sharedPrefs.edit();
				editor.putBoolean("sendReminder", false);
				editor.putBoolean("sendNotification", false);
				editor.putString("people", "");
				editor.putString("phone", "");
				editor.commit();
			}

			String peopleStr = sharedPrefs.getString("people", "");
			String[] peopleList = peopleStr.split(",");
			String phoneStr = sharedPrefs.getString("phone", "");
			String[] phoneList = phoneStr.split(",");
			for (int i = 0; i < peopleList.length; i++) {
				if (peopleList[i].isEmpty() == false) {
					people.add(new Person(peopleList[i], phoneList[i]));
				}
			}
			sendMessage = sharedPrefs.getBoolean("sendReminder", false);
			sendNotification = sharedPrefs
					.getBoolean("sendNotification", false);
			useBookOfMormon = sharedPrefs.getBoolean("bookOfMormon", false);
			useBible = sharedPrefs.getBoolean("bible", false);
			int hours = sharedPrefs.getInt("hours", 0);
			int minutes = sharedPrefs.getInt("minutes", 0);
			alarmSet = sharedPrefs.getBoolean("alarmSet", false);
			Calendar updateTime = Calendar.getInstance();
			updateTime.set(Calendar.HOUR_OF_DAY, hours);
			updateTime.set(Calendar.MINUTE, minutes);
			Calendar today = Calendar.getInstance();
			if (updateTime.before(today)) {
				updateTime.add(Calendar.DAY_OF_MONTH, 1);
			}
			time = updateTime.getTimeInMillis();
		}

		private void sendRandomScriptureVerses() {
			List<Integer> minMaxValues = setStartAndEnd();
			int minValue = minMaxValues.get(0);
			int maxValue = minMaxValues.get(1);
			int randomNumber = minValue
					+ (int) (Math.random() * ((maxValue - minValue) + 1));
			Cursor cursor = dbConn.getRandomScripture(randomNumber);
			cursor.moveToFirst();
			rawScripture = cursor.getString(0) + "- " + cursor.getString(1);
			cursor.close();
			dbConn.close();
			scriptureToView = rawScripture;
			String scripture = rawScripture;
			if (sendMessage == false) {
				return;
			}
			send150CharactersUntilFinished(scripture);
		}

		private List<Integer> setStartAndEnd() {
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

		private void send150CharactersUntilFinished(String messageToSend) {
			textScriptureToRecipients(messageToSend);
			while (messageToSend.length() > 0) {
				if (messageToSend.length() > 160) {
					String tempScripture = messageToSend.substring(0, 160);
					messageToSend = messageToSend.substring(160);
					textScriptureToRecipients(tempScripture);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					textScriptureToRecipients(messageToSend);
					messageToSend = "";
				}
			}
		}

		private void textScriptureToRecipients(String scripture) {
			String DELIVERED = "SMS_DELIVERED";
			String SENT = "SMS_SENT";
			for (Person person : people) {
				PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
						new Intent(SENT), 0);
				PendingIntent deliveredPI = PendingIntent.getBroadcast(context,
						0, new Intent(DELIVERED), 0);
				SmsManager sms = SmsManager.getDefault();
				sms.sendTextMessage(person.getPhone(), null, scripture, sentPI,
						deliveredPI);
			}
		}

		private void sendNotification() {
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
}
