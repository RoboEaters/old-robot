package com.gbc.andcar;

import java.util.Locale;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;

public class AndCarActivity extends Activity implements IOIOLooperProvider, OnInitListener, OnFocusChangeListener {
	private static final String TAG = "Sample::Activity";

	private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(
			this, this);

	AndCarActivity app;

	public static final int VIEW_MODE_CANNY = 0;
	public static final int VIEW_MODE_RGBA = 1;
	public static final int VIEW_MODE_FIND = 2;
	public static int viewMode = VIEW_MODE_RGBA;

	private MenuItem mItemPreviewRGBA;
	private MenuItem mItemPreviewFind;
	private MenuItem mItemPreviewCanny;
	//UI stuff
	TextView hueLower,hueUpper,satLower,satUpper,valueLower,valueUpper;
	EditText hueL,hueH, satL, satH, valueL, valueH;
	int hueLi, hueHi, satLi, satHi, valueLi, valueHi;
	FrameLayout frame;
	LinearLayout UI;
	Controller viewScreen;
	
	//Threadings
	IOIOThread ioio_thread;

	
	//BLUETOOTH
	
	// Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mChatService = null;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    private String mConnectedDeviceName = null;
    int menuSelection;
    
    //for TTS
  	private TextToSpeech mTts;
  	
