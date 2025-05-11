# SmartRideDBD - Driving Behavior Detection

SmartRideDBD is an Android application that monitors driving behavior to promote safer driving habits. The app uses sensor data to detect aggressive driving patterns and provides real-time feedback to help drivers improve their behavior.

## Features

- Real-time speed monitoring using GPS
- Aggressive driving detection
- Driving session management with START/STOP functionality
- Detailed driving history and statistics
- Real-time alerts for unsafe driving behavior
- Beautiful UI with animations and modern design

## Latest Release: v1.2.0 - Driving Session Management Update

### What's New
- Added proper driving session management with START/STOP functionality
- Fixed issue with premature alerts before driving sessions start
- Improved UI with contextual elements that respond to driving state
- Reorganized Monitor Prediction button to appear only during active driving sessions

### Bug Fixes
1. Fixed issue where prediction warnings appeared before driving session started
2. Fixed START/STOP DRIVING button functionality - now properly changes state and appearance
3. "Smart Ride Active" notification now only appears when a driving session is active

### Technical Improvements
- Added DrivingSessionHelper for better SQLite database management
- Implemented proper state tracking for driving sessions
- Improved session saving with speed tracking and statistics

### User Experience Enhancements
- More intuitive UI with buttons that appear only when relevant
- Clearer indication of current driving session status
- Better organization of related functionality

## Requirements

- Android 8.0 (API level 26) or higher
- Location permissions
- Notification permissions (for alerts)

## Installation

Download and install the APK from the releases section or build the project using Android Studio.

## Usage

1. Launch the app
2. Click "START DRIVING" to begin monitoring
3. Drive safely and receive feedback on your driving behavior
4. Click "STOP DRIVING" when your journey is complete
5. Review your driving history in the logs 