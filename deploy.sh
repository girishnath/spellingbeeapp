#!/bin/bash

# Deployment script for GCP Cloud Run
# This script builds and deploys the Spelling Bee app to Cloud Run

set -e

PROJECT_ID="portfolio-458705"
SERVICE_NAME="spellingbee-app"
REGION="us-central1"  # Change to your preferred region

echo "🚀 Starting deployment to Cloud Run..."

# Set the project
gcloud config set project $PROJECT_ID

# Enable required APIs
echo "📦 Enabling required GCP APIs..."
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable texttospeech.googleapis.com

# Build and submit to Cloud Build
echo "🔨 Building Docker image..."
gcloud builds submit --tag gcr.io/$PROJECT_ID/$SERVICE_NAME

# Deploy to Cloud Run
echo "🌐 Deploying to Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image gcr.io/$PROJECT_ID/$SERVICE_NAME \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --min-instances 0 \
  --max-instances 2 \
  --memory 512Mi \
  --cpu 1 \
  --cpu-throttling \
  --port 8080

echo "✅ Deployment complete!"
echo ""
echo "Your app is now live at:"
gcloud run services describe $SERVICE_NAME --platform managed --region $REGION --format 'value(status.url)'
