package cordova.plugin.swklp;

import java.util.Iterator;
import java.util.Vector;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;

import com.sewoo.port.android.BluetoothPort;
import com.sewoo.request.android.RequestHandler;


public class sewooklp extends CordovaPlugin {

	private static final String TAG = "Sewoo Bluetooth Printer";
	private static final int REQUEST_ENABLE_BT = 2;

	private Vector<BluetoothDevice> remoteDevices;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothPort bluetoothPort;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("listPairedDevices")) {
            this.listPairedDevices(callbackContext);
            return true;
        }else if(action.equals("setupBluetooth")){
			this.bluetoothSetup();
			return true;
		}
        return false;
    }

    private void listPairedDevices(CallbackContext callbackContext) {
		JSONArray pairedDevices = this.addPairedDevices();
        if (pairedDevices.length() > 0) {
            callbackContext.success(pairedDevices);
        } else {
            callbackContext.error("Not Paired devices");
        }
    }

	private JSONArray addPairedDevices()
	{
		BluetoothDevice pairedDevice;
		JSONArray jsonArray = new JSONArray();
		Iterator<BluetoothDevice> iter = (mBluetoothAdapter.getBondedDevices()).iterator();
		while(iter.hasNext())
		{
			pairedDevice = iter.next();
			if(bluetoothPort.isValidAddress(pairedDevice.getAddress()))
			{
				remoteDevices.add(pairedDevice);
				//adapter.add(pairedDevice.getName() +"\n["+pairedDevice.getAddress()+"] [Paired]");
				try{
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("name", pairedDevice.getName());
					jsonObject.put("address",pairedDevice.getAddress());
					jsonObject.put("deviceType",getBTMajorDeviceClass(pairedDevice.getBluetoothClass().getMajorDeviceClass()));
					jsonArray.put(jsonObject);
				}catch(JSONException ex){
					Log.e(TAG, ex.getMessage(), ex);
				}

			}
		}
		return jsonArray;
	}

	private String getBTMajorDeviceClass(int major) {
		switch (major) {
			case BluetoothClass.Device.Major.AUDIO_VIDEO:
				return "AUDIO_VIDEO";
			case BluetoothClass.Device.Major.COMPUTER:
				return "COMPUTER";
			case BluetoothClass.Device.Major.HEALTH:
				return "HEALTH";
			case BluetoothClass.Device.Major.IMAGING:
				return "IMAGING";
			case BluetoothClass.Device.Major.MISC:
				return "MISC";
			case BluetoothClass.Device.Major.NETWORKING:
				return "NETWORKING";
			case BluetoothClass.Device.Major.PERIPHERAL:
				return "PERIPHERAL";
			case BluetoothClass.Device.Major.PHONE:
				return "PHONE";
			case BluetoothClass.Device.Major.TOY:
				return "TOY";
			case BluetoothClass.Device.Major.UNCATEGORIZED:
				return "UNCATEGORIZED";
			case BluetoothClass.Device.Major.WEARABLE:
				return "AUDIO_VIDEO";
			default:
				return "unknown!";
		}
	}

	private void clearBtDevData()
	{
		remoteDevices = new Vector<BluetoothDevice>();
	}

	private void bluetoothSetup()
	{
		// Initialize
		clearBtDevData();
		bluetoothPort = BluetoothPort.getInstance();
		bluetoothPort.SetMacFilter(false);//not using mac filtering
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null)
		{
		    // Device does not support Bluetooth
			return;
		}
		if (!mBluetoothAdapter.isEnabled())
		{
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

			cordova.getActivity().startActivityForResult (enableBtIntent, REQUEST_ENABLE_BT);
		    //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}
}
