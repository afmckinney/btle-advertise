package com.jifalops.btleadvertise;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;


public class MainActivity extends Activity {
    private static final String SERVICE_UUID = "52dcaf8e-2d15-11e5-b345-feff819cdc9f";
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text);
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "App cannot work with Bluetooth disabled.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing()) {
            return;
        }
        btAdapter = btManager.getAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
            return;
        }

        // Choose advertise settings for long range, but infrequent advertising.
        AdvertiseSettings.Builder settings = new AdvertiseSettings.Builder();
        settings.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER); // Default
        settings.setConnectable(false); // We are not handling connections.
        settings.setTimeout(0); // No time limit;
        settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH); // Long range. Conflict with low power mode?

        AdvertiseData.Builder data = new AdvertiseData.Builder();
        data.addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID)));
        data.setIncludeDeviceName(true);
        data.setIncludeTxPowerLevel(true);

        btAdapter.getBluetoothLeAdvertiser().startAdvertising(settings.build(), data.build(), adCallback);
        btAdapter.getBluetoothLeScanner().startScan(scanCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        btAdapter.getBluetoothLeAdvertiser().stopAdvertising(adCallback);
        btAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        btAdapter.getBluetoothLeScanner().flushPendingScanResults(scanCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private final AdvertiseCallback adCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            textView.append("Started advertising at " + settingsInEffect.getTxPowerLevel() + "dBm.\n");
        }

        @Override
        public void onStartFailure(int errorCode) {
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    textView.append("Advertise failed: already started.\n");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    textView.append("Advertise failed: data too large.\n");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    textView.append("Advertise failed: feature unsupported.\n");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    textView.append("Advertise failed: internal error.\n");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    textView.append("Advertise failed: too many advertisers.\n");
                    break;
            }
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            printScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            textView.append("Received " + results.size() + " batch results:\n");
            for (ScanResult r : results) {
                printScanResult(r);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                    textView.append("Scan failed: already started.\n");
                    break;
                case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    textView.append("Scan failed: app registration failed.\n");
                    break;
                case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                    textView.append("Scan failed: feature unsupported.\n");
                    break;
                case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                    textView.append("Scan failed: internal error.\n");
                    break;
            }
        }

        private void printScanResult(ScanResult result) {
            String id = "unknown";
            int tx = 0;
            ScanRecord rec = result.getScanRecord();
            if (rec != null) {
                id = rec.getDeviceName();
                tx = rec.getTxPowerLevel();
            }
            textView.append("TX: " + tx + " RX: " + result.getRssi() + " from " + id+ ".\n");
        }
    };

}
