# GlassGPT


GlassGPT is an innovative Android application for Google Glass that interacts with OpenAI's advanced language model, GPT-4.1-nano. Users can make requests and get text predictions from the model directly on their Google Glass device. The application now utilizes GPT-4.1-nano, which offers a perfect balance of intelligence, speed, and cost-effectiveness while still supporting image processing capabilities.

Features
Seamless integration with Google Glass Explorer Edition.
Fast and efficient interaction with OpenAI's GPT-4.1-nano model.
On-demand generation and display of AI responses.
Support for image processing capabilities.
Optimized for quick response times and reduced API costs.
Installation
To clone and run this application, you'll need Git and Android Studio installed on your computer. From your command line:
```
# Clone this repository
$ git clone https://github.com/t3mr0i/ChatGPTGlass.git

# Go into the repository
$ cd ChatGPTGlass

# Clone this repository
$ git clone https://github.com/t3mr0i/ChatGPTGlass.git

# Go into the repository
$ cd ChatGPTGlass
```
You need to provide your OpenAI API key to use this app. The app supports two methods of providing your API key:

## Option 1: Manual Setup (RECOMMENDED)
**This is the most reliable method for Google Glass Explorer Edition and should be your first choice:**

1. Create a new file called `Secrets.java` in the `app/src/main/java/com/example/chatgptglass/` directory
2. Add the following code to the file, replacing `your_openai_api_key_here` with your actual OpenAI API key:

```java
package com.example.chatgptglass;

public class Secrets {
    public static final String API_KEY = "your_openai_api_key_here";
}
```

3. Save the file and build the app

**Note:** The `Secrets.java` file is in `.gitignore` for security reasons, so it won't be committed to version control. Each developer must create their own `Secrets.java` file locally.

## Option 2: QR Code Scanning (ALTERNATIVE)
**Note: Due to Google Glass Explorer Edition camera limitations, this method may be less reliable:**

1. Create a QR code containing ONLY your OpenAI API key (no additional text or formatting)
   - Use any QR code generator website and print the QR code or display it on another device
   - Make the QR code as large as possible for better scanning results
2. Launch the app without setting up the Secrets.java file
3. The app will automatically show a QR scanner screen
4. Tap on the screen to start scanning
5. Position your Google Glass at the optimal distance from the QR code:
   - Hold the QR code approximately 8-12 inches (20-30cm) from Glass
   - Ensure good lighting conditions
   - Keep Glass steady and the QR code centered in view
   - Since Glass lacks autofocus, finding the right distance is critical
6. If scanning fails repeatedly, consider using Option 1 instead

**Important:** The app will always prioritize checking for a manually configured API key in `Secrets.java` (Option 1) before showing the QR scanner, so you only need to use one method or the other. The `Secrets.java` approach is recommended for its reliability, especially on Google Glass Explorer Edition.

After setting the API key, import the project into Android Studio. Then you can build the project and run it on your Google Glass device!

# Usage
To interact with the AI, press and hold the touchpad on Google Glass, then speak your command or request. The AI response will then be fetched and displayed directly on the Google Glass screen.

# Contributing
We welcome contributions! For details on how to contribute, please refer to the guidelines outlined in the CONTRIBUTING.md file.

# License
This project is licensed under the terms of the MIT license. Additional details can be found in the LICENSE file.

# Contact
For any inquiries or issues, please open an issue in this GitHub repository.
