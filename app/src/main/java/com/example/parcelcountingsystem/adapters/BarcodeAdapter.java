package com.example.parcelcountingsystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.parcelcountingsystem.R;

import java.util.ArrayList;

public class BarcodeAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<String> barcodes;

    public BarcodeAdapter(Context context, ArrayList<String> barcodes) {
        this.context = context;
        this.barcodes = barcodes;
    }

    @Override
    public int getCount() {
        return barcodes.size();
    }

    @Override
    public Object getItem(int position) {
        return barcodes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_barcode, parent, false);
            holder = new ViewHolder();
            holder.barcodeText = convertView.findViewById(R.id.barcodeText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Set the barcode text
        String barcode = barcodes.get(position);
        holder.barcodeText.setText(barcode);

        return convertView;
    }

    static class ViewHolder {
        TextView barcodeText;
    }
}

