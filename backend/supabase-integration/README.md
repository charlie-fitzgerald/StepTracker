# StepTracker Supabase Integration

## Overview

Supabase provides a powerful alternative to a custom backend, offering PostgreSQL database, real-time subscriptions, authentication, and file storage in a single platform. This integration guide shows how to migrate StepTracker to use Supabase.

## Why Supabase?

### Advantages

- **PostgreSQL Database**: Full SQL database with real-time capabilities
- **Built-in Authentication**: OAuth, email/password, magic links
- **Real-time Subscriptions**: Live data updates without WebSockets
- **File Storage**: Built-in S3-compatible storage
- **Edge Functions**: Serverless functions for custom logic
- **Database Functions**: PostgreSQL functions for complex queries
- **Row Level Security (RLS)**: Fine-grained data access control
- **Auto-generated APIs**: REST and GraphQL APIs
- **Dashboard**: Web-based database management

### Cost Benefits

- **Free Tier**: 500MB database, 1GB file storage, 50MB bandwidth
- **Pro Plan**: $25/month for 8GB database, 100GB storage
- **No Server Management**: Fully managed infrastructure
- **Automatic Scaling**: Handles traffic spikes automatically

## Setup

### 1. Create Supabase Project

1. Go to [supabase.com](https://supabase.com)
2. Create a new project
3. Note your project URL and anon key

### 2. Environment Variables

```bash
# .env
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
```

### 3. Install Supabase Client

```bash
npm install @supabase/supabase-js
```

## Database Schema

### SQL Migration Script

```sql
-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table (extends Supabase auth.users)
CREATE TABLE public.profiles (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    date_of_birth DATE,
    height_cm INTEGER,
    weight_kg DECIMAL(5,2),
    units_system TEXT DEFAULT 'metric',
    timezone TEXT DEFAULT 'UTC',
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITH TIME ZONE
);

-- Step data (time-series optimized)
CREATE TABLE public.step_data (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    steps INTEGER NOT NULL DEFAULT 0,
    distance_meters DECIMAL(10,2) DEFAULT 0,
    calories INTEGER DEFAULT 0,
    active_minutes INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, date)
);

-- Walk sessions
CREATE TABLE public.walk_sessions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    name TEXT,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,
    distance_meters DECIMAL(10,2),
    average_pace_minutes_per_km DECIMAL(5,2),
    max_elevation_meters DECIMAL(8,2),
    elevation_gain_meters DECIMAL(8,2),
    weather_conditions JSONB,
    notes TEXT,
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Route coordinates
CREATE TABLE public.route_coordinates (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    walk_session_id UUID REFERENCES public.walk_sessions(id) ON DELETE CASCADE,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    elevation_meters DECIMAL(8,2),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    accuracy_meters DECIMAL(5,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Saved routes
CREATE TABLE public.saved_routes (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    distance_meters DECIMAL(10,2),
    estimated_duration_minutes INTEGER,
    difficulty_level TEXT,
    route_data JSONB,
    thumbnail_url TEXT,
    is_public BOOLEAN DEFAULT false,
    is_favorite BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Goals
CREATE TABLE public.goals (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('daily', 'weekly', 'monthly', 'custom')),
    target_steps INTEGER NOT NULL,
    target_distance_meters DECIMAL(10,2),
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Friends
CREATE TABLE public.friendships (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    friend_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'blocked')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, friend_id)
);

-- Challenges
CREATE TABLE public.challenges (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    creator_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    challenge_type TEXT NOT NULL CHECK (challenge_type IN ('steps', 'distance', 'duration')),
    target_value INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_public BOOLEAN DEFAULT false,
    max_participants INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Challenge participants
CREATE TABLE public.challenge_participants (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    challenge_id UUID REFERENCES public.challenges(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    current_progress INTEGER DEFAULT 0,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(challenge_id, user_id)
);

-- Notifications
CREATE TABLE public.notifications (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_step_data_user_date ON public.step_data(user_id, date);
CREATE INDEX idx_walk_sessions_user_time ON public.walk_sessions(user_id, start_time);
CREATE INDEX idx_route_coordinates_session ON public.route_coordinates(walk_session_id, timestamp);
CREATE INDEX idx_goals_user_active ON public.goals(user_id, is_active);
CREATE INDEX idx_friendships_user_status ON public.friendships(user_id, status);
CREATE INDEX idx_challenge_participants_challenge ON public.challenge_participants(challenge_id);
CREATE INDEX idx_notifications_user_read ON public.notifications(user_id, is_read);

-- Enable Row Level Security
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.step_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.walk_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.route_coordinates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_routes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.challenge_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
```

## Row Level Security Policies

### Profiles

```sql
-- Users can read their own profile
CREATE POLICY "Users can view own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id);

-- Users can update their own profile
CREATE POLICY "Users can update own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

-- Users can insert their own profile
CREATE POLICY "Users can insert own profile" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);
```

### Step Data

```sql
-- Users can manage their own step data
CREATE POLICY "Users can manage own step data" ON public.step_data
    FOR ALL USING (auth.uid() = user_id);
```

### Walk Sessions

```sql
-- Users can manage their own walk sessions
CREATE POLICY "Users can manage own walk sessions" ON public.walk_sessions
    FOR ALL USING (auth.uid() = user_id);

-- Users can view public walk sessions
CREATE POLICY "Users can view public walk sessions" ON public.walk_sessions
    FOR SELECT USING (is_public = true);
```

### Saved Routes

```sql
-- Users can manage their own routes
CREATE POLICY "Users can manage own routes" ON public.saved_routes
    FOR ALL USING (auth.uid() = user_id);

-- Users can view public routes
CREATE POLICY "Users can view public routes" ON public.saved_routes
    FOR SELECT USING (is_public = true);
```

## Supabase Client Configuration

### Client Setup

```typescript
// lib/supabase.ts
import { createClient } from "@supabase/supabase-js";

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!;
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!;

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: true,
  },
});
```

### Authentication Service

```typescript
// services/auth.ts
import { supabase } from "../lib/supabase";

export class AuthService {
  // Sign up with email/password
  static async signUp(email: string, password: string, name: string) {
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
      options: {
        data: {
          name,
        },
      },
    });

    if (error) throw error;

    // Create profile
    if (data.user) {
      await supabase.from("profiles").insert({
        id: data.user.id,
        email,
        name,
      });
    }

    return data;
  }

  // Sign in with email/password
  static async signIn(email: string, password: string) {
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });

    if (error) throw error;
    return data;
  }

  // Sign in with Google
  static async signInWithGoogle() {
    const { data, error } = await supabase.auth.signInWithOAuth({
      provider: "google",
      options: {
        redirectTo: `${window.location.origin}/auth/callback`,
      },
    });

    if (error) throw error;
    return data;
  }

  // Sign out
  static async signOut() {
    const { error } = await supabase.auth.signOut();
    if (error) throw error;
  }

  // Get current user
  static async getCurrentUser() {
    const {
      data: { user },
      error,
    } = await supabase.auth.getUser();
    if (error) throw error;
    return user;
  }

  // Get user profile
  static async getUserProfile(userId: string) {
    const { data, error } = await supabase
      .from("profiles")
      .select("*")
      .eq("id", userId)
      .single();

    if (error) throw error;
    return data;
  }
}
```

### Step Data Service

```typescript
// services/steps.ts
import { supabase } from "../lib/supabase";

export class StepService {
  // Get daily step data
  static async getDailySteps(date: string) {
    const { data, error } = await supabase
      .from("step_data")
      .select("*")
      .eq("date", date)
      .single();

    if (error && error.code !== "PGRST116") throw error;
    return (
      data || {
        date,
        steps: 0,
        distance_meters: 0,
        calories: 0,
        active_minutes: 0,
      }
    );
  }

  // Get step data for date range
  static async getStepRange(startDate: string, endDate: string) {
    const { data, error } = await supabase
      .from("step_data")
      .select("*")
      .gte("date", startDate)
      .lte("date", endDate)
      .order("date");

    if (error) throw error;
    return data;
  }

  // Sync step data
  static async syncSteps(stepsData: any[]) {
    const { data, error } = await supabase
      .from("step_data")
      .upsert(stepsData, { onConflict: "user_id,date" });

    if (error) throw error;
    return data;
  }

  // Get step statistics
  static async getStatistics(period: string, startDate?: string) {
    let query = supabase.from("step_data").select("*");

    if (startDate) {
      const endDate = new Date(startDate);
      endDate.setDate(
        endDate.getDate() +
          (period === "week" ? 7 : period === "month" ? 30 : 365)
      );

      query = query
        .gte("date", startDate)
        .lt("date", endDate.toISOString().split("T")[0]);
    } else {
      const endDate = new Date();
      const startDate = new Date();
      startDate.setDate(
        startDate.getDate() -
          (period === "week" ? 7 : period === "month" ? 30 : 365)
      );

      query = query
        .gte("date", startDate.toISOString().split("T")[0])
        .lte("date", endDate.toISOString().split("T")[0]);
    }

    const { data, error } = await query;
    if (error) throw error;

    // Calculate statistics
    const totalSteps = data.reduce((sum, item) => sum + item.steps, 0);
    const totalDistance = data.reduce(
      (sum, item) => sum + (item.distance_meters || 0),
      0
    );
    const averageSteps =
      data.length > 0 ? Math.round(totalSteps / data.length) : 0;

    return {
      period,
      totalSteps,
      totalDistance,
      averageSteps,
      daysWithData: data.length,
    };
  }
}
```

### Real-time Subscriptions

```typescript
// services/realtime.ts
import { supabase } from "../lib/supabase";

export class RealtimeService {
  // Subscribe to step updates
  static subscribeToSteps(callback: (payload: any) => void) {
    return supabase
      .channel("step_updates")
      .on(
        "postgres_changes",
        {
          event: "*",
          schema: "public",
          table: "step_data",
        },
        callback
      )
      .subscribe();
  }

  // Subscribe to friend activity
  static subscribeToFriendActivity(callback: (payload: any) => void) {
    return supabase
      .channel("friend_activity")
      .on(
        "postgres_changes",
        {
          event: "INSERT",
          schema: "public",
          table: "walk_sessions",
          filter: "is_public=eq.true",
        },
        callback
      )
      .subscribe();
  }

  // Subscribe to challenge updates
  static subscribeToChallengeUpdates(
    challengeId: string,
    callback: (payload: any) => void
  ) {
    return supabase
      .channel(`challenge_${challengeId}`)
      .on(
        "postgres_changes",
        {
          event: "*",
          schema: "public",
          table: "challenge_participants",
          filter: `challenge_id=eq.${challengeId}`,
        },
        callback
      )
      .subscribe();
  }
}
```

## File Storage Integration

### Route Images and GPX Files

```typescript
// services/storage.ts
import { supabase } from "../lib/supabase";

export class StorageService {
  // Upload route image
  static async uploadRouteImage(file: File, routeId: string) {
    const fileExt = file.name.split(".").pop();
    const fileName = `${routeId}/thumbnail.${fileExt}`;

    const { data, error } = await supabase.storage
      .from("route-images")
      .upload(fileName, file, {
        cacheControl: "3600",
        upsert: false,
      });

    if (error) throw error;

    // Get public URL
    const {
      data: { publicUrl },
    } = supabase.storage.from("route-images").getPublicUrl(fileName);

    return publicUrl;
  }

  // Upload GPX file
  static async uploadGPXFile(file: File, walkSessionId: string) {
    const fileName = `${walkSessionId}/route.gpx`;

    const { data, error } = await supabase.storage
      .from("gpx-files")
      .upload(fileName, file, {
        cacheControl: "3600",
        upsert: false,
      });

    if (error) throw error;
    return data;
  }

  // Download GPX file
  static async downloadGPXFile(walkSessionId: string) {
    const { data, error } = await supabase.storage
      .from("gpx-files")
      .download(`${walkSessionId}/route.gpx`);

    if (error) throw error;
    return data;
  }
}
```

## Database Functions

### Step Statistics Function

```sql
-- Function to calculate step statistics
CREATE OR REPLACE FUNCTION get_step_statistics(
    p_user_id UUID,
    p_start_date DATE,
    p_end_date DATE
)
RETURNS TABLE (
    total_steps BIGINT,
    total_distance DECIMAL,
    total_calories BIGINT,
    average_steps NUMERIC,
    current_streak INTEGER,
    longest_streak INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH step_stats AS (
        SELECT
            SUM(steps) as total_steps,
            SUM(distance_meters) as total_distance,
            SUM(calories) as total_calories,
            AVG(steps) as average_steps
        FROM step_data
        WHERE user_id = p_user_id
        AND date BETWEEN p_start_date AND p_end_date
    ),
    streak_calc AS (
        SELECT
            date,
            steps,
            CASE
                WHEN steps > 0 THEN 1
                ELSE 0
            END as has_steps
        FROM step_data
        WHERE user_id = p_user_id
        AND date BETWEEN p_start_date AND p_end_date
        ORDER BY date
    ),
    streaks AS (
        SELECT
            SUM(has_steps) as current_streak,
            MAX(SUM(has_steps)) OVER () as longest_streak
        FROM streak_calc
        WHERE has_steps = 1
    )
    SELECT
        s.total_steps,
        s.total_distance,
        s.total_calories,
        s.average_steps,
        st.current_streak,
        st.longest_streak
    FROM step_stats s, streaks st;
END;
$$ LANGUAGE plpgsql;
```

### Achievement Check Function

```sql
-- Function to check and award achievements
CREATE OR REPLACE FUNCTION check_achievements(p_user_id UUID)
RETURNS TABLE (
    achievement_type TEXT,
    achievement_name TEXT,
    description TEXT
) AS $$
DECLARE
    total_steps BIGINT;
    total_walks INTEGER;
    current_streak INTEGER;
BEGIN
    -- Get user statistics
    SELECT SUM(steps) INTO total_steps
    FROM step_data
    WHERE user_id = p_user_id;

    SELECT COUNT(*) INTO total_walks
    FROM walk_sessions
    WHERE user_id = p_user_id;

    -- Check for step milestones
    IF total_steps >= 1000000 THEN
        achievement_type := 'steps';
        achievement_name := 'Million Steps';
        description := 'Walked 1 million steps!';
        RETURN NEXT;
    END IF;

    -- Check for walk milestones
    IF total_walks >= 100 THEN
        achievement_type := 'walks';
        achievement_name := 'Century Walker';
        description := 'Completed 100 walks!';
        RETURN NEXT;
    END IF;

    RETURN;
END;
$$ LANGUAGE plpgsql;
```

## Edge Functions

### Achievement Notification

```typescript
// supabase/functions/achievement-notification/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const { user_id, achievement_type, achievement_name } = await req.json();

    // Create notification
    const { error } = await supabaseClient.from("notifications").insert({
      user_id,
      type: "achievement",
      title: "Achievement Unlocked!",
      message: `Congratulations! You've earned the "${achievement_name}" achievement.`,
      data: { achievement_type, achievement_name },
    });

    if (error) throw error;

    return new Response(JSON.stringify({ success: true }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
```

## Migration from Custom Backend

### 1. Data Migration Script

```typescript
// scripts/migrate-to-supabase.ts
import { createClient } from "@supabase/supabase-js";

const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

async function migrateData() {
  // Migrate users
  const users = await fetchUsersFromOldDB();
  for (const user of users) {
    await supabase.from("profiles").upsert({
      id: user.id,
      email: user.email,
      name: user.name,
      // ... other fields
    });
  }

  // Migrate step data
  const stepData = await fetchStepDataFromOldDB();
  await supabase.from("step_data").upsert(stepData);

  // Migrate walk sessions
  const walkSessions = await fetchWalkSessionsFromOldDB();
  await supabase.from("walk_sessions").upsert(walkSessions);

  console.log("Migration completed successfully!");
}
```

### 2. Update Android App

```kotlin
// Add Supabase dependencies
implementation("io.github.jan-tennert.supabase:postgrest-kt:1.4.7")
implementation("io.github.jan-tennert.supabase:realtime-kt:1.4.7")
implementation("io.github.jan-tennert.supabase:storage-kt:1.4.7")

// Update API client
class SupabaseAPIClient {
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://your-project.supabase.co",
        supabaseKey = "your-anon-key"
    ) {
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }

    suspend fun getDailySteps(date: String): StepData {
        return supabase.postgrest["step_data"]
            .select { eq("date", date) }
            .single()
    }

    suspend fun syncSteps(steps: List<StepData>) {
        supabase.postgrest["step_data"]
            .upsert(steps)
    }
}
```

## Benefits of Supabase Integration

### 1. **Reduced Infrastructure**

- No server management
- Automatic scaling
- Built-in monitoring
- Global CDN

### 2. **Real-time Capabilities**

- Live data updates
- WebSocket connections
- Presence detection
- Broadcasting

### 3. **Security**

- Row Level Security
- Built-in authentication
- API key management
- SSL/TLS encryption

### 4. **Developer Experience**

- Auto-generated APIs
- Type-safe queries
- Real-time subscriptions
- Dashboard interface

### 5. **Cost Efficiency**

- Pay-per-use pricing
- Free tier available
- No server costs
- Automatic optimization

This Supabase integration provides a modern, scalable, and cost-effective solution for the StepTracker backend while maintaining all the functionality of the custom backend approach.
