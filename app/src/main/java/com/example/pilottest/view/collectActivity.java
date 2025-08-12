package com.example.pilottest.view;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pilottest.R;
import com.example.pilottest.csvreader.CSVParser;
import com.example.pilottest.csvwrite.CSCWrite;
import com.example.pilottest.multimodel.MultiModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class collectActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    String[] modeItems = {"걷기", "정지", "버스", "지하철", "자동차", "모드 선택"};
    String[] E_modeItems = {"walking", "still", "bus", "metro", "car", ""};
    String[] positionItems = {"손", "가방", "주머니", "기타", "핸드폰 위치 선택"};
    String[] E_positionItems = {"Hand", "Bag", "Pocket", "Etc", ""};

    long sTime, eTime;

    String Mode = null, Position = null;
    String m = null, p = null;
    Button startBtn;
    private TextView countdownView;
    private Sensor gravity, linearAccel, gyro, magnetic;
    //baro, proxi, light, step_sensor, rotation, accOrientation, magOrientation, accel, gameRotation
    private Handler handler;
    private boolean isSensorListening = false;
    private boolean continueSensor = true;
    public List<String[]> sensorData = new ArrayList<String[]>();
    public List<String[]> predictionData = new ArrayList<String[]>();
    public float gra_x, gra_y, gra_z,
            linearAccel_x, linearAccel_y, linearAccel_z,
            gyro_x, gyro_y, gyro_z,
            mag_x, mag_y, mag_z;
    private static final int SENSOR_UPDATE_INTERVAL = 1000 / 60;
    private static final int MAX_SENSOR_DATA_COUNT = 300;
    private int sensorDataCount = 0;
    private CountDownTimer countDownTimer;
    private Calendar calendar;
    public double startTime;
    private MultiModel cls;
    private List<Float> meanList = new ArrayList<>(12);
    private List<Float> stdList = new ArrayList<>(12);
    int num_sensor = 4, batchSize = 1, inputSize = 300, inputChannels = 3, classes = 5;

    int result_index=0;

    public float gra_sensorData[][][] = new float[batchSize][inputSize][inputChannels];
    public float acc_sensorData[][][] = new float[batchSize][inputSize][inputChannels];
    public float gyro_sensorData[][][] = new float[batchSize][inputSize][inputChannels];
    public float mag_sensorData[][][] = new float[batchSize][inputSize][inputChannels];
    int year, month, date, hour, min, second;
    static final int POPUP_REQUEST = 1;

    float [][] probMatrix = new float[390][5];
    int count = 0;

    private float[][] output;
    private float[][] dummyLabels = new float[1][5];

    private TextView textResult;
    private ImageView imageMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect);

        handler = new Handler(Looper.getMainLooper());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        countdownView = findViewById(R.id.countdownView);

        calendar = new GregorianCalendar(Locale.KOREA);
        startBtn = findViewById(R.id.startBtn);

        textResult = findViewById(R.id.textResult);
        imageMode  = findViewById(R.id.imageMode);

        // 모델 초기화
        cls = new MultiModel(this);
        CSVParser csvParser = new CSVParser();

        Log.i("Model", "Load Model Success!");

        try {
            meanList = csvParser.getMean(this);
            stdList = csvParser.getStd(this);
            Log.i("CSV_Parser", "Load CSV Success!");
        } catch (IOException e) {
            Log.e("CSV_Parser", "Load CSV Fail..");
        }

        Spinner spinnerMode = findViewById(R.id.spinnerMode);
        Spinner spinnerPosition = findViewById(R.id.spinnerPosition);

        ArrayAdapter<String> adapter_mode = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,            // 닫힌 뷰용
                modeItems
        );
        adapter_mode.setDropDownViewResource(
                R.layout.spinner_dropdown_item     // 드롭다운 뷰용
        );
        spinnerMode.setAdapter(adapter_mode);

        ArrayAdapter<String> adapter_position = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,            // 닫힌 뷰용
                positionItems
        );
        adapter_position.setDropDownViewResource(
                R.layout.spinner_dropdown_item     // 드롭다운 뷰용
        );
        spinnerPosition.setAdapter(adapter_position);

        final int lastModeIndex = modeItems.length - 1;
        final int lastPosIndex  = positionItems.length - 1;

        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int idx, long id) {
                if (idx == lastModeIndex) { Mode = null; m = null; }  // ← 플레이스홀더면 비움
                else { Mode = E_modeItems[idx]; m = modeItems[idx]; }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerPosition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int idx, long id) {
                if (idx == lastPosIndex) { Position = null; p = null; } // ← 플레이스홀더면 비움
                else { Position = E_positionItems[idx]; p = positionItems[idx]; }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMode.setSelection(lastModeIndex, false);
        spinnerPosition.setSelection(lastPosIndex, false);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Mode != "" && Position != ""){
                    startSensor(v);
                    year = calendar.get(Calendar.YEAR);
                    month = (calendar.get(Calendar.MONTH) + 1);
                    date = calendar.get(Calendar.DATE);
                    hour = calendar.get(Calendar.HOUR_OF_DAY);
                    min = calendar.get(Calendar.MINUTE);
                    second = calendar.get(Calendar.SECOND);
                    Log.e("Mode", "" + Mode);
                    Log.e("Position", "" + Position);
                } else {
                    Toast.makeText(getApplicationContext(), "모드와 포지션을 선택해야 합니다.", Toast.LENGTH_LONG).show();
                }
            }
        });

        if (sensorManager != null) {
            gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        //다이얼로그 밖의 화면은 흐리게 만들어줌
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);
    }

    public void startSensor(View view) {

        if (!isSensorListening) {
            registerSensor();

            countDownTimer = new CountDownTimer(5000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    countdownView.setText((millisUntilFinished / 1000) + "초 후 측정됩니다.");
                }
                @Override
                public void onFinish() {
                    countdownView.setText("측정 중 입니다!");
                    handler.postDelayed(sensorUpdateRunnable, SENSOR_UPDATE_INTERVAL);
                    sTime = System.currentTimeMillis();
                    startBtn.setText("STOP");
                }
            }.start();
            startBtn.setText("READY");
        }
        isSensorListening = !isSensorListening;
    }
    public void registerSensor() {

        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, linearAccel, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregisterSensor() {
        sensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gra_x = event.values[0];
            gra_y = event.values[1];
            gra_z = event.values[2];
        }
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            linearAccel_x = event.values[0];
            linearAccel_y = event.values[1];
            linearAccel_z = event.values[2];
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyro_x = event.values[0];
            gyro_y = event.values[1];
            gyro_z = event.values[2];
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mag_x = event.values[0];
            mag_y = event.values[1];
            mag_z = event.values[2];
        }
    }
    private final Runnable sensorUpdateRunnable = new Runnable() {
        @Override
        public void run() {

            addData();
            if (continueSensor == true) {
                handler.postDelayed(sensorUpdateRunnable, SENSOR_UPDATE_INTERVAL);
            } else {
                stopSensorUpdates();
                modelOutput();
                openPopup();
            }
        }
    };

    private void stopSensorUpdates() {
        handler.removeCallbacks(sensorUpdateRunnable);
        startBtn.setText("START");
        countdownView.setText("START 버튼을 눌러 주세요!");
        unregisterSensor();
        vibrate();
    }
    private void resetVariables() {
        isSensorListening = false;
        sensorDataCount = 0;
        sensorData.clear();
        predictionData.clear();
        countdownView.setText("측정 완료!");
        startTime = System.currentTimeMillis();
        continueSensor = true;
    }

    private void addData() {
        if (sensorDataCount < MAX_SENSOR_DATA_COUNT) {
            addSensorData();
            sensorData.add(new String[] {"" + gra_x, "" + gra_y, "" + gra_z, "" + linearAccel_x, "" + linearAccel_y, "" + linearAccel_z, "" +
                    gyro_x, "" + gyro_y, "" + gyro_z, ""+
                    mag_x, "" + mag_y, "" + mag_z});
            sensorDataCount++;

            if (sensorDataCount == MAX_SENSOR_DATA_COUNT) {
                continueSensor = false;
            }
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(500); // 0.5초 동안 진동
        }
    }

    private float normalizeSensor(float sensor, int index) {
        float normSensor = 0;
        normSensor = (sensor - meanList.get(index))/stdList.get(index);
        return normSensor;
    }

    public void modelOutput() {
        // 센서별 [1][300][3] 데이터를 B 프로젝트 포맷인 [4][1][300][3]로 묶기
        float[][][][] input_list = new float[][][][] {
                gra_sensorData,
                acc_sensorData,
                gyro_sensorData,
                mag_sensorData
        };

        // inferModel 호출 → [numBatches][CLASS_NUM] 행렬 반환
        float[][] probs = cls.inferModel(input_list);

        // 첫 번째(유일한) 배치 결과에서 최대값 인덱스 추출
        float[] firstProbs = probs[0];

        // argmax 직접 구현 (float[] 전용)
        int index = 0;
        for (int i = 1; i < firstProbs.length; i++) {
            if (firstProbs[i] > firstProbs[index]) {
                index = i;
            }
        }

        // 결과 텍스트 갱신
        textResult.setText(modeItems[index]);

        // 결과 이미지 갱신
        int[] drawables = {
                R.drawable.walk,
                R.drawable.still,
                R.drawable.bus,
                R.drawable.subway,
                R.drawable.car
        };
        imageMode.setImageResource(drawables[index]);

        // 로그
        eTime = System.currentTimeMillis();
        Log.e("InferenceTime", "" + (eTime - sTime));
    }

    private void addSensorData() {

        // 전체 센서 데이터 [1][300][12] Shape
        gra_sensorData[0][sensorDataCount][0] = normalizeSensor(gra_x, 0);
        gra_sensorData[0][sensorDataCount][1] = normalizeSensor(gra_y, 1);
        gra_sensorData[0][sensorDataCount][2] = normalizeSensor(gra_z, 2);

        acc_sensorData[0][sensorDataCount][0] = normalizeSensor(linearAccel_x, 3);
        acc_sensorData[0][sensorDataCount][1] = normalizeSensor(linearAccel_y, 4);
        acc_sensorData[0][sensorDataCount][2] = normalizeSensor(linearAccel_z, 5);

        gyro_sensorData[0][sensorDataCount][0] = normalizeSensor(gyro_x, 6);
        gyro_sensorData[0][sensorDataCount][1] = normalizeSensor(gyro_y, 7);
        gyro_sensorData[0][sensorDataCount][2] = normalizeSensor(gyro_z, 8);

        mag_sensorData[0][sensorDataCount][0] = normalizeSensor(mag_x, 9);
        mag_sensorData[0][sensorDataCount][1] = normalizeSensor(mag_y, 10);
        mag_sensorData[0][sensorDataCount][2] = normalizeSensor(mag_z, 11);
    }
    private void saveCSV() {
        //Save sensor data
        String sensorFile = String.format(
                "%d_%d_%d_%d_%d_%d_%s_%s_SensorData.csv",
                year, month, date, hour, min, second, Mode, Position
        );
        CSCWrite.writeSensorToCSV(this, sensorData, sensorFile, Mode);

        // prediction data
        predictionData.clear();
        predictionData.add(new String[]{
                Mode,
                Position,
                E_modeItems[result_index]
        });
        String predFile = String.format(
                "%d_%d_%d_%d_%d_%d_%s_%s_SensorData_Prediction.csv",
                year, month, date, hour, min, second, Mode, Position
        );
        CSCWrite.writePredictionToCSV(this, predictionData, predFile, Mode);
    }

    private void openPopup() {
        Intent intent = new Intent(collectActivity.this, PopUp_warning.class);
        intent.putExtra("Mode", m);
        intent.putExtra("Position", p);
        startActivityForResult(intent, POPUP_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == POPUP_REQUEST && resultCode == RESULT_OK) {
            int result = data.getIntExtra("result", 0);
            if (result == 1) {
                saveCSV();
                // probMatrix → CSV
                String probFile = "prob.csv";
                CSCWrite.probToCsv(this, probMatrix, probFile);
            }
            resetVariables();
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
