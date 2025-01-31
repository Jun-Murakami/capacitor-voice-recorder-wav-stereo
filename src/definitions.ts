export type Base64String = string

export interface RecordingData {
  value: {
    recordDataBase64: Base64String
    msDuration: number
    mimeType: string
  };
}

export interface GenericResponse {
  value: boolean;
}

export interface CurrentRecordingStatus {
  status: 'RECORDING' | 'PAUSED' | 'NONE';
}

export interface AudioDevice {
  deviceId: string;
  kind: MediaDeviceKind; // "audioinput" | "audiooutput"
  label: string;
  groupId: string | null;
}

export interface GetConnectedDevicesResult {
  devices: AudioDevice[];
}

export interface VoiceRecorderPlugin {
  canDeviceVoiceRecord(): Promise<GenericResponse>;

  requestAudioRecordingPermission(): Promise<GenericResponse>;

  hasAudioRecordingPermission(): Promise<GenericResponse>;

  startRecording(): Promise<GenericResponse>;

  stopRecording(): Promise<RecordingData>;

  pauseRecording(): Promise<GenericResponse>;

  resumeRecording(): Promise<GenericResponse>;

  getCurrentStatus(): Promise<CurrentRecordingStatus>;

  getConnectedDevices(): Promise<{ devices: MediaDeviceInfo[] }>;
}
