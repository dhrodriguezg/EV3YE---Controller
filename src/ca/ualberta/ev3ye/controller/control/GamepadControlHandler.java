package ca.ualberta.ev3ye.controller.control;

import android.util.Log;
import android.util.Pair;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.util.HashMap;
import java.util.Map;

import ca.ualberta.ev3ye.controller.comm.auxiliary.Helper;

/**
 * Created by Yuey on 2015-03-25.
 */
public class GamepadControlHandler
		extends ControlHandler
{
	public static final    String                                  NAME                = "Gamepad";
	protected static final float                                   CONTROL_INPUT_SCALE = 100.0f;
	protected              InputDevice                             inputDevice         = null;
	protected              Map< Integer, InputDevice.MotionRange > motionRanges        = null;
	protected              GamepadControlData                      latestData          = new GamepadControlData();
	protected              GamepadControlScheme                    controlScheme       = new ForzaControlScheme();

	protected static final int AXISES[] = {
			MotionEvent.AXIS_X,
			MotionEvent.AXIS_Y,
			MotionEvent.AXIS_Z,
			MotionEvent.AXIS_RZ,
			MotionEvent.AXIS_RTRIGGER,
			MotionEvent.AXIS_LTRIGGER
	};

	public GamepadControlHandler( ControlEventCallbacks callbackTarget )
	{
		super( callbackTarget );

		motionRanges = new HashMap<>();
		
		int[] deviceIds = InputDevice.getDeviceIds();
		int controllerID = -1;
		for ( int deviceId : deviceIds )
		{
			InputDevice dev = InputDevice.getDevice( deviceId );
			int sources = dev.getSources();

			// Verify that the device has gamepad buttons, and control sticks.
			if ( ( ( sources & InputDevice.SOURCE_GAMEPAD ) == InputDevice.SOURCE_GAMEPAD ) && ( ( sources & InputDevice.SOURCE_JOYSTICK ) == InputDevice.SOURCE_JOYSTICK ) )
			{
				// This device is a game controller. Store its device ID.
				controllerID = deviceId;
			}
		}

		if ( controllerID != -1 )
		{
			inputDevice = InputDevice.getDevice( controllerID );

			for ( int axis : AXISES )
			{
				motionRanges.put( axis, inputDevice.getMotionRange( axis ) );
				logSetupStatus( axis );
			}
		}
		else
		{
			Helper.LogE( "CTRL",
						 "GamepadControlProvider could not be initialized properly because the device has no available gamepad input." );
			callbackTarget.onHandlerSetupFailure( "The device has no available gamepad input." );
		}
	}

	private void logSetupStatus( int axis )
	{
		InputDevice.MotionRange item = motionRanges.get( axis );
		if ( item != null )
		{
			Helper.LogV( "CTRL", axisIdToString( axis ) + " configured." );
		}
		else
		{
			Helper.LogE( "CTRL", axisIdToString( axis ) + " not configured!" );
		}
	}

	private String axisIdToString( int axis )
	{
		switch ( axis )
		{
		case MotionEvent.AXIS_X:
			return "X Axis";
		case MotionEvent.AXIS_Y:
			return "Y Axis";
		case MotionEvent.AXIS_Z:
			return "Z Axis";
		case MotionEvent.AXIS_RZ:
			return "RZ Axis";
		case MotionEvent.AXIS_RTRIGGER:
			return "RT Axis";
		case MotionEvent.AXIS_LTRIGGER:
			return "LT Axis";
		default:
			Helper.LogW( "An axis with id " + axis + " couldn't be translated into a string." );
			return "Unknown: " + axis;
		}
	}

	@Override
	public void receiveControlEvent( Object event )
	{
		// Do nothing!
	}

	@Override
	public void cleanup()
	{

	}

	@Override
	public String getName()
	{
		return NAME + " " + controlScheme.getName();
	}

	public class GamepadControlData
	{
		public Map< Integer, Float > data = new HashMap<>();

		public GamepadControlData()
		{
			for ( int axis : AXISES )
			{
				data.put( axis, 0f );
			}
		}

		public void update( MotionEvent event )
		{
			for ( int axis : AXISES )
			{
				data.put( axis, updateAxisValue( event, axis ) );
			}
		}

		public float getAxisValue( int axis )
		{
			return data.get( axis );
		}

		protected float updateAxisValue( MotionEvent event, int axis )
		{
			float result = event.getAxisValue( axis );
			return ( Math.abs( result ) < motionRanges.get( axis ).getFlat() ) ? 0 : result;
		}
	}

	public abstract class GamepadControlScheme
	{
		public abstract ControlerResultData getControl( GamepadControlData data );
		public abstract String getName();
	}
	
	public class ForzaControlScheme
			extends GamepadControlScheme
	{
		@Override
		public ControlerResultData getControl(GamepadControlData data)
		{
			float fwdBase = 
					data.getAxisValue(MotionEvent.AXIS_RTRIGGER) - 
					data.getAxisValue(MotionEvent.AXIS_LTRIGGER);
			
			float rot = 
					data.getAxisValue(MotionEvent.AXIS_X);
			
			int cam = (int) -(data.getAxisValue(MotionEvent.AXIS_RZ) * 5f);
			
			float
				motorL = (fwdBase * 100) + (rot * 50),
				motorR = (fwdBase * 100) - (rot * 50);
			
			// Clamp the values to [-100, 100]
			if (motorL < -100 || motorL > 100)
				motorL -= motorL % 100;
			if (motorR < -100 || motorR > 100)
				motorR -= motorR % 100;
			
			return new ControlerResultData((int) motorL, (int) motorR, cam);
		}

		@Override
		public String getName()
		{
			return "Forza scheme";
		}	
	}

	@Override
	public boolean receiveGenericMotionEvent(MotionEvent event)
	{
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE)
        {            
    		latestData.update( event );
    		ControlerResultData controls = controlScheme.getControl(latestData);
    		callbackTarget.onControlEventResult( controls.L, controls.R, controls.C );
            return true;
        }
        else return false;
	}

	@Override
	public void init()
	{
		// TODO Auto-generated method stub
		
	}
}
