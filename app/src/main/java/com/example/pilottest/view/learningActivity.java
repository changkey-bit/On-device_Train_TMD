package com.example.pilottest.view;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pilottest.R;
import com.example.pilottest.csvreader.CSVParser;
import com.example.pilottest.multimodel.MultiModel;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class learningActivity extends AppCompatActivity {

    private MultiModel multiModel;
    private TextView taskView, timeView;
    private ProgressBar loadingSpinner;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private long trainStartNano;
    private Runnable timerRunnable;

    private static final String ROOT_FOLDER = "HCILabData";
    private static final String[] MODES = {"walking","still","bus","metro","car"};
    private static final int WINDOW_SIZE = 300;
    private static final int SENSOR_NUM  = 4;
    private static final int FEATURES    = 3;
    private static final int CLASS_NUM   = 5;

    private CSVParser csvParser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning);

        taskView = findViewById(R.id.TaskView);
        timeView = findViewById(R.id.TimeView);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        loadingSpinner.setVisibility(View.GONE);

        multiModel = new MultiModel(this);
        csvParser  = new CSVParser();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                double elapsedSec = (System.nanoTime() - trainStartNano) / 1e9;
                timeView.setText(String.format(Locale.KOREA, "%.2f sec", elapsedSec));
                uiHandler.postDelayed(this, 100);
            }
        };
    }

    public void Train(View view) {
        loadingSpinner.setVisibility(View.VISIBLE);
        taskView.setText("Training…");
        trainStartNano = System.nanoTime();
        uiHandler.post(timerRunnable);

        new Thread(() -> {
            List<Float> means, stds;
            try {
                means = csvParser.getMean(this);
                stds  = csvParser.getStd(this);
            } catch (IOException e) {
                Log.e("learningActivity","Error loading stats",e);
                runOnUiThread(() -> {
                    uiHandler.removeCallbacks(timerRunnable);
                    loadingSpinner.setVisibility(View.GONE);
                    taskView.setText("Failed to load stats");
                });
                return;
            }

            List<float[][]> trainListRaw = new ArrayList<>();
            List<Integer>   trainLabelIdx = new ArrayList<>();
            List<float[][]> validListRaw = new ArrayList<>();
            List<Integer>   validLabelIdx = new ArrayList<>();

            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File rootDir  = new File(downloads, ROOT_FOLDER);

            for (int classIdx = 0; classIdx < MODES.length; classIdx++) {
                String mode = MODES[classIdx];
                File modeDir = new File(rootDir, mode);
                File[] csvFiles = modeDir.listFiles((d,n) -> n.toLowerCase(Locale.ROOT).endsWith(".csv"));
                if (csvFiles == null || csvFiles.length < 2) {
                    Log.e("learningActivity", "Need 2 CSVs in " + modeDir.getAbsolutePath());
                    continue;
                }

                Arrays.sort(csvFiles, Comparator.comparing(File::getName));
                File trainFile = csvFiles[0];
                File validFile = csvFiles[1];

                // CSV -> windows
                List<float[][]> tWins = readCsvToWindows(trainFile, WINDOW_SIZE);
                List<float[][]> vWins = readCsvToWindows(validFile, WINDOW_SIZE);

                // 누적
                for (float[][] w : tWins) { trainListRaw.add(w); trainLabelIdx.add(classIdx); }
                for (float[][] w : vWins) { validListRaw.add(w); validLabelIdx.add(classIdx); }
            }

            int trainSamples = trainListRaw.size();
            int validSamples = validListRaw.size();
            Log.e("learningActivity", "trainSamples=" + trainSamples + ", validSamples=" + validSamples);

            // 4D 텐서로 변환 + 정규화
            float[][][][] trainX = new float[SENSOR_NUM][trainSamples][WINDOW_SIZE][FEATURES];
            float[][]      trainY = new float[trainSamples][CLASS_NUM];
            for (int m = 0; m < trainSamples; m++) {
                float[][] raw = trainListRaw.get(m); // (300,12)
                for (int w = 0; w < WINDOW_SIZE; w++) {
                    for (int s = 0; s < SENSOR_NUM; s++) {
                        for (int f = 0; f < FEATURES; f++) {
                            int idx = s * FEATURES + f; // 0..11
                            float val = raw[w][idx];
                            trainX[s][m][w][f] = (val - means.get(idx)) / stds.get(idx);
                        }
                    }
                }
                trainY[m][trainLabelIdx.get(m)] = 1f;  // ← 모드 인덱스로 one-hot
            }

            float[][][][] validX = new float[SENSOR_NUM][validSamples][WINDOW_SIZE][FEATURES];
            float[][]      validY = new float[validSamples][CLASS_NUM];
            for (int m = 0; m < validSamples; m++) {
                float[][] raw = validListRaw.get(m);
                for (int w = 0; w < WINDOW_SIZE; w++) {
                    for (int s = 0; s < SENSOR_NUM; s++) {
                        for (int f = 0; f < FEATURES; f++) {
                            int idx = s * FEATURES + f;
                            float val = raw[w][idx];
                            validX[s][m][w][f] = (val - means.get(idx)) / stds.get(idx);
                        }
                    }
                }
                validY[m][validLabelIdx.get(m)] = 1f;
            }

            // 학습
            multiModel.trainModel(trainX, trainY, validX, validY);

            runOnUiThread(() -> {
                uiHandler.removeCallbacks(timerRunnable);
                loadingSpinner.setVisibility(View.GONE);
                taskView.setText("Train Completed");
                double totalSec = (System.nanoTime() - trainStartNano) / 1e9;
                timeView.setText(String.format(Locale.KOREA, "%.2f sec", totalSec));
            });
        }).start();
    }

    private List<float[][]> readCsvToWindows(File csvFile, int windowSize) {
        List<float[]> rows = new ArrayList<>();
        try (FileReader fr = new FileReader(csvFile);
             CSVReader reader = new CSVReader(fr)) {

            String[] line;
            boolean skipHeader = true;
            while ((line = reader.readNext()) != null) {
                if (skipHeader) { // 첫 줄 헤더 스킵
                    skipHeader = false;
                    // 헤더가 아닌 경우(숫자로 시작)엔 다시 사용
                    try { Float.parseFloat(line[0]); }
                    catch (Exception e) { continue; }
                }
                float[] row = new float[line.length];
                for (int i = 0; i < line.length; i++) row[i] = Float.parseFloat(line[i]);
                rows.add(row);
            }
        } catch (Exception e) {
            Log.e("learningActivity", "readCsvToWindows error: " + csvFile.getAbsolutePath(), e);
        }

        // 300행 단위로 분할 (겹치기 없음)
        List<float[][]> windows = new ArrayList<>();
        int total = rows.size();
        for (int start = 0; start + windowSize <= total; start += windowSize) {
            float[][] win = new float[windowSize][12]; // 4 sensors * 3 features = 12
            for (int w = 0; w < windowSize; w++) {
                System.arraycopy(rows.get(start + w), 0, win[w], 0, 12);
            }
            windows.add(win);
        }
        return windows;
    }
}
