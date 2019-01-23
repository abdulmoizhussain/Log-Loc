package com.example.abdul.logloc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
	private String[] _repeatDurations;
	private ArrayList<String> allContacts;
	private ArrayList<String> allNumbers;
	private ArrayList<String> selectedContacts;
	private ArrayList<String> selectedNumbers;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private Spinner spinnerContacts;
	private Spinner spinnerTimeDuration;
	private TextView textView;
	private Long selectedDuration;
	private int granted;
	ProgressDialog progressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		granted = PackageManager.PERMISSION_GRANTED;
		int finePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
		int coarsePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
		int smsPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
		int contactsPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
		
		if (finePermission != granted || coarsePermission != granted ||
				smsPermission != granted || contactsPermission != granted) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				requestPermissions(new String[]{
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.SEND_SMS,
						Manifest.permission.READ_CONTACTS,
				}, 10);
			} else {
				denied();
			}
			return;
		}
		
		initAll();
	}
	
	private void denied() {
		Toast.makeText(this, "One of the permissions is not granted !", Toast.LENGTH_LONG).show();
		ScrollView parent = ((ScrollView) findViewById(R.id.parentView));
		parent.removeAllViews();
	}
	
	private void initAll() {
		//		ProgressBar p = (ProgressBar)findViewById(R.id.progressBar);
//		p.setVisibility(View.VISIBLE);
//		findViewById(R.id.progressBar).setVisibility(View.GONE);
		progressDialog = new ProgressDialog(this);
		allContacts = new ArrayList<String>();
		allNumbers = new ArrayList<String>();
		selectedContacts = new ArrayList<String>();
		selectedNumbers = new ArrayList<String>();
		_repeatDurations = new String[]{"2", "3", "5", "10"};
		
		spinnerContacts = (Spinner) findViewById(R.id.spinnerContacts);
		spinnerTimeDuration = (Spinner) findViewById(R.id.spinnerTimeDuration);
		textView = (TextView) findViewById(R.id.textView);
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		progressDialog.setMessage("Reading Contacts, please wait...");
		
		initSearchContactTextInput();
		getContactList("");
		initTimeDurations();
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case 10:
				if (grantResults.length > 0) {
					Boolean allowed = true;
					for (int result : grantResults) {
						if (result != granted) {
							allowed = false;
							break;
						}
					}
					if (allowed) {
						initAll();
					} else {
						denied();
					}
				}
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
	
	@SuppressLint("MissingPermission")
	private void startCheckingGeoLocation() {
		
		stopLogger(null);
		locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				String lat = Double.toString(location.getLatitude());
				String lng = Double.toString(location.getLongitude());
				
				String textMessage = "Check location at: " +
						"https://maps.google.com/maps?q=" +
						lat + "," + lng;
				sendSMS(textMessage);
			}
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
			
			@Override
			public void onProviderEnabled(String provider) {
				Toast.makeText(MainActivity.this, "GPS Enabled", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onProviderDisabled(String provider) {
				Toast.makeText(MainActivity.this, "GPS is disabled", Toast.LENGTH_LONG).show();
				stopLogger(null);
			}
		};
		
		locationManager.requestLocationUpdates("gps",
				selectedDuration,
				0,
				locationListener
		);
	}
	
	private void sendSMS(String message) {
		final int MAX_SMS_MESSAGE_LENGTH = 160;
		SmsManager manager = SmsManager.getDefault();
		
		for (String phoneNumber : selectedNumbers) {
			int length = message.length();
			
			if (length > MAX_SMS_MESSAGE_LENGTH) {
				ArrayList<String> messageList = manager.divideMessage(message);
				
				manager.sendMultipartTextMessage(phoneNumber, null, messageList, null, null);
			} else {
				manager.sendTextMessage(phoneNumber, null, message, null, null);
			}
		}
	}
	
	private void getContactList(String matchString) {
		allContacts.clear();
		allNumbers.clear();
		allContacts.add("");
		allNumbers.add("");
		progressDialog.show();
		
		String selection = matchString.length() > 0 ? ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?" : null;
		String[] selectionArgs = matchString.length() > 0 ? new String[]{"%" + matchString + "%"} : null;
		ContentResolver cr = this.getContentResolver();
		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
				null,
				selection,
				selectionArgs,
				ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
		
		if ((cur != null ? cur.getCount() : 0) > 0) {
			
			while (cur.moveToNext()) {
				String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
				String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				
				if (
						cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0
						) {
					Cursor pCur = cr.query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							null,
							ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
							new String[]{id}, null);
					
					
					while (pCur != null && pCur.moveToNext()) {
						String phoneNo = pCur.getString(pCur.getColumnIndex(
								ContactsContract.CommonDataKinds.Phone.NUMBER));
						
						if (phoneNo.indexOf(' ') == -1) {
							allContacts.add(name + " " + phoneNo);
							allNumbers.add(phoneNo);
						}
					}
					
					if (pCur != null) {
						pCur.close();
					}
					
				}
			}
			
			ArrayAdapter<String> adapter = new ArrayAdapter<>(
					this,
					android.R.layout.simple_spinner_item, allContacts
			);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			
			spinnerContacts.setAdapter(adapter);
			spinnerContacts.setSelection(Adapter.NO_SELECTION, true);
			spinnerContacts.setOnItemSelectedListener(this);
		}
		if (cur != null) {
			cur.close();
		}
		progressDialog.dismiss();
	}
	
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
		
		if (position > 0) {
			selectedNumbers.add(allNumbers.get(position));
			selectedContacts.add(allContacts.get(position));
		}
		
		updateSelectedContacts();
	}
	
	private void updateSelectedContacts() {
		textView.setText(_arrayListToString(selectedContacts));
	}
	
	private String _arrayListToString(ArrayList<String> list) {
		StringBuilder s = new StringBuilder();
		for (String str : list) {
			s.append(str);
			s.append("\n");
		}
		return s.toString();
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
	}
	
	private void initTimeDurations() {
		final String[] repeatDurations = new String[]{"2 minutes", "3 minutes", "5 minutes", "10 minutes"};
		
		ArrayAdapter<String> adapter = new ArrayAdapter<>(
				this,
				android.R.layout.simple_spinner_item,
				repeatDurations
		);
		
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerTimeDuration.setAdapter(adapter);
		spinnerTimeDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				selectedDuration = (Long.parseLong(_repeatDurations[position].split(" ")[0], 10) * 1000 * 60);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
	}
	
	public void startLogger(View v) {
		startCheckingGeoLocation();
		Toast.makeText(this, "New Logger Started", Toast.LENGTH_SHORT).show();
	}
	
	public void stopLogger(View v) {
		if (locationListener != null) {
			locationManager.removeUpdates(locationListener);
			locationListener = null;
			Toast.makeText(this, "Stopped Logger", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this,
					"No Active Logger",
					Toast.LENGTH_SHORT).show();
		}
	}
	
	public void removeContact(View v) {
		if (selectedNumbers.size() > 0) {
			selectedNumbers.remove(selectedNumbers.size() - 1);
			selectedContacts.remove(selectedContacts.size() - 1);
			updateSelectedContacts();
		}
	}
	
	private void initSearchContactTextInput() {
		EditText searchContact = (EditText) findViewById(R.id.editText);
		searchContact.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
				if (s.length() > 0) {
					getContactList(s.toString());
				} else {
					getContactList("");
				}
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopLogger(null);
	}
}
