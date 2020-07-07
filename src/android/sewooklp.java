package cordova.plugin.swklp;

import cordova.plugin.swklp.util.ChkPrinterStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Vector;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothClass;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.sewoo.port.android.BluetoothPort;
import com.sewoo.request.android.RequestHandler;
import com.sewoo.jpos.command.ESCPOS;
import com.sewoo.jpos.command.ESCPOSConst;
import com.sewoo.jpos.printer.ESCPOSPrinter;
import com.sewoo.jpos.printer.LKPrint;


public class sewooklp extends CordovaPlugin {

	private static final String TAG = "Sewoo Bluetooth Printer";
	private static final int REQUEST_ENABLE_BT = 2;

	private Vector<BluetoothDevice> remoteDevices;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothPort bluetoothPort;

	private String lastConnAddr;
	private ESCPOSPrinter posPtr;
	private ChkPrinterStatus chkStatus;
	private Thread hThread;
	private int sts;
	private Bitmap mBitmap;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("listPairedDevices")) {
            this.listPairedDevices(callbackContext);
            return true;
        }else if(action.equals("setupBluetooth")){
			this.bluetoothSetup();
			return true;
		}else if(action.equals("connectToDevice")){
			String deviceMACAddress = args.getString(0);
			this.connectToDevice(deviceMACAddress,callbackContext);
			return true;
		}else if(action.equals("disconnectFromDevice")){
			this.disconnectFromDevice(callbackContext);
			return true;
		}else if(action.equals("printBulkData")){
            this.printBulkData(args.getString(0), callbackContext);
            return true;
        }
        return false;
    }

	private void printBulkData(String arg, CallbackContext callbackContext){
	  posPtr = new ESCPOSPrinter("ISO8859_1");
	  chkStatus = new ChkPrinterStatus();
      cordova.getThreadPool().execute(new Runnable() {
          public void run() {
			  sts = chkStatus.PrinterStatus(posPtr);
			  if(sts != ESCPOSConst.LK_SUCCESS){
				  callbackContext.error(sts);
				  return;
			  }
              try{
                JSONObject obj = new JSONObject(arg);
                JSONArray printableArray = obj.getJSONArray("printableObjects");

                Integer datalen = printableArray.length();

                for (int i = 0; i < datalen; ++i){
                  JSONObject printable = printableArray.getJSONObject(i);
                  if(printable.has("text")){
                    printText(printable, false, callbackContext);
                  }
                  if(printable.has("image")){
                    printBase64Image(printable,false,callbackContext);
                  }
                  if(printable.has("qrtext")){
                    printQR(printable,false,callbackContext);
                  }
                }
              } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    callbackContext.error(sw.toString());
              }
          }
      });
    }

	private void printText(JSONObject obj, Boolean standalone, CallbackContext callbackContext) throws IOException, JSONException {
	  String text = obj.getString("text");
	  Integer align = obj.getInt ("align");
	  posPtr.printText(text, align, LKPrint.LK_FNT_DEFAULT, LKPrint.LK_TXT_1WIDTH | LKPrint.LK_TXT_1HEIGHT);
	  if(standalone){
		callbackContext.success("Text  sent to printer");
	  }
    }

	private void printBase64Image(JSONObject obj, Boolean standalone, CallbackContext callbackContext) throws IOException, JSONException{
	  String base64Img = obj.getString("image");
	  String cleanImage = base64Img.replace("data:image/png;base64,", "").replace("data:image/jpeg;base64,","");
	  byte[] decodedString = Base64.decode(cleanImage, Base64.DEFAULT);
	  mBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
	  posPtr.lineFeed(1);
	  posPtr.printBitmap(mBitmap, LKPrint.LK_ALIGNMENT_CENTER);
	  if(standalone){
		callbackContext.success("Image sent to printer");
	  }
    }

	private void printQR(JSONObject obj, Boolean standalone, CallbackContext callbackContext) throws IOException, JSONException{
	  String qr = obj.getString("qrtext");
	  posPtr.lineFeed(1);
	  posPtr.printQRCode(qr, qr.length(), 6, ESCPOSConst.LK_QRCODE_EC_LEVEL_L, ESCPOSConst.LK_ALIGNMENT_CENTER);
	  if(standalone){
		callbackContext.success("QR sent to print");
	  }
    }

	private void connectToDevice(String deviceMACAddress, CallbackContext callbackContext){
		cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            try
			{
				BluetoothDevice btDevice = mBluetoothAdapter.getRemoteDevice(deviceMACAddress);
				bluetoothPort.connect(btDevice);
				//bluetoothPort.connect(deviceMACAddress, true); // Register Bluetooth device
				lastConnAddr = deviceMACAddress;
				JSONObject connectionResult = new JSONObject();
				connectionResult.put("name", btDevice.getName());
				connectionResult.put("address",deviceMACAddress);
				connectionResult.put("connected", true);
				RequestHandler rh = new RequestHandler();
				hThread = new Thread(rh);
				hThread.start();
				callbackContext.success(connectionResult);
			}
			catch (Exception e)
			{
				Log.e(TAG, e.getMessage());
				callbackContext.error(e.getMessage());
			}
          }
      });
	}

	private void disconnectFromDevice(CallbackContext callbackContext){
		try
		{
			bluetoothPort.disconnect();
			BluetoothDevice btDevice = mBluetoothAdapter.getRemoteDevice(lastConnAddr);
			JSONObject disconnectionResult = new JSONObject();
			disconnectionResult.put("name", btDevice.getName());
			disconnectionResult.put("address",btDevice.getAddress());
			disconnectionResult.put("connected", false);
			if((hThread != null) && (hThread.isAlive()))
				hThread.interrupt();
			callbackContext.success(disconnectionResult);
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage(), e);
			callbackContext.error(e.getMessage());
		}
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
