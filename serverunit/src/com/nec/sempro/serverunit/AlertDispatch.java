package com.nec.sempro.serverunit;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.telephony.SmsManager;
import android.widget.Toast;



public class AlertDispatch {

	private String phonenumber = "9790977336";
	//private String phonenumber = "9840186146";
    private	String timestamp = new SimpleDateFormat("dd-MMM-yyyy h:mm:ssa").format(new Date());
	private String message = "Intrusion detected at " + timestamp;

	
	 public AlertDispatch (Context context){
		 	
		     SmsManager sms = SmsManager.getDefault();
	       // sms.sendTextMessage(phonenumber, null, message, null,null);
	        Toast.makeText(context, "Alert Dispatched to client" + message, Toast.LENGTH_SHORT).show();
	       
	}
	
}
