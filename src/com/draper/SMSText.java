package com.draper;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.widget.Toast;

public class SMSText {
	public SMSText(Context context, String phoneNumber, String message) {
		sendSMS(context, phoneNumber, message);
	}

	// ---sends an SMS message to another device---
	private void sendSMS(final Context context, String phoneNumber,
			String message) {
		String DELIVERED = "SMS_DELIVERED";
		String SENT = "SMS_SENT";

		PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
				new Intent(SENT), 0);
		PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
				new Intent(DELIVERED), 0);
		// ---when the SMS has been sent---
		context.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				Toast toast;
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					// toast = Toast.makeText(getBaseContext(),
					// "reminder message was sent.",
					// Toast.LENGTH_LONG);
					// toast.setGravity(Gravity.CENTER, 0, 0);
					// toast.show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					toast = Toast.makeText(context,
							"reminder message failed to send.",
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					toast = Toast.makeText(context,
							"reminder message failed to send.",
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					toast = Toast.makeText(context,
							"reminder message failed to send.",
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					toast = Toast.makeText(context,
							"reminder message failed to send.",
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					break;
				}
			}
		}, new IntentFilter(SENT));
		// ---when the SMS has been delivered---
		context.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				Toast toast;
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					toast = Toast.makeText(context,
							"reminder message was delivered.",
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					break;
				case Activity.RESULT_CANCELED:
					toast = Toast.makeText(context,
							"reminder message was not delivered.",
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					break;
				}
			}
		}, new IntentFilter(DELIVERED));
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
	}
}
