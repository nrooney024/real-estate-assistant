<h1 align="center">
  <a href="https://chromewebstore.google.com/detail/real-estate-assistant/ikbmgehhghnkohaoehjlmfniegppchnd">Real Estate Assistant</a>
</h1>

<p align="center">
  <img src="https://github.com/nrooney024/real-estate-assistant/blob/main/chrome-extension/chrome-button-logo.png?raw=true" alt="Real Estate Assistant" title="Real Estate Assistant" width="250"/>
</p>

A Chrome Extension designed to provide users with valuable information about amenities near any given address. With a simple interface, users can quickly discover nearby supermarkets, gyms, coffee shops, schools, parks, and banks, making it easier to learn about the area surrounding a particular address. [Click here to download the Chrome Extension!](https://chromewebstore.google.com/detail/real-estate-assistant/ikbmgehhghnkohaoehjlmfniegppchnd)


<p align="center">
  <a href="#key-features">Key Features</a> •
  <a href="#how-to-use">How To Use</a> •
  <a href="#download">Download</a> •
  <a href="#credits">Credits</a>
</p>

<p align="center">
  <img src="https://github.com/nrooney024/real-estate-assistant/blob/main/chrome-extension/real-estate-sssistant-recording.gif?raw=true" alt="Screen recording" width="250"/>
</p>

## Key Features

* Enter an address and find the closest:
  - Supermarkets
  - Gyms
  - Coffee shops
  - Schools
  - Parks
  - Banks  

## How It Works

The extension interfaces with a custom-built Java backend comprising several components to process address information, interact with external APIs for geolocation and nearby places, and calculate distances to amenities. Here's a brief overview of the backend architecture:

- **RequestHandler**: Parses incoming requests from the extension and prepares responses.
- **AddressExtractor**: Extracts and encodes addresses from request payloads.
- **OpenCageGeocoder**: Uses the OpenCage API to convert addresses into geographical coordinates (latitude and longitude).
- **OverPass**: Queries the Overpass API to find amenities near the provided coordinates.
- **HaversineCalculator**: Calculates distances between geographical coordinates to identify the closest amenities.

Together, these components ensure the extension can provide users with accurate, up-to-date information about what's around any given address.

## Setup and Running Locally

To run the Real Estate Assistant backend locally, you'll need:

- Java JDK 11 or newer
- [Maven](https://maven.apache.org/) installed on your computer
- Environment variables for API keys (e.g., OpenCage API key)

The backend is built with Maven, making it easy to compile and run with just a few commands.

### Running the Server

First, ensure you have set the API key in your environment. This is required for the server to interact with the OpenCage Geocoder API:

```bash
export API_KEY=your_api_key # Use set instead of export on Windows
```

Replace your_api_key with your actual OpenCage API key. Then, navigate to the project directory and run the following Maven commands:

```bash
# Clean and compile the project
mvn clean compile

# Execute the main class to start the server
mvn exec:java -Dexec.mainClass="com.myapp.SimpleServer"
```

## Credits

This software uses the following open source packages:

- OpenCage Geocoding API: https://opencagedata.com/
- Overpass API: https://overpass-api.de/ 
