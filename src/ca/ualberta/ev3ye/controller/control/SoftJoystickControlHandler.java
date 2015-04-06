package ca.ualberta.ev3ye.controller.control;

import android.app.Activity;
import android.view.View;

/**
 * Created by Yuey on 2015-03-25.
 */
public class SoftJoystickControlHandler
	extends ControlHandler
{
	public SoftJoystickControlHandler( ControlEventCallbacks callbackTarget, View controlerView )
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

	@Override
	public void init()
	{
		// TODO Auto-generated method stub
		
	}
}
