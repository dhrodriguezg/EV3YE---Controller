package ca.ualberta.ev3ye.controller.streaming;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ToggleButton;
import android.graphics.BitmapFactory;
import android.widget.SeekBar;

import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import ca.ualberta.ev3ye.controller.R;
import ca.ualberta.ev3ye.controller.comm.ClientTCP;

public class ControllerActivity extends Activity implements LoaderCallbackInterface{
	
	private static final String TAG = "ControllerActivity"; 
	private ImageView imageView;
	private ClientTCP clientTCP;
    private int height = 0;
    
    private Mat mMarker;
    private MatOfKeyPoint mMarkerKP;
    private Mat mMarkerDesc;
    
    private Mat mOutput;
    private Mat mMatch;
    private boolean showVisualServoing = false;
    
	
	protected BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	        case LoaderCallbackInterface.SUCCESS:
	        {
	        	Log.i("OK", "OpenCV loaded successfully");
                // Load native library after(!) OpenCV initialization
                System.loadLibrary("jniOCV");
                
                try {
                	mOutput = new Mat();
                    mMatch = new Mat(10,1,CvType.CV_32FC1);
                	
					InputStream is = getResources().openRawResource(R.raw.panel_07_jpg);
					
                    byte[] rawMarker = new byte[is.available()];
					is.read(rawMarker);
					MatOfByte mRawMarker = new MatOfByte(rawMarker);
                    mMarker = Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
                    mMarkerKP = new MatOfKeyPoint();
                    mMarkerDesc = new Mat();
                    FindFeatures(mMarker.getNativeObjAddr(), mMarkerKP.getNativeObjAddr(), mMarkerDesc.getNativeObjAddr());
                    
                    Log.i(TAG, "Marker loaded successfully :D "+mMarkerKP.rows());
                                        
				} catch (IOException e) {
					e.printStackTrace();
				}
	        
	        } break;
	        default:
	        {
	            super.onManagerConnected(status);
	        } break;
	        }
	    }
	};

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

    	Intent intent = getIntent();
    	clientTCP = new ClientTCP(intent.getStringExtra("CameraIP"), true);
    	
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_controller);


        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                imageView = (ImageView) findViewById(R.id.imageView1);
                InputStream is = getResources().openRawResource(R.raw.loading);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                button.setVisibility(View.GONE);
                imageView.setImageBitmap(bitmap);
                updateStream();
                updateControls();
            }
        });
        
        ToggleButton toggle = (ToggleButton) findViewById(R.id.tracking);
        /*toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                	showVisualServoing = true;
                } else {
                	showVisualServoing = false;
                }
            }
        });*/

        final SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                height=progress;
                Log.i("SeekBar","value:"+height);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void refreshView(final Bitmap bitmap){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
    }
    
    private void updateControls(){
    	Thread thread = new Thread() {
            public void run() {
            	boolean stop=false;
            	while(!stop){
            		//TODO logic to send controller ""+height
            		try {
						Thread.sleep(10); //100fps
						clientTCP.updateController("0;"+height);
						
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
            		
            	}
            }
        };
        thread.start();
    }

    private void updateStream(){


        Thread thread = new Thread() {
            public void run() {
                boolean stop=false;
                //serverTCP.initServices(0);
                Bitmap bitmap;
                while(!stop){
                	
                	double initime = System.currentTimeMillis();
                	boolean success = clientTCP.updateStream();
                	final byte[] textureByteArray = clientTCP.getPicture();
                	
                	if(!success){
                		continue;
                	}
                	if(textureByteArray==null)
                        continue;
                	
                	if(showVisualServoing){
                		//Load the image using OCV
                		MatOfByte mRawMarker = new MatOfByte(textureByteArray);
                        //Mat mRgba = Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_COLOR);
                        //Mat mGray = new Mat();
                        //Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY, 1);
                        
                        Mat mRgba = Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
                        Mat mGray = new Mat();
                        Imgproc.threshold(mRgba, mGray, 128, 255, Imgproc.THRESH_BINARY);
                        
                        MatchFeatures(mMarker.getNativeObjAddr(), mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(), mOutput.getNativeObjAddr(), mMatch.getNativeObjAddr(), true);
                        
                        Mat imageMat = new Mat();
                        if (mOutput.channels()==1)
                        	Imgproc.cvtColor(mOutput, imageMat, Imgproc.COLOR_GRAY2BGR, 3);
                        else
                        	Imgproc.cvtColor(mOutput, imageMat, Imgproc.COLOR_RGBA2RGB, 3);
                        	
                        MatOfByte bufByte = new MatOfByte();
                    	Highgui.imencode(".jpg", imageMat, bufByte);
                    	byte[] texture = bufByte.toArray();
                    	bitmap = BitmapFactory.decodeByteArray(texture, 0, texture.length);
                	}else{
                		bitmap = BitmapFactory.decodeByteArray(textureByteArray, 0, textureByteArray.length);
                	}
                	//float [] corners = new float[10];
                	//mMatch.get(0, 0,corners);
					
					double endtime = System.currentTimeMillis();
					Log.e("FPS",""+1000/(endtime-initime));
                    refreshView(bitmap);
                }
            }
        };
        thread.start();
    }
    
    public void onToggleClicked(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
        	showVisualServoing = true;
        } else {
        	showVisualServoing = false;
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mOpenCVCallBack);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //no inspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    
    
	@Override
	public void onManagerConnected(int status) {
	}

	@Override
	public void onPackageInstall(int operation, InstallCallbackInterface callback) {
	}
	
	public native void FindFeatures(long matAddrGray, long matAddrKeypoint, long matAddrDescriptor);
	
	public native void MatchFeatures(long matAddrMarker, long matAddrScene, long matAddrSceneRgba, long matAddrOutput, long matAddrMatches, boolean debug);
	
}
