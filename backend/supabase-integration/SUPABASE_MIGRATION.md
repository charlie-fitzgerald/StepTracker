# Migration Guide: Custom Backend to Supabase

## Overview

This guide walks you through migrating the StepTracker app from a custom Node.js backend to Supabase, which provides PostgreSQL database, real-time subscriptions, authentication, and file storage in a single platform.

## Migration Benefits

### Cost Savings

- **Free Tier**: 500MB database, 1GB storage, 50MB bandwidth
- **Pro Plan**: $25/month vs. $100+ for custom infrastructure
- **No Server Costs**: Eliminates EC2, RDS, ElastiCache costs
- **Automatic Scaling**: No need for load balancers or auto-scaling groups

### Development Speed

- **Instant Setup**: No server provisioning or configuration
- **Auto-generated APIs**: REST and GraphQL APIs out of the box
- **Real-time Subscriptions**: Built-in WebSocket support
- **Dashboard Interface**: Web-based database management

### Maintenance Reduction

- **No Server Management**: Fully managed infrastructure
- **Automatic Backups**: Daily backups with point-in-time recovery
- **Security Updates**: Automatic security patches
- **Monitoring**: Built-in performance monitoring

## Migration Steps

### 1. Create Supabase Project

```bash
# Install Supabase CLI
npm install -g supabase

# Login to Supabase
supabase login

# Create new project
supabase projects create steptracker-app

# Get project credentials
supabase projects api-keys --project-ref your-project-ref
```

### 2. Set Up Database Schema

```sql
-- Run the complete schema from supabase-integration/README.md
-- This creates all tables with proper indexes and RLS policies
```

### 3. Migrate Existing Data

```typescript
// scripts/migrate-data.ts
import { createClient } from "@supabase/supabase-js";
import { Pool } from "pg";

// Connect to old database
const oldDb = new Pool({
  host: process.env.OLD_DB_HOST,
  database: process.env.OLD_DB_NAME,
  user: process.env.OLD_DB_USER,
  password: process.env.OLD_DB_PASSWORD,
});

// Connect to Supabase
const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

async function migrateData() {
  console.log("Starting data migration...");

  // Migrate users
  await migrateUsers();

  // Migrate step data
  await migrateStepData();

  // Migrate walk sessions
  await migrateWalkSessions();

  // Migrate routes
  await migrateRoutes();

  // Migrate goals
  await migrateGoals();

  console.log("Migration completed successfully!");
}

async function migrateUsers() {
  console.log("Migrating users...");

  const { rows: users } = await oldDb.query("SELECT * FROM users");

  for (const user of users) {
    // Create auth user
    const { data: authUser, error: authError } =
      await supabase.auth.admin.createUser({
        email: user.email,
        password: "temporary-password", // Users will reset on first login
        email_confirm: true,
      });

    if (authError) {
      console.error(`Error creating auth user for ${user.email}:`, authError);
      continue;
    }

    // Create profile
    const { error: profileError } = await supabase.from("profiles").insert({
      id: authUser.user.id,
      email: user.email,
      name: user.name,
      date_of_birth: user.date_of_birth,
      height_cm: user.height_cm,
      weight_kg: user.weight_kg,
      units_system: user.units_system || "metric",
      timezone: user.timezone || "UTC",
      avatar_url: user.avatar_url,
      created_at: user.created_at,
      updated_at: user.updated_at,
      last_login: user.last_login,
    });

    if (profileError) {
      console.error(`Error creating profile for ${user.email}:`, profileError);
    }
  }
}

async function migrateStepData() {
  console.log("Migrating step data...");

  const { rows: stepData } = await oldDb.query("SELECT * FROM step_data");

  // Batch insert in chunks of 1000
  const chunkSize = 1000;
  for (let i = 0; i < stepData.length; i += chunkSize) {
    const chunk = stepData.slice(i, i + chunkSize);

    const { error } = await supabase.from("step_data").insert(
      chunk.map((data) => ({
        id: data.id,
        user_id: data.user_id,
        date: data.date,
        steps: data.steps,
        distance_meters: data.distance_meters,
        calories: data.calories,
        active_minutes: data.active_minutes,
        created_at: data.created_at,
        updated_at: data.updated_at,
      }))
    );

    if (error) {
      console.error(
        `Error inserting step data chunk ${i / chunkSize + 1}:`,
        error
      );
    }
  }
}

async function migrateWalkSessions() {
  console.log("Migrating walk sessions...");

  const { rows: walkSessions } = await oldDb.query(
    "SELECT * FROM walk_sessions"
  );

  for (const session of walkSessions) {
    // Insert walk session
    const { data: walkSession, error: walkError } = await supabase
      .from("walk_sessions")
      .insert({
        id: session.id,
        user_id: session.user_id,
        name: session.name,
        start_time: session.start_time,
        end_time: session.end_time,
        duration_seconds: session.duration_seconds,
        distance_meters: session.distance_meters,
        average_pace_minutes_per_km: session.average_pace_minutes_per_km,
        max_elevation_meters: session.max_elevation_meters,
        elevation_gain_meters: session.elevation_gain_meters,
        weather_conditions: session.weather_conditions,
        notes: session.notes,
        is_public: session.is_public,
        created_at: session.created_at,
        updated_at: session.updated_at,
      })
      .select()
      .single();

    if (walkError) {
      console.error(`Error inserting walk session ${session.id}:`, walkError);
      continue;
    }

    // Migrate route coordinates
    const { rows: coordinates } = await oldDb.query(
      "SELECT * FROM route_coordinates WHERE walk_session_id = $1 ORDER BY timestamp",
      [session.id]
    );

    if (coordinates.length > 0) {
      const { error: coordError } = await supabase
        .from("route_coordinates")
        .insert(
          coordinates.map((coord) => ({
            walk_session_id: walkSession.id,
            latitude: coord.latitude,
            longitude: coord.longitude,
            elevation_meters: coord.elevation_meters,
            timestamp: coord.timestamp,
            accuracy_meters: coord.accuracy_meters,
            created_at: coord.created_at,
          }))
        );

      if (coordError) {
        console.error(
          `Error inserting coordinates for session ${session.id}:`,
          coordError
        );
      }
    }
  }
}

// Run migration
migrateData().catch(console.error);
```

