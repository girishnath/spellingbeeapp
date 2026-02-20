package me.girish.spellingbeeapp.service;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TtsService {

    public byte[] generateAudio(String text) throws IOException {
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

            // Select the voice request parameters
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("en-US")
                    .setName("en-US-Neural2-F") // High quality female voice
                    .build();

            // Select the type of audio file you want returned
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .setSpeakingRate(0.9) // Slightly slower for clarity
                    .build();

            // Perform the text-to-speech request on the text input with the selected voice
            // parameters and audio file type
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();

            return audioContents.toByteArray();
        }
    }
}
