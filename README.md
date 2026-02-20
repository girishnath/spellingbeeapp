# Spelling Bee Study App

A comprehensive study application designed to help students prepare for Spelling Bee competitions. The app provides various tools for studying, testing, and reviewing vocabulary and spelling.

## Features

### 📚 Study Zone
Practice words from different difficulty levels:
- **One Bee**: Beginner level words.
- **Two Bee**: Intermediate level words.
- **Three Bee**: Advanced level words.
- **New Words**: Words introduced in the current year.

In Study Mode, you can:
- Listen to the word pronunciation.
- Read the definition.
- Type in the spelling and get immediate feedback.

### 🃏 Flash Cards
A visual way to review words:
- **Card Interface**: Flip cards to see the word on one side and its definition/audio on the other.
- **Self-Paced**: Navigate through words at your own speed using previous/next buttons.
- **Audio Support**: Listen to pronunciation before flipping the card.

### 📝 Tests & Review
- **Normal Test**: 50 minutes, 45 questions. Simulates a real test environment.
- **Quick Test**: 5 minutes, 10 questions. Great for quick practice sessions.
- **Spelling Only**: A test mode focused solely on spelling accuracy.
- **My Mistakes**: Review words you've missed in previous tests to reinforce learning.

## Running Locally

To run the application on your local machine:

1.  **Prerequisites**: Ensure you have Java 17+ installed.
2.  **Start the Server**:
    ```bash
    ./mvnw spring-boot:run
    ```
    Alternatively, if you have Maven installed globally:
    ```bash
    mvn spring-boot:run
    ```
3.  **Access the App**: Open your browser and go to `http://localhost:8080`.

## Deployment

The application is configured for deployment on **Google Cloud Run**.

### Build and Deploy
1.  **Build the Container Image**:
    ```bash
    gcloud builds submit --tag gcr.io/[PROJECT-ID]/spellingbee-app
    ```
2.  **Deploy to Cloud Run**:
    ```bash
    gcloud run deploy spellingbee-app \
      --image gcr.io/[PROJECT-ID]/spellingbee-app \
      --platform managed \
      --region [REGION] \
      --allow-unauthenticated
    ```

### Configuration
The `cloudrun.yaml` file contains the deployment configuration, including:
- **Autoscaling**: Scales to zero when idle (minScale: 0) and limits max instances to 2.
- **Resources**: configured with 512Mi memory and 1 CPU for cost optimization.
