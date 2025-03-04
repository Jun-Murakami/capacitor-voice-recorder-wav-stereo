import Foundation
import AVFoundation

class CustomMediaRecorder {
    
    private var recordingSession: AVAudioSession!
    private var audioRecorder: AVAudioRecorder!
    private var audioFilePath: URL!
    private var originalRecordingSessionCategory: AVAudioSession.Category!
    private var status = CurrentRecordingStatus.NONE
    
    private let settings = [
        AVFormatIDKey: Int(kAudioFormatLinearPCM), // Changed to Linear PCM for WAV
        AVSampleRateKey: 44100,
        AVNumberOfChannelsKey: 2, // Stereo
        AVLinearPCMBitDepthKey: 16, // Typical bit depth for WAV
        AVLinearPCMIsBigEndianKey: false,
        AVLinearPCMIsFloatKey: false,
        AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
    ] as [String : Any]
    
    private func getDirectoryToSaveAudioFile() -> URL {
        return URL(fileURLWithPath: NSTemporaryDirectory(), isDirectory: true)
    }
    
    public func startRecording() -> Bool {
        do {
            recordingSession = AVAudioSession.sharedInstance()
            originalRecordingSessionCategory = recordingSession.category
            try recordingSession.setCategory(AVAudioSession.Category.playAndRecord, options: [.allowBluetoothA2DP, .defaultToSpeaker])
            try recordingSession.setActive(true)
            audioFilePath = getDirectoryToSaveAudioFile().appendingPathComponent("\(UUID().uuidString).wav")
            audioRecorder = try AVAudioRecorder(url: audioFilePath, settings: settings)
            audioRecorder.record()
            status = CurrentRecordingStatus.RECORDING
            return true
        } catch {
            return false
        }
    }
    
    public func stopRecording() {
        do {
            audioRecorder.stop()
            try recordingSession.setActive(false)
            try recordingSession.setCategory(originalRecordingSessionCategory)
            originalRecordingSessionCategory = nil
            audioRecorder = nil
            recordingSession = nil
            status = CurrentRecordingStatus.NONE
        } catch {}
    }
    
    public func getOutputFile() -> URL {
        return audioFilePath
    }
    
    public func pauseRecording() -> Bool {
        if(status == CurrentRecordingStatus.RECORDING) {
            audioRecorder.pause()
            status = CurrentRecordingStatus.PAUSED
            return true
        } else {
            return false
        }
    }
    
    public func resumeRecording() -> Bool {
        if(status == CurrentRecordingStatus.PAUSED) {
            audioRecorder.record()
            status = CurrentRecordingStatus.RECORDING
            return true
        } else {
            return false
        }
    }
    
    public func getCurrentStatus() -> CurrentRecordingStatus {
        return status
    }
    
}
