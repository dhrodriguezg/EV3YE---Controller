package ca.ualberta.ev3ye.controller.comm;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.util.Log;

public class ClientTCP {
	
	private static final String TAG = "ClientTCP";
	private static final int GREETING_PORT = 5555;
	private static final int TRANSFER_PORT = 8888;
	
	private boolean isDirectWIFI = false;
	private String serverAddress=null;
	
	private boolean isEV3Camera = false;
	
	private Socket clientSocket = null;
	private DataOutputStream dataOutput = null;
	private DataInputStream dataInput = null;
	private boolean isTransferingData = false;
	private byte[] picture = null;
	
	private String btDevices = "";
	
	public ClientTCP(String host, boolean isP2PiP){
		serverAddress = host;
		isDirectWIFI = isP2PiP;
	}
	
	public boolean greetServer(){
		isEV3Camera = false;
		try {
			Socket serverGreeting;
			if(isDirectWIFI){
				serverGreeting = new Socket();
				serverGreeting.bind(null);
				serverGreeting.connect((new InetSocketAddress(serverAddress, GREETING_PORT)), 500);
			}else{
				serverGreeting = new Socket(serverAddress, GREETING_PORT);
			}
			
			DataOutputStream dataOutput = new DataOutputStream(serverGreeting.getOutputStream());
			DataInputStream dataInput = new DataInputStream(serverGreeting.getInputStream());
			
			dataOutput.writeUTF("Are you EV3 Camera?");
			isEV3Camera = dataInput.readBoolean();
			if(isEV3Camera){
				btDevices=dataInput.readUTF();
			}
    		dataOutput.close();
    		dataInput.close();
    		serverGreeting.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isEV3Camera;
	}
	
	public void connect2Server(){
		Thread thread = new Thread() {
            public void run() {
            	try {
        			clientSocket = new Socket(serverAddress, TRANSFER_PORT);
        			dataInput = new DataInputStream(clientSocket.getInputStream());
        			dataOutput = new DataOutputStream(clientSocket.getOutputStream());
        			clientSocket.setKeepAlive(true);
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
            }
        };
        thread.start();
	}
	
	
	public boolean updateStream(String msg){
		
		if(isTransferingData)
			return false; //Is still transfering data, skip the next frame.
		
		boolean requestCompleted = false;
		boolean reconnect = false;
		isTransferingData = true;
		int requestNumber = 0;
		
		if(clientSocket==null)
			reconnect=true;
		
		while (!requestCompleted && requestNumber++ < 100){ //100 tries
			Log.i(TAG, "Sending Data to server...");
			try {
				
				if(reconnect){
					
					if(clientSocket!=null && !clientSocket.isClosed())
						clientSocket.close();
					Log.e(TAG, "Client disconnected, connecting...");
					clientSocket = new Socket(serverAddress, TRANSFER_PORT);
					dataOutput = new DataOutputStream(clientSocket.getOutputStream());
					dataInput = new DataInputStream(clientSocket.getInputStream());
					clientSocket.setKeepAlive(true);
					Log.i(TAG, "***Client connected");
					reconnect = false;
				}
				
				while(dataInput.available()==0){ //maybe this device is going too fast, so wait until there is new data...
                    Thread.sleep(1);
                }
                //Transfering picture
                int arrayLength = dataInput.readInt();
                picture = new byte[arrayLength];
                Log.i(TAG, "Receiving: "+arrayLength);
                dataInput.readFully(picture);

                dataOutput.writeUTF(msg);
				
				//Data transfer completed
				requestCompleted = true;				
				Log.i(TAG, "Data sent successfully, tries: "+requestNumber);
			} catch (IOException e) {
				Log.e(TAG, "Sudden disconnection from the Server °O° ");
				e.printStackTrace();
				reconnect = true;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isTransferingData = false;
		return requestCompleted;
	}
	
	private boolean updateControls(){
		// TODO  controls make the code to update controls.....
		return true;
	}
	
	public void shutdown(){
		try {
			if(clientSocket!=null && !clientSocket.isClosed())
				clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Client socket not closed properly, check port availability: "+TRANSFER_PORT);
		}
		clientSocket=null;
	}
	
	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public String getBtDevices() {
		return btDevices;
	}

	public void setBtDevices(String btDevices) {
		this.btDevices = btDevices;
	}
	
	public byte[] getPicture() {
		return picture;
	}

	public void setPicture(byte[] picture) {
		this.picture = picture;
	}
	
	
}