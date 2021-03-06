package com.pad.sss04.pianocontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Shared preferences save/load functionality
    private static final String MY_PREFERENCES = "My_Preferences";
    private static SharedPreferences sharedPreferences;
    private SharedPreferences.Editor preferenceEditor;
    private static String prefMACAddress = null;
    private static String prefMACkey = "prefMAC";

    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Button
    private Button mConnectButton;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Local Broadcast Receiver
    private BroadcastReceiver mBroadcastReceiver;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (D) Log.e(TAG, "+++ ON CREATE +++");

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_unavailable), Toast.LENGTH_LONG).show();
            finish();
        }

        // Tries the connection and connect with the remembered device when found
        tryConnection();

        // Sets up the broadcast receiver
        setupReceiver();

        // Create the connect button with the connection functionality
        mConnectButton = (Button) findViewById(R.id.buttonConnect);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        });


    }


    // This method sets up the receiver to receive messages from the BluetoothClientService
    private void setupReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get the data received from the broadcast
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    // Get the message from the data bundle
                    String ServiceMessage = extras.getString("ServiceMessage");
                    if (ServiceMessage != null) {
                        switch (ServiceMessage) {
                            // If a new connection is made, go to a new activity and notify the user
                            case "CONNECTED":
                                Intent i = new Intent(MainActivity.this, CollectionsActivity.class);
                                Toast.makeText(MainActivity.this, "Connection to the toy successful.", Toast.LENGTH_LONG).show();
                                startActivity(i);
                                break;
                            // Notify the user if the attempted connection failed
                            case "CONNECTION_FAILED":
                                Toast.makeText(MainActivity.this, "Connection failed.", Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                }
            }
        };
    }

    // This methods attempts to connect to a known device saved in sharedPreferences
    private void tryConnection() {
        // Get the sharedPreferences and set the MAC address when it exists
        sharedPreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        prefMACAddress = sharedPreferences.getString(prefMACkey, null);

        // Connect with the remembered device if it exists
        if (prefMACAddress != null && mBluetoothAdapter.isEnabled()) {
            setupConnection();
            try {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(prefMACAddress);
                connectDevice(device.getAddress());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister receiver when it's not needed anymore
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        // Register the receiver to the BluetoothClientService broadcaster
        LocalBroadcastManager.getInstance(this).registerReceiver((mBroadcastReceiver),
                new IntentFilter(BluetoothClientService.BLUETOOTH_RESULT)
        );

        // If BT is not on, request that it be enabled.
        // setupConnection() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            // Otherwise, setup the connection
            setupConnection();
        }
    }

    private void setupConnection() {
        Log.d(TAG, "setupConnection()");

        // Initialize the BluetoothClientService to perform bluetooth connections
        Intent i = new Intent(MainActivity.this, BluetoothClientService.class);
        startService(i);

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    connectDevice(address);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set the connection
                    setupConnection();
                    tryConnection();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, getString(R.string.bt_not_enabled_leaving), Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(String address) {
        // Set the preferences MAC address to the current fetched address and apply it
        preferenceEditor = sharedPreferences.edit();
        prefMACAddress = address;
        preferenceEditor.putString(prefMACkey, prefMACAddress);
        preferenceEditor.apply();

        // Attempt to connect to the device
        Intent i = new Intent(MainActivity.this, BluetoothClientService.class);
        i.putExtra("address", address);
        startService(i);
    }

}