### 4. Update Android App

#### Add Supabase Dependencies

```kotlin
// app/build.gradle.kts
dependencies {
    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:1.4.7")
    implementation("io.github.jan-tennert.supabase:realtime-kt:1.4.7")
    implementation("io.github.jan-tennert.supabase:storage-kt:1.4.7")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:1.4.7")
}
```

#### Create Supabase Client

```kotlin
// app/src/main/java/com/steptracker/app/data/supabase/SupabaseClient.kt
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.gotrue.GoTrue

object SupabaseClient {
    private val supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Realtime)
        install(Storage)
        install(GoTrue)
    }

    val postgrest = supabase.postgrest
    val realtime = supabase.realtime
    val storage = supabase.storage
    val auth = supabase.auth
}
```

#### Update Repository Classes

```kotlin
// app/src/main/java/com/steptracker/app/data/repository/StepRepository.kt
class StepRepository @Inject constructor() {

    suspend fun getStepDataForDate(date: LocalDate): StepData? {
        return try {
            val response = SupabaseClient.postgrest["step_data"]
                .select { eq("date", date.toString()) }
                .singleOrNull()

            response?.let { StepData.fromSupabase(it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncStepData(steps: List<StepData>) {
        val stepData = steps.map { it.toSupabase() }

        SupabaseClient.postgrest["step_data"]
            .upsert(stepData) {
                onConflict = "user_id,date"
            }
    }

    fun subscribeToStepUpdates(callback: (StepData) -> Unit) {
        SupabaseClient.realtime.createChannel("step_updates")
            .on<StepData>("postgres_changes") {
                event = PostgresChangeEvent.ALL
                schema = "public"
                table = "step_data"
            } { callback(it.record) }
            .subscribe()
    }
}
```

#### Update Authentication

```kotlin
// app/src/main/java/com/steptracker/app/auth/SupabaseAuthManager.kt
class SupabaseAuthManager @Inject constructor() {

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val response = SupabaseClient.auth.signInWith(email, password)
            AuthResult.Success(response.user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Authentication failed")
        }
    }

    suspend fun signInWithGoogle(): AuthResult {
        return try {
            val response = SupabaseClient.auth.signInWith(Provider.GOOGLE)
            AuthResult.Success(response.user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google sign-in failed")
        }
    }

    suspend fun signOut() {
        SupabaseClient.auth.signOut()
    }

    fun getCurrentUser(): User? {
        return SupabaseClient.auth.currentUserOrNull()
    }
}
```

