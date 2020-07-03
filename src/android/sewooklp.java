package cordova.plugin.swklp;

import java.util.Iterator;
import java.util.Vector;

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

	private static final int REQUEST_ENABLE_BT = 2;

	private Vector<BluetoothDevice> remoteDevices;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothPort bluetoothPort;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }else if(action.equals("setupBluetooth")){
			this.bluetoothSetup();
			return true;
		}
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
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
