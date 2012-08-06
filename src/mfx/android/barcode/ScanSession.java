package mfx.android.barcode;

import java.util.ArrayList;
import java.util.Date;

public class ScanSession
{
	public String name;
	public Date timeStamp;
	public ArrayList<ScannedItem> items;
	public String last_part_number = null;
	private boolean dirty = false;
	
	public ScanSession()
	{
		name = "unnamed";
		items = new ArrayList<ScannedItem>();
		timeStamp = new Date();
		dirty = false;
	}

	public ScanSession(String aName)
	{
		name = aName;
		items = new ArrayList<ScannedItem>();
		timeStamp = new Date();
		dirty = false;
	}

	public boolean isDirty()
	{
		return dirty;
	}
	public void setDirty(boolean yn)
	{
		dirty = yn;
	}
	public ScannedItem findByPartNumber(String pn)
	{
		for( ScannedItem it : items )
		{
			if( it.part_number != null && it.part_number.equals(pn) )
				return it;
		}
		return null;
	}
	public ScannedItem findBySerialNumber(String sn)
	{
		for( ScannedItem it : items )
		{
			if( it.serial_number != null && it.serial_number.equals(sn) )
				return it;
		}
		return null;
	}
	
	public ScannedItem addItem( String part_number, String serial_number )
	{
		ScannedItem it = new ScannedItem();
		it.part_number = part_number;
		it.serial_number = serial_number;
		it.qty = 1;
		
		items.add(it);

		last_part_number = part_number;
		dirty = true;
		return it;
	}
	public ScannedItem addItem( String part_number )
	{
		return addItem(part_number, null);
	}
	public ScannedItem addItem()
	{
		return addItem(last_part_number);
	}
}
