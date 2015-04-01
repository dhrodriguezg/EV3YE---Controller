package ca.ualberta.ev3ye.controller.control;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by Yuey on 2015-03-25.
 * <p/>
 * A base class for a control system on Android. Intended to be used as the state within the state
 * machine ControlSystem.
 *
 * @see ca.ualberta.ev3ye.controller.control.logic.control.ControlSystem
 */
public abstract class ControlHandler
{
	/**
	 * @see ca.ualberta.ev3ye.controller.control.logic.control.ControlHandler.ControlEventCallbacks
	 */
	protected ControlEventCallbacks callbackTarget;

	public ControlHandler( ControlEventCallbacks callbackTarget )
	{
		this.callbackTarget = callbackTarget;
	}

	/**
	 * A method for the handler to receive a control event if the handler is not designed to process
	 * them automatically.
	 *
	 * @param event The event data to be passed to the handler.
	 */
	public void receiveControlEvent( Object event )
	{
		// Do nothing
	}
	
	/**
	 * A method for the handler to receive motion events from the hosting activity.
	 * @param event The event data to be passed to the handler.
	 * @return True if the handler consumed the event, false otherwise.
	 */
	public boolean receiveGenericMotionEvent(MotionEvent event)
	{
		return false;
	}
	
	/**
	 * To be called when the handler should start accepting commands (for example, subclasses 
	 * using sensors should register their listeners during this time).
	 */
	public abstract void init();

	/**
	 * To be called when the handler is no longer needed, and should execute its code to release
	 * resources. Usually called when the app closes or when input methods are changed. Subclasses
	 * using sensors should unregister their listeners on this call.
	 */
	public abstract void cleanup();

	/**
	 * Returns the human-readable name of the handler.
	 *
	 * @return A (mostly) human readable name for the handler.
	 */
	public String getName()
	{
		return this.toString();
	}

	/**
	 * A callback interface for consumers of processed control commands.
	 */
	public interface ControlEventCallbacks
	{
		/**
		 * To be called when the handler fails to setup. This call implies that the handler has
		 * failed in a way that cannot be recovered from.
		 *
		 * @param msg Details about the failure.
		 */
		public void onHandlerSetupFailure( String msg );

		/**
		 * To be called when the handler's state changes. This could be for any reason, such as
		 * sensor disconnects or accuracy changes. The parameter OK indicates if the handler will
		 * continue producing usable output. It's implied that even if OK is false, that the state
		 * can be recovered from in the future.
		 *
		 * @param msg Details about the change.
		 * @param ID  An ID code associated with the change.
		 * @param OK  If true, the handler can still produce usable output, otherwise false.
		 */
		public void onHandlerStateChanged( String msg, int ID, boolean OK );

		/**
		 * To be called when input data has been received and handled. The returned object is the
		 * portion of the control string associated with motor control, and may need further
		 * processing before sending it over WiFi to the slave device.
		 *
		 * @param leftMotor A formatted control string.
		 * @param rightMotor TODO
		 */
		public void onControlEventResult( int leftMotor, int rightMotor, int deltaCamera );
	}

	/**
	 * A basic callback target that simply logs callback events.
	 */
	public class BaseControlEventCallbackTarget
			implements ControlEventCallbacks
	{
		@Override
		public void onHandlerSetupFailure( String msg )
		{
			if ( msg == null )
			{
				msg = "null";
			}
			Log.v( "ControlHandler", "onHandlerSetupFailure( msg:" + msg + " )" );
		}

		@Override
		public void onHandlerStateChanged( String msg, int ID, boolean OK )
		{
			if ( msg == null )
			{
				msg = "null";
			}
			Log.v( "ControlHandler",
				   "onHandlerStateChanged( msg:" + msg + ", ID:" + ID + ", OK:" + OK + " )" );
		}

		@Override
		public void onControlEventResult( int leftMotor, int rightMotor, int deltaCamera )
		{
			Log.v( "ControlHandler", "onControlEventResult( leftMotor:" + leftMotor + " rightMotor:" + rightMotor + " deltaCamera:" + deltaCamera + " )" );
		}
	}
	
	public class ControlerResultData
	{
		public int L;
		public int R;
		public int C;
		
		public ControlerResultData(int L, int R, int C)
		{
			this.L = L;
			this.R = R;
			this.C = C;
		}
	}
}
