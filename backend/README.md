# StepTracker Backend Architecture

## Overview

The StepTracker backend provides cloud services for data synchronization, social features, and advanced analytics while maintaining the app's privacy-first approach.

## Architecture

### Technology Stack

- **API**: Node.js with Express.js
- **Database**: PostgreSQL with TimescaleDB extension for time-series data
- **Authentication**: JWT with refresh tokens
- **Real-time**: WebSocket connections for live updates
- **File Storage**: AWS S3 for route images and GPX files
- **Caching**: Redis for session management and API caching
- **Message Queue**: RabbitMQ for background processing
- **Monitoring**: Prometheus + Grafana
- **Deployment**: Docker + Kubernetes

## Database Schema

### Core Tables

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    google_id VARCHAR(255) UNIQUE,
    name VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    date_of_birth DATE,
    height_cm INTEGER,
    weight_kg DECIMAL(5,2),
    units_system VARCHAR(10) DEFAULT 'metric',
    timezone VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Daily step data (time-series optimized)
CREATE TABLE step_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    steps INTEGER NOT NULL DEFAULT 0,
    distance_meters DECIMAL(10,2) DEFAULT 0,
    calories INTEGER DEFAULT 0,
    active_minutes INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, date)
);

-- Walk sessions
CREATE TABLE walk_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_seconds INTEGER,
    distance_meters DECIMAL(10,2),
    average_pace_minutes_per_km DECIMAL(5,2),
    max_elevation_meters DECIMAL(8,2),
    elevation_gain_meters DECIMAL(8,2),
    weather_conditions JSONB,
    notes TEXT,
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Route coordinates (time-series)
CREATE TABLE route_coordinates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walk_session_id UUID REFERENCES walk_sessions(id) ON DELETE CASCADE,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    elevation_meters DECIMAL(8,2),
    timestamp TIMESTAMP NOT NULL,
    accuracy_meters DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Saved routes
CREATE TABLE saved_routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    distance_meters DECIMAL(10,2),
    estimated_duration_minutes INTEGER,
    difficulty_level VARCHAR(20),
    route_data JSONB, -- GPX data or route coordinates
    thumbnail_url TEXT,
    is_public BOOLEAN DEFAULT false,
    is_favorite BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Goals and achievements
CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL, -- 'daily', 'weekly', 'monthly', 'custom'
    target_steps INTEGER NOT NULL,
    target_distance_meters DECIMAL(10,2),
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Social features
CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    friend_id UUID REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'accepted', 'blocked'
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, friend_id)
);

-- Challenges
CREATE TABLE challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    challenge_type VARCHAR(20) NOT NULL, -- 'steps', 'distance', 'duration'
    target_value INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_public BOOLEAN DEFAULT false,
    max_participants INTEGER,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Challenge participants
CREATE TABLE challenge_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id UUID REFERENCES challenges(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    current_progress INTEGER DEFAULT 0,
    joined_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(challenge_id, user_id)
);

-- Notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);
```

## API Endpoints

### Authentication

```typescript
// POST /api/auth/register
interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  dateOfBirth?: string;
  heightCm?: number;
  weightKg?: number;
}

// POST /api/auth/login
interface LoginRequest {
  email: string;
  password: string;
}

// POST /api/auth/google
interface GoogleAuthRequest {
  idToken: string;
}

// POST /api/auth/refresh
interface RefreshRequest {
  refreshToken: string;
}

// POST /api/auth/logout
// Requires authentication
```

### User Profile

```typescript
// GET /api/user/profile
// Returns user profile data

// PUT /api/user/profile
interface UpdateProfileRequest {
  name?: string;
  dateOfBirth?: string;
  heightCm?: number;
  weightKg?: number;
  unitsSystem?: "metric" | "imperial";
  timezone?: string;
}

// DELETE /api/user/account
// Deletes user account and all associated data
```

### Step Data

```typescript
// GET /api/steps/daily?date=2024-01-15
// Returns daily step data

// GET /api/steps/range?startDate=2024-01-01&endDate=2024-01-31
// Returns step data for date range

// POST /api/steps/sync
interface SyncStepsRequest {
  steps: Array<{
    date: string;
    steps: number;
    distanceMeters?: number;
    calories?: number;
    activeMinutes?: number;
  }>;
}

// GET /api/steps/statistics
// Returns aggregated statistics (weekly, monthly, yearly)
```

### Walk Sessions

```typescript
// GET /api/walks
// Returns user's walk sessions with pagination

// GET /api/walks/:id
// Returns specific walk session with route data

// POST /api/walks
interface CreateWalkRequest {
  name?: string;
  startTime: string;
  endTime?: string;
  distanceMeters?: number;
  notes?: string;
  isPublic?: boolean;
  routeCoordinates?: Array<{
    latitude: number;
    longitude: number;
    elevationMeters?: number;
    timestamp: string;
    accuracyMeters?: number;
  }>;
}

// PUT /api/walks/:id
// Update walk session

// DELETE /api/walks/:id
// Delete walk session
```

### Routes

```typescript
// GET /api/routes
// Returns user's saved routes

// GET /api/routes/public
// Returns public routes from all users

// POST /api/routes
interface CreateRouteRequest {
  name: string;
  description?: string;
  distanceMeters: number;
  estimatedDurationMinutes?: number;
  difficultyLevel?: "easy" | "moderate" | "hard";
  routeData: any; // GPX data
  isPublic?: boolean;
}

