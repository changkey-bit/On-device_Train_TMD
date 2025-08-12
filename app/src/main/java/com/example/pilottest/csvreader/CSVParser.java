package com.example.pilottest.csvreader;

import android.content.Context;

import com.example.pilottest.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CSVParser {

    public CSVParser() {
    }
    public List<Float> getMean(Context context) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(R.raw.data_mean);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String line = "";
        reader.readLine();
        List<Float> itemList = new ArrayList<>(12);

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");
            itemList.add(Float.parseFloat(tokens[0]));
        }

        reader.close();
        inputStream.close();

        return itemList;
    }
    public List<Float> getStd(Context context) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(R.raw.data_std);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String line = "";
        reader.readLine();
        List<Float> itemList = new ArrayList<>(12);

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");
            itemList.add(Float.parseFloat(tokens[0]));
        }

        reader.close();
        inputStream.close();

        return itemList;
    }
}

