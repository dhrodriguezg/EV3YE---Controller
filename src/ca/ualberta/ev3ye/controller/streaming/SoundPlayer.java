package ca.ualberta.ev3ye.controller.streaming;

import ca.ualberta.ev3ye.controller.R;
import android.content.Context;
import android.media.MediaPlayer;

public class SoundPlayer {
	
	private MediaPlayer mediaControllerOffline = null;
	private MediaPlayer mediaControllerOnline = null;
	private MediaPlayer mediaTargetLost = null;
	private MediaPlayer mediaTargetFound = null;
	
	public SoundPlayer(Context context){
		
		mediaControllerOffline = MediaPlayer.create(context , R.raw.controller_offline);
		mediaControllerOnline = MediaPlayer.create(context , R.raw.controller_online);
		mediaTargetLost = MediaPlayer.create(context , R.raw.target_lost);
		mediaTargetFound = MediaPlayer.create(context , R.raw.target_found);
	}
	
	public void controllerOnline(){
		mediaControllerOnline.start();
	}
	
	public void controllerOffline(){
		mediaControllerOffline.start();
	}
	
	public void targetLost(){
		mediaTargetLost.start();
	}
	
	public void targetFound(){
		mediaTargetFound.start();
	}
}
