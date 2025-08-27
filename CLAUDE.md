# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

- **Build**: `npm run build` - Compiles TypeScript, generates docs, and bundles the plugin
- **Clean**: `npm run clean` - Removes the dist directory
- **Watch**: `npm run watch` - Watches TypeScript files for changes
- **Lint**: `npm run lint` - Runs ESLint and Prettier checks
- **Format**: `npm run fmt` - Auto-fixes ESLint and Prettier issues
- **Doc Generation**: `npm run docgen` - Generates API documentation for README.md

### Platform-Specific Verification
- **iOS**: `npm run verify:ios` - Builds iOS plugin in Xcode
- **Android**: `npm run verify:android` - Builds and tests Android plugin with Gradle  
- **Web**: `npm run verify:web` - Runs build to verify web compatibility
- **All Platforms**: `npm run verify` - Runs all platform verifications

### Linting Commands
- **Standard Lint**: `npm run lint` - ESLint + Prettier
- **With iOS**: `npm run lint-with-ios` - Includes SwiftLint for iOS code
- **SwiftLint**: `npm run swiftlint -- lint` - Swift code linting only

## Project Architecture

This is a **Capacitor plugin** for voice recording that provides a unified API across iOS, Android, and Web platforms.

### Core Structure
- **TypeScript API** (`src/`): Plugin interface and web implementation
- **iOS Implementation** (`ios/Plugin/`): Native Swift code  
- **Android Implementation** (`android/src/main/java/`): Native Java/Kotlin code
- **Example App** (`example/`): Demo application for testing

### Key Files
- `src/definitions.ts`: TypeScript interfaces and types for the plugin API
- `src/web.ts`: Web platform implementation using MediaRecorder API
- `src/index.ts`: Main plugin export and registration
- `CapacitorVoiceRecorder.podspec`: iOS CocoaPods specification
- `android/build.gradle`: Android build configuration

### Platform-Specific Native Code
- **iOS**: Swift implementation in `ios/Plugin/VoiceRecorder.swift`  
- **Android**: Java implementation in `android/src/main/java/com/tchvu3/capacitorvoicerecorder/VoiceRecorder.java`

### Dependencies
- Uses `capacitor-blob-writer` and `get-blob-duration` for web platform file handling
- Targets Capacitor v7.x (follows Capacitor versioning)
- TypeScript version locked to <4.5.0 for compatibility

### Build Process
1. TypeScript compilation to `dist/esm/`
2. Rollup bundling for browser distribution (`dist/plugin.js`, `dist/plugin.cjs.js`)
3. Automatic README.md generation from JSDoc comments
4. Type declaration files generated automatically

## Development Notes

- Plugin follows Capacitor's plugin architecture pattern
- All platforms must implement the same `VoiceRecorderPlugin` interface
- Audio formats vary by platform: AAC (mobile), WebM Opus (Chrome/Firefox), MP4 (Safari)
- File system integration available via Capacitor Filesystem API with directory/subdirectory options
- Permission handling required for microphone access on all platforms