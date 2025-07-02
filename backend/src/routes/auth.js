const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const { body, validationResult } = require("express-validator");
const { OAuth2Client } = require("google-auth-library");

const { db } = require("../config/database");
const { redis } = require("../config/redis");

const router = express.Router();

// Google OAuth client
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

// Generate JWT tokens
const generateTokens = (userId) => {
  const accessToken = jwt.sign(
    { userId, type: "access" },
    process.env.JWT_SECRET,
    { expiresIn: "15m" }
  );

  const refreshToken = jwt.sign(
    { userId, type: "refresh" },
    process.env.JWT_REFRESH_SECRET,
    { expiresIn: "7d" }
  );

  return { accessToken, refreshToken };
};

// Store refresh token in Redis
const storeRefreshToken = async (userId, refreshToken) => {
  await redis.setex(`refresh_token:${userId}`, 7 * 24 * 60 * 60, refreshToken);
};

// Register new user
router.post(
  "/register",
  [
    body("email").isEmail().normalizeEmail(),
    body("password").isLength({ min: 8 }),
    body("name").trim().isLength({ min: 2 }),
    body("dateOfBirth").optional().isISO8601(),
    body("heightCm").optional().isInt({ min: 100, max: 250 }),
    body("weightKg").optional().isFloat({ min: 30, max: 300 }),
  ],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { email, password, name, dateOfBirth, heightCm, weightKg } =
        req.body;

      // Check if user already exists
      const existingUser = await db("users").where({ email }).first();
      if (existingUser) {
        return res.status(409).json({ error: "User already exists" });
      }

      // Hash password
      const saltRounds = 12;
      const passwordHash = await bcrypt.hash(password, saltRounds);

      // Create user
      const [userId] = await db("users")
        .insert({
          email,
          password_hash: passwordHash,
          name,
          date_of_birth: dateOfBirth,
          height_cm: heightCm,
          weight_kg: weightKg,
        })
        .returning("id");

      // Generate tokens
      const { accessToken, refreshToken } = generateTokens(userId);

      // Store refresh token
      await storeRefreshToken(userId, refreshToken);

      // Update last login
      await db("users").where({ id: userId }).update({
        last_login: new Date(),
      });

      res.status(201).json({
        message: "User registered successfully",
        accessToken,
        refreshToken,
        user: {
          id: userId,
          email,
          name,
        },
      });
    } catch (error) {
      console.error("Registration error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

// Login user
router.post(
  "/login",
  [body("email").isEmail().normalizeEmail(), body("password").notEmpty()],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { email, password } = req.body;

      // Find user
      const user = await db("users").where({ email }).first();
      if (!user) {
        return res.status(401).json({ error: "Invalid credentials" });
      }

      // Check password
      const isValidPassword = await bcrypt.compare(
        password,
        user.password_hash
      );
      if (!isValidPassword) {
        return res.status(401).json({ error: "Invalid credentials" });
      }

      // Generate tokens
      const { accessToken, refreshToken } = generateTokens(user.id);

      // Store refresh token
      await storeRefreshToken(user.id, refreshToken);

      // Update last login
      await db("users").where({ id: user.id }).update({
        last_login: new Date(),
      });

      res.json({
        message: "Login successful",
        accessToken,
        refreshToken,
        user: {
          id: user.id,
          email: user.email,
          name: user.name,
          avatarUrl: user.avatar_url,
        },
      });
    } catch (error) {
      console.error("Login error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

// Google OAuth login
router.post("/google", [body("idToken").notEmpty()], async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const { idToken } = req.body;

    // Verify Google token
    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_CLIENT_ID,
    });

    const payload = ticket.getPayload();
    const { sub: googleId, email, name, picture } = payload;

    // Find or create user
    let user = await db("users").where({ google_id: googleId }).first();

    if (!user) {
      // Check if email already exists
      user = await db("users").where({ email }).first();

      if (user) {
        // Link existing account to Google
        await db("users").where({ id: user.id }).update({
          google_id: googleId,
          avatar_url: picture,
        });
      } else {
        // Create new user
        const [userId] = await db("users")
          .insert({
            google_id: googleId,
            email,
            name,
            avatar_url: picture,
          })
          .returning("id");

        user = { id: userId, email, name, avatar_url: picture };
      }
    }

    // Generate tokens
    const { accessToken, refreshToken } = generateTokens(user.id);

    // Store refresh token
    await storeRefreshToken(user.id, refreshToken);

    // Update last login
    await db("users").where({ id: user.id }).update({
      last_login: new Date(),
    });

    res.json({
      message: "Google login successful",
      accessToken,
      refreshToken,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        avatarUrl: user.avatar_url,
      },
    });
  } catch (error) {
    console.error("Google login error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// Refresh access token
router.post("/refresh", [body("refreshToken").notEmpty()], async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const { refreshToken } = req.body;

    // Verify refresh token
    const decoded = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET);

    if (decoded.type !== "refresh") {
      return res.status(401).json({ error: "Invalid token type" });
    }

    // Check if refresh token exists in Redis
    const storedToken = await redis.get(`refresh_token:${decoded.userId}`);
    if (!storedToken || storedToken !== refreshToken) {
      return res.status(401).json({ error: "Invalid refresh token" });
    }

    // Generate new tokens
    const { accessToken, refreshToken: newRefreshToken } = generateTokens(
      decoded.userId
    );

    // Store new refresh token
    await storeRefreshToken(decoded.userId, newRefreshToken);

    res.json({
      accessToken,
      refreshToken: newRefreshToken,
    });
  } catch (error) {
    console.error("Token refresh error:", error);
    res.status(401).json({ error: "Invalid refresh token" });
  }
});

// Logout
router.post("/logout", async (req, res) => {
  try {
    const authHeader = req.headers.authorization;
    if (authHeader) {
      const token = authHeader.split(" ")[1];
      const decoded = jwt.decode(token);

      if (decoded && decoded.userId) {
        // Remove refresh token from Redis
        await redis.del(`refresh_token:${decoded.userId}`);
      }
    }

    res.json({ message: "Logout successful" });
  } catch (error) {
    console.error("Logout error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

module.exports = router;
