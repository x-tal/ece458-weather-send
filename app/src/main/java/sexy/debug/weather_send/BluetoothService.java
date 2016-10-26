package sexy.debug.weather_send;

import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

// TODO: Check Service
public class BluetoothService {

    public final static String TAG = "BluetoothService";

	public final static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public final static int REQUEST_ENABLE_BT = 2;

	private BluetoothAdapter btAdapter;
	private Activity mActivity;
	private Handler mHandler;

	public BluetoothService(Activity ma, Handler mh) {
		this.mActivity = ma;
		this.mHandler = mh;
		
		this.btAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	
	public boolean getDeviceState() {
		if (this.btAdapter == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public void enableBluetooth() {
		if (!this.btAdapter.isEnabled()) {
			Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			this.mActivity.startActivityForResult(i, REQUEST_ENABLE_BT);
		}
	}
	
	public Set<BluetoothDevice> getBondedDevices() {
		return this.btAdapter.getBondedDevices();
	}
	
	public BluetoothDevice eceDevice() {
		Set<BluetoothDevice> pairedDevices = this.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				Log.d(TAG, device.getName() + ", " + device.getAddress());
				
				// Temply. 
				if (device.getName().equals("yeop")) {
					Log.d(TAG, "SUCCESS to yeop");
					return device;
				}
			}
		}

		return null;
	}
	
	public BluetoothAdapter getBTAdapter() {
		return this.btAdapter;
	}

}
