package ca.ualberta.ev3ye.controller.comm.logic.control;

import android.app.Activity;

/**
 * Created by Yuey on 2015-03-25.
 */
public class SoftJoystickControlHandler
	extends ControlHandler
{
	public SoftJoystickControlHandler( ControlEventCallbacks callbackTarget, Activity parentActivity )
	{
		super( callbackTarget );
	}

	@Override
	public void receiveControlEvent( Object event )
	{

	}

	@Override
	public void cleanup()
	{

	}

	@Override
	public String getName()
	{
		return "Soft Joystick";
	}
}
