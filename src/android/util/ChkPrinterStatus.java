
package cordova.plugin.swklp.util;

import com.sewoo.jpos.command.ESCPOSConst;
import com.sewoo.jpos.printer.ESCPOSPrinter;

public class ChkPrinterStatus {

	private int returnValue;

	public int PrinterStatus(ESCPOSPrinter posPtr)
	{
		returnValue = posPtr.printerCheck();
		if(ESCPOSConst.LK_SUCCESS == returnValue)
		{
			returnValue = posPtr.status();
			if(ESCPOSConst.LK_STS_NORMAL == returnValue)
			{ // No errors
				return returnValue;
			} else {
				// Cover open error.
				if((ESCPOSConst.LK_STS_COVER_OPEN & returnValue) > 0)
				{
					return ESCPOSConst.LK_STS_COVER_OPEN;
				}
				// Paper empty error.
				if((ESCPOSConst.LK_STS_PAPER_EMPTY & returnValue) > 0)
				{
					return ESCPOSConst.LK_STS_PAPER_EMPTY;
				}
				// Battery low warning.
				if((ESCPOSConst.LK_STS_BATTERY_LOW & returnValue) > 0)
				{
					return ESCPOSConst.LK_STS_BATTERY_LOW;
				}
			}
		} else { // Error(LK_FAIL, LK_STS_PRINTEROFF, LK_STS_TIMEOUT)
			return returnValue;
		}
		return 0;
	}
}
