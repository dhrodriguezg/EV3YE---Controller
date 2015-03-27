package ca.ualberta.ev3ye.controller.comm.logic.control;

import android.util.Pair;
import android.view.InputDevice;
import android.view.MotionEvent;

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
	protected              GamepadControlScheme                    controlScheme       = new HaloControlScheme();

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
		public Map< Integer, Float > data;

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
		public abstract Pair<Integer, Integer> getControl( GamepadControlData data );
		public abstract String getName();
	}

	public class HaloControlScheme
			extends GamepadControlScheme
	{
		@Override
		public Pair<Integer, Integer> getControl( GamepadControlData data )
		{
			// Throttle is controlled with the right stick,
			// Steering is controlled with the left.
			float motorL;
			float motorR;

			// First, determine motor scale from the RSX component.
			motorL = data.getAxisValue( MotionEvent.AXIS_Z );
			motorR = data.getAxisValue( MotionEvent.AXIS_Z );

			// Determine how much to scale each motor based on the LSY component.
			// If LSY is close to 0, then we don't need to scale either component (we're going
			// straight ahead). If it's close to 1, then we're turning right, and if it's close to
			// -1 we're turning left.

			// For now, just threshold the left stick input.
			float steering = data.getAxisValue( MotionEvent.AXIS_Y );
			if ( Math.abs( steering ) < 0.25 )
			{
				steering = 0f;
			}

			if ( steering != 0 )
			{
				motorL = ( steering < 0 ) ? -motorL : motorL;
				motorR = ( steering < 0 ) ? motorR : -motorR;
			}

			return new Pair<>((int) motorL, (int) motorR);
		}

		@Override
		public String getName()
		{
			return "Halo Scheme";
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
    		Pair<Integer, Integer> controls = controlScheme.getControl(latestData);
    		callbackTarget.onControlEventResult( controls.first, controls.second );
            return true;
        }
        else return false;
	}
}
