package com.draper;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class NotificationViewer extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notification_viewer);
		String txtScripture = (String) getIntent().getStringExtra("Scripture");
		TextView scripture = (TextView) findViewById(R.id.txtScripture);
		scripture.setText(txtScripture);
		Button close = (Button) findViewById(R.id.btnClose);
		close.setOnClickListener(closeScriptureListener);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.cancel(10001);
	}
	
	public OnClickListener closeScriptureListener = new OnClickListener() {
		public void onClick(View v) {
			finish();
		}
	};

}
