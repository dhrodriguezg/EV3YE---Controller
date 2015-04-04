package ca.ualberta.ev3ye.controller.vs;

import java.io.IOException;
import java.io.InputStream;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import ca.ualberta.ev3ye.controller.streaming.ControllerActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class VisualServoing {
	
	private int MAX_POWER;
	private boolean enable = false;
	//Marker data
	private Mat mMarker;
    private Mat mMarkerKP;
    private Mat mMarkerDesc;
    
    private Mat mOutput;
    private Mat mMatch;
    
    private ControllerActivity activity; 
    
    private int minInt = 180;
    private Scalar maxColor = new Scalar(255, 255, 255);
    private Scalar minColor = new Scalar(minInt,minInt,minInt);
    
    public VisualServoing(ControllerActivity activity, int maxPower){
    	this.activity = activity;
    	this.MAX_POWER=maxPower;
	}
    
	public boolean init(InputStream is){
		boolean success = false;
		
		mMarkerKP = new Mat();
        mMarkerDesc = new Mat();
        mOutput = new Mat();
        
        mMatch = new Mat(10,1,CvType.CV_32FC1);
        
        try {
        	byte[] rawMarker = new byte[is.available()];
        	is.read(rawMarker);
    		MatOfByte mRawMarker = new MatOfByte(rawMarker);
    		mMarker=Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
    		
        	FindFeatures(mMarker.getNativeObjAddr(), mMarkerKP.getNativeObjAddr(), mMarkerDesc.getNativeObjAddr());
        	success = true;
        	is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return success;
	}

	public void setActivity(ControllerActivity activity){
		this.activity=activity;
	}
	
	public void enable(){
		enable=true;
	}
	
	public void disable(){
		enable=false;
	}
	
	public Bitmap processStreaming(byte[] textureByteArray, boolean debug){
		if(!enable)
			return null;
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {}
		/* working
		MatOfByte mRawMarker = new MatOfByte(textureByteArray);
        Mat mRgba = Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_COLOR);
        Mat mGray = new Mat();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY, 1);*/
        
        //TEST 1
		MatOfByte mRawMarker = new MatOfByte(textureByteArray);
		Mat color = Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_COLOR);
		
		Size sz = new Size(color.cols()/2,color.rows()/2); //it's ...ok
		Mat mRgba = new Mat();
		Imgproc.resize( color, mRgba, sz );
		
		Mat mGray = new Mat();
		Core.inRange(mRgba, minColor, maxColor, mGray);
		if(debug)
			Imgproc.cvtColor(mGray, mRgba, Imgproc.COLOR_GRAY2BGR, 3); //To see the filer
		  
        MatchFeatures(mMarker.getNativeObjAddr(), mMarkerKP.getNativeObjAddr(), mMarkerDesc.getNativeObjAddr(), mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(), mOutput.getNativeObjAddr(), mMatch.getNativeObjAddr(), debug);
        
        Mat imageMat = new Mat();
        if (mOutput.channels()==1)
        	Imgproc.cvtColor(mOutput, imageMat, Imgproc.COLOR_GRAY2BGR, 3);
        else
        	Imgproc.cvtColor(mOutput, imageMat, Imgproc.COLOR_BGRA2BGR, 3);
        	
        MatOfByte bufByte = new MatOfByte();
    	Highgui.imencode(".jpg", imageMat, bufByte);
    	
    	float[] corners = new float[10];
    	mMatch.get(0, 0,corners);
    	int rightPower = 0;
    	int leftPower = 0;
    	if(corners[0] > 10){
    		float direction = (1.f - 2.f*corners[0]/(float)mRgba.cols());
    		direction = direction > 0 ? 1.f : -1.f;
    		rightPower = (int)direction*MAX_POWER/10;
        	leftPower = -rightPower;
    	}else{
    		rightPower = MAX_POWER/20; //searching
        	leftPower = -MAX_POWER/20;
    	}
    	activity.setRightPower(rightPower);
    	activity.setLeftPower(leftPower);
    	
    	byte[] texture = bufByte.toArray();
    	return BitmapFactory.decodeByteArray(texture, 0, texture.length);
	}
	
	
	public native void FindFeatures(long matAddrMarker, long matAddrKeypoint, long matAddrDescriptor);
	public native void MatchFeatures(long matAddrMarker, long matAddrKeypoint, long matAddrDescriptor, long matAddrScene, long matAddrSceneRgba, long matAddrOutput, long matAddrMatches, boolean debug);
	
}
