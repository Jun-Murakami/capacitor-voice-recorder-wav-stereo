package com.tchvu3.capacitorvoicerecorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CustomMediaRecorder {

    private final Context context;
    private AudioRecord audioRecord;
    private File outputFile;
    private CurrentRecordingStatus currentRecordingStatus = CurrentRecordingStatus.NONE;
    private Thread recordingThread;
    private boolean isRecording = false;

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public CustomMediaRecorder(Context context) {
        this.context = context;
    }

    private void initializeAudioRecord() throws IOException {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
        setRecorderOutputFile();
    }

    private void setRecorderOutputFile() throws IOException {
        File outputDir = context.getCacheDir();
        outputFile = File.createTempFile("voice_record_temp", ".wav", outputDir);
        outputFile.deleteOnExit();
    }

    public void startRecording() throws IOException {
        if (audioRecord == null) {
            initializeAudioRecord();
        }
        audioRecord.startRecording();
        isRecording = true;
        currentRecordingStatus = CurrentRecordingStatus.RECORDING;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte[] data = new byte[AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outputFile);
            while (isRecording) {
                int read = audioRecord.read(data, 0, data.length);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    os.write(data);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopRecording() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            recordingThread = null;
            currentRecordingStatus = CurrentRecordingStatus.NONE;
            writeWavHeader();
        }
    }

    private void writeWavHeader() {
        try {
            byte[] audioData = java.nio.file.Files.readAllBytes(outputFile.toPath());
            byte[] header = createWavHeader(audioData.length);
            FileOutputStream os = new FileOutputStream(outputFile);
            os.write(header);
            os.write(audioData);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            isRecording = false;
            currentRecordingStatus = CurrentRecordingStatus.PAUSED;
            return true;
        } else {
            return false;
        }
    }

    public boolean resumeRecording() throws NotSupportedOsVersion {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw new NotSupportedOsVersion();
        }

        if (currentRecordingStatus == CurrentRecordingStatus.PAUSED) {
            isRecording = true;
            currentRecordingStatus = CurrentRecordingStatus.RECORDING;
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");
            recordingThread.start();
            return true;
        } else {
            return false;
        }
    }

    public CurrentRecordingStatus getCurrentStatus() {
        return currentRecordingStatus;
    }

    public boolean deleteOutputFile() {
        return outputFile.delete();
    }

    public static boolean canPhoneCreateMediaRecorder(Context context) {
        return true;
    }
}