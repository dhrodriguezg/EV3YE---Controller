package ca.ualberta.ev3ye.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import ca.ualberta.ev3ye.controller.comm.ClientTCP;
import ca.ualberta.ev3ye.controller.comm.auxiliary.AppState;
import ca.ualberta.ev3ye.controller.comm.auxiliary.TwoLineArrayAdapter;
import ca.ualberta.ev3ye.controller.comm.auxiliary.WiFiP2PBroadcastReceiver;
import ca.ualberta.ev3ye.controller.comm.auxiliary.WiFiP2PBroadcastReceiver.WiFiP2PBroadcastCallbacks;
import ca.ualberta.ev3ye.controller.comm.logic.BluetoothCom;
import ca.ualberta.ev3ye.controller.comm.logic.RegexValidator;
import ca.ualberta.ev3ye.controller.streaming.ControllerActivity;


public class MainActivity
		extends Activity
		implements WiFiP2PBroadcastReceiver.WiFiP2PBroadcastCallbacks
{
	protected ViewHolder               viewHolder            = null;
	protected BluetoothCom             com                   = null;
	protected WifiP2pManager           p2pManager            = null;
	protected WifiP2pManager.Channel   p2pChannel            = null;
	protected WifiP2pConfig            p2pConfig             = null;
	protected WiFiP2PBroadcastReceiver p2pBroadcastReceiver  = null;
	protected IntentFilter             p2pIntentFilter       = null;
	protected P2PDiscoveryReceiver     p2pDiscoveryReceiver  = null;
	protected P2PPeerListReceiver      p2pPeerListReceiver   = null;
	protected P2PConnectionReceiver    p2pConnectionReceiver = null;
	protected P2PInfoReceiver          p2pInfoReciever       = null;
	
	boolean goodConnection = false;
	private ClientTCP clientTCP = null;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		Log.v( AppState.LOG_TAG, "[INFO] > ----- MainActivity onCreate() -----" );
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		viewHolder = new ViewHolder();
		viewHolder.init();
		
		initWiFiP2p();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		this.registerReceiver( p2pBroadcastReceiver, p2pIntentFilter );
		p2pManager.discoverPeers(p2pChannel, p2pDiscoveryReceiver);
	}

	@Override
	protected void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );
	}

	@Override
	protected void onRestoreInstanceState( Bundle savedInstanceState )
	{
		super.onRestoreInstanceState( savedInstanceState );
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		this.unregisterReceiver( p2pBroadcastReceiver );
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
		stopWiFiP2p();
	}

	private void stopWiFiP2p()
	{
		
	}

	@Override
	protected void onDestroy()
	{
		Log.v( AppState.LOG_TAG, "[INFO] > ----- MainActivity onDestroy() -----" );
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.menu_main, menu );
		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch ( item.getItemId() )
		{
		case R.id.action_settings:
			return true;

		default:
			return super.onOptionsItemSelected( item );
		}
	}

	@Override
	public void onP2pStateChanged( Context context, Intent intent )
	{
		Log.v( AppState.LOG_TAG, "[WIFI] > P2P state changed." );
	}

	@Override
	public void onP2pPeersChanged( Context context, Intent intent )
	{
		Log.v( AppState.LOG_TAG, "[WIFI] > P2P peers changed." );
		if ( p2pManager != null )
		{
			p2pManager.requestPeers( p2pChannel, p2pPeerListReceiver );
		}
	}

	@Override
	public void onP2pConnectionChanged( final Context context, Intent intent )
	{
		Log.v( AppState.LOG_TAG, "[WIFI] > P2P connection changed." );
		if(goodConnection){ //already connected to group leader. So leader info should be available now.
			p2pManager.requestConnectionInfo(p2pChannel, new WifiP2pManager.ConnectionInfoListener() {
				@Override
				public void onConnectionInfoAvailable(WifiP2pInfo info) {
					if(info.groupFormed){
						Toast.makeText(context, "Leader found!", Toast.LENGTH_LONG).show();
						connectToLeader(info);
					}else{
						Toast.makeText(context, "NO leader found :(", Toast.LENGTH_SHORT).show();
						System.out.println("Group not formed yet :(");
					}
				}
			});
			//goodConnection = false; //in case I want to connect to a second device...
		}
	}
	
	private void connectToLeader(final WifiP2pInfo info){
		Thread thread = new Thread() {
            public void run() {
            	if(clientTCP==null){
					System.out.println("Group exists!, creating connection");
					clientTCP = new ClientTCP(info.groupOwnerAddress.getHostAddress(),true);
					boolean isCamera = clientTCP.greetServer();
					if(isCamera){
						viewHolder.enableNewActivity();
					}
				}else{
					System.out.println("Group exists! but conecction already created....");
				}
            }
        };
        thread.start();
	}

	@Override
	public void onP2pThisDeviceChanged( Context context, Intent intent )
	{
		Log.v( AppState.LOG_TAG, "[WIFI] > P2P local device changed." );
	}

	private void initWiFiP2p()
	{
		p2pManager = (WifiP2pManager) getSystemService( Context.WIFI_P2P_SERVICE );
		p2pChannel = p2pManager.initialize( this, getMainLooper(), null );
		p2pBroadcastReceiver = new WiFiP2PBroadcastReceiver( this );
		p2pDiscoveryReceiver = new P2PDiscoveryReceiver();
		p2pPeerListReceiver = new P2PPeerListReceiver();
		p2pConnectionReceiver = new P2PConnectionReceiver();

		p2pIntentFilter = new IntentFilter();
		p2pIntentFilter.addAction( WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION );
		p2pIntentFilter.addAction( WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION );
		p2pIntentFilter.addAction( WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION );
		p2pIntentFilter.addAction( WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION );
	}

	/**
	 * A receiver for P2P discovery results. Simply notifies success or failure.
	 */
	protected class P2PDiscoveryReceiver
			implements WifiP2pManager.ActionListener
	{

		@Override
		public void onSuccess()
		{
			Log.v( AppState.LOG_TAG, "[WIFI] > P2P discovery success." );
			p2pManager.requestPeers(p2pChannel, p2pPeerListReceiver);
		}

		@Override
		public void onFailure( int reason )
		{
			Log.e( AppState.LOG_TAG, "[WIFI] > P2P discovery failed with reason: " + reason );
		}
	}

	/**
	 * A receiver for P2P peer lists. Provides the list of all available WiFi peers.
	 */
	protected class P2PPeerListReceiver
			implements WifiP2pManager.PeerListListener
	{
		@Override
		public void onPeersAvailable( WifiP2pDeviceList peers )
		{
			Log.v( AppState.LOG_TAG, "[WIFI] > P2P peers available: " + peers.getDeviceList().size() );
			viewHolder.populateP2pList( peers );
		}
	}

	/**
	 * A receiver for P2P connection events.
	 */
	protected class P2PConnectionReceiver
			implements WifiP2pManager.ActionListener
	{
		@Override
		public void onSuccess()
		{
			Log.v( AppState.LOG_TAG, "[WIFI] > P2P connection accepted!" );
			if(goodConnection){ //already connected to group leader. So leader info should be available now.
				p2pManager.requestConnectionInfo(p2pChannel, new WifiP2pManager.ConnectionInfoListener() {
					@Override
					public void onConnectionInfoAvailable(WifiP2pInfo info) {
						if(info.groupFormed){
							Toast.makeText(MainActivity.this, "Leader found!", Toast.LENGTH_LONG).show();
							connectToLeader(info);
						}else{
							Toast.makeText(MainActivity.this, "NO leader found :(", Toast.LENGTH_SHORT).show();
							System.out.println("Group not formed yet :(");
						}
					}
				});
			}
			goodConnection = true;
			// http://developer.android.com/guide/topics/connectivity/wifip2p.html

		}

		@Override
		public void onFailure( int reason )
		{
			Log.e( AppState.LOG_TAG, "[WIFI] > P2P connection failed with reason: " + reason );
		}
	}

	/**
	 *
	 */
	protected class P2PInfoReceiver
		implements WifiP2pManager.ConnectionInfoListener
	{
		@Override
		public void onConnectionInfoAvailable(WifiP2pInfo info)
		{
			if (!info.groupFormed)
			{
				Log.e(AppState.LOG_TAG, "[WIFI] > P2P group not formed");
			}
			
			Log.d(AppState.LOG_TAG, "[WIFI] > Group formed:");
			Log.d(AppState.LOG_TAG, "[WIFI] >     isGroupOwner:" + info.isGroupOwner);
			Log.d(AppState.LOG_TAG, "[WIFI] >     groupOwnerAddress:" + info.groupOwnerAddress);
		}	
	}
	
	protected class ViewHolder
	{
		public static final String MODE_HOTSPOT = "WiFi Hotspot";
		public static final String MODE_IP      = "IP Address";
		public static final String MODE_P2P     = "WiFi Direct";
		public Spinner                             modeSpinner            = null;
		public EditText                            ipEntry                = null;
		public Spinner                             p2pSpinner             = null;
		public Button                              goButton               = null;
		public ArrayAdapter<String>                modesArrayAdapter      = null;
		public ArrayAdapter<String>                wifiP2pArrayAdapter    = null;
		public Map<String, WifiP2pDevice>          deviceMap              = new HashMap<> ();
		public List<String>                        modes                  = null;
		public List<String>                        wifiP2pDevices         = null;

		public ViewHolder()
		{
			modeSpinner = (Spinner) findViewById(R.id.main_mode_spinner);
			ipEntry = (EditText) findViewById(R.id.main_addr_entry);
			p2pSpinner = (Spinner) findViewById(R.id.main_partner_spinner);
			goButton = (Button) findViewById( R.id.main_go_button );

			modes = new ArrayList<>();
			modes.add( MODE_HOTSPOT );
			modes.add( MODE_IP );
			modes.add( MODE_P2P );
			modesArrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, modes);
			modeSpinner.setAdapter(modesArrayAdapter);
			modesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			
			wifiP2pDevices = new ArrayList<>();
			wifiP2pArrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, wifiP2pDevices);
			wifiP2pArrayAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
			p2pSpinner.setAdapter(wifiP2pArrayAdapter);
		}

		public void init()
		{
			setupListeners();
		}
		
		private void enableNewActivity(){
	        runOnUiThread(new Runnable() {
	            @Override
	            public void run() {
	            	goButton.setVisibility(Button.VISIBLE);;
	            	goButton.setBackgroundColor(Color.GREEN);
	            }
	        });
	    }
		
		public String intToIPv4(int i) {
			   return ((i) & 0xFF ) + "." + ((i >> 8 ) & 0xFF) + "." +((i >> 16 ) & 0xFF) + "." +( i >> 24 & 0xFF) ;
		}

		private void populateP2pList( WifiP2pDeviceList peers )
		{
			wifiP2pDevices.clear();
			deviceMap.clear();

			if ( !peers.getDeviceList().isEmpty() )
			{
				Log.v( AppState.LOG_TAG, "[WIFI] > Found peers:" );
			}

			for ( WifiP2pDevice device : peers.getDeviceList() )
			{
				Log.v( AppState.LOG_TAG,
					   "[WIFI] >     " + device.deviceName + " at " + device.deviceAddress );
				wifiP2pDevices.add( device.deviceName );
				deviceMap.put(device.deviceName, device);
			}

			wifiP2pArrayAdapter.notifyDataSetChanged();
		}

		private void setupListeners()
		{			
			modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3)
				{
					/* Doesn't work for some reason.
					String item = (String) arg0.getItemAtPosition(arg2);
					switch (item)
					{
					case MODE_HOTSPOT:
						disable(ipEntry);
						disable(p2pPartner);
						break;
						
					case MODE_IP:
						enable(ipEntry);
						disable(p2pPartner);
						break;
						
					case MODE_P2P:
						disable(ipEntry);
						enable(p2pPartner);
						break;
						
					default:
						
					}//*/
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
				
				private void disable(View v)
				{
					v.setActivated(false);
					v.setFocusable(false);
					v.setClickable(false);
				}
				
				private void enable(View v)
				{
					v.setActivated(true);
					v.setFocusable(true);
					v.setClickable(true);
					v.requestFocus();
				}
			});
			
			p2pSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3)
				{
					
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
			});
			
			goButton.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick( View v )
				{
					switch((String) modeSpinner.getSelectedItem())
					{
					case MODE_HOTSPOT:
						hotspotConnect();
						break;
						
					case MODE_IP:
						ipConnect();
						break;
						
					case MODE_P2P:
						p2pConnect();
						break;
						
					default:
							
					}
				}
			} );
		}
		
		protected void hotspotConnect()
		{
			Intent myIntent = new Intent(MainActivity.this, ControllerActivity.class);
			String ipv4 = null;
			try{
				if(clientTCP==null){//not connected to group leader
					WifiManager wifii= (WifiManager) getSystemService(Context.WIFI_SERVICE);
					DhcpInfo d=wifii.getDhcpInfo();
					ipv4 = intToIPv4(d.gateway);
				}else{
					ipv4=clientTCP.getServerAddress();
				}
			}catch (Exception e){
				e.printStackTrace();
			}
			if(ipv4==null){
				Toast.makeText(MainActivity.this, "Not connected to Camera yet", Toast.LENGTH_LONG).show();
			}else if(ipv4.equals("0.0.0.0")){
				Toast.makeText(MainActivity.this, "Not connected to Camera yet", Toast.LENGTH_LONG).show();
			}else{
				myIntent.putExtra("CameraIP", ipv4); //Optional parameters
				MainActivity.this.startActivity(myIntent);
			}
		}

		private void ipConnect()
		{
			Intent myIntent = new Intent(MainActivity.this, ControllerActivity.class);
			String ipv4 = ipEntry.getText().toString();
			if(!isValidInet4Address(ipv4)){
				Toast.makeText(MainActivity.this, "That is not a valid IP address!", Toast.LENGTH_LONG).show();
			}else{
				myIntent.putExtra("CameraIP", ipv4); //Optional parameters
				MainActivity.this.startActivity(myIntent);
			}
		}

		private void p2pConnect()
		{
			String arg = (String) p2pSpinner.getSelectedItem();
			if(arg == null || arg.isEmpty())
			{
				Toast.makeText(MainActivity.this, "You haven't selected a WiFi Direct device to connect to!", Toast.LENGTH_LONG).show();
				return;
			}
			
			WifiP2pDevice device = deviceMap.get(arg);
			
			p2pManager.connect(p2pChannel, p2pConfig, p2pConnectionReceiver);
		}
		
		private static final String IPV4_REGEX = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
		private final RegexValidator ipv4Validator = new RegexValidator(IPV4_REGEX);
	    /**
	     * Taken from the Apache Commons. Licensed under the Apache License version 2.
	     * http://svn.apache.org/viewvc/commons/proper/validator/trunk/src/main/java/org/apache/commons/validator/routines/InetAddressValidator.java?view=markup
	     * Validates an IPv4 address. Returns true if valid.
		 * @param inet4Address the IPv4 address to validate
		 * @return true if the argument contains a valid IPv4 address
		 */
		public boolean isValidInet4Address(String inet4Address) {
		// verify that address conforms to generic IPv4 format
		String[] groups = ipv4Validator.match(inet4Address);
		
		if (groups == null) {
		return false;
		}
		
		// verify that address subgroups are legal
		for (int i = 0; i <= 3; i++) {
		String ipSegment = groups[i];
		if (ipSegment == null || ipSegment.length() == 0) {
		return false;
		}
		
		int iIpSegment = 0;
		
		try {
		iIpSegment = Integer.parseInt(ipSegment);
		} catch(NumberFormatException e) {
		return false;
		}
		
		if (iIpSegment > 255) {
		return false;
		}
		
		if (ipSegment.length() > 1 && ipSegment.startsWith("0")) {
		return false;
		}
		
		}
		
		return true;
		}
	}
}