// GET /api/routes/:id
// Returns specific route

// PUT /api/routes/:id
// Update route

// DELETE /api/routes/:id
// Delete route
```

### Goals

```typescript
// GET /api/goals
// Returns user's goals

// POST /api/goals
interface CreateGoalRequest {
  type: "daily" | "weekly" | "monthly" | "custom";
  targetSteps: number;
  targetDistanceMeters?: number;
  startDate: string;
  endDate?: string;
}

// PUT /api/goals/:id
// Update goal

// DELETE /api/goals/:id
// Delete goal
```

### Social Features

```typescript
// GET /api/friends
// Returns user's friends

// POST /api/friends/request/:userId
// Send friend request

// PUT /api/friends/accept/:userId
// Accept friend request

// DELETE /api/friends/:userId
// Remove friend

// GET /api/challenges
// Returns available challenges

// POST /api/challenges
interface CreateChallengeRequest {
  name: string;
  description?: string;
  challengeType: "steps" | "distance" | "duration";
  targetValue: number;
  startDate: string;
  endDate: string;
  isPublic?: boolean;
  maxParticipants?: number;
}

// POST /api/challenges/:id/join
// Join challenge

// GET /api/challenges/:id/leaderboard
// Returns challenge leaderboard
```

## Real-time Features

### WebSocket Events

```typescript
// Client connects with JWT token
// Server validates and adds to user's room

// Live step updates
interface StepUpdateEvent {
  type: "step_update";
  data: {
    userId: string;
    steps: number;
    timestamp: string;
  };
}

// Friend activity
interface FriendActivityEvent {
  type: "friend_activity";
  data: {
    friendId: string;
    activity: "walk_started" | "goal_achieved" | "challenge_completed";
    details: any;
  };
}

// Challenge updates
interface ChallengeUpdateEvent {
  type: "challenge_update";
  data: {
    challengeId: string;
    leaderboard: Array<{
      userId: string;
      progress: number;
      rank: number;
    }>;
  };
}
```

## Background Services

### Data Processing

```typescript
// Step data aggregation (runs every hour)
async function aggregateStepData() {
  // Aggregate hourly data into daily totals
  // Calculate weekly and monthly statistics
  // Update user achievements
}

// Route analysis (runs after each walk)
async function analyzeRoute(walkSessionId: string) {
  // Calculate elevation gain/loss
  // Determine difficulty level
  // Generate route statistics
  // Create route thumbnail
}

// Achievement checking (runs daily)
async function checkAchievements() {
  // Check for milestone achievements
  // Award badges
  // Send notifications
}
```

## Security Features

### API Security

```typescript
// Rate limiting
const rateLimit = require("express-rate-limit");

const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: "Too many requests from this IP",
});

// JWT authentication middleware
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];

  if (!token) {
    return res.sendStatus(401);
  }

  jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
    if (err) return res.sendStatus(403);
    req.user = user;
    next();
  });
};

// Data validation
const validateStepData = (data) => {
  // Validate step count ranges
  // Check for suspicious patterns
  // Verify GPS coordinates
  // Validate timestamps
};
```

### Data Privacy

```typescript
// GDPR compliance
async function exportUserData(userId: string) {
  // Export all user data in JSON format
  // Include step data, routes, goals, etc.
}

async function deleteUserData(userId: string) {
  // Anonymize or delete all user data
  // Remove from all related tables
  // Clear cached data
}

// Data encryption
const encryptSensitiveData = (data) => {
  // Encrypt sensitive fields before storage
  // Use AES-256 encryption
};
```

## Deployment

### Docker Configuration

```dockerfile
# Dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY . .

EXPOSE 3000

CMD ["npm", "start"]
```

```yaml
# docker-compose.yml
version: "3.8"

services:
  api:
    build: .
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - DATABASE_URL=postgresql://user:pass@db:5432/steptracker
      - REDIS_URL=redis://redis:6379
    depends_on:
      - db
      - redis

  db:
    image: timescale/timescaledb:latest-pg14
    environment:
      - POSTGRES_DB=steptracker
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=pass
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

### Environment Variables

```bash
# .env
NODE_ENV=production
PORT=3000

# Database
DATABASE_URL=postgresql://user:pass@localhost:5432/steptracker

# Redis
REDIS_URL=redis://localhost:6379

# JWT
JWT_SECRET=your-super-secret-jwt-key
JWT_REFRESH_SECRET=your-super-secret-refresh-key

# AWS S3
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=us-east-1
S3_BUCKET=steptracker-routes

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASS=your-app-password
```

## Monitoring & Analytics

### Health Checks

```typescript
// Health check endpoint
app.get("/health", async (req, res) => {
  try {
    // Check database connection
    await db.query("SELECT 1");

    // Check Redis connection
    await redis.ping();

    res.json({
      status: "healthy",
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
    });
  } catch (error) {
    res.status(503).json({
      status: "unhealthy",
      error: error.message,
    });
  }
});
```

### Analytics Dashboard

```typescript
// Analytics endpoints
app.get("/analytics/users", async (req, res) => {
  // User growth over time
  // Active users
  // User retention
});

app.get("/analytics/activity", async (req, res) => {
  // Total steps across all users
  // Popular walking times
  // Route popularity
});

app.get("/analytics/performance", async (req, res) => {
  // API response times
  // Error rates
  // Database performance
});
```

This backend architecture provides a solid foundation for a production StepTracker app with social features, data synchronization, and advanced analytics while maintaining security and privacy standards.