### 5. Update Environment Configuration

```bash
# .env
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key

# Remove old backend variables
# DB_HOST=localhost
# DB_PORT=5432
# JWT_SECRET=...
```

### 6. Deploy and Test

```bash
# Test Supabase connection
npm run test:supabase

# Deploy to production
npm run deploy:supabase

# Monitor migration
supabase projects logs --project-ref your-project-ref
```

## Data Validation

### Verify Migration Success

```typescript
// scripts/verify-migration.ts
import { createClient } from "@supabase/supabase-js";

const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

async function verifyMigration() {
  console.log("Verifying migration...");

  // Check user count
  const { count: userCount } = await supabase
    .from("profiles")
    .select("*", { count: "exact", head: true });

  console.log(`Users migrated: ${userCount}`);

  // Check step data count
  const { count: stepCount } = await supabase
    .from("step_data")
    .select("*", { count: "exact", head: true });

  console.log(`Step records migrated: ${stepCount}`);

  // Check walk sessions count
  const { count: walkCount } = await supabase
    .from("walk_sessions")
    .select("*", { count: "exact", head: true });

  console.log(`Walk sessions migrated: ${walkCount}`);

  // Verify data integrity
  await verifyDataIntegrity();
}

async function verifyDataIntegrity() {
  // Check for orphaned records
  const { data: orphanedSteps } = await supabase
    .from("step_data")
    .select("user_id")
    .not("user_id", "in", `(select id from profiles)`);

  if (orphanedSteps && orphanedSteps.length > 0) {
    console.warn(`Found ${orphanedSteps.length} orphaned step records`);
  }

  // Check for data consistency
  const { data: inconsistentData } = await supabase
    .from("step_data")
    .select("*")
    .or("steps.lt.0,steps.gt.100000");

  if (inconsistentData && inconsistentData.length > 0) {
    console.warn(
      `Found ${inconsistentData.length} records with inconsistent step counts`
    );
  }
}
```

## Rollback Plan

### If Migration Fails

```typescript
// scripts/rollback.ts
async function rollbackMigration() {
  console.log("Rolling back migration...");

  // Delete all data from Supabase
  await supabase
    .from("notifications")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");
  await supabase
    .from("challenge_participants")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");
  await supabase
    .from("challenges")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");
  await supabase
    .from("friendships")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");
  await supabase
    .from("goals")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");
  await supabase
    .from("saved_routes")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");
  await supabase
    .from("route_coordinates")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");
  await supabase
    .from("walk_sessions")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");
  await supabase
    .from("step_data")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");
  await supabase
    .from("profiles")
    .delete()
    .neq("id", "00000000-0000-0000-0000-000000000000");

  console.log("Rollback completed");
}
```

## Post-Migration Tasks

### 1. Update Documentation

- Update API documentation to reflect Supabase endpoints
- Update deployment guides
- Update troubleshooting guides

### 2. Monitor Performance

```typescript
// Monitor key metrics
const metrics = {
  responseTime: await measureResponseTime(),
  errorRate: await calculateErrorRate(),
  userActivity: await trackUserActivity(),
  dataSync: await monitorDataSync(),
};
```

### 3. User Communication

- Notify users about the migration
- Provide migration timeline
- Offer support for any issues

### 4. Clean Up

- Remove old backend code
- Update CI/CD pipelines
- Archive old database backups

## Migration Checklist

- [ ] Create Supabase project
- [ ] Set up database schema
- [ ] Migrate user data
- [ ] Migrate step data
- [ ] Migrate walk sessions
- [ ] Migrate routes and goals
- [ ] Update Android app
- [ ] Test authentication
- [ ] Test real-time features
- [ ] Verify data integrity
- [ ] Update environment variables
- [ ] Deploy to production
- [ ] Monitor performance
- [ ] Update documentation
- [ ] Clean up old infrastructure

## Support

If you encounter issues during migration:

1. **Check Supabase Status**: https://status.supabase.com
2. **Review Logs**: Use Supabase dashboard to check logs
3. **Contact Support**: Use Supabase Discord or GitHub issues
4. **Rollback**: Use the rollback script if needed

The migration to Supabase will significantly reduce infrastructure costs while providing better performance and developer experience.
