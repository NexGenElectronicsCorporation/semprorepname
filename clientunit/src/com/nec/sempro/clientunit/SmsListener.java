package com.nec.sempro.clientunit;

import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;

import android.widget.Toast;

public class SmsListener  extends BroadcastReceiver{
	 
    
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub

        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
            Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
            SmsMessage[] msgs = null;
            String msg_from= "";
            String str = "";
            if (bundle != null){
                //---retrieve the SMS message received---
                try{   
            	Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for(int i=0; i<msgs.length; i++){
                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        String msgBody = msgs[i].getMessageBody();
                        //str = "SMS from " + msgs[i].getOriginatingAddress()+ " :" + msgs[i].getMessageBody().toString();
                        str = "Intel received from Surveillance System :" + msgs[i].getMessageBody().toString();
                    }
                }
                    catch(Exception e){
                      Log.d("Exception caught",e.getMessage());
          }
                
                    }
                    Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
                    if(msg_from.equals("+918939544345")||msg_from.equals("15555215556")||msg_from.equals("8939544345")){                    	
                    Intent i = new Intent();
                    i.putExtra("sendstr", str);
                    
                    i.setClassName("com.nec.sempro.clientunit", "com.nec.sempro.clientunit.AlertNotifier");
                   i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);}  
                    }
            }
        }
    

	/*@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sms_listener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sms_listener, menu);
		return true;
	}

}*/
