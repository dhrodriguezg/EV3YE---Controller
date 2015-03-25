package ca.ualberta.ev3ye.controller.comm.logic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Yuey on 2015-03-18.
 */
public class BluetoothServer
{
	BluetoothAdapter  mBluetoothAdapter = null;
	HashSet< String > pairedEV3         = null;

	public BluetoothServer()
	{

	}

	public boolean startServer()
	{
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
	}

	public void getPairedEV3()
	{
		Set< BluetoothDevice > pairedDevices = mBluetoothAdapter.getBondedDevices();
		pairedEV3 = new HashSet< String >();
		// If there are paired devices
		for ( BluetoothDevice device : pairedDevices )
		{
			if ( device.getName().equalsIgnoreCase( "EV3" ) )
			{
				pairedEV3.add( device.getAddress() );
			}
		}
	}

	public void connectEV3()
	{

	}
}
