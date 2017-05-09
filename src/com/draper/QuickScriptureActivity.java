package com.draper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class QuickScriptureActivity extends Activity {
	private TimePicker time;
	private DatabaseHelper dbConn;
	private MessageController controller;
	private View editMessageView, editPersonView, mainView;
	private Button btnSend, btnSendMessage, btnSave, btnViewPeople,
			btnMainView, btnSetAlarm, btnCancelAlarm;
	private CheckBox cbxSendMessage, cbxSendNotification, cbxBookOfMormon,
			cbxBible;
	private TextView lblAlarmSet;
	private TableLayout mainLayout;
	private int hours;
	private int minutes;
	private String oldPerson = "";
	private String oldPhone = "";
	private boolean sendMessage, sendNotification, useBookOfMormon, useBible,
			alarmSet;
	private List<Person> people = new ArrayList<Person>();
	private static final int CONTACT_PICKER_RESULT = 1001;
	private static final String DEBUG_TAG = null;
	private static final Uri URI = ContactsContract.Contacts.CONTENT_URI;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = new MessageController(this);
		setContentView(R.layout.main);
		btnMainView = (Button) findViewById(R.id.btnMainView);
		btnMainView.setOnClickListener(mainViewListener);
		btnSendMessage = (Button) findViewById(R.id.btnEditMessageView);
		btnSendMessage.setOnClickListener(editMessageListener);
		btnViewPeople = (Button) findViewById(R.id.btnViewPeople);
		btnViewPeople.setOnClickListener(viewPeopleListener);
		mainLayout = (TableLayout) findViewById(R.id.MainLayoutTable);
		loadUserSettings();
		populateMainView();
		dbConn = new DatabaseHelper(this);
		try {
			dbConn.createDataBase();
		} catch (IOException e) {
			System.out.println("broken");
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		savePreferences(people, sendMessage, sendNotification);
	}

	private void loadUserSettings() {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Map<String, ?> items = sharedPrefs.getAll();
		if (items.isEmpty()) {
			Editor editor = sharedPrefs.edit();
			editor.putBoolean("sendReminder", false);
			editor.putBoolean("sendNotification", false);
			editor.putString("people", "");
			editor.putString("phone", "");
			editor.putInt("hours", 0);
			editor.putInt("minutes", 0);
			editor.putBoolean("bookOfMormon", false);
			editor.putBoolean("bible", false);
			editor.putBoolean("alarmSet", false);
			editor.commit();
		}

		String peopleStr = sharedPrefs.getString("people", "");
		String[] peopleList = peopleStr.split(",");
		String phoneStr = sharedPrefs.getString("phone", "");
		String[] phoneList = phoneStr.split(",");
		people.clear();
		for (int i = 0; i < peopleList.length; i++) {
			if (peopleList[i].isEmpty() == false) {
				people.add(new Person(peopleList[i], phoneList[i]));
			}
		}
		sendMessage = sharedPrefs.getBoolean("sendReminder", false);
		sendNotification = sharedPrefs.getBoolean("sendNotification", false);
		useBookOfMormon = sharedPrefs.getBoolean("bookOfMormon", false);
		useBible = sharedPrefs.getBoolean("bible", false);
		hours = sharedPrefs.getInt("hours", 0);
		minutes = sharedPrefs.getInt("minutes", 0);
		alarmSet = sharedPrefs.getBoolean("alarmSet", false);
	}

	public void savePreferences(List<Person> people, Boolean sendMessage,
			Boolean sendNotification) {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = sharedPrefs.edit();
		if (sendMessage != null) {
			editor.putBoolean("sendReminder", sendMessage);
		}
		if (sendNotification != null) {
			editor.putBoolean("sendNotification", sendNotification);
		}
		String peopleStr = "";
		String phoneStr = "";
		if (people.isEmpty() == false) {
			for (Person person : people) {
				peopleStr = peopleStr + "," + person.getName();
				phoneStr = phoneStr + "," + person.getPhone();
			}
			peopleStr.replaceFirst(",", "");
			phoneStr.replaceFirst(",", "");
		}
		editor.putString("people", peopleStr);
		editor.putString("phone", phoneStr);
		editor.putInt("hours", time.getCurrentHour());
		editor.putInt("minutes", time.getCurrentMinute());
		editor.putBoolean("bookOfMormon", useBookOfMormon);
		editor.putBoolean("bible", useBible);
		editor.putBoolean("alarmSet", alarmSet);

		editor.commit();
	}

	private void setRepeatingAlarm(Boolean sendReminder,
			Boolean sendNotification) {
		Bundle bundle = new Bundle();
		// add extras here..
		bundle.putBoolean("sendToRecipients", sendReminder);
		bundle.putBoolean("sendNotification", sendNotification);
		bundle.putBoolean("alarmSet", alarmSet);
		time = (TimePicker) mainView.findViewById(R.id.timePicker);
		DateTime update = new DateTime(System.currentTimeMillis());
		Calendar updateTime = update.toCalendar(Locale.ENGLISH);
		updateTime.set(Calendar.HOUR_OF_DAY, time.getCurrentHour());
		updateTime.set(Calendar.MINUTE, time.getCurrentMinute());
		updateTime.set(Calendar.SECOND, 0);
		Calendar today = Calendar.getInstance();
		if (updateTime.before(today)) {
			updateTime.add(Calendar.DAY_OF_MONTH, 1);
		}
		bundle.putLong("time", updateTime.getTimeInMillis());
		Date date3 = new Date(updateTime.getTimeInMillis());
		date3.getHours();
		new Alarm(this, bundle, 24);
	}

	public OnClickListener sendScriptureListener = new OnClickListener() {
		public void onClick(View v) {
			List<Boolean> prefs = new ArrayList<Boolean>();
			prefs.add(sendMessage);
			prefs.add(useBible);
			prefs.add(useBookOfMormon);
			controller.sendRandomScriptureVerses(dbConn, prefs, people);
			if (sendNotification) {
				controller.sendNotification();
			}
		}
	};

	public OnClickListener editMessageListener = new OnClickListener() {
		public void onClick(View v) {
			mainLayout.removeAllViews();
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			editMessageView = inflater.inflate(R.layout.edit_message, null);
			btnSend = (Button) editMessageView.findViewById(R.id.btnSend);
			btnSend.setOnClickListener(btnSendMessageListener);
			mainLayout.addView(editMessageView, 0);
		}
	};

	public OnClickListener btnSendMessageListener = new OnClickListener() {
		public void onClick(View v) {
			EditText message = (EditText) editMessageView
					.findViewById(R.id.txtMessage);
			String messageToSend = message.getText().toString();

			controller.send150CharactersUntilFinished(messageToSend, people);
		}
	};

	public OnClickListener viewPeopleListener = new OnClickListener() {
		public void onClick(View v) {
			populateViewPeople();
		}
	};

	public OnClickListener deletePersonListener = new OnClickListener() {
		public void onClick(View v) {
			// delete Person
			TableRow btnTableRow = (TableRow) v.getParent();
			Button btnSearch = (Button) btnTableRow.findViewById(R.id.Entry);
			String personToDelete = btnSearch.getText().toString();
			Person deadGuy = null;
			for (Person person : people) {
				if (person.getName().equals(personToDelete)) {
					deadGuy = person;
				}
			}
			people.remove(deadGuy);
			populateViewPeople();
		}
	};

	public OnClickListener mainViewListener = new OnClickListener() {
		public void onClick(View v) {
			populateMainView();
		}
	};

	public OnClickListener addPersonListener = new OnClickListener() {
		public void onClick(View v) {
			populateAddPerson();
		}
	};

	public OnClickListener editPersonListener = new OnClickListener() {
		public void onClick(View v) {
			TextView name = (TextView) v.findViewById(R.id.Entry);
			oldPerson = name.getText().toString();
			for (Person person : people) {
				if (person.getName().equals(oldPerson)) {
					oldPhone = person.getPhone();
				}
			}
			populateEditPerson(oldPerson, oldPhone);
		}
	};

	public OnClickListener btnCancelListener = new OnClickListener() {
		public void onClick(View v) {
			populateViewPeople();
		}
	};

	public OnClickListener addFromContactsListener = new OnClickListener() {
		public void onClick(View v) {
			doLaunchContactPicker(v);
		}
	};

	public OnClickListener btnEditPersonListener = new OnClickListener() {
		public void onClick(View v) {

			EditText editPerson = (EditText) editPersonView
					.findViewById(R.id.txtPerson);
			EditText editPhone = (EditText) editPersonView
					.findViewById(R.id.txtPhone);
			String personName = editPerson.getText().toString();
			String phone = editPhone.getText().toString();
			for (Person person : people) {
				if (person.getName().equals(oldPerson)) {
					person.setName(personName);
					person.setPhone(phone);
				}
			}
			oldPerson = "";
			oldPhone = "";
			populateViewPeople();
		}
	};

	public OnClickListener btnSavePersonListener = new OnClickListener() {
		public void onClick(View v) {
			EditText editPerson = (EditText) editMessageView
					.findViewById(R.id.txtPerson);
			EditText editPhone = (EditText) editMessageView
					.findViewById(R.id.txtPhone);
			String personName = editPerson.getText().toString();
			String phone = editPhone.getText().toString();
			people.add(new Person(personName, phone));
			populateViewPeople();
		}
	};

	public OnClickListener cbxSendToRecipientsListener = new OnClickListener() {

		public void onClick(View v) {
			CheckBox checkBox = (CheckBox) v.findViewById(R.id.cbxSendDaily);
			sendMessage = checkBox.isChecked();
		}
	};

	public OnClickListener setAlarmListener = new OnClickListener() {

		public void onClick(View v) {
			minutes = time.getCurrentMinute();
			hours = time.getCurrentHour();
			alarmSet = true;
			lblAlarmSet.setText("Alarm is Currently On");
			setRepeatingAlarm(sendMessage, sendNotification);
		}
	};

	public OnClickListener cancelAlarmListener = new OnClickListener() {

		public void onClick(View v) {
			alarmSet = false;
			lblAlarmSet.setText("Alarm is Currently Off");
			setRepeatingAlarm(sendMessage, sendNotification);
		}
	};

	public OnClickListener cbxSendNotificationListener = new OnClickListener() {

		public void onClick(View v) {
			CheckBox checkBox = (CheckBox) v
					.findViewById(R.id.cbxSendNotification);
			sendNotification = checkBox.isChecked();
		}
	};
	public OnClickListener cbxBookOfMormonListener = new OnClickListener() {

		public void onClick(View v) {
			CheckBox checkBox = (CheckBox) v.findViewById(R.id.cbxBookOfMormon);
			useBookOfMormon = checkBox.isChecked();
		}
	};

	public OnClickListener cbxBibleListener = new OnClickListener() {

		public void onClick(View v) {
			CheckBox checkBox = (CheckBox) v.findViewById(R.id.cbxBible);
			useBible = checkBox.isChecked();
		}
	};

	private void populateAddPerson() {
		mainLayout.removeAllViews();
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		editMessageView = inflater.inflate(R.layout.new_person_view, null);
		btnSave = (Button) editMessageView.findViewById(R.id.btnSave);
		btnSave.setOnClickListener(btnSavePersonListener);
		Button btnContacts = (Button) editMessageView
				.findViewById(R.id.btnContacts);
		btnContacts.setOnClickListener(addFromContactsListener);
		mainLayout.addView(editMessageView, 0);
	}

	private void populateEditPerson(String oldName, String oldPhone) {
		mainLayout.removeAllViews();
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		editPersonView = inflater.inflate(R.layout.edit_person_view, null);
		btnSave = (Button) editPersonView.findViewById(R.id.btnSave);
		btnSave.setOnClickListener(btnEditPersonListener);
		TextView name = (TextView) editPersonView.findViewById(R.id.txtPerson);
		name.setText(oldName);
		TextView phone = (TextView) editPersonView.findViewById(R.id.txtPhone);
		phone.setText(oldPhone);
		mainLayout.addView(editPersonView, 0);
	}

	private void populateViewPeople() {
		mainLayout.removeAllViews();
		for (int index = 0; index < people.size(); index++) {
			makeNewPersonGUI(people.get(index), index);
		}
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View btnRow = inflater.inflate(R.layout.button_row_view, null);
		Button addNewPerson = (Button) btnRow
				.findViewById(R.id.btnAddNewPerson);
		addNewPerson.setOnClickListener(addPersonListener);
		mainLayout.addView(btnRow);
	}

	private void populateMainView() {
		mainLayout.removeAllViews();
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mainView = inflater.inflate(R.layout.main_view, null);
		time = (TimePicker) mainView.findViewById(R.id.timePicker);
		time.setCurrentHour(hours);
		time.setCurrentMinute(minutes);
		btnSetAlarm = (Button) mainView.findViewById(R.id.btnSetDailyAlarm);
		btnSetAlarm.setOnClickListener(setAlarmListener);
		btnCancelAlarm = (Button) mainView.findViewById(R.id.btnCancelAlarm);
		btnCancelAlarm.setOnClickListener(cancelAlarmListener);
		lblAlarmSet = (TextView) mainView.findViewById(R.id.txtAlarmStatus);
		lblAlarmSet.setText(alarmSet == true ? "Alarm is Currently On"
				: "Alarm is Currently Off");
		btnSend = (Button) mainView.findViewById(R.id.btnSend);
		btnSend.setOnClickListener(sendScriptureListener);
		cbxSendMessage = (CheckBox) mainView.findViewById(R.id.cbxSendDaily);
		cbxSendMessage.setChecked(sendMessage);
		cbxSendMessage.setOnClickListener(cbxSendToRecipientsListener);
		cbxSendNotification = (CheckBox) mainView
				.findViewById(R.id.cbxSendNotification);
		cbxSendNotification.setChecked(sendNotification);
		cbxSendNotification.setOnClickListener(cbxSendNotificationListener);
		cbxBookOfMormon = (CheckBox) mainView
				.findViewById(R.id.cbxBookOfMormon);
		cbxBookOfMormon.setChecked(useBookOfMormon);
		cbxBookOfMormon.setOnClickListener(cbxBookOfMormonListener);
		cbxBible = (CheckBox) mainView.findViewById(R.id.cbxBible);
		cbxBible.setChecked(useBible);
		cbxBible.setOnClickListener(cbxBibleListener);
		mainLayout.addView(mainView);
	}

	private void makeNewPersonGUI(Person person, int index) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View personView = inflater.inflate(R.layout.row_view, null);

		Button btnEdit = (Button) personView.findViewById(R.id.Entry);
		btnEdit.setText(person.getName());
		btnEdit.setOnClickListener(editPersonListener);
		Button btnDelete = (Button) personView.findViewById(R.id.btnNewDelete);
		btnDelete.setOnClickListener(deletePersonListener);
		mainLayout.addView(personView, index);
	}

	public void doLaunchContactPicker(View view) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
				Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				Cursor cursor = null;
				Cursor cursor2 = null;
				String phone = "";
				String name = "";
				try {
					Uri result = data.getData();

					// get the contact id from the Uri
					String id = result.getLastPathSegment();
					// query for name
					cursor2 = getContentResolver().query(URI, null,
							BaseColumns._ID + "=?", new String[] { id }, null);
					int nameIdx = cursor2.getColumnIndex(Contacts.DISPLAY_NAME);
					if (cursor2.moveToFirst()) {
						name = cursor2.getString(nameIdx);
						Log.v(DEBUG_TAG, "Got name: " + name);
					}
					// query for phone
					cursor = getContentResolver().query(Phone.CONTENT_URI,
							null, Phone.CONTACT_ID + "=?", new String[] { id },
							null);

					int phoneIdx = cursor.getColumnIndex(Phone.DATA);

					// just get the first phone
					if (cursor.moveToFirst()) {
						phone = cursor.getString(phoneIdx);
						Log.v(DEBUG_TAG, "Got phone: " + phone);
					} else {
						Log.w(DEBUG_TAG, "No results");
					}
				} catch (Exception e) {
					Log.e(DEBUG_TAG, "Failed to get data", e);
				} finally {
					if (cursor != null) {
						cursor.close();
					}
					if (cursor2 != null) {
						cursor2.close();
					}
					EditText phoneEntry = (EditText) findViewById(R.id.txtPhone);
					EditText nameEntry = (EditText) findViewById(R.id.txtPerson);
					phoneEntry.setText(phone);
					nameEntry.setText(name);
					if (phone.length() == 0) {
						Toast.makeText(this, "No phone found for contact.",
								Toast.LENGTH_LONG).show();
					}
				}
				break;
			}
		}
	}
}