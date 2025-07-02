# StepTracker - Modern Android Step Tracking App

A streamlined, modern step tracking and walking route app designed for Android phones, built with Kotlin and Jetpack Compose. This app is specifically designed to be user-friendly for people in their 60s, with clear navigation and intuitive features.

## Features

### Core Features

- **Daily Step Tracking**: Uses phone's step counter sensor for accurate step counting
- **Local Data Storage**: All data is stored locally using Room database
- **Statistics Viewing**: View daily, weekly, monthly, and total step counts
- **Distance Tracking**: GPS-based distance calculation during walk sessions only
- **Elevation Tracking**: Track max elevation and elevation gain/loss during walks

### Walk Session Modes

1. **Auto-Route Generator**: Create walkable loops of user-defined distances
2. **Draw Route**: Sketch routes on the map that snap to real walking paths
3. **Just Walk Mode**: Start tracking with no predefined route, record and save later

### Additional Features

- **Habit Tracker**: GitHub-style calendar grid showing activity levels
- **Route Storage**: Save and favorite walking routes
- **Milestone Notifications**: Configurable distance-based notifications
- **Weather Integration**: Current conditions display using OpenWeatherMap API
- **Unit Settings**: Toggle between imperial and metric systems
- **Theme Support**: Light, dark, and system theme modes

## Technical Stack

### Architecture

- **MVVM Pattern**: ViewModel + StateFlow for state management
- **Jetpack Compose**: Modern declarative UI framework
- **Room Database**: Local data persistence with encryption support
- **DataStore**: User preferences storage with encryption
- **Hilt**: Dependency injection

### Security & Authentication

- **OAuth 2.0**: Google Sign-In integration
- **Android Keystore**: Hardware-backed encryption
- **AES-256-GCM**: Military-grade encryption for sensitive data
- **Certificate Pinning**: Network security protection
- **Secure Preferences**: Encrypted API key and token storage

### Key Libraries

- **Google Maps SDK**: For routing and map functionality
- **FusedLocationProviderClient**: GPS location tracking
- **Retrofit**: Weather API integration with security
- **Kotlin Coroutines**: Asynchronous operations
- **Work Manager**: Background task management
- **Google Play Services Auth**: OAuth authentication

## Project Structure

```
app/src/main/java/com/steptracker/app/
├── auth/              # OAuth authentication
├── data/
│   ├── api/           # API interfaces and models
│   ├── dao/           # Room database DAOs
│   ├── model/         # Data entities
│   ├── preferences/   # User preferences
│   └── repository/    # Data repositories
├── di/                # Dependency injection
├── security/          # Security and encryption
├── service/           # Background services
├── ui/
│   ├── components/    # Reusable UI components
│   ├── navigation/    # Navigation setup
│   ├── screens/       # Main app screens
│   ├── theme/         # App theming
│   └── viewmodel/     # ViewModels
└── MainActivity.kt    # Main activity
```

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24+ (API level 24)

### Installation

1. Clone the repository
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Build and run the app

**Note**: The app comes with pre-configured API keys for immediate use. No additional setup required!

### Required Permissions

The app requires the following permissions:

- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`: For GPS tracking
- `ACTIVITY_RECOGNITION`: For step counting
- `INTERNET`: For weather API calls
- `FOREGROUND_SERVICE`: For background walk tracking
- `VIBRATE`: For milestone notifications

## Usage

### Getting Started

1. **First Launch**: Sign in with Google or create a local account
2. **Grant Permissions**: Allow necessary permissions when prompted
3. **Home Screen**: View today's step count, weekly stats, and weather
4. **Walk Screen**: Choose a walk mode and start tracking
5. **History**: Review past walks and save favorite routes
6. **Goals**: Set and track daily/weekly step goals
7. **Settings**: Customize units, theme, and notifications

### Walk Modes

- **Auto Route**: Enter desired distance, app generates a loop route
- **Draw Route**: Tap on map to create a custom route
- **Just Walk**: Start walking immediately, route is recorded automatically

### Data Privacy & Security

- **Local Data Storage**: All user data is stored locally on the device
- **Encrypted Storage**: Sensitive data is encrypted using AES-256-GCM
- **Secure Authentication**: OAuth 2.0 with Google Sign-In and secure local authentication
- **API Key Protection**: All API keys are encrypted and securely managed
- **HTTPS Enforcement**: All network requests use secure HTTPS connections
- **Certificate Pinning**: Prevents man-in-the-middle attacks
- **GDPR Compliant**: User data handling follows privacy regulations
- **No Personal Data Sharing**: No personal data transmitted to external servers (except weather API)
- **GPS Privacy**: GPS is only active during walk sessions
- **Step Counting**: Works without GPS for daily tracking

## Future Enhancements

### Planned Features

- Cloud sync (Firebase integration)
- Rewards and badges system
- Social features and leaderboards
- Home screen widget
- Apple Health/Google Fit integration
- Advanced route planning with points of interest

### Technical Improvements

- Offline map support
- Battery optimization improvements
- Enhanced accessibility features
- Multi-language support
- Wear OS companion app

## Security Features

### Authentication & Authorization

- **OAuth 2.0**: Secure Google Sign-In integration
- **Local Authentication**: Email/password with encrypted storage
- **Session Management**: Secure token handling and automatic refresh
- **Biometric Support**: Optional fingerprint/face unlock

### Data Protection

- **AES-256-GCM Encryption**: Military-grade encryption for all sensitive data
- **Android Keystore**: Hardware-backed key storage
- **Encrypted Database**: Room database with encryption support
- **Secure Preferences**: Encrypted storage for API keys and tokens

### Network Security

- **HTTPS Enforcement**: All API calls use secure connections
- **Certificate Pinning**: Prevents man-in-the-middle attacks
- **API Key Protection**: Encrypted storage and secure management
- **Request Validation**: Input sanitization and validation

### Privacy Compliance

- **GDPR Compliant**: User data handling according to regulations
- **Data Minimization**: Only necessary data is collected
- **User Control**: Complete data deletion and export capabilities
- **Transparency**: Clear privacy policies and data usage

## Contributing

This project is designed to be easily maintainable and extensible. Key areas for contribution:

- UI/UX improvements for accessibility
- Performance optimizations
- Additional walk modes
- Enhanced data visualization
- Testing and bug fixes
- Security enhancements

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:

- Check the app's Help & Support section
- Review the settings for customization options
- Ensure all required permissions are granted

---

**Note**: This app is designed with older users in mind, featuring large touch targets, clear navigation, and intuitive interfaces. The design prioritizes ease of use over complex features.
