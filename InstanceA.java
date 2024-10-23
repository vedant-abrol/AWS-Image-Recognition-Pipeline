package edu.njit;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.regions.Region;
import java.util.List;

public class InstanceA {
    private static final String BUCKET_NAME = "cs643-njit-project1";
    private static final String SQS_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/830164316541/image-processing-queue";

    public static void main(String[] args) {
        // Create S3, SQS, and Rekognition Clients
        S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
        RekognitionClient rekognition = RekognitionClient.builder().region(Region.US_EAST_1).build();
        SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();

        // List the first 10 images in the S3 bucket
        List<String> images = s3.listObjectsV2Paginator(b -> b.bucket(BUCKET_NAME)).contents().stream()
                .limit(10)
                .map(obj -> obj.key())
                .toList();

        for (String imageKey : images) {
            // Perform car detection on each image
            boolean carDetected = detectCarInImage(rekognition, imageKey);

            if (carDetected) {
                // If car detected, send image index to SQS
                sendMessageToSQS(sqs, imageKey);
            }
        }

        // Signal termination by sending -1 to the queue
        sendMessageToSQS(sqs, "-1");
    }

    // Function to detect cars in an image using AWS Rekognition
    private static boolean detectCarInImage(RekognitionClient rekognition, String imageKey) {
        DetectLabelsRequest request = DetectLabelsRequest.builder()
                .image(Image.builder().s3Object(S3Object.builder().bucket(BUCKET_NAME).name(imageKey).build()).build())
                .minConfidence(80f)
                .build();

        DetectLabelsResponse response = rekognition.detectLabels(request);
        for (Label label : response.labels()) {
            if (label.name().equalsIgnoreCase("Car") && label.confidence() > 80) {
                System.out.println("Car detected in image: " + imageKey);
                return true;
            }
        }
        return false;
    }

    // Function to send a message to the SQS queue
    private static void sendMessageToSQS(SqsClient sqs, String message) {
        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(SQS_QUEUE_URL)
                .messageBody(message)
                .build();
        sqs.sendMessage(sendMsgRequest);
        System.out.println("Sent message: " + message + " to SQS");
    }
}
