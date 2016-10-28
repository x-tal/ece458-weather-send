package sexy.debug.weather_send;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.design.widget.Snackbar;
import android.util.Log;


// TODO: Check thread
public class BluetoothThread extends Thread {

	public final static String TAG = "BluetoothThread";

    private BluetoothThreadListener listener;

	private BluetoothSocket mmSocket;
	private InputStream mmInStream;
	private OutputStream mmOutStream;

	public BluetoothThread(BluetoothDevice device, BluetoothThreadListener listener) {
		try {
			this.mmSocket = device.createRfcommSocketToServiceRecord(BluetoothService.MY_UUID);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.mmSocket != null) {
			Log.d(TAG, "AcceptThread create");
			try {
				this.mmInStream = this.mmSocket.getInputStream();
				this.mmOutStream = this.mmSocket.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

        this.listener = listener;
	}
	
	public void run() {
        if (this.mmSocket.isConnected()) {
            return;
        }

		Log.d(TAG, "AcceptThread goto while");
		while (true) {
            if (!this.mmSocket.isConnected()) {
                try {
                    this.mmSocket.connect();
                    listener.connected();
                    Log.d(TAG, "AcceptThread connect success!");
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            } else {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(this.mmInStream));
                    String inputLine;
                    while ((inputLine = br.readLine()) != null) {
                        String message = inputLine;
                        String result = "";
                        for (int i = 0; i < message.length(); i++) {
                            result += String.format("%02X", (int) message.charAt(i));
                        }
                        Log.d(TAG, result);
                    }
                } catch (IOException e) {
                    // Bluetooth data 못받아 올 시의 임시 처리.
                    e.printStackTrace();
                    break;
                }
            }
		}

        listener.disconnected();
	}

	public void write(byte[] bytes) {
		try {
			mmOutStream.write(bytes);
			mmOutStream.write(0x0A);		// for line break
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void cancel() {
		try {
			this.mmSocket.close();
		} catch(IOException e) {
			Log.d("OK", "AcceptThread cancel exception");
		}
	}

    public interface BluetoothThreadListener {
        void connected();
        void disconnected();
    }
}
