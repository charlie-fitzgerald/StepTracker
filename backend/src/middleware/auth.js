const jwt = require("jsonwebtoken");
const { redis } = require("../config/redis");

// Authenticate JWT token
const authenticateToken = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.split(" ")[1];

    if (!token) {
      return res.status(401).json({ error: "Access token required" });
    }

    // Verify token
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    if (decoded.type !== "access") {
      return res.status(401).json({ error: "Invalid token type" });
    }

    // Check if token is blacklisted (for logout)
    const isBlacklisted = await redis.get(`blacklist:${token}`);
    if (isBlacklisted) {
      return res.status(401).json({ error: "Token has been revoked" });
    }

    // Add user info to request
    req.user = {
      userId: decoded.userId,
    };

    next();
  } catch (error) {
    if (error.name === "TokenExpiredError") {
      return res.status(401).json({ error: "Token expired" });
    }
    if (error.name === "JsonWebTokenError") {
      return res.status(401).json({ error: "Invalid token" });
    }

    console.error("Authentication error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
};

// Optional authentication (doesn't fail if no token)
const optionalAuth = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.split(" ")[1];

    if (token) {
      const decoded = jwt.verify(token, process.env.JWT_SECRET);

      if (decoded.type === "access") {
        const isBlacklisted = await redis.get(`blacklist:${token}`);
        if (!isBlacklisted) {
          req.user = {
            userId: decoded.userId,
          };
        }
      }
    }

    next();
  } catch (error) {
    // Continue without authentication
    next();
  }
};

// Check if user owns resource
const checkOwnership = (resourceTable, resourceIdField = "id") => {
  return async (req, res, next) => {
    try {
      const userId = req.user.userId;
      const resourceId = req.params[resourceIdField];

      const { db } = require("../config/database");

      const resource = await db(resourceTable)
        .where({ [resourceIdField]: resourceId, user_id: userId })
        .first();

      if (!resource) {
        return res.status(404).json({ error: "Resource not found" });
      }

      req.resource = resource;
      next();
    } catch (error) {
      console.error("Ownership check error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  };
};

// Rate limiting for specific endpoints
const createRateLimiter = (windowMs, maxRequests) => {
  const rateLimit = require("express-rate-limit");

  return rateLimit({
    windowMs,
    max: maxRequests,
    message: {
      error: "Too many requests",
      message: "Please try again later",
    },
    standardHeaders: true,
    legacyHeaders: false,
    keyGenerator: (req) => {
      // Use user ID if authenticated, otherwise use IP
      return req.user ? req.user.userId : req.ip;
    },
  });
};

// Admin authentication
const requireAdmin = async (req, res, next) => {
  try {
    const userId = req.user.userId;

    const { db } = require("../config/database");

    const user = await db("users").where({ id: userId }).first();

    if (!user || user.role !== "admin") {
      return res.status(403).json({ error: "Admin access required" });
    }

    next();
  } catch (error) {
    console.error("Admin check error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
};

module.exports = {
  authenticateToken,
  optionalAuth,
  checkOwnership,
  createRateLimiter,
  requireAdmin,
};
