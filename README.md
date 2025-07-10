# MicGuard - Microphone Privacy Protection

MicGuard is an Android application designed to protect your privacy by monitoring microphone usage on your device. It notifies you when apps access your microphone, allowing you to take control of your device's audio privacy.

## Features

### 🔒 **Real-time Microphone Monitoring**
- Continuously monitors microphone usage across all apps
- Detects when apps start and stop using the microphone
- Provides instant notifications for microphone access

### 📱 **Smart App Selection**
- View all installed apps on your device
- Select which apps to monitor for microphone usage
- Filter apps by name or package
- See which apps have microphone permissions

### 🔔 **Customizable Notifications**
- Enable/disable notifications
- Configure notification sound and vibration
- Get notified only for apps you choose to monitor
- Ongoing microphone usage alerts

### ⚙️ **Privacy-First Design**
- No data collection or cloud storage
- All monitoring happens locally on your device
- Open source and transparent
- Respects your privacy choices

## How It Works

### 1. **Accessibility Service**
MicGuard uses Android's accessibility service to monitor microphone usage. This allows the app to:
- Detect when apps access the microphone
- Identify which app is using the microphone
- Monitor usage in real-time

### 2. **Usage Stats Permission**
The app uses usage stats to:
- Identify which apps are currently active
- Correlate microphone usage with specific apps
- Provide accurate app identification

### 3. **Notification System**
When microphone usage is detected:
- Instant notification with app name
- Ongoing notification for continuous usage
- Tap notification to open the app

## Installation & Setup

### Prerequisites
- Android 7.0 (API 24) or higher
- Accessibility permissions
- Usage stats permissions

### Setup Steps

1. **Install the App**
   - Download and install MicGuard from Google Play Store
   - Launch the app

2. **Grant Permissions**
   - **Accessibility Permission**: Go to Settings > Accessibility > MicGuard and enable it
   - **Usage Stats Permission**: Go to Settings > Apps > Special app access > Usage access > MicGuard and enable it

3. **Configure Monitoring**
   - Select which apps you want to monitor
   - Configure notification settings
   - Enable monitoring

4. **Start Monitoring**
   - Toggle the monitoring switch to start
   - The app will now notify you when monitored apps use the microphone

## Usage Guide

### Main Screen
- **Status Indicator**: Shows current monitoring status
- **Quick Actions**: Access app list and settings
- **Recent Activity**: Shows monitored apps

### App List
- **Search**: Find specific apps quickly
- **Toggle Monitoring**: Enable/disable monitoring for each app
- **Permission Indicators**: See which apps have microphone access

### Settings
- **Notification Settings**: Configure notification preferences
- **Permission Management**: Grant required permissions
- **Auto-start**: Enable monitoring on device boot

## Privacy & Security

### What MicGuard Does NOT Do
- ❌ Record or store audio
- ❌ Send data to external servers
- ❌ Track your conversations
- ❌ Monitor other apps' data

### What MicGuard Does
- ✅ Monitors microphone access events
- ✅ Identifies which apps use the microphone
- ✅ Notifies you of microphone usage
- ✅ Stores settings locally on your device

## Technical Details

### Permissions Required
- `RECORD_AUDIO`: Required for microphone monitoring
- `POST_NOTIFICATIONS`: Required for notifications
- `FOREGROUND_SERVICE`: Required for background monitoring
- `QUERY_ALL_PACKAGES`: Required to list installed apps
- `PACKAGE_USAGE_STATS`: Required to identify active apps
- `BIND_ACCESSIBILITY_SERVICE`: Required for accessibility service

### Architecture
- **MVVM Pattern**: Clean separation of concerns
- **Jetpack Compose**: Modern UI framework
- **DataStore**: Secure preference storage
- **WorkManager**: Background task management
- **Navigation Component**: Screen navigation

### Services
- **Accessibility Service**: Monitors microphone usage
- **Foreground Service**: Ensures continuous monitoring
- **Notification Service**: Handles user notifications

## Troubleshooting

### Common Issues

**"Permission Required" Message**
- Ensure accessibility service is enabled
- Grant usage stats permission
- Restart the app after granting permissions

**No Notifications**
- Check notification permissions
- Verify notification settings in app
- Ensure monitoring is enabled

**App Not Detecting Microphone Usage**
- Verify accessibility service is active
- Check if app has microphone permission
- Restart monitoring service

### Performance
- Minimal battery impact
- Low memory usage
- Efficient background monitoring

## Development

### Building from Source
```bash
# Clone the repository
git clone https://github.com/your-username/micguard.git

# Open in Android Studio
# Sync Gradle files
# Build and run on device
```

### 🔐 Release Signing

This project does **not** include a release keystore or passwords for security reasons.

If you want to build a signed release APK or App Bundle:

1. **Generate your own keystore:**
   ```sh
   keytool -genkeypair -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Set the following environment variables:**
   - `KEYSTORE_FILE` (path to your keystore)
   - `KEYSTORE_PASSWORD` (your keystore password)
   - `KEY_ALIAS` (your key alias)
   - `KEY_PASSWORD` (your key password)

3. **Build the release:**
   ```sh
   ./gradlew assembleRelease
   ```

**Note:**  
Never commit your keystore or passwords to the repository!

### Requirements
- Android Studio Arctic Fox or later
- Android SDK 36
- Kotlin 1.9.0+
- Minimum SDK: 24 (Android 7.0)

## Contributing

We welcome contributions! Please see our contributing guidelines for details.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

If you encounter any issues or have questions:
- Check the troubleshooting section
- Open an issue on GitHub
- Contact support through the app

## Privacy Policy

MicGuard is committed to protecting your privacy. Our privacy policy can be found at [privacy-policy-url].

---

**MicGuard** - Take control of your microphone privacy! 🛡️ 