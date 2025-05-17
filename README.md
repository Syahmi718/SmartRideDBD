# SmartRide Driving Behavior Detection App

An Android application that detects aggressive driving behavior in real-time using sensors and machine learning.

## Direct Sensor Integration

This branch implements direct sensor data collection from the device's accelerometer and gyroscope, eliminating the need for the external SensorLogger app. The key features of this integration are:

- Direct access to accelerometer and gyroscope sensors
- Real-time sensor data processing
- Higher sampling rate for better prediction accuracy
- Streamlined user experience with fewer dependencies

## How it Works

1. The app collects accelerometer and gyroscope data directly from the device sensors
2. The data is processed to extract features like magnitude and jerk
3. A TensorFlow Lite model analyzes the data to predict driving behavior
4. Users receive alerts for aggressive driving patterns

## Requirements

- Android device with accelerometer and gyroscope sensors
- Android API level 24+ (Android 7.0 Nougat or higher)
- Location services enabled

## License

[MIT License](LICENSE) 