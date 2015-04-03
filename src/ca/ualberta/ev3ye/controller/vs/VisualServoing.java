package ca.ualberta.ev3ye.controller.vs;

import java.io.IOException;
import java.io.InputStream;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import ca.ualberta.ev3ye.controller.streaming.ControllerActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class VisualServoing {
	
	private int MAX_POWER;
	private boolean enable = false;
	//Marker data
	private Mat mMarker;
    private MatOfKeyPoint mMarkerKP;
    private Mat mMarkerDesc;
    
    private Mat mOutput;
    private Mat mMatch;
    
    private ControllerActivity activity; 
    
    public VisualServoing(ControllerActivity activity, int maxPower){
    	this.activity = activity;
    	this.MAX_POWER=maxPower;
	}
    
	public boolean init(InputStream is){
		boolean success = false;
		
		mMarkerKP = new MatOfKeyPoint();
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
	
	public Bitmap processStreaming(byte[] textureByteArray){
		if(!enable)
			return null;
		
		MatOfByte mRawMarker = new MatOfByte(textureByteArray);
        Mat mRgba = Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_COLOR);
        Mat mGray = new Mat();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY, 1);
        
		
        /*Mat mRgba = Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        Mat mGray = new Mat();
        Imgproc.threshold(mRgba, mGray, 128, 255, Imgproc.THRESH_BINARY);*/
        System.out.println("Col:"+mMarker.cols());
        MatchFeatures(mMarker.getNativeObjAddr(), mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(), mOutput.getNativeObjAddr(), mMatch.getNativeObjAddr(), true);
        
        Mat imageMat = new Mat();
        if (mOutput.channels()==1)
        	Imgproc.cvtColor(mOutput, imageMat, Imgproc.COLOR_GRAY2BGR, 3);
        else
        	Imgproc.cvtColor(mOutput, imageMat, Imgproc.COLOR_RGBA2RGB, 3);
        	
        MatOfByte bufByte = new MatOfByte();
    	Highgui.imencode(".jpg", imageMat, bufByte);
    	
    	float [] corners = new float[10];
    	mMatch.get(0, 0,corners);
    	
    	
    	int rightPower = (int) (MAX_POWER*(1 - 2*corners[0]/mRgba.cols()));
    	int leftPower = -rightPower;
    	//activity.setRightPower(rightPower);
    	//activity.setLeftPower(leftPower);
    	
    	byte[] texture = bufByte.toArray();
    	return BitmapFactory.decodeByteArray(texture, 0, texture.length);
	}
	
	
	public native void FindFeatures(long matAddrGray, long matAddrKeypoint, long matAddrDescriptor);
	public native void MatchFeatures(long matAddrMarker, long matAddrScene, long matAddrSceneRgba, long matAddrOutput, long matAddrMatches, boolean debug);
}
