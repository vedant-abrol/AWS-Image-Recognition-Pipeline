---

# AWS Image Recognition Pipeline

This project is a cloud-based image recognition pipeline built using AWS services. It utilizes two EC2 instances, S3 storage, SQS messaging, and AWS Rekognition for detecting cars and performing text recognition on images. The system processes images in parallel using two EC2 instances running Java applications on Amazon Linux, and the results are output to a file containing images with both cars and text.

## Features

- **Car Detection**: Instance A reads images from an S3 bucket, detects cars with confidence > 80% using AWS Rekognition, and stores image indices in an SQS queue.
- **Text Recognition**: Instance B reads image indices from SQS, performs text recognition on images with cars, and saves results to an output file.
- **Parallel Processing**: Both EC2 instances work in parallel, ensuring efficient processing of images.

## AWS Services Used

- **EC2**: Two EC2 instances for running Java applications.
- **S3**: Storing images for processing.
- **SQS**: Message queue service to handle communication between EC2 instances.
- **AWS Rekognition**: Detects objects (cars) and performs text recognition.
- **EBS**: Stores the output file on Instance B.

## Prerequisites

To run this project, you need:

- An AWS account with access to EC2, S3, SQS, and Rekognition services.
- Java installed on your local machine.
- Maven for dependency management (refer to `pom.xml`).
- An SSH client to connect to EC2 instances (e.g., PuTTY or terminal).
  
Ensure you have the necessary access keys and temporary credentials for AWS services.

## Setup Instructions

### 1. Launch Two EC2 Instances

1. Go to the AWS Management Console and launch two **Amazon Linux** EC2 instances.
2. Use the same **.pem** key to SSH into both instances.
3. Configure security groups to allow access through **SSH, HTTP, and HTTPS** only.
4. Install **Java** and **Maven** on both instances.

### 2. Set Up AWS SDK and Credentials

1. Configure AWS credentials for each instance by following the instructions [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html). Temporary credentials may be used from the AWS Learner Lab.
2. Set up environment variables for AWS access keys (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`).

### 3. Clone the Project and Install Dependencies

1. SSH into each EC2 instance.
2. Clone this GitHub repository into both instances:

   ```bash
   git clone https://github.com/your-repo/aws-image-recognition-pipeline.git
   ```

3. Navigate to the project folder and build the project using Maven:

   ```bash
   cd aws-image-recognition-pipeline
   mvn clean package
   ```

4. Ensure the `dependency-reduced-pom.xml` file is correctly set up for dependencies.

### 4. Running the Application

- **Instance A (Car Detection)**:
  1. Run `InstanceA.java` on the first EC2 instance. It reads images from the S3 bucket and uses AWS Rekognition to detect cars with confidence > 80%.
  2. Detected image indices are sent to an SQS queue.

   ```bash
   java -cp target/cloud_assignment-1.jar InstanceA
   ```

- **Instance B (Text Recognition)**:
  1. Run `InstanceB.java` on the second EC2 instance. It reads image indices from the SQS queue and performs text recognition on the images.
  2. Results are written to `output.txt` containing image indices and recognized text.

   ```bash
   java -cp target/cloud_assignment-1.jar InstanceB
   ```

### 5. Output File

- The result file `output.txt` is saved on Instance B’s EBS volume. It contains the indices of images that have both cars and text, along with the recognized text from the image.

## AWS Rekognition Configuration

Ensure you have the AWS Rekognition SDK properly configured to detect both objects (cars) and text in the images. You may want to adjust the confidence threshold for both recognition tasks by modifying the relevant sections in `InstanceA.java` and `InstanceB.java`.

```java
rekognitionClient.detectLabels(detectLabelsRequest.withMinConfidence(80F));
```

## Error Handling

- The program is designed to handle unexpected situations, such as missing images or communication failures between instances.
- If Instance A finishes processing all images, it sends a `-1` message to SQS to signal Instance B that processing is complete.

## Additional Notes

- **Security**: Ensure you terminate both EC2 instances when the task is complete to avoid unnecessary charges.
- **AWS Cost Management**: Be mindful of AWS service usage, especially S3 storage, SQS, and EC2 instances, as they can incur charges if not managed properly.
- **Logging**: You can enable logging to troubleshoot any issues by redirecting console output to log files.

## Future Improvements

- **Scalability**: Extend the pipeline to handle more images by adding more EC2 instances.
- **Improved Performance**: Use AWS Lambda functions to further optimize the performance of the image processing tasks.
- **Automated Deployment**: Consider using AWS CloudFormation or Terraform to automate the deployment of this project.
