package edu.njit;

import java.util.List;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.DetectTextRequest;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.TextDetection;
import software.amazon.awssdk.regions.Region; // Add this line
import java.io.FileWriter;
import java.io.IOException;


public class InstanceB {
    private static final String BUCKET_NAME = "cs643-njit-project1";
    private static final String SQS_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/830164316541/image-processing-queue";
    private static final String OUTPUT_FILE = "output.txt";

    public static void main(String[] args) {
        // Create S3, SQS, and Rekognition Clients
        S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
        RekognitionClient rekognition = RekognitionClient.builder().region(Region.US_EAST_1).build();
        SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();

        try (FileWriter writer = new FileWriter(OUTPUT_FILE, true)) {
            boolean running = true;

            while (running) {
                // Receive messages from SQS
                ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                        .queueUrl(SQS_QUEUE_URL)
                        .maxNumberOfMessages(1)
                        .build();
                List<Message> messages = sqs.receiveMessage(receiveMessageRequest).messages();

                for (Message message : messages) {
                    String imageKey = message.body();

                    if (imageKey.equals("-1")) {
                        running = false; // End the loop
                        System.out.println("Termination signal received.");
                        break;
                    }  

                    // Perform text detection
                    String detectedText = detectTextInImage(rekognition, imageKey);

                    if (detectedText != null) {
                        // Append the result to output.txt
                        writer.write("Image: " + imageKey + " Text: " + detectedText + "\n");
                        System.out.println("Detected text in image: " + imageKey + " Text: " + detectedText);
                    }

                    // Delete the message from the queue
                    deleteMessageFromSQS(sqs, message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to detect text in an image using AWS Rekognition
    private static String detectTextInImage(RekognitionClient rekognition, String imageKey) {
        DetectTextRequest request = DetectTextRequest.builder()
                .image(Image.builder().s3Object(S3Object.builder().bucket(BUCKET_NAME).name(imageKey).build()).build())
                .build();

        DetectTextResponse response = rekognition.detectText(request);
        for (TextDetection text : response.textDetections()) {
            if (text.confidence() > 80) {
                return text.detectedText();
            }
        }
        return null;
    }

    // Function to delete a message from the SQS queue
    private static void deleteMessageFromSQS(SqsClient sqs, Message message) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(SQS_QUEUE_URL)
                .receiptHandle(message.receiptHandle())
                .build();
        sqs.deleteMessage(deleteMessageRequest);
        System.out.println("Deleted message from SQS");
    }
}
