package hw1;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

public class LambdaInvoker {

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            // gradle run --args="<function> <k> [path/to/input.json]"
            System.err.println("Usage: LambdaInvoker <function> <k> [path/to/input.json]");
            System.exit(1);
        }

        // For AWS "function" should be: arn:aws:lambda:region:xxxxxxxxxxx:function:functionName
        String function = args[0];
        int k = Integer.parseInt(args[1]);
        // Get input from file
        String input = "{}";
        if (args.length >= 3) {
            Path inputPath = new File(args[2]).toPath();
            if (Files.exists(inputPath)) {
                input = Files.readString(inputPath);
            }
        }

        // Load credentials from ~/.aws/credentials
        Path credentials = Paths.get(System.getProperty("user.home"), ".aws", "credentials");
        if (!Files.exists(credentials)) {
            System.err.println("Missing credentials file: " + credentials.toString());
            System.exit(1);
        }

        Properties properties = new Properties();
        properties.load(new FileInputStream(credentials.toString()));
        if (!properties.containsKey("aws_access_key_id")
                || !properties.containsKey("aws_secret_access_key")
                || !properties.containsKey("aws_session_token")) {
            System.err.println("Missing credentials: aws_access_key_id, aws_secret_access_key or aws_session_token");
            System.exit(1);
        }

        final String AWS_ACCESS_KEY_ID = properties.getProperty("aws_access_key_id");
        final String AWS_SECRET_ACCESS_KEY = properties.getProperty("aws_secret_access_key");
        final String SESSION_TOKEN = properties.getProperty("aws_session_token");

        // Create AWS lambda client
        AWSCredentials sessCredentials = new BasicSessionCredentials(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, SESSION_TOKEN);
        AWSLambda lambda = AWSLambdaClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(sessCredentials))
                .build();

        // Create request with function and input
        InvokeRequest request = new InvokeRequest()
                .withFunctionName(function)
                .withPayload(input)
                .withInvocationType(InvocationType.RequestResponse); // Wait for response (synchronous)

        // Invoke the lambda function k times and measure elapsed time
        long totalElapsed = 0;
        for (int i = 1; i <= k; i++) {

            Instant start = Instant.now();
            InvokeResult result = lambda.invoke(request);
            Instant end = Instant.now();

            assert result.getStatusCode() == 200;

            long elapsed = Duration.between(start, end).toMillis();
            totalElapsed += elapsed;
            System.out.println("Iteration " + i + ": " + elapsed + "ms");

            // DEBUG
            //String resultJson = new String(result.getPayload().array(), Charset.defaultCharset());
            //System.out.println(resultJson);
        }

        System.out.println("Completed! Total elapsed time: " + totalElapsed + "ms");
    }
}
