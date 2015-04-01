package ca.ualberta.ev3ye.controller.control;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by Yuey on 2015-03-25.
 */
public class TiltControlHandler
		extends ControlHandler
		implements SensorEventListener
{
	protected static final int           SENSOR_TYPE = Sensor.TYPE_GRAVITY;
	protected static final float         GRAVITY     = 9.81f;
	protected              SensorManager manager     = null;
	protected              Sensor        sensor      = null;
	protected              float         movingAverageX = 0;
	protected              float         movingAverageY = 0;
	protected              float         movingAverageAlpha = 0.5f;

	public TiltControlHandler( ControlEventCallbacks callbackTarget, Activity activity )
	{
		super( callbackTarget );

		manager = (SensorManager) activity.getSystemService( Context.SENSOR_SERVICE );

		if ( hasCorrectSensor( manager ) )
		{
			sensor = manager.getDefaultSensor( SENSOR_TYPE );
		}
		else
		{
			callbackTarget.onHandlerSetupFailure( "The device does not have the correct sensors!" );
		}
	}

	private boolean hasCorrectSensor( SensorManager manager )
	{
		if ( manager.getDefaultSensor( SENSOR_TYPE ) != null )
		{
			// Success!
			return true;
		}
		else
		{
			// Failure!
			return false;
		}
	}

	@Override
	public void cleanup()
	{
		manager.unregisterListener( this );
	}

	@Override
	public String getName()
	{
		return "Tilt";
	}

	@Override
	public void onSensorChanged( SensorEvent event )
	{
		// Extract the x, y, and z from the event.
		float y = -event.values[ 0 ] / GRAVITY;
		float x = event.values[ 1 ] / GRAVITY;
		float z = event.values[ 2 ] / GRAVITY;
		
		x = Math.abs(x);

		//Log.d( "Control", String.format( "%+6f, %+6f, %+6f", x, y, z ) );

		// The x component gives us tilt forward / backward
		x -= 0.5;
		x *= 200;
		if ( Math.abs(x) < 10 ) x = 0;

		// The y component gives us steering
		y *= 50;
		if ( Math.abs(y) < 10 ) y = 0;

		float
				motorL = x + y,
				motorR = x - y;

		//Log.d( "Control", String.format( "%+6f, %+6f, %+6f", motorL, motorR, 0f ) );

		// Clamp the values to [-100, 100]
		if ( motorL < -100 || motorL > 100 )
		{
			motorL -= motorL % 100;
		}
		if ( motorR < -100 || motorR > 100 )
		{
			motorR -= motorR % 100;
		}
		
		movingAverageX = (movingAverageAlpha * motorL) + (1.0f - movingAverageAlpha) * movingAverageX;
		movingAverageY = (movingAverageAlpha * motorR) + (1.0f - movingAverageAlpha) * movingAverageY;

		callbackTarget.onControlEventResult( (int) movingAverageX, (int) movingAverageY, 0 );
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

	@Override
	public void init()
	{
		manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
	}
}
