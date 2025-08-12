package com.example.pilottest.csvreader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.InputStreamReader;

public class CSVRead {

    private static final int WINDOW_SIZE = 300;
    private static final int SENSOR_NUM = 4;
    private static final int SENSOR_FEATURES = 3;
    private static final int CLASS_NUM = 5;

    public static float[][][][] readInputCSV(Context context, String assetFileName, int TOTAL_ROWS) {

        float[][][][] resultArray = new float[SENSOR_NUM][TOTAL_ROWS][WINDOW_SIZE][SENSOR_FEATURES];
        AssetManager assetManager = context.getAssets();

        try (CSVReader reader = new CSVReader(new InputStreamReader(assetManager.open(assetFileName)))) {
            String[] line;
            int row = 0;
            int col = 0;

            while ((line = reader.readNext()) != null) {

                if (line.length != SENSOR_NUM * SENSOR_FEATURES) {
                    Log.e("CSV", "Unexpected line length: " + line.length);
                    continue; // Skip this line if unexpected length
                }

                for (int sensor = 0; sensor < SENSOR_NUM; sensor++) {
                    for (int feature = 0; feature < SENSOR_FEATURES; feature++) {
                        int index = sensor * SENSOR_FEATURES + feature;
                        if (index < line.length) {
                            try {
                                resultArray[sensor][row][col][feature] = Float.parseFloat(line[index]);
                            } catch (NumberFormatException e) {
                                Log.e("CSV", "Error parsing float value: " + line[index]);
                            }
                        }
                    }
                }

                col++;
                if (col == WINDOW_SIZE) {
                    col = 0;
                    row++;
                }
                if (row == TOTAL_ROWS) {
                    break;
                }
            }
            Log.e("CSV", "Complete Read X_CSV!");

        } catch (IOException | CsvValidationException e) {
            Log.e("CSV", "Error occurred: " + e.getMessage());
        }

        return resultArray;
    }

    public static float[][] readLabelCSV(Context context, String assetFileName, int TOTAL_ROWS) {

        float[][] resultArray = new float[TOTAL_ROWS][CLASS_NUM];

        AssetManager assetManager = context.getAssets();

        try (CSVReader reader = new CSVReader(new InputStreamReader(assetManager.open(assetFileName)))) {
            String[] line;
            int row = 0;

            while ((line = reader.readNext()) != null) {
                if (line.length != CLASS_NUM) {
                    Log.e("CSV", "Unexpected line length: " + line.length);
                    continue;
                }

                for (int i = 0; i < CLASS_NUM; i++) {
                    try {
                        resultArray[row][i] = Float.parseFloat(line[i]);
                    } catch (NumberFormatException e) {
                        Log.e("CSV", "Error parsing float value: " + line[i]);
                        resultArray[row][i] = 0.0f;
                    }
                }
                row++;

                if (row == TOTAL_ROWS) {
                    break;
                }
            }
            Log.e("CSV", "Complete Read Y_CSV!");

        } catch (IOException | CsvValidationException e) {
            Log.e("CSV", "Error occurred: " + e.getMessage());
        }
        return resultArray;
    }
}
