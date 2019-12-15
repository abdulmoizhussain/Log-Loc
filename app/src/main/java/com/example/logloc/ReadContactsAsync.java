package com.example.logloc;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import java.util.ArrayList;

public class ReadContactsAsync extends AsyncTask<String, Integer, ArrayList<String>> {
	private IContactsResponse delegate;
	private ContentResolver cr;

	ReadContactsAsync(IContactsResponse IContactsResponse, ContentResolver _cr) {
		this.delegate = IContactsResponse;
		cr = _cr;
	}

	@Override
	public ArrayList<String> doInBackground(String... params) {
		String matchString = params[0];
		ArrayList<String> allContacts = new ArrayList<>();
		allContacts.add("select contact");

		String selection = matchString.length() > 0 ? ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?" : null;
		String[] selectionArgs = matchString.length() > 0 ? new String[]{"%" + matchString + "%"} : null;

		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
				null,
				selection,
				selectionArgs,
				ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

		if ((cur != null ? cur.getCount() : 0) > 0) {

			while (!isCancelled() && cur.moveToNext()) {
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


					while (!isCancelled() && pCur != null && pCur.moveToNext()) {
						String phoneNo = pCur.getString(pCur.getColumnIndex(
								ContactsContract.CommonDataKinds.Phone.NUMBER));

						if (phoneNo.indexOf(' ') == -1) {
							allContacts.add(name + " " + phoneNo);
						}
					}

					if (pCur != null) {
						pCur.close();
					}
				}
			}
		}

		if (cur != null) {
			cur.close();
		}

		return allContacts;
	}

	@Override
	public void onPostExecute(ArrayList<String> contacts) {
		super.onPostExecute(contacts);
		delegate.onContactsRead(contacts);
	}

	@Override
	public void onProgressUpdate(Integer... params) {
	}

	@Override
	public void onPreExecute() {
	}

}
