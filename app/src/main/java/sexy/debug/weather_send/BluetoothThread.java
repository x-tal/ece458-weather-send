package sexy.debug.weather_send;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


// TODO: Check thread
public class BluetoothThread extends Thread {

	public final static String TAG = "BluetoothThread";

	private BluetoothSocket mmSocket;
	private InputStream mmInStream;
	
	public BluetoothThread(BluetoothDevice device) {
		try {
			this.mmSocket = device.createRfcommSocketToServiceRecord(BluetoothService.MY_UUID);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.mmSocket != null) {
			Log.d(TAG, "AcceptThread create");
			try {
				this.mmInStream = this.mmSocket.getInputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void run() {
		try {
			this.mmSocket.connect();
			Log.d(TAG, "AcceptThread connect success!");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "AcceptThread goto while");
		while (true) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(this.mmInStream));
				String inputLine;
				while((inputLine = br.readLine()) != null) {
					String message = inputLine;
					Log.d(TAG, message);
				}
			} catch (IOException e) {
				// Bluetooth data 못받아 올 시의 임시 처리.
				e.printStackTrace();
				try {
//					if (this.mmSocket.isConnected() == true) {
//						this.mmSocket.close();
//					}
					this.mmSocket.connect();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	public void cancel() {
		try {
			this.mmSocket.close();
		} catch(IOException e) {
			Log.d("OK", "AcceptThread cancel exception");
		}
	}
}
