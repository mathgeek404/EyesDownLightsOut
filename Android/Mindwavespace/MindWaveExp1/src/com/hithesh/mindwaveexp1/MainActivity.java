package com.hithesh.mindwaveexp1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;

public class MainActivity extends Activity {
	
	private TGDevice tgDevice;
	private BluetoothAdapter btAdapter;
	private MediaPlayer mp;
	private Context mContext=this;
	private double[] rawData=new double[512]; 
	private int rawDataIndex=0;
	private FFT fft=new FFT(512);
	private double[] window=fft.getWindow();
	private double[] re=new double[512];
	private double[] im=new double[512];
	private boolean writable=false;
	private File file=null;
	private BufferedWriter writer=null;
	private String fileName="default.csv"; 
	
	private Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			case TGDevice.MSG_STATE_CHANGE:
				switch(msg.arg1){
				case TGDevice.STATE_IDLE:
					break;
				case TGDevice.STATE_CONNECTING:
					break;
				case TGDevice.STATE_CONNECTED:
					tgDevice.start();
					break;
				case TGDevice.STATE_DISCONNECTED:
					break;
				case TGDevice.STATE_NOT_FOUND:
					break;
				case TGDevice.STATE_NOT_PAIRED:
					break;
				}
				break;
			case TGDevice.MSG_POOR_SIGNAL:
				break;
			case TGDevice.MSG_RAW_DATA:
				if (rawDataIndex==512){
					rawDataIndex=0;
					re = rawData;
					for (int i=0;i<512;i++){
						im[i]=0;
						re[i]=re[i]*window[i];
					}
					fft.fft(re, im);
					Log.d("fft","done");
				}
				double data=msg.arg1;
				rawData[rawDataIndex]=data;
				try {
					writer.write(Double.toString(data)+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				rawDataIndex++;
				break;
			case TGDevice.MSG_ATTENTION:
				Log.d("attention",""+msg.arg1);
				if (msg.arg1<30){
					if (mp != null) {
					     mp.release();
					  }
					mp = MediaPlayer.create(mContext, R.raw.finger_snap_sound_effect);
					mp.start();
				}
				break;
			case TGDevice.MSG_BLINK:
				break;
			case TGDevice.MSG_EEG_POWER:
				double pow=0;
				for (int j=18;j<31;j++){
					pow=pow+(re[j]*re[j]+im[j]*im[j])/512;
				}
				TGEegPower ep = (TGEegPower) msg.obj;
				double beta_pow= ep.highBeta;
				Log.d("beta_ratio",""+pow);
				break;
				
			}
		}
	};
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btAdapter= BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null){
        	tgDevice=new TGDevice(btAdapter,handler);
        }
        if (mp != null) {
            mp.release();
         }
        writable=isExternalStorageWritable();
        File path=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        path.mkdirs();
        file=new File(path,fileName);
        try{
        	writer = new BufferedWriter(new FileWriter(file,true));
        } catch (IOException e) {
			e.printStackTrace();
		}
        // Create a new MediaPlayer to play this sound
	    mp = MediaPlayer.create(this, R.raw.finger_snap_sound_effect);
	    mp.start(); 
    }
    
    public void connectClick(View view){
    	if(tgDevice!=null){
        	tgDevice.connect(true);
        }
    }
    public void disconnectClick(View view){
    	tgDevice.close();
    }
    @Override
    protected void onPause() {
    	super.onPause();
    }

    @Override
    protected void onResume() {
    	
    	super.onResume();
    }
    @Override
    protected void onDestroy(){
    	if (mp != null) {
		     mp.release();
		     mp=null;
		  }
    	if (writer!=null){
    		try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	Log.d("destroy","destroyed");
    	super.onDestroy();
    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
