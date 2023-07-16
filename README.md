GlassGPT
GlassGPT is an innovative Android application for Google Glass that interacts with OpenAI's advanced language model, GPT-3.5-turbo. Users can make requests and get text predictions from the model directly on their Google Glass device.

Features
Seamless integration with Google Glass Explorer Edition.
Fast and efficient interaction with OpenAI's GPT-3.5-turbo.
On-demand generation and display of AI responses.
Installation
To clone and run this application, you'll need Git and Android Studio installed on your computer. From your command line:

bash
Code kopieren

# Clone this repository
$ git clone https://github.com/t3mr0i/ChatGPTGlass.git

# Go into the repository
$ cd ChatGPTGlass
You also need to have your OpenAI API key. Create a Secrets.java file in the src folder of your project and add your OpenAI API key.

java
Code kopieren

public class Secrets {
    public static final String OPEN_AI_KEY="your_api_key_goes_here";
}
After setting the API key, import the project into Android Studio. Then you can build the project and run it on your Google Glass device!

Usage
To interact with the AI, press and hold the touchpad on Google Glass, then speak your command or request. The AI response will then be fetched and displayed directly on the Google Glass screen.

Contributing
We welcome contributions! For details on how to contribute, please refer to the guidelines outlined in the CONTRIBUTING.md file.

License
This project is licensed under the terms of the MIT license. Additional details can be found in the LICENSE file.

Contact
For any inquiries or issues, please open an issue in this GitHub repository.
