package com.example.abdul.logloc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

// CHECK !! HAVE DISABLED SOME PERMISSIONS !!
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
	private String[] _repeatDurations;
	private ArrayList<String> allContacts;
	private ArrayList<String> selectedContacts;
	private ArrayList<String> selectedNumbers;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private Spinner spinnerContacts;
	private Spinner spinnerTimeDuration;
	private EditText searchContact;
	private TextView selectedContactsView;
	private TextView searchTitle;
	private Button startButton;
	private Button stopButton;
	private Long selectedDuration;
	private int granted;
	private ReadContactsAsync readContactsAsync;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		AlertDialog a = new AlertDialog.Builder(this).create();
		a.setTitle("What is LogLoc?");
		a.setMessage(getResources().getString(R.string.app_description));
		a.show();
		
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
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopLogger(null);
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
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
		
		if (position > 0) {
			String contact = allContacts.get(position);
			selectedNumbers.add(
					contact.substring(contact.lastIndexOf(' ') + 1)
			);
			selectedContacts.add(contact);
		}
		
		updateSelectedContacts();
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}
	
	public void startLogger(View v) {
		String message = "";
		if (selectedNumbers.size() < 1) {
			message = "• Select at least 1 contact.\n\n";
		}
		LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (manager != null && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			message += "• Turn on GPS-location.";
		}
		if (message.length() > 0) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setMessage(message.trim());
			alertDialog.show();
			return;
		}
		
		startCheckingGeoLocation();
		Toast.makeText(this, "Started New Logger", Toast.LENGTH_SHORT).show();
		toggleStartStop();
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
		toggleStartStop();
	}
	
	public void removeContact(View v) {
		if (selectedNumbers.size() > 0) {
			selectedNumbers.remove(selectedNumbers.size() - 1);
			selectedContacts.remove(selectedContacts.size() - 1);
			updateSelectedContacts();
		}
	}
	
	public void removeSearchText(View v) {
		searchContact.setText("");
	}
	
	private void initAll() {
		allContacts = new ArrayList<>();
		selectedContacts = new ArrayList<>();
		selectedNumbers = new ArrayList<>();
		_repeatDurations = new String[]{"2", "3", "5", "10", "15", "20", "25"};
		
		spinnerContacts = (Spinner) findViewById(R.id.spinnerContacts);
		spinnerTimeDuration = (Spinner) findViewById(R.id.spinnerTimeDuration);
		searchContact = (EditText) findViewById(R.id.editText);
		selectedContactsView = (TextView) findViewById(R.id.textView);
		searchTitle = (TextView) findViewById(R.id.textView3);
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		startButton = (Button) findViewById(R.id.startbutton);
		stopButton = (Button) findViewById(R.id.stopButton);
		stopButton.setEnabled(false);
		
		initSearchContactTextInput();
		getContactList("");
		initTimeDurations();
	}
	
	private void denied() {
		Toast.makeText(this, "Permission(s) Denied !", Toast.LENGTH_LONG).show();
		ScrollView parent = ((ScrollView) findViewById(R.id.parentView));
		parent.removeAllViews();
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
		final String searching = "Searching Contacts...";
		final String searchComplete = "Select contacts to inform:";
		searchTitle.setText(searching);
		
		if (readContactsAsync != null) {
			readContactsAsync.cancel(true);
		}
		
		readContactsAsync = new ReadContactsAsync(new ContactsResponse() {
			@Override
			public void onContactsRead(ArrayList<String> list) {
				allContacts = list;
				searchTitle.setText(searchComplete);
				ArrayAdapter<String> adapter = new ArrayAdapter<>(
						MainActivity.this,
						android.R.layout.simple_spinner_item,
						allContacts
				);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				
				spinnerContacts.setAdapter(adapter);
				spinnerContacts.setSelection(Adapter.NO_SELECTION, true);
				spinnerContacts.setOnItemSelectedListener(MainActivity.this);
			}
		}, this.getContentResolver());
		
		readContactsAsync.execute(matchString);
	}
	
	private void initTimeDurations() {
		String[] repeatDurations = new String[_repeatDurations.length];
		for (int i = 0; i < _repeatDurations.length; i++) {
			repeatDurations[i] = _repeatDurations[i] + " minutes";
		}
		
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
				selectedDuration = (Long.parseLong(_repeatDurations[position], 10) * 1000 * 60);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
	}
	
	private void toggleStartStop() {
		startButton.setEnabled(locationListener == null);
		stopButton.setEnabled(locationListener != null);
	}
	
	private void initSearchContactTextInput() {
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
	
	private void updateSelectedContacts() {
		selectedContactsView.setText(_arrayListToString(selectedContacts));
	}
	
	private String _arrayListToString(ArrayList<String> list) {
		StringBuilder s = new StringBuilder();
		for (String str : list) {
			s.append(str);
			s.append("\n");
		}
		return s.toString();
	}
	
}