package ca.ualberta.ev3ye.controller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.ev3ye.controller.R;
import ca.ualberta.ev3ye.controller.comm.ClientTCP;
import ca.ualberta.ev3ye.controller.comm.auxiliary.AppState;
import ca.ualberta.ev3ye.controller.comm.auxiliary.TwoLineArrayAdapter;
import ca.ualberta.ev3ye.controller.comm.auxiliary.WiFiP2PBroadcastReceiver;
import ca.ualberta.ev3ye.controller.comm.logic.BluetoothCom;
import ca.ualberta.ev3ye.controller.streaming.ControllerActivity;

/*
* NOTE TO SELF:
* WIFI COMMAND FORMAT:
* 		;left_right_cam
* 	EG:
	* 	;100_100_100
* */

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
	protected GamePadHandler           gamePad               = null;
	
	private static final boolean isWiFiDirect = false;
	public Context context = null;
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
		context = this;
		if(isWiFiDirect)
			initWiFiP2p();

		checkForJoystick();
	}

	private void checkForJoystick()
	{
		ArrayList< Integer > gameControllerDeviceIds = new ArrayList<>();
		int[] deviceIds = InputDevice.getDeviceIds();
		for ( int deviceId : deviceIds )
		{
			InputDevice dev = InputDevice.getDevice( deviceId );
			int sources = dev.getSources();

			// Verify that the device has gamepad buttons, control sticks, or both.
			if ( ( ( sources & InputDevice.SOURCE_GAMEPAD ) == InputDevice.SOURCE_GAMEPAD ) && ( ( sources & InputDevice.SOURCE_JOYSTICK ) == InputDevice.SOURCE_JOYSTICK ) )
			{
				// This device is a game controller. Store its device ID.
				if ( !gameControllerDeviceIds.contains( deviceId ) )
				{
					gameControllerDeviceIds.add( deviceId );
				}
			}
		}

		Log.v( AppState.LOG_TAG,
			   "[JOYS] > Device has " + gameControllerDeviceIds + " game controllers." );

		if ( gameControllerDeviceIds.size() > 0 )
		{
			int deviceId = gameControllerDeviceIds.get( 0 );

			Log.v( AppState.LOG_TAG, "[JOYS] > Using device ID " + deviceId + " as gamepad." );
			gamePad = new GamePadHandler( deviceId );
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if(isWiFiDirect){
			this.registerReceiver( p2pBroadcastReceiver, p2pIntentFilter );
			viewHolder.wifiP2pRefreshButton.callOnClick();
		}
		//viewHolder.bluetoothRefreshButton.callOnClick();
		
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
		if(isWiFiDirect)
			this.unregisterReceiver( p2pBroadcastReceiver );
	}

	@Override
	protected void onDestroy()
	{
		Log.v( AppState.LOG_TAG, "[INFO] > ----- MainActivity onDestroy() -----" );
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event )
	{
		if ( ( event.getSource() & InputDevice.SOURCE_GAMEPAD ) == InputDevice.SOURCE_GAMEPAD )
		{
			gamePad.handleEventAuto( keyCode, event );
			return true;
		}
		else
		{
			return super.onKeyDown( keyCode, event );
		}
	}

	@Override
	public boolean onGenericMotionEvent( MotionEvent event )
	{
		if ( ( event.getSource() & InputDevice.SOURCE_JOYSTICK ) == InputDevice.SOURCE_JOYSTICK && event.getAction() == MotionEvent.ACTION_MOVE && gamePad != null )
		{
			gamePad.handleEventAuto( event );
			return true;
		}
		else
		{
			return super.onGenericMotionEvent( event );
		}
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

	public void connectBT()
	{
		com = new BluetoothCom();
		com.enableBT( this );
		if ( com.connectToNXTs() )
		{
			try
			{
				com.writeMessage( "Hello EV3" );
			}
			catch ( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
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
			Log.v( AppState.LOG_TAG, "[WIFI] > P2P peers available." );
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
							Toast.makeText(context, "Leader found!", Toast.LENGTH_LONG).show();
							connectToLeader(info);
						}else{
							Toast.makeText(context, "NO leader found :(", Toast.LENGTH_SHORT).show();
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

	protected class GamePadHandler
	{
		public volatile GamepadData             latestData      = null;
		protected       InputDevice             inputDevice     = null;
		protected       InputDevice.MotionRange lsX_motionRange = null;
		protected       InputDevice.MotionRange lsY_motionRange = null;
		protected       InputDevice.MotionRange rsX_motionRange = null;
		protected       InputDevice.MotionRange rsY_motionRange = null;
		protected       InputDevice.MotionRange lt_motionRange  = null;
		protected       InputDevice.MotionRange rt_motionRange  = null;

		public GamePadHandler( int deviceId )
		{
			inputDevice = InputDevice.getDevice( deviceId );
			lsX_motionRange = inputDevice.getMotionRange( MotionEvent.AXIS_X );
			lsY_motionRange = inputDevice.getMotionRange( MotionEvent.AXIS_Y );
			rsX_motionRange = inputDevice.getMotionRange( MotionEvent.AXIS_Z );
			rsY_motionRange = inputDevice.getMotionRange( MotionEvent.AXIS_RZ );
			lt_motionRange = inputDevice.getMotionRange( MotionEvent.AXIS_LTRIGGER );
			rt_motionRange = inputDevice.getMotionRange( MotionEvent.AXIS_RTRIGGER );

			if ( lsX_motionRange != null )
			{
				Log.v( AppState.LOG_TAG, "[JOYS] > LSX configured." );
				Log.v( AppState.LOG_TAG,
					   "Range: [" + lsX_motionRange.getMax() + ", " + lsX_motionRange.getMin() + "]" );
				Log.v( AppState.LOG_TAG, "Flat:  [" + lsX_motionRange.getFlat() + "]" );
			}
			if ( lsY_motionRange != null )
			{
				Log.v( AppState.LOG_TAG, "[JOYS] > LSY configured." );
				Log.v( AppState.LOG_TAG,
					   "[JOYS] >     Range: [" + lsY_motionRange.getMax() + ", " + lsY_motionRange.getMin() + "]" );
				Log.v( AppState.LOG_TAG,
					   "[JOYS] >     Flat:  [" + lsY_motionRange.getFlat() + "]" );
			}
		}

		public float getLeftJoystickX( MotionEvent event )
		{
			return getAxisValueImpl( event, MotionEvent.AXIS_X, lsX_motionRange.getFlat() );
		}

		public float getLeftJoystickY( MotionEvent event )
		{
			return getAxisValueImpl( event, MotionEvent.AXIS_Y, lsY_motionRange.getFlat() );
		}

		protected float getAxisValueImpl( MotionEvent event, int axis, float flat )
		{
			float result = event.getAxisValue( axis );
			return ( Math.abs( result ) < flat ) ? 0 : result;
		}

		public void handleEventAuto( MotionEvent event )
		{

		}

		public void handleEventAuto( int keyCode, KeyEvent event )
		{

		}

		public class GamepadData
		{
			public float LSX = 0;
			public float LSY = 0;
			public float RSX = 0;
			public float RSY = 0;
			public float RT  = 0;
			public float LT  = 0;
		}
	}

	protected class ViewHolder
	{
		public ImageButton                         bluetoothAcceptButton  = null;
		public ImageButton                         bluetoothRefreshButton = null;
		public ImageButton                         wifiP2pAcceptButton    = null;
		public ImageButton                         wifiP2pRefreshButton   = null;
		public Button                              moveAnotherActivity    = null;
		public Spinner                             bluetoothSpinner       = null;
		public Spinner                             wifiP2pSpinner         = null;
		public TwoLineArrayAdapter                 bluetoothArrayAdapter  = null;
		public TwoLineArrayAdapter                 wifiP2pArrayAdapter    = null;
		public List< Pair< String, String > >      bluetoothDevices       = null;
		public ArrayList< Pair< String, String > > wifiP2pDevices         = null;

		public ViewHolder()
		{
			bluetoothAcceptButton = (ImageButton) findViewById( R.id.a_main_bluetooth_accept_button );
			bluetoothRefreshButton = (ImageButton) findViewById( R.id.a_main_bluetooth_refresh_button );
			wifiP2pAcceptButton = (ImageButton) findViewById( R.id.a_main_wifi_accept_button );
			wifiP2pRefreshButton = (ImageButton) findViewById( R.id.a_main_wifi_refresh_button );
			bluetoothSpinner = (Spinner) findViewById( R.id.a_main_bluetooth_spinner );
			wifiP2pSpinner = (Spinner) findViewById( R.id.a_main_wifi_spinner );
			moveAnotherActivity = (Button) findViewById( R.id.move_to_another_activity );
			moveAnotherActivity.setBackgroundColor(Color.RED);
			bluetoothDevices = new ArrayList<>();
			bluetoothArrayAdapter = new TwoLineArrayAdapter( MainActivity.this, bluetoothDevices );
			bluetoothArrayAdapter.setDropDownViewResource( R.layout.list_item_spinner );
			bluetoothSpinner.setAdapter( bluetoothArrayAdapter );

			wifiP2pDevices = new ArrayList<>();
			wifiP2pArrayAdapter = new TwoLineArrayAdapter( MainActivity.this, wifiP2pDevices );
			wifiP2pArrayAdapter.setDropDownViewResource( R.layout.list_item_spinner );
			wifiP2pSpinner.setAdapter( wifiP2pArrayAdapter );
		}

		public void init()
		{
			populateBtList();
			populateP2pList( new WifiP2pDeviceList() );
			setupListeners();
		}
		
		private void enableNewActivity(){
	        runOnUiThread(new Runnable() {
	            @Override
	            public void run() {
	            	moveAnotherActivity.setVisibility(Button.VISIBLE);;
	            	moveAnotherActivity.setBackgroundColor(Color.GREEN);
	            }
	        });
	    }
		
		public String intToIPv4(int i) {
			   return ((i) & 0xFF ) + "." + ((i >> 8 ) & 0xFF) + "." +((i >> 16 ) & 0xFF) + "." +( i >> 24 & 0xFF) ;
		}

		private void populateBtList()
		{
			bluetoothDevices.clear();

			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if ( adapter != null )
			{
				Log.v( AppState.LOG_TAG, "[BLUE] > Found bonded devices:" );
				for ( BluetoothDevice device : adapter.getBondedDevices() )
				{
					Log.v( AppState.LOG_TAG,
						   "[BLUE] >     " + device.getName() + " at " + device.getAddress() );
					bluetoothDevices.add( new Pair<>( device.getName(), device.getAddress() ) );
				}
			}

			bluetoothArrayAdapter.notifyDataSetChanged();
		}

		private void populateP2pList( WifiP2pDeviceList peers )
		{
			wifiP2pDevices.clear();

			if ( !peers.getDeviceList().isEmpty() )
			{
				Log.v( AppState.LOG_TAG, "[WIFI] > Found peers:" );
			}

			for ( WifiP2pDevice device : peers.getDeviceList() )
			{
				Log.v( AppState.LOG_TAG,
					   "[WIFI] >     " + device.deviceName + " at " + device.deviceAddress );
				wifiP2pDevices.add( new Pair<>( device.deviceName, device.deviceAddress ) );
			}

			wifiP2pArrayAdapter.notifyDataSetChanged();
		}

		private void setupListeners()
		{
			bluetoothRefreshButton.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick( View v )
				{
					populateBtList();
				}
			} );

			bluetoothAcceptButton.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick( View v )
				{
					AsyncTask< Void, Void, Void > task = new AsyncTask< Void, Void, Void >()
					{
						@Override
						protected Void doInBackground( Void... params )
						{
							connectBT();
							return null;
						}
					};

					task.execute();
				}
			} );

			wifiP2pRefreshButton.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick( View v )
				{
					p2pManager.discoverPeers( p2pChannel, p2pDiscoveryReceiver );
				}
			} );
			
			moveAnotherActivity.setOnClickListener( new View.OnClickListener() //TODO
			{
				@Override
				public void onClick( View v )
				{
					Intent myIntent = new Intent(MainActivity.this, ControllerActivity.class);
					String ipv4 = null;
					if(clientTCP==null){//not connected to group leader
						WifiManager wifii= (WifiManager) getSystemService(Context.WIFI_SERVICE);
						DhcpInfo d=wifii.getDhcpInfo();
						ipv4 = intToIPv4(d.gateway);
						
					}else{
						ipv4=clientTCP.getServerAddress();
					}
					myIntent.putExtra("CameraIP", ipv4); //Optional parameters
					MainActivity.this.startActivity(myIntent);
				}
			} );

			wifiP2pAcceptButton.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick( View v )
				{
					if(wifiP2pSpinner.getSelectedItemPosition()==android.widget.AdapterView.INVALID_POSITION)
						return;
					String address = wifiP2pArrayAdapter.getItem( wifiP2pSpinner.getSelectedItemPosition() ).second;

					p2pConfig = new WifiP2pConfig();
					p2pConfig.deviceAddress = address;
					p2pManager.connect( p2pChannel, p2pConfig, p2pConnectionReceiver );
				}
			} );

			bluetoothSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener()
			{
				@Override
				public void onItemSelected( AdapterView< ? > parent,
											View view,
											int position,
											long id )
				{
					Log.v( AppState.LOG_TAG,
						   "[ UI ] > bluetoothSpinner.setOnItemSelectedListener.onItemSelected()" );
				}

				@Override
				public void onNothingSelected( AdapterView< ? > parent )
				{
					Log.v( AppState.LOG_TAG,
						   "[ UI ] > bluetoothSpinner.setOnItemSelectedListener.onNothingSelected()" );
				}
			} );

			wifiP2pSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener()
			{
				@Override
				public void onItemSelected( AdapterView< ? > parent,
											View view,
											int position,
											long id )
				{
					Log.v( AppState.LOG_TAG,
						   "[ UI ] > wifiP2pSpinner.setOnItemSelectedListener.onItemSelected()" );
				}

				@Override
				public void onNothingSelected( AdapterView< ? > parent )
				{
					Log.v( AppState.LOG_TAG,
						   "[ UI ] > wifiP2pSpinner.setOnItemSelectedListener.onNothingSelected()" );
				}
			} );
		}
	}
}

