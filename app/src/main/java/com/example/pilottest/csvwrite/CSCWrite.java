package com.example.pilottest.csvwrite;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSCWrite {

    private static final String ROOT_FOLDER = "HCILabData";
    private static final Map<String, String> MODE_FOLDERS = createModeFolderMap();

    private static Map<String, String> createModeFolderMap() {
        Map<String, String> map = new HashMap<>();
        map.put("still", "Still");
        map.put("walking", "Walking");
        map.put("bus", "Bus");
        map.put("metro", "Metro");
        map.put("car", "Car");
        return map;
    }

    private static File getOutputFile(Context ctx, String fileName, String mode) {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File rootDir = new File(downloads, ROOT_FOLDER);
        String subfolder = MODE_FOLDERS.getOrDefault(mode, "");
        File modeDir = subfolder.isEmpty() ? rootDir : new File(rootDir, subfolder);

        if (!modeDir.exists()) {
            modeDir.mkdirs();
        }
        return new File(modeDir, fileName);
    }

    public static void writeSensorToCSV(Context ctx, List<String[]> rows, String fileName, String mode) {
        File file = getOutputFile(ctx, fileName, mode);
        String header = "GraX,GraY,GraZ,LinearAccX,LinearAccY,LinearAccZ,GyroX,GyroY,GyroZ,MagX,MagY,MagZ,";
        writeCsv(file, header, rows);
    }

    public static void writePredictionToCSV(Context ctx, List<String[]> rows, String fileName, String mode) {
        File file = getOutputFile(ctx, fileName, mode);
        String header = "Groundtruth Mode,Groundtruth Position,Prediction Mode,Prediction Position,"
                + "powerChar,manualChar,walking,still,bus,metro,car";

        writeCsv(file, header, rows);
    }

    public static void probToCsv(Context ctx, float[][] matrix, String fileName) {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloads, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            for (float[] row : matrix) {
                for (int j = 0; j < row.length; j++) {
                    writer.append(Float.toString(row[j]));
                    if (j < row.length - 1) writer.append(',');
                }
                writer.append('\n');
            }
        } catch (IOException e) {
            Log.e("CsvWrite", "Failed to write prob CSV", e);
        }
    }

    // 공통 CSV 작성 메서드
    private static void writeCsv(File file, String header, List<String[]> rows) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.append(header).append('\n');
            for (String[] row : rows) {
                writer.append(TextUtils.join(",", row)).append('\n');
            }
        } catch (IOException e) {
            Log.e("CsvWrite", "Failed to write CSV to " + file.getAbsolutePath(), e);
        }
    }
}
