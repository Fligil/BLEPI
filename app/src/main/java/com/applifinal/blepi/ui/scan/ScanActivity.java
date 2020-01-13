package com.applifinal.blepi.ui.scan;


import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.applifinal.blepi.R;
import com.applifinal.blepi.data.local.LocalPreferences;
import com.applifinal.blepi.ui.scan.adapter.ScanAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//import com.applifinal.blepi.ui.main.adapter.ExempleAdapter;
////////////

public class ScanActivity extends AppCompatActivity {
    public static Intent getStartIntent(final Context ctx){
        return new Intent(ctx, ScanActivity.class);
    }

    // REQUEST Code de gestion
    private static final int REQUEST_LOCATION_CODE = 1235;
    private static final int REQUEST_ENABLED_LOCATION_CODE = 1236;
    private static final long SCAN_DURATION_MS = 10_000L;
    private static final int REQUEST_ENABLE_BLE = 999;

    // Gestion du bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt currentBluetoothGatt = null; // Connexion actuelle
    private boolean isScanning = false;
    private final Handler scanningHandler = new Handler();

    // Partie adapter
    private ScanAdapter deviceAdapter;
    private ArrayList<BluetoothDevice> deviceArrayList = new ArrayList<>();

    // Filtre UUID
    private static UUID DEVICE_UUID = UUID.fromString("795090c7-420d-4048-a24e-18e60180e23c");
    private static UUID CHARACTERISTIC_TOGGLE_LED_UUID = UUID.fromString("59b6bf7f-44de-4184-81bd-a0e3b30c919b");

    private BluetoothDevice selectedDevice;
/////////////


