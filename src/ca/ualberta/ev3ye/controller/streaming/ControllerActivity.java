package ca.ualberta.ev3ye.controller.streaming;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.graphics.BitmapFactory;
import android.widget.SeekBar;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import ca.ualberta.ev3ye.controller.R;
import ca.ualberta.ev3ye.controller.comm.ClientTCP;
import ca.ualberta.ev3ye.controller.control.ControlSystem;
import ca.ualberta.ev3ye.controller.control.GamepadControlHandler;
import ca.ualberta.ev3ye.controller.control.SoftJoystickControlHandler;
import ca.ualberta.ev3ye.controller.control.TiltControlHandler;
import ca.ualberta.ev3ye.controller.control.ControlHandler.ControlEventCallbacks;
import ca.ualberta.ev3ye.controller.vs.VisualServoing;

public class ControllerActivity extends Activity implements LoaderCallbackInterface, ControlEventCallbacks{
	
	private static final String TAG = "ControllerActivity"; 
	private ImageView imageView;
	private ClientTCP clientTCP;
	private VisualServoing vs;
    
    private boolean showVisualServoing = false;
    public final static int MAX_POWER = 100;
    
    private int operator = 1;
    private int leftPower = 0;
	
	private int rightPower = 0;
    private int cameraHeight = 50;
    private String secondControls = "";
    private String reserved = "";
    
    private SeekBar seekBar = null;

	private Spinner controlSpinner = null;
	private Spinner resolutionSpinner = null;
    private ToggleButton toggleButton = null;
    private ToggleButton lightButton = null;
    private View softControlView = null;
    
    private ControlSystem controls = null;
    private MediaPlayer mediaControllerOffline = null;
    private MediaPlayer mediaControllerOnline = null;
    
    private boolean firstUpdate = true;
    private ArrayAdapter<String> resolutionAdapter=null;
    private  List<String> resolutions = new ArrayList<String>();
    
