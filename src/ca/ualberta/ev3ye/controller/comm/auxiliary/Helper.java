package ca.ualberta.ev3ye.controller.comm.auxiliary;

import android.util.Log;

/**
 * Created by Yuey on 2015-03-25.
 * <p/>
 * Convenience functions
 */
public class Helper
{
	public static final String DEFAULT_TYPE = "    ";
	public static final String LOG_TAG      = "EV3YE";

	public static void LogV( String type, String msg )
	{
		Log.v( LOG_TAG, "[" + type + "] > " + msg );
	}

	public static void LogV( String msg )
	{
		Helper.LogV( DEFAULT_TYPE, msg );
	}

	public static void LogD( String type, String msg )
	{
		Log.d( LOG_TAG, "[" + type + "] > " + msg );
	}

	public static void LogD( String msg )
	{
		Helper.LogD( DEFAULT_TYPE, msg );
	}

	public static void LogW( String type, String msg )
	{
		Log.w( LOG_TAG, "[" + type + "] > " + msg );
	}

	public static void LogW( String msg )
	{
		Helper.LogW( DEFAULT_TYPE, msg );
	}

	public static void LogE( String type, String msg )
	{
		Log.e( LOG_TAG, "[" + type + "] > " + msg );
	}

	public static void LogE( String msg )
	{
		Helper.LogE( DEFAULT_TYPE, msg );
	}
}
