package ca.ualberta.ev3ye.controller.comm.logic;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

/**
 * Created by Yuey on 2015-03-18.
 */
public class BluetoothCom
{
	final String nxt1 = "00:16:53:44:c1:4a";

	BluetoothAdapter localAdapter;
	BluetoothSocket  socket_nxt1;
	boolean success = false;

	public void enableBT( Activity activity )
	{
		localAdapter = BluetoothAdapter.getDefaultAdapter();

		if ( !localAdapter.isEnabled() )
		{
			Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
			activity.startActivityForResult( enableBtIntent, 1 );
		}

	}

	//connect to both NXTs
	public boolean connectToNXTs()
	{
		//get the BluetoothDevice of the NXT
		BluetoothDevice nxt_1 = localAdapter.getRemoteDevice( "00:16:53:44:C1:4A" );
		if ( nxt_1 == null )
		{
			Log.d( "Bluetooth", "Err: Device not found" );
			return false;
		}

		try
		{
			socket_nxt1 = nxt_1.createRfcommSocketToServiceRecord( UUID.fromString(
					"00001101-0000-1000-8000-00805F9B34FB" ) );
			socket_nxt1.connect();
			success = true;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			Log.d( "Bluetooth", "Err: Device not found or cannot connect" );
			success = false;
		}
		return success;
	}


	public void writeMessage( String msg )
			throws InterruptedException
	{
		BluetoothSocket connSock = socket_nxt1;

		if ( connSock != null )
		{
			try
			{
				//OutputStreamWriter out=new OutputStreamWriter(connSock.getOutputStream());
				DataOutputStream out = new DataOutputStream( connSock.getOutputStream() );
				out.writeUTF( msg );
				out.flush();
				Thread.sleep( 1000 );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	public int readMessage( String nxt )
	{
		BluetoothSocket connSock = socket_nxt1;
		int n;

		if ( connSock != null )
		{
			try
			{

				InputStreamReader in = new InputStreamReader( connSock.getInputStream() );
				n = in.read();

				return n;


			}
			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			}
		}
		else
		{
			//Error
			return -1;
		}
	}
}
