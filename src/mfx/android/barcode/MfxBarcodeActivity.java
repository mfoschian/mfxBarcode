package mfx.android.barcode;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MfxBarcodeActivity extends Activity
{
	private enum ScanMode
	{
		SCANMODE_MULTIPLE_SERIALS,
		SCANMODE_MULTIPLE_ARTICLES,
		SCANMODE_SINGLE_PARTNUMBER
	}

	private ScanMode m_scanMode = ScanMode.SCANMODE_MULTIPLE_SERIALS;
	private ScanSession m_session = null;
	
	private static String CSV_HEADERS[] = { "Modello","PartNumber","Seriale","Seriale Fvg","Qta" };
	private static String DEFAULT_SEPARATOR = ";";
	private static String DEFAULT_NEWLINE = "\n";

	private boolean newSession(ScanMode sm, String name)
	{
		if( m_session != null )
		{
			if( m_session.isDirty() )
			{
				// Ask to save the session !!!!
				// If action cancelled: return false
			}
		}
		
		m_session = new ScanSession(name);
		m_scanMode = sm;

        ListView lst = (ListView)findViewById(R.id.lstScanned);
        m_adapter = new ScanItemsAdapter(this,R.layout.scanned_item, m_session.items);
        lst.setAdapter(m_adapter);
		return true;
	}

	
    private void onScancodeAquired(String code)
    {
    	if( code == null )
    		return;
    	
    	ScannedItem item;
    	
    	switch(m_scanMode)
    	{
    		case SCANMODE_MULTIPLE_SERIALS:
    			item = m_session.findBySerialNumber(code);
    			if( item == null )
    			{
    				item = m_session.addItem(m_session.last_part_number, code);
    			}
    			else
    			{
    				Toast.makeText(this, "Attenzione! Seriale duplicato: " + code, Toast.LENGTH_LONG).show();
    			}

    			break;
    			
    		case SCANMODE_MULTIPLE_ARTICLES:
    			item = m_session.findByPartNumber(code);
    			if( item == null )
    			{
    				item = m_session.addItem(code);
    			}
    			else
    			{
    				item.qty++;
    				m_session.setDirty(true);
    			}

    			break;
    			
    		case SCANMODE_SINGLE_PARTNUMBER:
    			m_session.last_part_number = code;
    			m_scanMode = ScanMode.SCANMODE_MULTIPLE_SERIALS;
    		    Toast.makeText(this, "Part Number aquired. Now scan serials.", Toast.LENGTH_SHORT).show();
    			break;
    	}
    	
    	m_adapter.notifyDataSetChanged();
    }

    @SuppressWarnings("unused")
	private void fakeScan()
    {
    	StringBuilder sb = new StringBuilder();
    	int scanLen = 12;
    	char dictionary[] = { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I' };
    	for( int i=0; i<scanLen; i++)
    	{
    		int x = (int)(dictionary.length * Math.random());
    		sb.append(dictionary,x,1);
    	}
    	String code = sb.toString();
    	onScancodeAquired(code);
    }
    
    private void goScan()
    {
    	if( m_session == null )
    	{
    		// TODO: ask session name !!!
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd.HHmm");
    		Date d = new Date();
    		String time = sdf.format(d);
    		String name = "barcode."+time;
    		newSession(m_scanMode, name);
    	}

    	//fakeScan();
    	
    	/**/
    	IntentIntegrator integrator = new IntentIntegrator(this);
    	integrator.initiateScan();
    	/**/
    }
    
    public void exportSession()
    {
    	if( m_session == null )
    		return;

    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	//String exportDir = preferences.getString("export_dir", null);
    	String c = preferences.getString("separator", DEFAULT_SEPARATOR);
		String eol = preferences.getString("line_end", DEFAULT_NEWLINE); // System.getProperty("line.separator") );

		String sd_state = Environment.getExternalStorageState();
		if( ! sd_state.equals(Environment.MEDIA_MOUNTED))
		{
			Toast.makeText(this, "SD not ready", Toast.LENGTH_LONG);
			return;
		}
		
	    File path = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS );
	    String fileName = m_session.name + ".csv";
	    File file = new File(path, fileName);
	    
		BufferedWriter writer = null;

		try
	    {
	        // Make sure the Pictures directory exists.
	        path.mkdirs();

			//writer = new BufferedWriter(new OutputStreamWriter(openFileOutput(fileName, MODE_WORLD_WRITEABLE)));
	        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			
			StringBuilder sb = new StringBuilder( CSV_HEADERS[0] );
			for( int i=1; i<CSV_HEADERS.length; i++ )
			{
				sb.append(c).append(CSV_HEADERS[i]);
			}
			String header =  sb.toString();
			
			writer.write(header + eol);

			for( ScannedItem it: m_session.items )
			{
				writer.write( (it.model_name == null ? "" : it.model_name) + c );
				writer.write( (it.part_number == null ? "" : it.part_number) + c );
				writer.write( (it.serial_number == null ? "" : it.serial_number) + c );
				writer.write( c );
				writer.write( String.valueOf(it.qty) );
				writer.write( eol );
			}
			
			Toast.makeText(this, "Data exported to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
		}
		catch (Exception e)
		{
				e.printStackTrace();
				Toast.makeText(this, "Error while exporting data", Toast.LENGTH_LONG).show();
		}
		finally
		{
			if (writer != null)
			{
				try
				{
					writer.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
    }
    
    public void editPreferences()
    {
		// Launch Preference activity
		Intent i = new Intent(this, MyPreferenceActivity.class);
		startActivity(i);
    }
    
	class ScanItemsAdapter extends ArrayAdapter<ScannedItem>
	{

		private final List<ScannedItem> mList;
		private final Activity mContext;
		
		class ViewHolder
		{
			public TextView q; // qty
			public TextView s; // serial
			public TextView p; // part
		}
		
		public ScanItemsAdapter(Activity context, int resource, List<ScannedItem> objects)
		{
			super(context, resource, objects);

			mList = objects;
			mContext = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View rowView = convertView;
			ViewHolder holder = null;
			
			if( rowView == null )
			{
				LayoutInflater inflater = mContext.getLayoutInflater();
				rowView = inflater.inflate(R.layout.scanned_item, null);
				holder = new ViewHolder();
				holder.q = (TextView)rowView.findViewById(R.id.txtQty);
				holder.s = (TextView)rowView.findViewById(R.id.txtSerialNumber);
				holder.p = (TextView)rowView.findViewById(R.id.txtPartNumber);
				rowView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder)rowView.getTag();
			}

			if( holder != null )
			{
				ScannedItem it = mList.get(position);
				String q = String.valueOf(it.qty);
				holder.q.setText(q);
				if(it.serial_number == null)
					holder.s.setText("");
				else
					holder.s.setText(it.serial_number);
				
				if(it.part_number == null)
					holder.p.setText("");
				else
					holder.p.setText(it.part_number);
			}
			
			return rowView;
		}
	}
    
	private ScanItemsAdapter m_adapter;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button go = (Button)findViewById(R.id.butScan);
        if( go != null )
        {
        	go.setOnClickListener(new OnClickListener()
			{
				
				@Override
				public void onClick(View v)
				{
					// TODO Auto-generated method stub
					goScan();
				}
			});
        }
        
        ListView lst = (ListView)findViewById(R.id.lstScanned);
        registerForContextMenu(lst);
    }
      
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
      if (v.getId()==R.id.lstScanned)
      {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        ScannedItem item = m_adapter.getItem( info.position );
        
		Intent i = new Intent(this, ItemDetailActivity.class);
		i.putExtra(ItemDetailActivity.SCANNED_ITEM_POSITION, info.position);
		i.putExtra(ItemDetailActivity.SCANNED_ITEM_SERIAL, item.serial_number);
		i.putExtra(ItemDetailActivity.SCANNED_ITEM_PART, item.part_number);
		i.putExtra(ItemDetailActivity.SCANNED_ITEM_QTY, item.qty);

		startActivityForResult(i, ItemDetailActivity.EDIT_SCANNED_OBJECT);
      }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch( item.getItemId() )
    	{
    		case R.id.mnuSetPartNum:
    			m_scanMode = ScanMode.SCANMODE_SINGLE_PARTNUMBER;
    		    Toast.makeText(this, "Ora scansiona un part number", Toast.LENGTH_SHORT).show();
    		    goScan();
    			break;
    		case R.id.mnuSetPartNumMode:
    			m_scanMode = ScanMode.SCANMODE_MULTIPLE_ARTICLES;
    		    Toast.makeText(this, "Modalità consumabili", Toast.LENGTH_SHORT).show();
    			break;
    		case R.id.mnuSetSerialMode:
    			m_scanMode = ScanMode.SCANMODE_MULTIPLE_SERIALS;
    		    Toast.makeText(this, "Modalità seriali", Toast.LENGTH_SHORT).show();
    			break;
    		case R.id.mnuExport:
    			exportSession();
    			break;
    		case R.id.mnuOptions:
    			editPreferences();
    			break;
    	}
	    return true;
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent intent) 
    {
	  IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
	  if (scanResult != null)
	  {
		  String scanCode = scanResult.getContents();
		  onScancodeAquired(scanCode);
		  return;
	  }
  
	  switch( requestCode )
	  {
		  case ItemDetailActivity.EDIT_SCANNED_OBJECT:
			  int position = intent.getIntExtra(ItemDetailActivity.SCANNED_ITEM_POSITION, -1);
			  switch( resultCode )
			  {
				  case ItemDetailActivity.DELETE_SCANNED_OBJECT:
					  m_session.items.remove(position);
					  m_adapter.notifyDataSetChanged();
					  break;
					  
				  case ItemDetailActivity.UPDATE_SCANNED_OBJECT:
					  ScannedItem it = m_session.items.get(position);
					  String value = intent.getStringExtra(ItemDetailActivity.SCANNED_ITEM_SERIAL);
					  if( value != null )
						  it.serial_number = value;
					  value = intent.getStringExtra(ItemDetailActivity.SCANNED_ITEM_PART);
					  if( value != null )
						  it.part_number = value;
					  
					  it.qty = intent.getIntExtra(ItemDetailActivity.SCANNED_ITEM_QTY, it.qty);
					  
					  m_adapter.notifyDataSetChanged();
					  break;
			  }
			  break;
	  }
	}  
    
}