package com.applifinal.blepi.ui.scan.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.applifinal.blepi.R;

import java.util.ArrayList;

public class ScanAdapter extends ArrayAdapter<BluetoothDevice>{


    public ScanAdapter(Context context, ArrayList<BluetoothDevice> maList){
        super(context, 0, maList);
    }

    @NonNull
    @Override
    public View getView ( int position, @Nullable View convertView, @NonNull ViewGroup parent){
        BluetoothDevice monElement = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        TextView tvTitle = convertView.findViewById(R.id.iddelitem);
        tvTitle.setText(monElement.getName());

        return convertView;

    }

}