    private void toggleLed() {
        if (currentBluetoothGatt == null) {
            Toast.makeText(this, "Non Connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        final BluetoothGattService service = currentBluetoothGatt.getService(DEVICE_UUID);
        if (service == null) {

            Toast.makeText(this, "UUID Introuvable", Toast.LENGTH_SHORT).show();
            return;
        }

        final BluetoothGattCharacteristic toggleLed = service.getCharacteristic(CHARACTERISTIC_TOGGLE_LED_UUID);
        toggleLed.setValue("1");
        currentBluetoothGatt.writeCharacteristic(toggleLed);
    }

    private void setUiMode(boolean isConnected) {
        if(isConnected){

            deviceArrayList.clear();
            deviceAdapter.notifyDataSetChanged();
            findViewById(R.id.listview).setVisibility(View.GONE);
            findViewById(R.id.scan_button).setVisibility(View.GONE);
            findViewById(R.id.connected_device_text).setVisibility(View.VISIBLE);
            ((TextView)(findViewById(R.id.connected_device_text))).setText("connecté à " + selectedDevice.getName());
            findViewById(R.id.disconnect_button).setVisibility(View.VISIBLE);
            findViewById(R.id.toggle_button).setVisibility(View.VISIBLE);

        } else {
            //Toast.makeText(this, R.string.no_connected_string, Toast.LENGTH_SHORT).show();
            findViewById(R.id.listview).setVisibility(View.VISIBLE);
            findViewById(R.id.scan_button).setVisibility(View.VISIBLE);
            findViewById(R.id.toggle_button).setVisibility(View.GONE);
            findViewById(R.id.disconnect_button).setVisibility(View.GONE);
            findViewById(R.id.connected_device_text).setVisibility(View.GONE);
        }

    }


    private void connectToCurrentDevice() {
        if (selectedDevice != null) {
            Toast.makeText(this, "Connexion en cours…", Toast.LENGTH_SHORT).show();
            currentBluetoothGatt = selectedDevice.connectGatt(this, false, gattCallback);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);
            runOnUiThread(() -> {
                Toast.makeText(ScanActivity.this, "Services discovered with success", Toast.LENGTH_SHORT).show();
                setUiMode(true);
            });
        }
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            runOnUiThread(() -> {
                switch (newState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        currentBluetoothGatt.discoverServices(); // start services
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        gatt.close();
                        setUiMode(false);
                        break;
                }

            });
        }
        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };
//////////////////

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
        } else {
            checkForLocationEnabled();
        }
    }


    private void discconnectFromCurrentDevice() {
        if(currentBluetoothGatt != null) {
            currentBluetoothGatt.disconnect();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkForLocationEnabled();
            } else {
                checkPermissions(); // force permission
            }
        }
    }


    private void checkForLocationEnabled() {
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            final boolean gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            final boolean network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!gps_enabled || !network_enabled) {
                new MaterialDialog.Builder(this)
                        .title(getString(R.string.no_identified_location))
                        .content(getString(R.string.need_for_enable_your_location))
                        .positiveText(getString(R.string.ok1))
                        .onPositive((dialog, which)->{
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION_CODE);
                        })
                        .onNegative((dialog, which)->{
                            Toast.makeText(this, R.string.Location_not_enabled, Toast.LENGTH_SHORT).show();
                        })
                        .show();
                //startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_ENABLED_LOCATION_CODE);
            } else {
                setupBLE();
            }
        } else {
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_ENABLED_LOCATION_CODE);
        }
    }


    private void setupBLE() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        if (bluetoothManager == null || !bluetoothAdapter.isEnabled()) { // bluetooth is off

            new MaterialDialog.Builder(this)
                    .title(getString(R.string.No_bluetooth_connection))
                    .content(getString(R.string.need_to_enable_bluetooth))
                    .positiveText(getString(R.string.ok2))
                    .onPositive((dialog, which)->{
                        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLE);
                    })
                    .onNegative((dialog, which)->{
                        Toast.makeText(this, R.string.Bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
                    })
                    .show();

                //Toast.makeText(this, "Non Connecté", Toast.LENGTH_SHORT).show();


            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLE);
        } else {
            scanNearbyDevices(); // start scanning by default
        }

    }


    private void scanNearbyDevices() {
        if (isScanning) {
            return;
        }

        isScanning = true;
        scanningHandler.postDelayed(scanDevicesRunnable, SCAN_DURATION_MS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // for recent version of android
            final ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            final List<ScanFilter> scanFilters = new ArrayList<>();

            // Filtre sur le scan
            // scanFilters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(DEVICE_UUID)).build()); // add service filters

            bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, settings, bleLollipopScanCallback);
        }
    }

    private final Runnable scanDevicesRunnable = () -> stopScan();


    private final ScanCallback bleLollipopScanCallback = new ScanCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bluetoothDevice = result.getDevice();
            // C'est ici que nous allons créer notre « device » et l'ajouter dans le RecyclerView (Datasource)
            // Et surtout notifier du changement

            if((!deviceArrayList.contains(bluetoothDevice))&&(bluetoothDevice.getName()!=null)) {
                deviceArrayList.add(bluetoothDevice);
                deviceAdapter.notifyDataSetChanged();
            }
        }
       // @Override
        /*public void onScanFailed(final int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(ScanActivity.this, getString(R.string.ble_scan_error, errorCode), Toast.LENGTH_SHORT).show();
        }*/
    };

    private void stopScan(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(bleLollipopScanCallback);
        }
        isScanning=false;
    }

    private void listClick(int position){
        final BluetoothDevice item = deviceAdapter.getItem(position);
        selectedDevice = item;
        LocalPreferences.getInstance(this).saveCurrentSelectedDevice(item.getName());
        connectToCurrentDevice();
    }

    private void scanActions(){
        checkPermissions();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);



        // ArrayList<Device> deviceArrayList = new ArrayList<Device>();
        deviceAdapter = new ScanAdapter(this, deviceArrayList);
        ListView rvDevices = findViewById(R.id.listview);

        rvDevices.setAdapter(deviceAdapter);
        rvDevices.setClickable(true);
        rvDevices.setOnItemClickListener((parent, view, position, id) -> listClick(position));

        /*   Device device1 = new Device("device1");
        Device device2 = new Device("device2");
        Device device3 = new Device("device3");
        Device device4 = new Device("device4");

        deviceArrayList.add(device1);
        deviceArrayList.add(device2);
        deviceArrayList.add(device3);
        deviceArrayList.add(device4);*/
        findViewById(R.id.toggle_button).setVisibility(View.GONE);
        findViewById(R.id.disconnect_button).setVisibility(View.GONE);
        findViewById(R.id.connected_device_text).setVisibility(View.GONE);
        //  findViewById(R.id.btn).setOnClickListener(v -> showDialog());
        findViewById(R.id.scan_button).setOnClickListener(v -> scanActions());
        findViewById(R.id.toggle_button).setOnClickListener(v -> toggleLed());
        findViewById(R.id.disconnect_button).setOnClickListener(v -> discconnectFromCurrentDevice());
    }


}


