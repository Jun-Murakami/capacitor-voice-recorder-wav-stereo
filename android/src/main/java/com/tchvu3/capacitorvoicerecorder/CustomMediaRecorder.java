package com.tchvu3.capacitorvoicerecorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.media.AudioDeviceInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class CustomMediaRecorder {

    private final Context context;
    private final AudioDeviceInfo usbMic;
    private AudioRecord audioRecord;
    private File outputFile;
    private CurrentRecordingStatus currentRecordingStatus = CurrentRecordingStatus.NONE;
    private Thread recordingThread;
    private boolean isRecording = false;

    private static final int SAMPLE_RATE = 44100;

    static {
        System.loadLibrary("native-lib");
    }

    private native boolean startOboeRecording(int deviceId);
    private native short[] getOboeRecordedData();
    private native void stopOboeRecording();
    private native boolean pauseOboeRecording();
    private native boolean resumeOboeRecording();

    public CustomMediaRecorder(Context context, AudioDeviceInfo usbMic) {
        this.context = context;
        this.usbMic = usbMic;
    }

    public void startRecording() throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!startOboeRecording(usbMic.getId())) {
                throw new IOException("Failed to start Oboe recording");
            }
        }
        isRecording = true;
        currentRecordingStatus = CurrentRecordingStatus.RECORDING;

        recordingThread = new Thread(this::writeAudioDataToFile, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outputFile);
            while (isRecording) {
                short[] data = getOboeRecordedData();
                if (data != null && data.length > 0) {
                    byte[] byteData = new byte[data.length * 2];
                    ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(data);
                    os.write(byteData);
                }
                try {
                    Thread.sleep(100); // Adjust this value as needed
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            Log.e("CustomMediaRecorder", "Error writing audio data to file", e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                Log.e("CustomMediaRecorder", "Error closing output stream", e);
            }
        }
    }

    public void stopRecording() {
        if (isRecording) {
            isRecording = false;
            stopOboeRecording();
            recordingThread = null;
            currentRecordingStatus = CurrentRecordingStatus.NONE;
            writeWavHeader();
        }
    }

    private void writeWavHeader() {
        try {
            FileInputStream fis = new FileInputStream(outputFile);
            byte[] audioData = new byte[(int) outputFile.length()];
            int bytesRead = fis.read(audioData);
            if (bytesRead < 0) {
                Log.e("CustomMediaRecorder", "Error reading audio data from file");
            } else if (bytesRead < audioData.length) {
                Log.w("CustomMediaRecorder", "Audio data size mismatch");
            }
            fis.close();
            byte[] header = createWavHeader(audioData.length);
            FileOutputStream os = new FileOutputStream(outputFile);
            os.write(header);
            os.write(audioData);
            os.close();
        } catch (IOException e) {
            Log.e("CustomMediaRecorder", "Error writing audio data to file", e);
        }
    }

    private byte[] createWavHeader(int audioLength) {
        int totalLength = audioLength + 36;
        byte[] header = new byte[44];
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put("RIFF".getBytes());
        buffer.putInt(totalLength);
        buffer.put("WAVE".getBytes());
        buffer.put("fmt ".getBytes());
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) 2);
        buffer.putInt(SAMPLE_RATE);
        buffer.putInt(SAMPLE_RATE * 2 * 2);
        buffer.putShort((short) 4);
        buffer.putShort((short) 16);
        buffer.put("data".getBytes());
        buffer.putInt(audioLength);

        return header;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public boolean pauseRecording() throws NotSupportedOsVersion {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw new NotSupportedOsVersion();
        }

        if (currentRecordingStatus == CurrentRecordingStatus.RECORDING) {
            if (pauseOboeRecording()) {
                isRecording = false;
                currentRecordingStatus = CurrentRecordingStatus.PAUSED;
                return true;
            }
        }
        return false;
    }

    public boolean resumeRecording() throws NotSupportedOsVersion {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw new NotSupportedOsVersion();
        }

        if (currentRecordingStatus == CurrentRecordingStatus.PAUSED) {
            if (resumeOboeRecording()) {
                isRecording = true;
                currentRecordingStatus = CurrentRecordingStatus.RECORDING;
                return true;
            }
        }
        return false;
    }

    public CurrentRecordingStatus getCurrentStatus() {
        return currentRecordingStatus;
    }

    public boolean deleteOutputFile() {
        return outputFile.delete();
    }

    public static boolean canPhoneCreateMediaRecorder(Context ignoredContext) {
        return true;
    }
}