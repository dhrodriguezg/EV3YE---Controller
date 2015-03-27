package ca.ualberta.ev3ye.controller.comm.logic.control;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;

/**
 * Created by Yuey on 2015-03-25.
 */
public class TiltControlHandler
	extends ControlHandler
	implements SensorEventListener
{
	public TiltControlHandler( ControlEventCallbacks callbackTarget )
	{
		super( callbackTarget );
	}

	@Override
	public void cleanup()
	{

	}

	@Override
	public String getName()
	{
		return "Tilt";
	}

	@Override
	public void onSensorChanged( SensorEvent event )
	{

	}

	@Override
	public void onAccuracyChanged( Sensor sensor, int accuracy )
	{
		switch ( accuracy )
		{
		case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
			break;
		case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
			break;
		case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
			break;
		case SensorManager.SENSOR_STATUS_UNRELIABLE:
			break;
		//case SensorManager.SENSOR_STATUS_NO_CONTACT:
			//break;
		default:
			break;
		}
	}
}
