package com.example.logloc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

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
	private final int
			PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED,
			APP_PERMISSIONS_REQUEST_CODE = 10;
	private ReadContactsAsync readContactsAsync;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		firstShowAppDescription();
	}

	private void firstShowAppDescription() {
		View checkBoxView = View.inflate(this, R.layout.start_up_note, null);
		CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Save to shared preferences
			}
		});
		checkBox.setText("Do not Show again");

		AlertDialog a = new AlertDialog.Builder(this).create();
		a.setTitle("What is LogLoc?");
//		a.setView(checkBoxView);
//		a.setCancelable(false);
		a.setMessage(getResources().getString(R.string.app_description));
		a.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						continueApplication();
					}
				});
		a.show();
	}

	private void continueApplication() {
		int finePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
		int coarsePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
		int smsPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
		int contactsPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);

		if (finePermission != PERMISSION_GRANTED || coarsePermission != PERMISSION_GRANTED ||
				smsPermission != PERMISSION_GRANTED || contactsPermission != PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				requestPermissions(new String[]{
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.SEND_SMS,
						Manifest.permission.READ_CONTACTS,
				}, APP_PERMISSIONS_REQUEST_CODE);
			} else {
				accessDenied();
			}
			return;
		}
		initAll();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == APP_PERMISSIONS_REQUEST_CODE) {

			for (int result : grantResults) {
				if (result != PERMISSION_GRANTED) {
					accessDenied();
					break;
				}
			}

			initAll();
		}
	}

	@SuppressLint("MissingPermission")
	private void startCheckingGeoLocation() {
		stopLogger(null);
		locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				String lat = Double.toString(location.getLatitude());
				String lng = Double.toString(location.getLongitude());

				String textMessage = "Check my current location: " +
						"https://maps.google.com/maps?q=" +
						lat + "," + lng;
				SMS.send(textMessage, selectedNumbers);
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			@Override
			public void onProviderEnabled(String provider) {
				showToast("GPS Enabled", false);
			}

			@Override
			public void onProviderDisabled(String provider) {
				showToast("GPS is disabled", true);
				stopLogger(null);
			}
		};

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
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

	private void setStartupNoteCheckbox() {

	}

	private void initAll() {
		allContacts = new ArrayList<>();
		selectedContacts = new ArrayList<>();
		selectedNumbers = new ArrayList<>();

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
		toggleStartStopButton();
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
		showToast("Started New Logger", false);
		startService(new Intent(this, LogLocationService.class));
		toggleStartStopButton();
	}

	public void stopLogger(View v) {
		if (locationListener != null) {
			locationManager.removeUpdates(locationListener);
			locationListener = null;
		}
		toggleStartStopButton();
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

	// ================================= PRIVATE METHODS ============================================

	private void accessDenied() {
		showToast("Permission(s) Denied !", true);
		finish();
//		ScrollView parent = (ScrollView) findViewById(R.id.parentView);
//		parent.removeAllViews();
	}

	private void getContactList(String matchString) {
		allContacts.clear();
		{
			final String searching = "Searching Contacts...";
			searchTitle.setText(searching);
		}

		if (readContactsAsync != null) {
			readContactsAsync.cancel(true);
		}

		readContactsAsync = new ReadContactsAsync(new IContactsResponse() {
			@Override
			public void onContactsRead(ArrayList<String> list) {
				allContacts = list;
				{
					final String searchComplete = "Select contacts whom to inform:";
					searchTitle.setText(searchComplete);
				}
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
		String[] repeatDurations = new String[Globals.rDurations.length];
		for (int i = 0; i < Globals.rDurations.length; i++) {
			repeatDurations[i] = Globals.rDurations[i] + " minutes";
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
				selectedDuration = (Long.parseLong(Globals.rDurations[position], 10) * 1000 * 60);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
	}

	private void toggleStartStopButton() {
		try {
			startButton.setEnabled(locationListener == null);
			stopButton.setEnabled(locationListener != null);
		} catch (Exception e) {
//			e.printStackTrace();
			System.out.println("Exception e" + e.toString());
		}
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

	/**
	 * @param msg        the message for toast
	 * @param lengthLong true->LONG false->SHORT
	 */
	private void showToast(String msg, boolean lengthLong) {
		Toast.makeText(MainActivity.this, msg, lengthLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
	}
}
