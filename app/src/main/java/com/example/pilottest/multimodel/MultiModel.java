package com.example.pilottest.multimodel;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.pilottest.MainActivity;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.flex.FlexDelegate;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiModel {

    private String tflite_model = "Multi_CNN_Signature.tflite";

    private Interpreter interpreter;
    private MainActivity mainActivity;
    private Context context;  // Context를 저장할 필드

    private float[][][][] inputData;
    private float[][] labelData;

    int NUM_EPOCHS = 10;
    int BATCH_SIZE = 1;
    int SENSOR_NUM = 4;
    int WINDOW_SIZE = 300;
    int SENSOR_FEATURES = 3;
    int CLASS_NUM = 5;

    float threshold_loss = 1;

    private File ckptDir;
    private String ckptPrefix;

    public MultiModel(Context context) {
        // 객체 생성 시 모델 파일을 로드
        ByteBuffer modelBuffer = loadModelFile(context);
        this.context = context;

        // 체크포인트 디렉토리 (앱 내부)
        ckptDir = new File(context.getFilesDir(), "checkpoints");
        if (!ckptDir.exists()) ckptDir.mkdirs();
        // TensorFlow 스타일 프리픽스(확장자 없이) 권장
        ckptPrefix = new File(ckptDir, "ckpt").getAbsolutePath();

        FlexDelegate flexDelegate = new FlexDelegate();
        Interpreter.Options options = new Interpreter.Options();
        options.addDelegate(flexDelegate);

        interpreter = new Interpreter(modelBuffer, options);
        Log.e("Model", "Load Model!");
        Log.e("CKPT", "ckptPrefix=" + ckptPrefix);

        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    public void trainModel(float[][][][] inputData, float[][] labelData,
                           float[][][][] validX, float[][] validY) {

        int numTrain = inputData[0].length;           // 샘플 수
        int numTrainBatches = numTrain / BATCH_SIZE;  // BATCH_SIZE=1 가정

        Log.e("BATCH", "NUM Batches: " + numTrainBatches);

        List<FloatBuffer> trainInputBatches = new ArrayList<>(numTrainBatches);
        List<FloatBuffer> trainLabelBatches = new ArrayList<>(numTrainBatches);

        for (int i = 0; i < numTrainBatches; i++) {
            ByteBuffer train_byteBuffer = ByteBuffer.allocateDirect(
                    SENSOR_NUM * BATCH_SIZE * WINDOW_SIZE * SENSOR_FEATURES * Float.BYTES
            ).order(ByteOrder.nativeOrder());
            FloatBuffer inputBuffer = train_byteBuffer.asFloatBuffer();

            ByteBuffer label_byteBuffer = ByteBuffer.allocateDirect(
                    BATCH_SIZE * CLASS_NUM * Float.BYTES
            ).order(ByteOrder.nativeOrder());
            FloatBuffer labelBuffer = label_byteBuffer.asFloatBuffer();

            for (int sensor = 0; sensor < SENSOR_NUM; sensor++) {
                for (int batch = 0; batch < BATCH_SIZE; batch++) {
                    int sampleIndex = i * BATCH_SIZE + batch;
                    for (int window = 0; window < WINDOW_SIZE; window++) {
                        for (int feature = 0; feature < SENSOR_FEATURES; feature++) {
                            inputBuffer.put(inputData[sensor][sampleIndex][window][feature]);
                        }
                    }
                }
            }
            inputBuffer.rewind();
            trainInputBatches.add(inputBuffer);

            for (int batch = 0; batch < BATCH_SIZE; batch++) {
                int sampleIndex = i * BATCH_SIZE + batch;
                for (int classIndex = 0; classIndex < CLASS_NUM; classIndex++) {
                    labelBuffer.put(labelData[sampleIndex][classIndex]);
                }
            }
            labelBuffer.rewind();
            trainLabelBatches.add(labelBuffer);
        }

        float minLoss = Float.MAX_VALUE;
        boolean stopTrain = false;

        for (int epoch = 0; epoch < NUM_EPOCHS; ++epoch) {
            if (stopTrain) {
                Log.e("EarlyStop", "EarlyStopping!");
                break;
            }
            for (int batchIdx = 0; batchIdx < numTrainBatches; ++batchIdx) {
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("x", trainInputBatches.get(batchIdx));
                inputs.put("y", trainLabelBatches.get(batchIdx));

                Map<String, Object> outputs = new HashMap<>();
                FloatBuffer loss = FloatBuffer.allocate(1);
                outputs.put("loss", loss);

                try {
                    interpreter.runSignature(inputs, outputs, "train");
                } catch (Exception e) {
                    Log.e("Model", "Error running signature: " + e.getMessage(), e);
                }
            }

            float validationLoss = validModel(validX, validY);
            Log.e("val_loss", "Validation Loss: "+validationLoss);

            if (minLoss > threshold_loss) {
                if (validationLoss < minLoss) {
                    minLoss = validationLoss;
                    try {
                        Map<String, Object> in = new HashMap<>();
                        in.put("checkpoint_path", ckptPrefix); // ← 확장자 없이 prefix 사용
                        Map<String, Object> out = new HashMap<>();
                        interpreter.runSignature(in, out, "save");
                        Log.e("Save", "Checkpoint saved at prefix: " + ckptPrefix);
                    } catch (Exception e) {
                        Log.e("Save", "Failed to save checkpoint: " + e.getMessage(), e);
                    }
                }
            } else if (minLoss < threshold_loss) {
                Log.e("Save", "Validation loss did not improve, model not saved.");
                stopTrain = true;
            }
        }
    }
    public float validModel(float[][][][] inputData, float[][] labelData) {
        int numValid = inputData[0].length;
        int numValidBatches = numValid / BATCH_SIZE;

        Log.e("BATCH", "NUM Valid Batches: " + numValidBatches);

        List<FloatBuffer> validInputBatches = new ArrayList<>(numValidBatches);
        List<FloatBuffer> validLabelBatches = new ArrayList<>(numValidBatches);

        for (int i = 0; i < numValidBatches; i++) {
            ByteBuffer valid_byteBuffer = ByteBuffer.allocateDirect(
                    SENSOR_NUM * BATCH_SIZE * WINDOW_SIZE * SENSOR_FEATURES * Float.BYTES
            ).order(ByteOrder.nativeOrder());
            FloatBuffer inputBuffer = valid_byteBuffer.asFloatBuffer();

            ByteBuffer label_byteBuffer = ByteBuffer.allocateDirect(
                    BATCH_SIZE * CLASS_NUM * Float.BYTES
            ).order(ByteOrder.nativeOrder());
            FloatBuffer labelBuffer = label_byteBuffer.asFloatBuffer();

            for (int sensor = 0; sensor < SENSOR_NUM; sensor++) {
                for (int batch = 0; batch < BATCH_SIZE; batch++) {
                    int sampleIndex = i * BATCH_SIZE + batch;
                    for (int window = 0; window < WINDOW_SIZE; window++) {
                        for (int feature = 0; feature < SENSOR_FEATURES; feature++) {
                            inputBuffer.put(inputData[sensor][sampleIndex][window][feature]);
                        }
                    }
                }
            }
            inputBuffer.rewind();
            validInputBatches.add(inputBuffer);

            for (int batch = 0; batch < BATCH_SIZE; batch++) {
                int sampleIndex = i * BATCH_SIZE + batch;
                for (int classIndex = 0; classIndex < CLASS_NUM; classIndex++) {
                    labelBuffer.put(labelData[sampleIndex][classIndex]);
                }
            }
            labelBuffer.rewind();
            validLabelBatches.add(labelBuffer);
        }

        float sumLoss = 0f;
        for (int batchIdx = 0; batchIdx < numValidBatches; ++batchIdx) {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("x", validInputBatches.get(batchIdx));
            inputs.put("y", validLabelBatches.get(batchIdx));

            Map<String, Object> outputs = new HashMap<>();
            FloatBuffer loss = FloatBuffer.allocate(1);
            outputs.put("val_loss", loss);

            try {
                interpreter.runSignature(inputs, outputs, "valid");
                sumLoss += loss.get(0);
            } catch (Exception e) {
                Log.e("Model", "Error running signature: " + e.getMessage(), e);
            }
        }
        return sumLoss / Math.max(1, numValidBatches); // ← 실제 배치 수로 평균
    }

    public void restoreModel() {
        try {
            // Load checkpoint file.
            File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "checkpoint.ckpt");
            Map<String, Object> loadFileName = new HashMap<>();

            loadFileName.put("checkpoint_path", outputFile.getAbsolutePath());

            Log.e("Restore", "checkpoint_path: " + outputFile.getAbsolutePath());

            Map<String, Object> checkpoint_path = new HashMap<>();
            interpreter.runSignature(loadFileName, checkpoint_path, "restore");

            Log.e("Restore", "Checkpoint load!");
        }
        catch (Exception e) {
            Log.e("Restore", "Failed to load checkpoint: " + e.getMessage(), e);
        }
    }

    public float[][] inferModel(float[][][][] inputData) {
        int totalSamples = inputData[0].length;         // e.g. 1
        int numBatches  = totalSamples / BATCH_SIZE;    // e.g. 1

        float[][] probs = new float[numBatches][CLASS_NUM];

        try {
            // Prepare all batches
            List<FloatBuffer> inputBuffers = new ArrayList<>(numBatches);
            for (int b = 0; b < numBatches; b++) {
                ByteBuffer buf = ByteBuffer
                        .allocateDirect(SENSOR_NUM * BATCH_SIZE * WINDOW_SIZE * SENSOR_FEATURES * Float.BYTES)
                        .order(ByteOrder.nativeOrder());
                FloatBuffer fb = buf.asFloatBuffer();

                int sampleIdx = b * BATCH_SIZE;
                for (int sensor = 0; sensor < SENSOR_NUM; sensor++) {
                    for (int w = 0; w < WINDOW_SIZE; w++) {
                        for (int f = 0; f < SENSOR_FEATURES; f++) {
                            fb.put(inputData[sensor][sampleIdx][w][f]);
                        }
                    }
                }
                fb.rewind();
                inputBuffers.add(fb);
            }

            // Inference per batch
            for (int b = 0; b < numBatches; b++) {
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("x", inputBuffers.get(b));

                ByteBuffer outBuf = ByteBuffer
                        .allocateDirect(BATCH_SIZE * CLASS_NUM * Float.BYTES)
                        .order(ByteOrder.nativeOrder());
                FloatBuffer outputFb = outBuf.asFloatBuffer();
                Map<String, Object> outputs = Collections.singletonMap("output", outputFb);

                interpreter.runSignature(inputs, outputs, "infer");

                outputFb.rewind();
                for (int i = 0; i < CLASS_NUM; i++) {
                    probs[b][i] = outputFb.get();
                }
            }
        } catch (Exception e) {
            Log.e("Model", "Inference failed: " + e.getMessage(), e);
        }

        return probs;
    }
    private int getArgMax(FloatBuffer buffer, int length) {
        float maxVal = -Float.MAX_VALUE;
        int argMax = -1;
        for (int i = 0; i < length; i++) {
            float val = buffer.get(i);
            if (val > maxVal) {
                maxVal = val;
                argMax = i;
            }
        }
        return argMax;
    }

    // 모델 로드
    private ByteBuffer loadModelFile(Context context) {
        try {
            return FileUtil.loadMappedFile(context, tflite_model);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model file", e);
        }
    }
}