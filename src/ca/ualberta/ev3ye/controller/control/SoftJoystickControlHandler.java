package ca.ualberta.ev3ye.controller.control;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ca.ualberta.ev3ye.controller.comm.auxiliary.AppState;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Yuey on 2015-03-25.
 */
public class SoftJoystickControlHandler
	extends ControlHandler
{
	protected View controlView = null;
	protected volatile int motorL = 0;
	protected volatile int motorR = 0;
	
	public SoftJoystickControlHandler( ControlEventCallbacks callbackTarget, View controlerView )
	{
		super( callbackTarget );
		controlView = controlerView;
		
		controlerView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
			{
				if (arg1.getAction() == MotionEvent.ACTION_UP)
				{
					motorL = 0;
					motorR = 0;
					
					SoftJoystickControlHandler.this.callbackTarget.onControlEventResult(motorL, motorR, 0);
				}
				else
				{					
					int [] viewCenter = new int [] {arg0.getWidth() / 2, arg0.getHeight() / 2};
					float [] touch = new float [] {arg1.getX(), arg1.getY()};
					
					Log.v(AppState.LOG_TAG, String.format("Touch %+5f, %+5f   Center %3d, %3d", touch[0], touch[1], viewCenter[0], viewCenter[1]));
					
					calculateMotorValues(touch, viewCenter);
				}
				return true;
			}
		});
	}

	private void calculateMotorValues(float[] touch, int[] viewCenter)
	{
		// Get the vector between the view center and the touch position and normalize it.
		float[] touchVector = new float[] {viewCenter[0] - touch[0], viewCenter[1] - touch[1]};
		touchVector[0] /= viewCenter[0];
		touchVector[1] /= viewCenter[1];
		
		// The Y component gives us throttle.
		motorL = (int) touchVector[1] * 100;
		motorR = (int) touchVector[1] * 100;
		
		// The X component gives us steering.
		motorL -= touchVector[0] * 50;
		motorR += touchVector[0] * 50;
		
		// Clamp the values to [-100, 100].
		if(motorL > 100) motorL = 100;
		if(motorR > 100) motorR = 100;
		if(motorL < -100) motorL = -100;
		if(motorR < -100) motorR = -100;
		
		// Send the result.
		callbackTarget.onControlEventResult(motorL, motorR, 0);
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
