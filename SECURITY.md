# Security Documentation

## Overview

This document outlines the security measures implemented in the StepTracker Android application to ensure user data protection, secure authentication, and API key management.

## Security Features

### 1. Authentication & Authorization

#### OAuth 2.0 Integration

- **Google Sign-In**: Implemented using Google Play Services Auth API
- **Secure Token Management**: Authentication tokens are encrypted and stored securely
- **Session Management**: Automatic token refresh and secure session handling

#### Local Authentication

- **Email/Password**: Secure credential storage with encryption
- **Password Hashing**: Salted password hashing for local authentication
- **Secure Token Generation**: Cryptographically secure random token generation

### 2. Data Encryption

#### Android Keystore Integration

- **AES-256-GCM Encryption**: Used for sensitive data encryption
- **Hardware-backed Keys**: Leverages Android Keystore for key storage
- **Key Rotation**: Automatic key generation and management

#### Encrypted Storage

- **SecurePreferences**: All sensitive data encrypted before storage
- **API Key Protection**: API keys encrypted and stored securely
- **User Credentials**: Email/password pairs encrypted at rest

### 3. Network Security

#### HTTPS Enforcement

- **Certificate Pinning**: Prevents man-in-the-middle attacks
- **HTTPS Only**: All network requests must use HTTPS
- **Secure OkHttp Client**: Custom client with security configurations

#### API Security

- **Token-based Authentication**: Secure API calls with encrypted tokens
- **Request Validation**: Input validation and sanitization
- **Rate Limiting**: Protection against abuse

### 4. Data Protection

#### Local Data Security

- **Room Database**: Local database with encryption support
- **DataStore Security**: Encrypted preferences storage
- **File System Security**: Sensitive files stored in app-private directories

#### Privacy Compliance

- **GDPR Compliance**: User data handling according to privacy regulations
- **Data Minimization**: Only necessary data is collected and stored
- **User Consent**: Clear privacy policy and terms of service

## Implementation Details

### Security Manager (`SecurityManager.kt`)

```kotlin
// AES-256-GCM encryption for sensitive data
fun encryptData(data: String): String
fun decryptData(encryptedData: String): String

// Secure token generation
fun generateSecureToken(): String

// Password hashing and verification
fun hashPassword(password: String): String
fun verifyPassword(password: String, storedHash: String): Boolean
```

### OAuth Manager (`OAuthManager.kt`)

```kotlin
// Google Sign-In integration
fun initializeGoogleSignIn(webClientId: String)
fun getGoogleSignInIntent(): Intent
suspend fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>)
```

### Secure Preferences (`SecurePreferences.kt`)

```kotlin
// Encrypted API key storage
suspend fun storeApiKey(keyName: String, apiKey: String)
suspend fun getApiKey(keyName: String): String?

// Encrypted token storage
suspend fun storeUserToken(token: String)
suspend fun getUserToken(): String?
```

## API Key Management

### Secure Storage

- API keys are never stored in plain text
- All keys are encrypted using AES-256-GCM
- Keys are stored in Android Keystore-backed secure storage

### Configuration

1. Create `app/src/main/res/values/api_keys.xml` with placeholder values
2. Replace placeholders with actual API keys
3. Never commit real API keys to version control

### Example Configuration

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="openweathermap_api_key">YOUR_ACTUAL_API_KEY</string>
    <string name="google_maps_api_key">YOUR_ACTUAL_API_KEY</string>
    <string name="google_oauth_web_client_id">YOUR_ACTUAL_CLIENT_ID</string>
</resources>
```

## Security Best Practices

### Development

1. **Never commit API keys**: Use placeholder values in version control
2. **Use HTTPS only**: All network requests must be secure
3. **Validate inputs**: Sanitize all user inputs
4. **Encrypt sensitive data**: Use encryption for all sensitive information
5. **Regular security audits**: Review code for security vulnerabilities

### Production

1. **Certificate pinning**: Implement proper certificate pinning
2. **ProGuard/R8**: Enable code obfuscation
3. **Network security config**: Configure network security policies
4. **App signing**: Use secure app signing keys
5. **Regular updates**: Keep dependencies updated

## Privacy Policy

### Data Collection

- **Step count data**: Stored locally, encrypted
- **Location data**: Only during active walk sessions
- **User preferences**: Stored locally, encrypted
- **Authentication data**: Minimal, encrypted storage

### Data Usage

- **Local processing**: All data processed locally when possible
- **No third-party sharing**: Data not shared with third parties
- **User control**: Users can delete all data
- **Transparency**: Clear data usage policies

## Compliance

### GDPR Compliance

- **Right to access**: Users can request their data
- **Right to deletion**: Users can delete all their data
- **Data portability**: Export functionality available
- **Consent management**: Clear consent mechanisms

### Android Security

- **Target SDK**: Latest Android version
- **Permissions**: Minimal required permissions
- **Background restrictions**: Proper background task handling
- **Foreground services**: Clear user notification

## Security Checklist

### Before Release

- [ ] All API keys are encrypted
- [ ] HTTPS enforcement is active
- [ ] Certificate pinning is implemented
- [ ] ProGuard/R8 obfuscation is enabled
- [ ] Network security config is set
- [ ] Privacy policy is updated
- [ ] Security audit is completed

### Regular Maintenance

- [ ] Dependencies are updated
- [ ] Security patches are applied
- [ ] API keys are rotated
- [ ] Security testing is performed
- [ ] Privacy compliance is verified

## Incident Response

### Security Breach Protocol

1. **Immediate Response**: Disable affected features
2. **Investigation**: Identify scope and impact
3. **Notification**: Inform users if necessary
4. **Remediation**: Fix vulnerabilities
5. **Prevention**: Implement additional safeguards

### Contact Information

For security issues, please contact:

- **Security Team**: security@steptracker.com
- **Bug Reports**: bugs@steptracker.com
- **Privacy Concerns**: privacy@steptracker.com

## Additional Resources

- [Android Security Best Practices](https://developer.android.com/topic/security)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-top-10/)
- [Google Play Security](https://support.google.com/googleplay/android-developer/answer/9859455)
- [GDPR Compliance Guide](https://gdpr.eu/)

---

**Last Updated**: January 2025
**Version**: 1.0
**Security Level**: High
