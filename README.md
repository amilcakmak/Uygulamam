# AIGarage - AI-Powered Image Processing App

AIGarage is an Android application that leverages artificial intelligence for advanced image processing and segmentation. The app uses TensorFlow Lite models and Google Cloud services to provide powerful image manipulation capabilities.

## Features

- **AI Image Segmentation**: Advanced image segmentation using DeepLabV3 Xception model
- **Camera Integration**: Direct camera capture with real-time processing
- **Gallery Support**: Process images from device gallery
- **Multi-language Support**: Turkish and English language support
- **Dark/Light Theme**: Customizable UI themes
- **Google Cloud Integration**: Vertex AI services for enhanced processing
- **Hugging Face Integration**: Access to various AI models

## Prerequisites

Before running this project, you need to set up the following:

### 1. Google Cloud Setup
- Create a Google Cloud Project
- Enable Vertex AI API
- Create a service account and download credentials
- Enable Google OAuth for authentication

### 2. Hugging Face Setup
- Create a Hugging Face account
- Generate an API token

### 3. AdMob Setup (Optional)
- Create an AdMob account
- Get your app ID

## Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/amilcakmak/Uygulamam.git
   cd Uygulamam
   ```

2. **Set up credentials**
   
   Copy the template files and fill in your actual credentials:
   
   ```bash
   # Copy credential templates
   cp app/src/main/assets/vertex-ai-credentials.json.template app/src/main/assets/vertex-ai-credentials.json
   cp app/src/main/assets/client_secret_template.json app/src/main/assets/client_secret_281318707341-qtvopdhbapime8qet11msunkkln96ogo.apps.googleusercontent.com.json
   cp keystore-template.properties keystore.properties
   ```

3. **Configure gradle.properties**
   
   Edit `gradle.properties` and replace placeholder values:
   ```properties
   KEYSTORE_PASSWORD=YOUR_ACTUAL_PASSWORD
   KEY_ALIAS=YOUR_ACTUAL_ALIAS
   KEY_PASSWORD=YOUR_ACTUAL_PASSWORD
   HUGGING_FACE_TOKEN=YOUR_ACTUAL_TOKEN
   ```

4. **Create keystore file**
   
   Generate a keystore file for app signing:
   ```bash
   keytool -genkey -v -keystore aigarage-release.keystore -alias aigarage_key -keyalg RSA -keysize 2048 -validity 10000
   ```

5. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```

## Project Structure

```
app/
├── src/main/
│   ├── assets/                 # AI models and credentials
│   │   ├── deeplabv3-xception65.tflite
│   │   ├── mask.tflite
│   │   └── *.json.template     # Credential templates
│   ├── java/com/rootcrack/aigarage/
│   │   ├── api/               # API interfaces
│   │   ├── components/        # Reusable UI components
│   │   ├── data/             # Data models and preferences
│   │   ├── navigation/       # Navigation components
│   │   ├── screens/          # App screens
│   │   ├── services/         # External services
│   │   ├── utils/            # Utility classes
│   │   └── viewmodel/        # ViewModels
│   └── res/                  # Resources
└── build.gradle.kts          # App-level build configuration
```

## Key Components

### AI Processing
- **DeepLabV3XceptionSegmenter**: Handles image segmentation using TensorFlow Lite
- **HuggingFaceService**: Manages interactions with Hugging Face API

### UI Components
- **CustomBottomNavBar**: Custom bottom navigation
- **AnimatedButton**: Animated button component
- **PhotoConfirmationDialog**: Image confirmation dialog

### Screens
- **HomeScreen**: Main app interface
- **CameraScreen**: Camera capture functionality
- **GalleryScreen**: Gallery image selection
- **AIProcessingScreen**: AI processing interface
- **SettingsScreen**: App settings and preferences

## Configuration

### Environment Variables
The app uses several environment variables defined in `gradle.properties`:

- `KEYSTORE_*`: Keystore configuration for app signing
- `HUGGING_FACE_TOKEN`: Hugging Face API token
- `ADMOB_APP_ID_*`: AdMob configuration

### API Configuration
- **Vertex AI**: Configured via `vertex-ai-credentials.json`
- **Google OAuth**: Configured via `client_secret_*.json`
- **Hugging Face**: Configured via API token

## Security Notes

⚠️ **Important**: Never commit actual credentials to version control. The project includes:
- Template files for credentials
- `.gitignore` configured to exclude sensitive files
- Placeholder values in configuration files

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please open an issue on GitHub.

## Acknowledgments

- TensorFlow Lite for mobile AI processing
- Google Cloud Vertex AI for cloud services
- Hugging Face for AI model access
- Android Jetpack Compose for modern UI development
