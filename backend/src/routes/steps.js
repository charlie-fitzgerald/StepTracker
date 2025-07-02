const express = require("express");
const { body, query, validationResult } = require("express-validator");
const moment = require("moment");

const { db } = require("../config/database");

const router = express.Router();

// Get daily step data
router.get(
  "/daily",
  [query("date").optional().isISO8601()],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const userId = req.user.userId;
      const date = req.query.date || moment().format("YYYY-MM-DD");

      const stepData = await db("step_data")
        .where({ user_id: userId, date })
        .first();

      if (!stepData) {
        return res.json({
          date,
          steps: 0,
          distanceMeters: 0,
          calories: 0,
          activeMinutes: 0,
        });
      }

      res.json({
        date: stepData.date,
        steps: stepData.steps,
        distanceMeters: stepData.distance_meters,
        calories: stepData.calories,
        activeMinutes: stepData.active_minutes,
      });
    } catch (error) {
      console.error("Get daily steps error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

// Get step data for date range
router.get(
  "/range",
  [query("startDate").isISO8601(), query("endDate").isISO8601()],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const userId = req.user.userId;
      const { startDate, endDate } = req.query;

      const stepData = await db("step_data")
        .where({ user_id: userId })
        .whereBetween("date", [startDate, endDate])
        .orderBy("date");

      res.json(
        stepData.map((data) => ({
          date: data.date,
          steps: data.steps,
          distanceMeters: data.distance_meters,
          calories: data.calories,
          activeMinutes: data.active_minutes,
        }))
      );
    } catch (error) {
      console.error("Get step range error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

// Sync step data from device
router.post(
  "/sync",
  [
    body("steps").isArray(),
    body("steps.*.date").isISO8601(),
    body("steps.*.steps").isInt({ min: 0, max: 100000 }),
    body("steps.*.distanceMeters").optional().isFloat({ min: 0 }),
    body("steps.*.calories").optional().isInt({ min: 0 }),
    body("steps.*.activeMinutes").optional().isInt({ min: 0 }),
  ],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const userId = req.user.userId;
      const { steps } = req.body;

      const results = [];

      for (const stepData of steps) {
        const {
          date,
          steps: stepCount,
          distanceMeters,
          calories,
          activeMinutes,
        } = stepData;

        // Check if data already exists
        const existing = await db("step_data")
          .where({ user_id: userId, date })
          .first();

        if (existing) {
          // Update existing data
          await db("step_data")
            .where({ user_id: userId, date })
            .update({
              steps: stepCount,
              distance_meters: distanceMeters || existing.distance_meters,
              calories: calories || existing.calories,
              active_minutes: activeMinutes || existing.active_minutes,
              updated_at: new Date(),
            });

          results.push({ date, action: "updated" });
        } else {
          // Insert new data
          await db("step_data").insert({
            user_id: userId,
            date,
            steps: stepCount,
            distance_meters: distanceMeters || 0,
            calories: calories || 0,
            active_minutes: activeMinutes || 0,
          });

          results.push({ date, action: "created" });
        }
      }

      res.json({
        message: "Step data synced successfully",
        results,
      });
    } catch (error) {
      console.error("Sync steps error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

// Get step statistics
router.get(
  "/statistics",
  [
    query("period").isIn(["week", "month", "year"]),
    query("startDate").optional().isISO8601(),
  ],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const userId = req.user.userId;
      const { period, startDate } = req.query;

      let endDate, startDateQuery;

      if (startDate) {
        startDateQuery = startDate;
        endDate = moment(startDate)
          .add(1, period)
          .subtract(1, "day")
          .format("YYYY-MM-DD");
      } else {
        endDate = moment().format("YYYY-MM-DD");
        startDateQuery = moment()
          .subtract(1, period)
          .add(1, "day")
          .format("YYYY-MM-DD");
      }

      const stepData = await db("step_data")
        .where({ user_id: userId })
        .whereBetween("date", [startDateQuery, endDate]);

      const totalSteps = stepData.reduce((sum, data) => sum + data.steps, 0);
      const totalDistance = stepData.reduce(
        (sum, data) => sum + (data.distance_meters || 0),
        0
      );
      const totalCalories = stepData.reduce(
        (sum, data) => sum + (data.calories || 0),
        0
      );
      const totalActiveMinutes = stepData.reduce(
        (sum, data) => sum + (data.active_minutes || 0),
        0
      );
      const averageSteps =
        stepData.length > 0 ? Math.round(totalSteps / stepData.length) : 0;

      // Calculate streaks
      const sortedData = stepData.sort((a, b) =>
        moment(a.date).diff(moment(b.date))
      );
      let currentStreak = 0;
      let longestStreak = 0;
      let tempStreak = 0;

      for (let i = 0; i < sortedData.length; i++) {
        if (sortedData[i].steps > 0) {
          tempStreak++;
          if (i === sortedData.length - 1) {
            currentStreak = tempStreak;
          }
        } else {
          if (tempStreak > longestStreak) {
            longestStreak = tempStreak;
          }
          tempStreak = 0;
        }
      }

      if (tempStreak > longestStreak) {
        longestStreak = tempStreak;
      }

      res.json({
        period,
        startDate: startDateQuery,
        endDate,
        totalSteps,
        totalDistance,
        totalCalories,
        totalActiveMinutes,
        averageSteps,
        currentStreak,
        longestStreak,
        daysWithData: stepData.length,
      });
    } catch (error) {
      console.error("Get statistics error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

// Get step trends
router.get(
  "/trends",
  [query("days").optional().isInt({ min: 7, max: 365 })],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const userId = req.user.userId;
      const days = parseInt(req.query.days) || 30;

      const endDate = moment().format("YYYY-MM-DD");
      const startDate = moment()
        .subtract(days - 1, "days")
        .format("YYYY-MM-DD");

      const stepData = await db("step_data")
        .where({ user_id: userId })
        .whereBetween("date", [startDate, endDate])
        .orderBy("date");

      // Fill in missing dates with zero steps
      const filledData = [];
      const currentDate = moment(startDate);
      const endMoment = moment(endDate);

      while (currentDate.isSameOrBefore(endMoment)) {
        const dateStr = currentDate.format("YYYY-MM-DD");
        const existingData = stepData.find((data) => data.date === dateStr);

        filledData.push({
          date: dateStr,
          steps: existingData ? existingData.steps : 0,
          distanceMeters: existingData ? existingData.distance_meters : 0,
          calories: existingData ? existingData.calories : 0,
        });

        currentDate.add(1, "day");
      }

      // Calculate moving averages
      const movingAverages = [];
      const windowSize = 7;

      for (let i = windowSize - 1; i < filledData.length; i++) {
        const window = filledData.slice(i - windowSize + 1, i + 1);
        const average = Math.round(
          window.reduce((sum, data) => sum + data.steps, 0) / windowSize
        );
        movingAverages.push({
          date: filledData[i].date,
          averageSteps: average,
        });
      }

      res.json({
        dailyData: filledData,
        movingAverages,
        period: `${days} days`,
      });
    } catch (error) {
      console.error("Get trends error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

module.exports = router;
