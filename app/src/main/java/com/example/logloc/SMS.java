package com.example.logloc;

import android.telephony.SmsManager;

import java.util.ArrayList;

public class SMS {
	static void send(String message, ArrayList<String> selectedNumbers) {
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
}
