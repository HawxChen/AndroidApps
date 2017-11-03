package com.example.neuromovieclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts.Intents.Insert;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;






import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;

public class HelloEEGActivity extends Activity {

	private static final String TAG = "HelloEEG";

	BluetoothAdapter            bluetoothAdapter;
	TGDevice                    device;
	RSA							R1 ;
	final boolean               rawEnabled = true;

	ScrollView                  sv;
	EditText 					et;
	TextView                    tv;
	Button                      b;
	static int 					i = 0;
	static int 					count = 0;
	Socket 						socketObject;
	String 						Operation ;
	String[]					Signal = new String[15502];
	String 						predictRCV = "0 0 0 0 0 0";
	String 						st, FinalSignal = "";
	PrintStream 				out;
	String 						HashValue;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		R1 = new RSA();
		sv = (ScrollView)findViewById(R.id.scrollView1);
		et = (EditText)findViewById(R.id.editText1);
		tv = (TextView)findViewById(R.id.textView1);
		et.setText("0");
		tv.setText( "" );
		tv.append( "Android version: " + Integer.valueOf(android.os.Build.VERSION.SDK) + "\n" );

		// Check if Bluetooth is available on the Android device
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if( bluetoothAdapter == null ) {            

			// Alert user that Bluetooth is not available
			Toast.makeText( this, "Bluetooth not available", Toast.LENGTH_LONG ).show();
			//	finish();
			return;

		} else {

			// create the TGDevice 	
			device = new TGDevice(bluetoothAdapter, handler);

		} 

		tv.append("NeuroSky: " + TGDevice.version + " " + TGDevice.build_title);
		tv.append("\n" );


	} 
	/* end onCreate() */



	@Override
	public void onStart() {
		super.onStart();
		//If BT is not on, request that it be enabled.
		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, 1);
		}
		
		
	}

	@Override
	public void onPause() {
		try{
			
			super.onPause();
			socketObject.close();
			//device.close();
			
		}
		catch(Exception E)
		{

		}
	}

	@Override
	public void onStop()  {
		try{
			super.onStop();
			socketObject.close();
			device.close();
			
		}
		catch(Exception E)
		{

		}
	}

	@Override
	public void onDestroy() {

		try{
			device.close();
			socketObject.close();
			super.onDestroy();
		}
		catch(Exception E)
		{

		}

	}

	byte[] hashCalculate(String st)throws Exception{
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		byte[] inputBytes = st.getBytes();
		byte[] hashBytes = messageDigest.digest(inputBytes);
		return hashBytes;
	};


	/**
	 * Handles messages from TGDevice
	 */
	final Handler handler = new Handler(){  	  	
		@Override
		public void handleMessage( Message msg ) {
			
			switch( msg.what ) {
			case TGDevice.MSG_STATE_CHANGE:
				switch( msg.arg1 ) {
				case TGDevice.STATE_IDLE:
					break;
				case TGDevice.STATE_CONNECTING:       	
					tv.append( "Connecting...\n" );
					break;	
				case TGDevice.STATE_CONNECTED:
					tv.append( "Connected.\n" );
					device.start();
					break;
				case TGDevice.STATE_NOT_FOUND:
					tv.append( "Could not connect any of the paired BT devices.  Turn them on and try again.\n" );
					break;
				case TGDevice.STATE_ERR_NO_DEVICE:
					tv.append( "No Bluetooth devices paired.  Pair your device and try again.\n" );
					break;
				case TGDevice.STATE_ERR_BT_OFF:
					tv.append( "Bluetooth is off.  Turn on Bluetooth and try again." );
					break;
				case TGDevice.STATE_DISCONNECTED:
					tv.append( "Disconnected.\n" );
				} /* end switch on msg.arg1 */
				break;
			case TGDevice.MSG_RAW_DATA:
				String st = Integer.toString(msg.arg1);
				
				if (i<15502){
					Signal[i] = st;
				}
				
				
				//FinalSignal = FinalSignal + st + ",";
				
				Log.i("Length value",Integer.toString(i));
				if(i > 15502){

					//Log.i("String length",Integer.toString(FinalSignal.length()));
					count++;
					FinalSignal = Arrays.toString(Signal);	
					if(count == 1)
					{

						try{
							
							//device.connect( false );
							
							new Thread(new Client()).start();
							this.wait(5000);
						}
						catch(Exception E){

						}
						break;
					}
				}
				i++;
				break;
			default:
				break;
			} /* end switch on msg.what */
		} /* end handleMessage() */
	}; /* end Handler */
	/**
	 * This method is called when the user clicks on the "Connect" button.
	 * 
	 * @param view
	 */
	public void doStuff(View view) {
		Operation = "Registration";

		if(et.getText()==null)
		{
			Toast.makeText( this, " Please enter subject ", Toast.LENGTH_LONG ).show();
		}
		else
		{
			i = 0 ;
			if( device.getState() != TGDevice.STATE_CONNECTING && device.getState() != TGDevice.STATE_CONNECTED ) {
				device.connect( true );
			}
		}
	} /* end doStuff() */
	public void doStuffIden(View view) {

		if(et.getText()==null)
		{
			Toast.makeText( this, "Please enter subject", Toast.LENGTH_LONG ).show();
		}
		else {
			Operation = "Identification";
			i = 0 ;
			if( device.getState() != TGDevice.STATE_CONNECTING && device.getState() != TGDevice.STATE_CONNECTED ) {
				device.connect( true );
			}
		}
	} /* end doStuff() */
	public void doStuffCom(View view) {

		if(et.getText()==null){
			Toast.makeText( this, "Please enter subject", Toast.LENGTH_LONG ).show();
		}
		else{
			Operation = "Comparator";
			i = 0 ;
			if( device.getState() != TGDevice.STATE_CONNECTING && device.getState() != TGDevice.STATE_CONNECTED ) {
				device.connect( true );
			}
		}

	} /* end doStuff() */
	class Client implements Runnable {
		public void run(){
			try{
				String ip ="10.218.111.164";
						
				socketObject = new Socket();
				socketObject.connect(new InetSocketAddress(ip,10007),1000);
				try{
					out = new PrintStream (socketObject.getOutputStream());
					out.println(Operation);
					out.flush();
					out.println(et.getText());
					out.flush();
					out.println(Arrays.toString(hashCalculate(FinalSignal)));
					out.flush();
					out.println(FinalSignal);
					out.flush();
					out.println(Integer.toString(FinalSignal.length()));
					out.flush();
					out.println(Arrays.toString(R1.encrypt(FinalSignal.getBytes())));
					out.flush();
				}finally{
					//socketObject.close();
				}
			}catch(Exception E){
				E.printStackTrace();
			}
		}
	}
} /* end HelloEEGActivity() */