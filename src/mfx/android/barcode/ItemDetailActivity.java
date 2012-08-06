package mfx.android.barcode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class ItemDetailActivity extends Activity
{
	// Intent data keys
	public static final String SCANNED_ITEM_POSITION = "scanned.item.position";
	public static final String SCANNED_ITEM_SERIAL = "scanned.item.serial";
	public static final String SCANNED_ITEM_PART = "scanned.item.part";
	public static final String SCANNED_ITEM_QTY = "scanned.item.qty";
	
	// Activity request and result codes
    public static final int IGNORE_OPERATION = 90000;
    public static final int EDIT_SCANNED_OBJECT = 90001;
    public static final int DELETE_SCANNED_OBJECT = 90002;
    public static final int UPDATE_SCANNED_OBJECT = 90003;

	private TextView m_serial_number = null;
	private TextView m_part_number = null;
	private TextView m_tqty = null;
	private int m_qty = 1;
	private int m_item_position = -1;
	
	private enum ACTION
	{
		DELETE_RECORD,
		UPDATE_RECORD,
		NO_OPERATION
	}
	private ACTION m_action = ACTION.NO_OPERATION;
	
	private void save()
	{
		m_action = ACTION.UPDATE_RECORD;
	}
	private void delete()
	{
		m_action = ACTION.DELETE_RECORD;
	}
	
	private int toInt(String s)
	{
		try
		{
			int v = Integer.parseInt(s);
			return v;
		}
		catch( NumberFormatException nfe )
		{
			return -1;
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_detail);
        
        Bundle extra = getIntent().getExtras();
        m_item_position = extra.getInt(SCANNED_ITEM_POSITION, -1);
        
        m_serial_number = (TextView)findViewById(R.id.txtSerialNumber);
        m_part_number = (TextView)findViewById(R.id.txtPartNumber);
        m_tqty = (TextView)findViewById(R.id.txtQty);

        String value = extra.getString(SCANNED_ITEM_SERIAL);
    	m_serial_number.setText( value == null ? "" : value );
    	
    	value = extra.getString(SCANNED_ITEM_PART);
    	m_part_number.setText( value == null ? "" : value );
    	
    	m_qty = extra.getInt(SCANNED_ITEM_QTY, 1);
    	m_tqty.setText(String.valueOf(m_qty));
    	//m_tqty.
    	EditText edit = (EditText)m_tqty;
    	if( edit != null )
    	{
    		edit.addTextChangedListener(new TextWatcher() {
				
				@Override
				public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
				{
				}
				
				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,int arg3)
				{
				}
				
				@Override
				public void afterTextChanged(Editable e)
				{
					String txt = e.toString();
					int value = toInt(txt);
					if( value >= 0 )
						m_qty = value;
				}
			});
    	}
        
        Button btnInc = (Button)findViewById(R.id.butIncQty);
        if( btnInc != null )
        {
        	btnInc.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					m_qty++;
					m_tqty.setText(String.valueOf(m_qty));					
				}
			});
        }
        
        Button btnDec = (Button)findViewById(R.id.butDecQty);
        if( btnDec != null )
        {
        	btnDec.setOnClickListener(new OnClickListener()
			{				
				@Override
				public void onClick(View v)
				{
					if( m_qty > 0 )
					{
						m_qty--;
						m_tqty.setText(String.valueOf(m_qty));
					}
				}
			});
        }
        
    	ImageButton btnSave = (ImageButton)findViewById(R.id.butSave);
    	btnSave.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				save();
				finish();
			}
		});
        	
    	ImageButton btnDelete = (ImageButton)findViewById(R.id.butDelete);
    	btnDelete.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//AlertDialog dlg;
				delete();
				finish();
			}
		});
    }

	@Override
	public void finish()
	{
		// Little check on form data
		int q = toInt(m_tqty.getText().toString());
		if( q >= 0 && q != m_qty )
			m_qty = q;
		
		// Prepare data intent
		Intent result = new Intent();
		result.putExtra(SCANNED_ITEM_POSITION, m_item_position);
		result.putExtra(SCANNED_ITEM_SERIAL, m_serial_number.getText().toString() );
		result.putExtra(SCANNED_ITEM_PART, m_part_number.getText().toString() );
		result.putExtra(SCANNED_ITEM_QTY, m_qty);
	
		int resultCode;
		switch( m_action )
		{
			case DELETE_RECORD:
				resultCode = DELETE_SCANNED_OBJECT;
				break;
			case UPDATE_RECORD:
				resultCode = UPDATE_SCANNED_OBJECT;
				break;
			default:
				resultCode = IGNORE_OPERATION;
				break;
		}
		setResult(resultCode, result);
		super.finish();
	}
	
}