	protected BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	        case LoaderCallbackInterface.SUCCESS:
	        {
	        	Log.i("OK", "OpenCV loaded successfully");
                // Load native library after(!) OpenCV initialization
                System.loadLibrary("jniOCV");
                /*
                try {
                	
					InputStream is = getResources().openRawResource(R.raw.panel_07_jpg);
					byte[] rawMarker = new byte[is.available()];
					is.read(rawMarker);
					MatOfByte mRawMarker = new MatOfByte(rawMarker);
                    //mMarker = Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
                    
                    vs = new VisualServoing(Highgui.imdecode(mRawMarker, Highgui.CV_LOAD_IMAGE_GRAYSCALE), MAX_POWER);
					
                    
                    mMarkerKP = new MatOfKeyPoint();
                    mMarkerDesc = new Mat();
                    FindFeatures(mMarker.getNativeObjAddr(), mMarkerKP.getNativeObjAddr(), mMarkerDesc.getNativeObjAddr());
                    
                    Log.i(TAG, "Marker loaded successfully :D "+mMarkerKP.rows());
                                        
				} catch (IOException e) {
					e.printStackTrace();
				}*/
	        
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
        super.onCreate(savedInstanceState);
        
        controls = new ControlSystem( new GamepadControlHandler(this) );
        mediaControllerOffline = MediaPlayer.create(getApplicationContext() , R.raw.controller_offline);
        mediaControllerOnline = MediaPlayer.create(getApplicationContext() , R.raw.controller_online);
        clientTCP = new ClientTCP(this,mediaControllerOnline, intent.getStringExtra("CameraIP"), true);
        vs = new VisualServoing(this,MAX_POWER);
        
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
        
        toggleButton = (ToggleButton) findViewById(R.id.tracking);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress(cameraHeight);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cameraHeight=progress;
                Log.i("SeekBar","value:"+cameraHeight);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        
        controlSpinner = (Spinner) findViewById( R.id.controlTypeSpinner );
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.control_types, android.R.layout.simple_spinner_item);
        controlSpinner.setAdapter(adapter);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        controlSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3)
			{
				String ctrl = arg0.getItemAtPosition(arg2).toString();
				toggleButton.setVisibility(View.GONE);
				switch (ctrl)
				{
				case "Gamepad":
					controls.setControlState(new GamepadControlHandler(ControllerActivity.this));
					controls.init();
					softControlView.setVisibility(View.GONE);
					toggleButton.setVisibility(View.GONE);
					toggleButton.setChecked(false);
					showVisualServoing = false;
					vs.disable();
					operator = 1;
					break;
					
				case "Soft Gamepad":
					controls.setControlState(new SoftJoystickControlHandler(ControllerActivity.this, softControlView));
					controls.init();
					softControlView.setVisibility(View.VISIBLE);
					toggleButton.setVisibility(View.GONE);
					toggleButton.setChecked(false);
					showVisualServoing = false;
					vs.disable();
					operator = 2;
					break;
					
				case "Tilt":
					controls.setControlState(new TiltControlHandler(ControllerActivity.this, ControllerActivity.this));
					controls.init();
					softControlView.setVisibility(View.GONE);
					toggleButton.setVisibility(View.GONE);
					toggleButton.setChecked(false);
					showVisualServoing = false;
					vs.disable();
					operator = 3;
					break;
					
				case "Visual Servoing":
					softControlView.setVisibility(View.GONE);
					toggleButton.setVisibility(View.VISIBLE);
					controls.setControlState(null);
					updateSeekBar(50);
					vs.enable();
					operator = 4;
					break;
					
				default:
					Log.e("Control", "The spinner defaulted and did not switch control schemes for: " + ctrl);
					controls.setControlState(null);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
				
			}
		});
        
        resolutionSpinner = (Spinner) findViewById(R.id.resolutionSpinner);
        resolutionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resolutions);
        resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionSpinner.setAdapter(resolutionAdapter);
        resolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3)
			{
				String item = (String) arg0.getItemAtPosition(arg2);
				if(!firstUpdate)
					secondControls="5;"+item;
				firstUpdate=false;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{ }
		});
        
        lightButton = (ToggleButton) findViewById(R.id.lightButton);
        lightButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					secondControls="6;ON";
				}
				else
				{
					secondControls="6;OFF";
				}
			}
		});
        
        softControlView = findViewById(R.id.softControlView);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        return controls.onGenericMotionEvent(event);
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
            		try {
						Thread.sleep(10); //100fps
						/**
						operator = 1 (from gamepad) , 2 (from touchscreen) , 3 (from acelerometers) , 4 (visual servoig)
						leftMotor power (-100 : 100 )
						rightMotor power (-100 : 100)
						cameraheight (0 : 100)
						*/
						if(operator<5){//Only for manual operation
						    String controllerInput = ":"+operator+";"+leftPower+";"+rightPower+";"+cameraHeight;
						    if(!secondControls.equals("")){
						    	controllerInput=":"+secondControls;
						    	secondControls="";
						    }
							clientTCP.updateController(controllerInput);
						}else{ //Automatic operation.
							clientTCP.updateController(":"+reserved);
						}
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
                vs.init(getResources().openRawResource(R.raw.panel_07_png));
                Bitmap bitmap;
                clientTCP.updateStreamThread();
                while(!stop){
                	bitmap=null;
                	double initime = System.currentTimeMillis();
                	//boolean success = clientTCP.updateStream();
                	final byte[] textureByteArray = clientTCP.getPicture();
                	
                	if(textureByteArray==null)
                        continue;
                	
                	bitmap = vs.processStreaming(textureByteArray,showVisualServoing);
                	
                	if(bitmap==null){
                		bitmap = BitmapFactory.decodeByteArray(textureByteArray, 0, textureByteArray.length);
                	}
                	double endtime = System.currentTimeMillis();
					Log.d("FPS",""+1000/(endtime-initime));
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
        
        controls.init();
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    	mediaControllerOffline.start();
    	controls.cleanup();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        clientTCP.shutdown();
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

	@Override
	public void onHandlerSetupFailure(String msg)
	{
		Toast.makeText(this, "The controler setup failed: " + msg, Toast.LENGTH_LONG).show();
		Log.e("Control", "The device could not set up a control handler: " + msg);
	}

	@Override
	public void onHandlerStateChanged(String msg, int ID, boolean OK)
	{
		Toast.makeText(this, "The controller hardware state changed: " + msg, Toast.LENGTH_LONG).show();
		Log.i("Control", "The controller hardware state changed: " + msg);
	}

	@Override
	public void onControlEventResult(int leftMotor, int rightMotor, int deltaCamera)
	{
		leftPower = leftMotor;
		rightPower = rightMotor;
		updateSeekBar(cameraHeight+deltaCamera);
		
		Log.d("CONTROL", "left:" + leftMotor + " right:" + rightMotor + " cam:" + cameraHeight);
	}
	
	public int getLeftPower() {
		return leftPower;
	}

	public void setLeftPower(int leftPower) {
		this.leftPower = leftPower;
	}

	public int getRightPower() {
		return rightPower;
	}

	public void setRightPower(int rightPower) {
		this.rightPower = rightPower;
	}
	
	public int getCameraHeight() {
		return cameraHeight;
	}

	public void setCameraHeight(int cameraHeight) {
		
		this.cameraHeight = cameraHeight;
	}
	
	public void updateSeekBar(int height){
		if (height < 0) height = 0;
		if (height > 100) height = 100;
		cameraHeight=height;
		seekBar.setProgress(cameraHeight);
	}
	
	public void updateResolutionList(final String[] resolutionList){
		runOnUiThread(new Runnable() {
            public void run() {
            	firstUpdate = true;
            	resolutions.addAll(Arrays.asList(resolutionList));
        		resolutionAdapter.notifyDataSetChanged();
        		for(int location = 0; location < resolutions.size(); location++){
        			String res=resolutions.get(location);
        			if(res.equals("800x450"))
        				resolutionSpinner.setSelection(location);
        		}
            }
        });
	}

}
