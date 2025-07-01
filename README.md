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
- **Room Database**: Local data persistence
- **DataStore**: User preferences storage
- **Hilt**: Dependency injection

### Key Libraries

- **Google Maps SDK**: For routing and map functionality
- **FusedLocationProviderClient**: GPS location tracking
- **Retrofit**: Weather API integration
- **Kotlin Coroutines**: Asynchronous operations
- **Work Manager**: Background task management

## Project Structure

```
app/src/main/java/com/steptracker/app/
├── data/
│   ├── api/           # API interfaces and models
│   ├── dao/           # Room database DAOs
│   ├── model/         # Data entities
│   ├── preferences/   # User preferences
│   └── repository/    # Data repositories
├── di/                # Dependency injection
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
- Google Maps API key
- OpenWeatherMap API key

### Installation

1. Clone the repository
2. Open the project in Android Studio
3. Add your API keys to `local.properties`:
   ```
   MAPS_API_KEY=your_google_maps_api_key
   WEATHER_API_KEY=your_openweathermap_api_key
   ```
4. Sync the project with Gradle files
5. Build and run the app

### Required Permissions

The app requires the following permissions:

- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`: For GPS tracking
- `ACTIVITY_RECOGNITION`: For step counting
- `INTERNET`: For weather API calls
- `FOREGROUND_SERVICE`: For background walk tracking
- `VIBRATE`: For milestone notifications

## Usage

### Getting Started

1. **First Launch**: Grant necessary permissions when prompted
2. **Home Screen**: View today's step count, weekly stats, and weather
3. **Walk Screen**: Choose a walk mode and start tracking
4. **History**: Review past walks and save favorite routes
5. **Goals**: Set and track daily/weekly step goals
6. **Settings**: Customize units, theme, and notifications

### Walk Modes

- **Auto Route**: Enter desired distance, app generates a loop route
- **Draw Route**: Tap on map to create a custom route
- **Just Walk**: Start walking immediately, route is recorded automatically

### Data Privacy

- All data is stored locally on the device
- No personal data is transmitted to external servers (except weather API)
- GPS is only active during walk sessions
- Step counting works without GPS for daily tracking

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

## Contributing

This project is designed to be easily maintainable and extensible. Key areas for contribution:

- UI/UX improvements for accessibility
- Performance optimizations
- Additional walk modes
- Enhanced data visualization
- Testing and bug fixes

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:

- Check the app's Help & Support section
- Review the settings for customization options
- Ensure all required permissions are granted

---

**Note**: This app is designed with older users in mind, featuring large touch targets, clear navigation, and intuitive interfaces. The design prioritizes ease of use over complex features.
