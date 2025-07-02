const express = require("express");
const { body, query, param, validationResult } = require("express-validator");
const moment = require("moment");

const { db } = require("../config/database");

const router = express.Router();

// Get user's walk sessions
router.get(
  "/",
  [
    query("page").optional().isInt({ min: 1 }),
    query("limit").optional().isInt({ min: 1, max: 100 }),
    query("startDate").optional().isISO8601(),
    query("endDate").optional().isISO8601(),
  ],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const userId = req.user.userId;
      const page = parseInt(req.query.page) || 1;
      const limit = parseInt(req.query.limit) || 20;
      const offset = (page - 1) * limit;

      let query = db("walk_sessions")
        .where({ user_id: userId })
        .orderBy("start_time", "desc");

      // Add date filters if provided
      if (req.query.startDate) {
        query = query.where("start_time", ">=", req.query.startDate);
      }
      if (req.query.endDate) {
        query = query.where("start_time", "<=", req.query.endDate);
      }

      const walkSessions = await query.limit(limit).offset(offset);

      // Get total count for pagination
      const totalCount = await db("walk_sessions")
        .where({ user_id: userId })
        .count("* as count")
        .first();

      res.json({
        walkSessions: walkSessions.map((session) => ({
          id: session.id,
          name: session.name,
          startTime: session.start_time,
          endTime: session.end_time,
          durationSeconds: session.duration_seconds,
          distanceMeters: session.distance_meters,
          averagePaceMinutesPerKm: session.average_pace_minutes_per_km,
          maxElevationMeters: session.max_elevation_meters,
          elevationGainMeters: session.elevation_gain_meters,
          weatherConditions: session.weather_conditions,
          notes: session.notes,
          isPublic: session.is_public,
          createdAt: session.created_at,
        })),
        pagination: {
          page,
          limit,
          total: parseInt(totalCount.count),
          totalPages: Math.ceil(totalCount.count / limit),
        },
      });
    } catch (error) {
      console.error("Get walk sessions error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

// Get specific walk session with route data
router.get("/:id", [param("id").isUUID()], async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const userId = req.user.userId;
    const { id } = req.params;

    const walkSession = await db("walk_sessions")
      .where({ id, user_id: userId })
      .first();

    if (!walkSession) {
      return res.status(404).json({ error: "Walk session not found" });
    }

    // Get route coordinates
    const routeCoordinates = await db("route_coordinates")
      .where({ walk_session_id: id })
      .orderBy("timestamp");

    res.json({
      id: walkSession.id,
      name: walkSession.name,
      startTime: walkSession.start_time,
      endTime: walkSession.end_time,
      durationSeconds: walkSession.duration_seconds,
      distanceMeters: walkSession.distance_meters,
      averagePaceMinutesPerKm: walkSession.average_pace_minutes_per_km,
      maxElevationMeters: walkSession.max_elevation_meters,
      elevationGainMeters: walkSession.elevation_gain_meters,
      weatherConditions: walkSession.weather_conditions,
      notes: walkSession.notes,
      isPublic: walkSession.is_public,
      createdAt: walkSession.created_at,
      routeCoordinates: routeCoordinates.map((coord) => ({
        latitude: coord.latitude,
        longitude: coord.longitude,
        elevationMeters: coord.elevation_meters,
        timestamp: coord.timestamp,
        accuracyMeters: coord.accuracy_meters,
      })),
    });
  } catch (error) {
    console.error("Get walk session error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// Create new walk session
router.post(
  "/",
  [
    body("name").optional().trim().isLength({ max: 255 }),
    body("startTime").isISO8601(),
    body("endTime").optional().isISO8601(),
    body("distanceMeters").optional().isFloat({ min: 0 }),
    body("notes").optional().trim(),
    body("isPublic").optional().isBoolean(),
    body("routeCoordinates").optional().isArray(),
    body("routeCoordinates.*.latitude")
      .optional()
      .isFloat({ min: -90, max: 90 }),
    body("routeCoordinates.*.longitude")
      .optional()
      .isFloat({ min: -180, max: 180 }),
    body("routeCoordinates.*.elevationMeters").optional().isFloat(),
    body("routeCoordinates.*.timestamp").optional().isISO8601(),
    body("routeCoordinates.*.accuracyMeters").optional().isFloat({ min: 0 }),
  ],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const userId = req.user.userId;
      const {
        name,
        startTime,
        endTime,
        distanceMeters,
        notes,
        isPublic = false,
        routeCoordinates = [],
      } = req.body;

      // Calculate duration if end time is provided
      let durationSeconds = null;
      if (endTime) {
        durationSeconds = moment(endTime).diff(moment(startTime), "seconds");
      }

      // Calculate average pace if distance and duration are available
      let averagePaceMinutesPerKm = null;
      if (distanceMeters && durationSeconds && distanceMeters > 0) {
        const paceSecondsPerKm = (durationSeconds / distanceMeters) * 1000;
        averagePaceMinutesPerKm = paceSecondsPerKm / 60;
      }

      // Calculate elevation statistics
      let maxElevationMeters = null;
      let elevationGainMeters = null;

      if (routeCoordinates.length > 0) {
        const elevations = routeCoordinates
          .map((coord) => coord.elevationMeters)
          .filter((elevation) => elevation !== null && elevation !== undefined);

        if (elevations.length > 0) {
          maxElevationMeters = Math.max(...elevations);

          // Calculate elevation gain
          let gain = 0;
          for (let i = 1; i < elevations.length; i++) {
            const diff = elevations[i] - elevations[i - 1];
            if (diff > 0) {
              gain += diff;
            }
          }
          elevationGainMeters = gain;
        }
      }

      // Create walk session
      const [walkSessionId] = await db("walk_sessions")
        .insert({
          user_id: userId,
          name,
          start_time: startTime,
          end_time: endTime,
          duration_seconds: durationSeconds,
          distance_meters: distanceMeters,
          average_pace_minutes_per_km: averagePaceMinutesPerKm,
          max_elevation_meters: maxElevationMeters,
          elevation_gain_meters: elevationGainMeters,
          notes,
          is_public: isPublic,
        })
        .returning("id");

      // Insert route coordinates if provided
      if (routeCoordinates.length > 0) {
        const coordinatesToInsert = routeCoordinates.map((coord) => ({
          walk_session_id: walkSessionId,
          latitude: coord.latitude,
          longitude: coord.longitude,
          elevation_meters: coord.elevationMeters,
          timestamp: coord.timestamp,
          accuracy_meters: coord.accuracyMeters,
        }));

        await db("route_coordinates").insert(coordinatesToInsert);
      }

      res.status(201).json({
        message: "Walk session created successfully",
        walkSessionId,
      });
    } catch (error) {
      console.error("Create walk session error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

// Update walk session
router.put(
  "/:id",
  [
    param("id").isUUID(),
    body("name").optional().trim().isLength({ max: 255 }),
    body("notes").optional().trim(),
    body("isPublic").optional().isBoolean(),
  ],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const userId = req.user.userId;
      const { id } = req.params;
      const { name, notes, isPublic } = req.body;

      // Check if walk session exists and belongs to user
      const existingSession = await db("walk_sessions")
        .where({ id, user_id: userId })
        .first();

      if (!existingSession) {
        return res.status(404).json({ error: "Walk session not found" });
      }

      // Update walk session
      await db("walk_sessions")
        .where({ id })
        .update({
          name: name !== undefined ? name : existingSession.name,
          notes: notes !== undefined ? notes : existingSession.notes,
          is_public:
            isPublic !== undefined ? isPublic : existingSession.is_public,
          updated_at: new Date(),
        });

      res.json({ message: "Walk session updated successfully" });
    } catch (error) {
      console.error("Update walk session error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

// Delete walk session
router.delete("/:id", [param("id").isUUID()], async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const userId = req.user.userId;
    const { id } = req.params;

    // Check if walk session exists and belongs to user
    const existingSession = await db("walk_sessions")
      .where({ id, user_id: userId })
      .first();

    if (!existingSession) {
      return res.status(404).json({ error: "Walk session not found" });
    }

    // Delete route coordinates first (due to foreign key constraint)
    await db("route_coordinates").where({ walk_session_id: id }).del();

    // Delete walk session
    await db("walk_sessions").where({ id }).del();

    res.json({ message: "Walk session deleted successfully" });
  } catch (error) {
    console.error("Delete walk session error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// Get walk statistics
router.get("/statistics/summary", async (req, res) => {
  try {
    const userId = req.user.userId;

    // Get total walks
    const totalWalks = await db("walk_sessions")
      .where({ user_id: userId })
      .count("* as count")
      .first();

    // Get total distance
    const totalDistance = await db("walk_sessions")
      .where({ user_id: userId })
      .sum("distance_meters as total")
      .first();

    // Get total duration
    const totalDuration = await db("walk_sessions")
      .where({ user_id: userId })
      .sum("duration_seconds as total")
      .first();

    // Get average pace
    const avgPace = await db("walk_sessions")
      .where({ user_id: userId })
      .whereNotNull("average_pace_minutes_per_km")
      .avg("average_pace_minutes_per_km as average")
      .first();

    // Get longest walk
    const longestWalk = await db("walk_sessions")
      .where({ user_id: userId })
      .orderBy("distance_meters", "desc")
      .first();

    // Get fastest walk
    const fastestWalk = await db("walk_sessions")
      .where({ user_id: userId })
      .whereNotNull("average_pace_minutes_per_km")
      .orderBy("average_pace_minutes_per_km", "asc")
      .first();

    res.json({
      totalWalks: parseInt(totalWalks.count),
      totalDistanceMeters: parseFloat(totalDistance.total || 0),
      totalDurationSeconds: parseInt(totalDuration.total || 0),
      averagePaceMinutesPerKm: parseFloat(avgPace.average || 0),
      longestWalk: longestWalk
        ? {
            id: longestWalk.id,
            name: longestWalk.name,
            distanceMeters: longestWalk.distance_meters,
            date: longestWalk.start_time,
          }
        : null,
      fastestWalk: fastestWalk
        ? {
            id: fastestWalk.id,
            name: fastestWalk.name,
            averagePaceMinutesPerKm: fastestWalk.average_pace_minutes_per_km,
            date: fastestWalk.start_time,
          }
        : null,
    });
  } catch (error) {
    console.error("Get walk statistics error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

module.exports = router;
