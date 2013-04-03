package com.nec.sempro.serverunit;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.nec.sempro.serverunit.hosting.StreamCameraActivity;



public class AlertDispatch {

	//private String phonenumber = "9790977336";
	private String phonenumber = "9840186146";
    private	String timestamp = new SimpleDateFormat("dd-MMM-yyyy h:mm:ssa").format(new Date());
    private String ipaddr = tryGetIpAddress(); 
    private String message = "http://" + ipaddr +":8080" + "Intrusion detected at " + timestamp;

	
	 public AlertDispatch (Context context){
		 	
		     SmsManager sms = SmsManager.getDefault();
	        sms.sendTextMessage(phonenumber, null, message, null,null);
	        Toast.makeText(context, "Alert Dispatched to client" + message, Toast.LENGTH_SHORT).show();
	       
	}
	
	 public static String tryGetIpAddress()
	    {
	 try {
         List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
         for (NetworkInterface intf : interfaces) {
             List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
             for (InetAddress addr : addrs) {
                 if (!addr.isLoopbackAddress()) {
                     String sAddr = addr.getHostAddress().toUpperCase();
                     boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
                     boolean useIPv4 = true;
                     if (useIPv4) {
                         if (isIPv4) 
                             return sAddr;
                     } else {
                         if (!isIPv4) {
                             int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                             return delim<0 ? sAddr : sAddr.substring(0, delim);
                         }
                     }
                 }
             }
         }
     } catch (Exception ex) { } // for now eat exceptions
     return "";
	}
 

}