	public AndCarActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}		

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = this;
		
		  //for Text to speech
        mTts = new TextToSpeech(this, this);

		
		Log.i(TAG, "onCreate");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setLayout();

		helper_.create();
		
		//BLUETOOTH METHODS
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ensureDiscoverable(); //makes the device discoverable
        menuSelection = 0; //set the inital selection        
	}
	
	
		//BLUETOOTH
	   @Override
	public synchronized void onResume() {
	        super.onResume();
	        if(true) Log.e(TAG, "+ ON RESUME +");

	        // Performing this check in onResume() covers the case in which BT was
	        // not enabled during onStart(), so we were paused to enable it...
	        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
	        if (mChatService != null) {
	            // Only if the state is STATE_NONE, do we know that we haven't started already
	            if (mChatService.getState() == BluetoothService.STATE_NONE) {
	              // Start the Bluetooth chat services
	              mChatService.start();
	            }
	        }
	    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "onCreateOptionsMenu");
		mItemPreviewCanny = menu.add("Canny");
		mItemPreviewRGBA = menu.add("Preview RGBA");
		mItemPreviewFind = menu.add("Find Color");
		return true;
	}
	
	
	public void bluetoothSelect()
	{
		if (menuSelection == 1)
		{
			setVisibility(false);
			viewMode = VIEW_MODE_CANNY;
		}
		if (menuSelection == 0)
		{
			setVisibility(false);
			viewMode = VIEW_MODE_RGBA;
		}
		
		Log.d("Message", "The message recieved was " + menuSelection);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "Menu Item selected " + item);
		if (item == mItemPreviewRGBA)
		{
			setVisibility(false);
			viewMode = VIEW_MODE_RGBA;
		}
		else if (item == mItemPreviewFind)
		{
			setVisibility(true);
			viewMode = VIEW_MODE_FIND;
		}
		else if (item == mItemPreviewCanny)
		{
			setVisibility(false);
			viewMode = VIEW_MODE_CANNY;
		}
			
		return true;
	}

	/****************************************************** functions from IOIOActivity *********************************************************************************/

	protected IOIOLooper createIOIOLooper() {
		ioio_thread = new IOIOThread(viewScreen);
		return ioio_thread;//send them the viewscreen thread
	}

	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		return createIOIOLooper();
	}

	@Override
	protected void onDestroy() {
		helper_.destroy();
		super.onDestroy();
		 if (mChatService != null) mChatService.stop();
	}

	@Override
	protected void onStart() {
		super.onStart();
        if(true) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
	        }
		helper_.start();
		}
	
	  private void setupChat() {
	        Log.d(TAG, "setupChat()");
	        // Initialize the BluetoothService to perform bluetooth connections
	        mChatService = new BluetoothService(this, mHandler);
	        // Initialize the buffer for outgoing messages
	        mOutStringBuffer = new StringBuffer("");
	    }
	  
	  private final Handler mHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
	            case MESSAGE_STATE_CHANGE:
	                if(true) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
	                switch (msg.arg1) {
	                case BluetoothService.STATE_CONNECTED:           
	                    break;
	                case BluetoothService.STATE_CONNECTING:
	                    break;
	                case BluetoothService.STATE_LISTEN:
	                case BluetoothService.STATE_NONE:
	                    break;
	                }
	                break;
	            case MESSAGE_READ:
	                byte[] readBuf = (byte[]) msg.obj;
	                // construct a string from the valid bytes in the buffer
	                String readMessage = new String(readBuf, 0, msg.arg1);
	                try
	                {
	                	 if (!(readMessage == null)) 
	                		 menuSelection = Integer.parseInt(readMessage);
	                }
	                catch (NumberFormatException e)
	                {
	                	Log.i("bluetooth pass", "Not a valid number");
	                }
	                bluetoothSelect();
	                Log.i("Them: " + mConnectedDeviceName, readMessage);
	                break;
	            case MESSAGE_DEVICE_NAME:
	                // save the connected device's name
	                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
	                Toast.makeText(getApplicationContext(), "Connected to "
	                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	                break;
	            case MESSAGE_TOAST:
	                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
	                               Toast.LENGTH_SHORT).show();
	                break;
	            }
	        }
	    };
	    
	    private void connectDevice(Intent data, boolean secure) {
	        // Get the device MAC address
	        String address = data.getExtras()
	            .getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
	        // Get the BLuetoothDevice object
	        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	        // Attempt to connect to the device
	        mChatService.connect(device, secure);
	    }
	    
	 private void ensureDiscoverable() {
	        if(true) Log.d(TAG, "ensure discoverable");
	        if (mBluetoothAdapter.getScanMode() !=
	            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
	            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
	            startActivity(discoverableIntent);
	        }
	    }
	    @Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
	        if(true) Log.d(TAG, "onActivityResult " + resultCode);
	        switch (requestCode) {
	        case REQUEST_CONNECT_DEVICE_SECURE:
	            // When BluetoothDeviceListActivity returns with a device to connect
	            if (resultCode == Activity.RESULT_OK) {
	                connectDevice(data, true);
	            }
	            break;
	        case REQUEST_CONNECT_DEVICE_INSECURE:
	            // When BluetoothDeviceListActivity returns with a device to connect
	            if (resultCode == Activity.RESULT_OK) {
	                connectDevice(data, false);
	            }
	            break;
	        case REQUEST_ENABLE_BT:
	            // When the request to enable Bluetooth returns
	            if (resultCode == Activity.RESULT_OK) {
	                // Bluetooth is now enabled, so set up a chat session
	                setupChat();
	            } else {
	                // User did not enable Bluetooth or an error occured
	                Log.d(TAG, "BT not enabled");
	                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
	                finish();
	            }
	        }
	    }
	@Override
	protected void onStop() {
		helper_.stop();
		super.onStop();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
			helper_.restart();
		}
	}

	public void setLayout()
	{
		 frame = new FrameLayout(this);
		 viewScreen = new Controller(this, mTts);

		 UI = new LinearLayout (this);
		 UI.setOrientation(LinearLayout.VERTICAL);
		 LinearLayout.LayoutParams lp;
		 lp = new LayoutParams(90,600);
		 UI.setLayoutParams(lp);

         hueLower = new TextView(this);
         hueUpper = new TextView(this);
         satLower = new TextView(this);
         satUpper = new TextView(this);
         valueLower = new TextView(this);
         valueUpper = new TextView(this);

         
         hueLower.setText("Hue Lower");
         hueUpper.setText("Hue Upper");
         satLower.setText("Sat Lower");
         satUpper.setText("Sat Upper");
         valueLower.setText("Value Lower");
         valueUpper.setText("Value Upper");
         
         hueL = new EditText(this);
         hueH = new EditText(this);
         satL = new EditText(this);
         satH = new EditText(this);
         valueL = new EditText(this);
         valueH = new EditText(this);
 
         hueL.setOnFocusChangeListener(this);
         hueH.setOnFocusChangeListener(this);
         satL.setOnFocusChangeListener(this);
         satH.setOnFocusChangeListener(this);
         valueL.setOnFocusChangeListener(this);
         valueH.setOnFocusChangeListener(this);
         
         hueL.setInputType(InputType.TYPE_CLASS_NUMBER);
         hueH.setInputType(InputType.TYPE_CLASS_NUMBER);
         satL.setInputType(InputType.TYPE_CLASS_NUMBER);
         satH.setInputType(InputType.TYPE_CLASS_NUMBER);
         valueL.setInputType(InputType.TYPE_CLASS_NUMBER);
         valueH.setInputType(InputType.TYPE_CLASS_NUMBER);
         
         
         //sets the default values to whatever is in sample2View
         double[] l = viewScreen.lo.val;
         double[] h = viewScreen.hi.val;
         hueL.setText(Integer.toString((int) l[0]));
         hueH.setText(Integer.toString((int) h[0]));
         satL.setText(Integer.toString((int) l[1]));
         satH.setText(Integer.toString((int) h[1]));
         valueL.setText(Integer.toString((int) l[2]));
         valueH.setText(Integer.toString((int) h[2]));


        
        hueL.setMaxHeight(10);
        hueL.setWidth(50);
        hueH.setMaxHeight(10);
        hueH.setWidth(50);
        satL.setMaxHeight(10);
        satL.setWidth(50);
        satH.setMaxHeight(10);
        satH.setWidth(50);
        valueL.setMaxHeight(10);
        valueL.setWidth(50);
        valueH.setWidth(50);
        valueH.setMaxHeight(10);

		// UI.addView(hueLower);
		 UI.addView(hueL);
		// UI.addView(hueUpper);
		 UI.addView(hueH);
	//	 UI.addView(satLower);
		 UI.addView(satL);
	//	 UI.addView(satUpper);
		 UI.addView(satH);
//		 UI.addView(valueLower);
		 UI.addView(valueL);
//		 UI.addView(valueUpper);
		 UI.addView(valueH);
		 
		 frame.addView(viewScreen);
		 frame.addView(UI);
		 setContentView(frame);	
	}
	
	public void setVisibility(boolean b)
	{
		if (b)
		{
			hueL.setVisibility(View.VISIBLE);
			hueH.setVisibility(View.VISIBLE);
			satL.setVisibility(View.VISIBLE);
			satH.setVisibility(View.VISIBLE);
			valueL.setVisibility(View.VISIBLE);
			valueH.setVisibility(View.VISIBLE);
		}
		else
		{
			hueL.setVisibility(View.INVISIBLE);
			hueH.setVisibility(View.INVISIBLE);
			satL.setVisibility(View.INVISIBLE);
			satH.setVisibility(View.INVISIBLE);
			valueL.setVisibility(View.INVISIBLE);
			valueH.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onFocusChange(View v, boolean b) 
	{
		if (!v.hasFocus())
		{
		
			//if (v.getId() == hueL.getId())
				hueLi = Integer.parseInt(hueL.getText().toString());
		//	else if (v.getId() == hueH.getId())
				hueHi = Integer.parseInt(hueH.getText().toString());
		//	else if (v.getId() == satH.getId())
				satHi = Integer.parseInt(satH.getText().toString());
		//	else if (v.getId() == satL.getId())
				satLi = Integer.parseInt(satL.getText().toString());
	//		else if (v.getId() == valueL.getId())
				valueLi = Integer.parseInt(valueL.getText().toString());
		//	else if (v.getId() == valueH.getId())
				valueHi = Integer.parseInt(valueH.getText().toString());
			
			viewScreen.setVectors(hueLi, hueHi, satLi, satHi, valueLi, valueHi);
			
		}		
	}

	@Override
	public void onInit(int status) 
	{
		mTts.setLanguage(Locale.KOREAN);
		//mTts.speak("Android Car Online",  TextToSpeech.QUEUE_FLUSH, null);		
	}
}
