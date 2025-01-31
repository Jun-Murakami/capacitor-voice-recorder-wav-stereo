import Foundation
import AVFoundation
import Capacitor

@objc(VoiceRecorder)
public class VoiceRecorder: CAPPlugin {

    private var customMediaRecorder: CustomMediaRecorder? = nil
    
    @objc func canDeviceVoiceRecord(_ call: CAPPluginCall) {
        call.resolve(ResponseGenerator.successResponse())
    }
    
    @objc func requestAudioRecordingPermission(_ call: CAPPluginCall) {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            if granted {
                call.resolve(ResponseGenerator.successResponse())
            } else {
                call.resolve(ResponseGenerator.failResponse())
            }
        }
    }
    
    @objc func hasAudioRecordingPermission(_ call: CAPPluginCall) {
        call.resolve(ResponseGenerator.fromBoolean(doesUserGaveAudioRecordingPermission()))
    }
    
    
    @objc func startRecording(_ call: CAPPluginCall) {
        if(!doesUserGaveAudioRecordingPermission()) {
            call.reject(Messages.MISSING_PERMISSION)
            return
        }
        
        if(customMediaRecorder != nil) {
            call.reject(Messages.ALREADY_RECORDING)
            return
        }
        
        customMediaRecorder = CustomMediaRecorder()
        if(customMediaRecorder == nil) {
            call.reject(Messages.CANNOT_RECORD_ON_THIS_PHONE)
            return
        }
        
        let successfullyStartedRecording = customMediaRecorder!.startRecording()
        if successfullyStartedRecording == false {
            call.reject(Messages.CANNOT_RECORD_ON_THIS_PHONE)
        } else {
            call.resolve(ResponseGenerator.successResponse())
        }
    }
    
    @objc func stopRecording(_ call: CAPPluginCall) {
        if(customMediaRecorder == nil) {
            call.reject(Messages.RECORDING_HAS_NOT_STARTED)
            return
        }
        
        customMediaRecorder?.stopRecording()
        
        let audioFileUrl = customMediaRecorder?.getOutputFile()
        if(audioFileUrl == nil) {
            customMediaRecorder = nil
            call.reject(Messages.FAILED_TO_FETCH_RECORDING)
            return
        }
        let recordData = RecordData(
            recordDataBase64: readFileAsBase64(audioFileUrl),
            mimeType: "audio/aac",
            msDuration: getMsDurationOfAudioFile(audioFileUrl)
        )
        customMediaRecorder = nil
        if recordData.recordDataBase64 == nil || recordData.msDuration < 0 {
            call.reject(Messages.EMPTY_RECORDING)
        } else {
            call.resolve(ResponseGenerator.dataResponse(recordData.toDictionary()))
        }
    }
    
    @objc func pauseRecording(_ call: CAPPluginCall) {
        if(customMediaRecorder == nil) {
            call.reject(Messages.RECORDING_HAS_NOT_STARTED)
        } else {
            call.resolve(ResponseGenerator.fromBoolean(customMediaRecorder?.pauseRecording() ?? false))
        }
    }
    
    @objc func resumeRecording(_ call: CAPPluginCall) {
        if(customMediaRecorder == nil) {
            call.reject(Messages.RECORDING_HAS_NOT_STARTED)
        } else {
            call.resolve(ResponseGenerator.fromBoolean(customMediaRecorder?.resumeRecording() ?? false))
        }
    }
    
    @objc func getCurrentStatus(_ call: CAPPluginCall) {
        if(customMediaRecorder == nil) {
            call.resolve(ResponseGenerator.statusResponse(CurrentRecordingStatus.NONE))
        } else {
            call.resolve(ResponseGenerator.statusResponse(customMediaRecorder?.getCurrentStatus() ?? CurrentRecordingStatus.NONE))
        }
    }
    
    @objc func getConnectedDevices(_ call: CAPPluginCall) {
        let audioSession = AVAudioSession.sharedInstance()
        var devices = [[String: Any]]()
        
        // ロックを取得して状態変更を防ぐ
        objc_sync_enter(audioSession)
        defer { objc_sync_exit(audioSession) }
        
        do {
            // 現在のルートを強制保持
            let originalRoute = audioSession.currentRoute
            print("Original Route: \(originalRoute)")
            
            // カテゴリ変更を完全に排除
            let availableInputs = audioSession.availableInputs ?? []
            for port in availableInputs {
                devices.append([
                    "deviceId": port.uid,
                    "kind": "audioinput",
                    "label": port.portName,
                    "groupId": NSNull()
                ])
            }
            
            // 出力デバイスは現在のルートから取得（変更なし）
            for port in originalRoute.outputs {
                devices.append([
                    "deviceId": port.uid,
                    "kind": "audiooutput",
                    "label": port.portName,
                    "groupId": NSNull()
                ])
            }
            
            // ルート変更検知
            if audioSession.currentRoute != originalRoute {
                print("Route changed unexpectedly!")
                print("New Route: \(audioSession.currentRoute)")
                throw NSError(domain: "AudioSessionError", code: 1, userInfo: nil)
            }
            
        } catch {
            call.reject("Audio session conflict: \(error.localizedDescription)")
            return
        }
        
        call.resolve(["devices": devices])
    }
    
    func doesUserGaveAudioRecordingPermission() -> Bool {
        return AVAudioSession.sharedInstance().recordPermission == AVAudioSession.RecordPermission.granted
    }
    
    func readFileAsBase64(_ filePath: URL?) -> String? {
        if(filePath == nil) {
            return nil
        }
        
        do {
            let fileData = try Data.init(contentsOf: filePath!)
            let fileStream = fileData.base64EncodedString(options: NSData.Base64EncodingOptions.init(rawValue: 0))
            return fileStream
        } catch {}
        
        return nil
    }
    
    func getMsDurationOfAudioFile(_ filePath: URL?) -> Int {
        if filePath == nil {
            return -1
        }
        return Int(CMTimeGetSeconds(AVURLAsset(url: filePath!).duration) * 1000)
    }
    
}
