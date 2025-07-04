# StepTracker Testing Instructions

## 1. Environment Setup

### Android Studio & Emulator

- Download and install Android Studio from [developer.android.com/studio](https://developer.android.com/studio)
- Open Android Studio, go to `Tools > SDK Manager` and install Android SDK 34 (API Level 34) and Emulator
- Go to `Tools > AVD Manager`, create a new device (e.g., Pixel 7), select API 34 system image, and finish setup

### Backend (Supabase or Local)

- For Supabase: create a project at [supabase.com](https://supabase.com), get your project URL and anon key
- For local backend: install Node.js, PostgreSQL, and run `npm install` in the `backend` directory
- Configure `.env` files as needed for your backend

---

## 2. Backend Testing

### API Testing

- Use Postman or create a script (see below) to test endpoints
- Example script (Node.js + axios):

```js
const axios = require("axios");
const BASE_URL = "http://localhost:3000";

async function testAPI() {
  // Register
  const reg = await axios.post(`${BASE_URL}/auth/register`, {
    email: "test@example.com",
    password: "Test1234",
    name: "Test User",
  });
  const token = reg.data.token;
  // Add steps
  await axios.post(
    `${BASE_URL}/steps`,
    { date: "2024-01-15", steps: 8500 },
    { headers: { Authorization: `Bearer ${token}` } }
  );
  // Add walk
  await axios.post(
    `${BASE_URL}/walks`,
    {
      name: "Morning Walk",
      start_time: "2024-01-15T08:00:00Z",
      distance_meters: 5000,
    },
    { headers: { Authorization: `Bearer ${token}` } }
  );
}
testAPI();
```

- Run with `node testAPI.js`

---

## 3. Android App Testing (Emulator)

- Open the project in Android Studio
- Start your AVD from `Tools > AVD Manager`
- Click the green Run button to build and deploy the app to the emulator
- Use the emulator's extended controls to simulate steps and GPS

---

## 4. Test Suite

### Backend

- Run `npm test` in the `backend` directory to execute all backend tests
- Add/modify tests in `backend/test/unit/` and `backend/test/integration/`

### Android

- Use Android Studio's built-in test runner for unit and UI tests
- Add tests in `app/src/test/java/` and `app/src/androidTest/java/`

---

## 5. Debugging

### Android

- Use Logcat in Android Studio to view logs
- Set breakpoints and use the debugger to step through code
- Use the provided DebugLogger and DebugMenu utilities for in-app debugging

### Backend

- Use `console.log` or a logger for debugging
- Inspect the database directly with psql or Supabase dashboard

---

## 6. Performance & Load Testing

- Use scripts (e.g., with axios) to simulate concurrent users and measure response times
- Monitor app performance in Android Studio's Profiler

---

## 7. Manual Testing Checklist

- [ ] Register/login/logout (email & Google)
- [ ] Step tracking and persistence
- [ ] Walk session creation and route tracking
- [ ] Goal and achievement notifications
- [ ] Social features (friend requests, challenges)
- [ ] Real-time updates
- [ ] App performance and battery usage

---

## 8. Troubleshooting

- **Emulator slow**: Enable hardware acceleration, allocate more RAM
- **API errors**: Check .env config, backend logs, and network connectivity
- **App crashes**: Check Logcat, dependencies, and API keys
- **Real-time not working**: Check WebSocket/realtime config

---

## 9. Useful Commands

```sh
# Start backend
cd backend && npm start
# Run backend tests
cd backend && npm test
# Build Android app
cd app && ./gradlew build
# Run Android app (from Android Studio or ./gradlew installDebug)
```

---

## 10. Resources

- [Android Studio](https://developer.android.com/studio)
- [Supabase Docs](https://supabase.com/docs)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Postman](https://www.postman.com/)

---

_Last updated: January 2024_
