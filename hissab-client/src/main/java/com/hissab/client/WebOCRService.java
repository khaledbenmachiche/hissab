package com.hissab.client;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Web-based OCR service using OCR.space API
 * This service extracts text from images using a free online OCR service
 */
public class WebOCRService {
    
    private static final Logger logger = Logger.getLogger(WebOCRService.class.getName());
    
    // OCR.space API endpoint (free tier)
    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";
    
    // Free API key for OCR.space (you can get your own at https://ocr.space/ocrapi)
    private static final String API_KEY = "helloworld"; // Free tier key
    
    /**
     * Extract text from image file using web-based OCR
     * @param imageFile The image file to process
     * @return Extracted text from the image
     * @throws Exception if OCR processing fails
     */
    public String extractTextFromImage(File imageFile) throws Exception {
        logger.log(Level.INFO, "Starting OCR processing for image: " + imageFile.getName());
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Create HTTP POST request
            HttpPost httpPost = new HttpPost(OCR_API_URL);
            
            // Build multipart form data
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addPart("file", new FileBody(imageFile));
            builder.addPart("apikey", new StringBody(API_KEY, ContentType.TEXT_PLAIN));
            builder.addPart("language", new StringBody("eng", ContentType.TEXT_PLAIN));
            builder.addPart("isOverlayRequired", new StringBody("false", ContentType.TEXT_PLAIN));
            builder.addPart("detectOrientation", new StringBody("false", ContentType.TEXT_PLAIN));
            builder.addPart("scale", new StringBody("true", ContentType.TEXT_PLAIN));
            builder.addPart("OCREngine", new StringBody("2", ContentType.TEXT_PLAIN));
            
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            
            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.log(Level.INFO, "OCR API response received");
                
                // Parse JSON response
                return parseOCRResponse(responseBody);
            }
        }
    }
    
    /**
     * Extract text from image byte array using web-based OCR
     * @param imageData The image data as byte array
     * @param filename The filename for the temporary file
     * @return Extracted text from the image
     * @throws Exception if OCR processing fails
     */
    public String extractTextFromImage(byte[] imageData, String filename) throws Exception {
        // Create temporary file
        File tempFile = File.createTempFile("ocr_temp_", "_" + filename);
        tempFile.deleteOnExit();
        
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(imageData);
        }
        
        // Process the temporary file
        String result = extractTextFromImage(tempFile);
        
        // Clean up
        tempFile.delete();
        
        return result;
    }
    
    /**
     * Parse the JSON response from OCR.space API
     * @param responseBody The JSON response body
     * @return Extracted text
     * @throws Exception if parsing fails
     */
    private String parseOCRResponse(String responseBody) throws Exception {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            
            // Check if OCR was successful
            if (!jsonResponse.optBoolean("IsErroredOnProcessing", true)) {
                JSONArray parsedResults = jsonResponse.getJSONArray("ParsedResults");
                
                if (parsedResults.length() > 0) {
                    JSONObject firstResult = parsedResults.getJSONObject(0);
                    String extractedText = firstResult.getString("ParsedText");
                    
                    // Clean up the extracted text
                    extractedText = cleanExtractedText(extractedText);
                    
                    logger.log(Level.INFO, "OCR extraction successful. Text length: " + extractedText.length());
                    return extractedText;
                }
            } else {
                // Check for error message
                String errorMessage = jsonResponse.optString("ErrorMessage", "Unknown OCR error");
                JSONArray errorDetails = jsonResponse.optJSONArray("ErrorDetails");
                if (errorDetails != null && errorDetails.length() > 0) {
                    errorMessage += ": " + errorDetails.getString(0);
                }
                throw new Exception("OCR processing failed: " + errorMessage);
            }
            
            throw new Exception("No text could be extracted from the image");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing OCR response", e);
            throw new Exception("Failed to parse OCR response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clean and format the extracted text for mathematical expression processing
     * @param rawText The raw text from OCR
     * @return Cleaned text suitable for mathematical processing
     */
    private String cleanExtractedText(String rawText) {
        if (rawText == null) return "";
        
        // Remove line breaks and extra whitespace
        String cleaned = rawText.replaceAll("\\r\\n|\\r|\\n", " ");
        cleaned = cleaned.replaceAll("\\s+", " ");
        cleaned = cleaned.trim();
        
        // Replace common OCR mistakes in mathematical expressions
        cleaned = cleaned.replace("×", "*");  // multiplication symbol
        cleaned = cleaned.replace("÷", "/");  // division symbol
        cleaned = cleaned.replace("−", "-");  // minus sign
        cleaned = cleaned.replace("—", "-");  // em dash
        cleaned = cleaned.replace("–", "-");  // en dash
        cleaned = cleaned.replace("'", "");   // remove quotes
        cleaned = cleaned.replace("\"", "");  // remove quotes
        cleaned = cleaned.replace("`", "");   // remove backticks
        
        // Remove common OCR artifacts
        cleaned = cleaned.replace(" ", "");   // Remove all spaces for math expressions
        
        logger.log(Level.INFO, "Cleaned OCR text: '" + cleaned + "'");
        return cleaned;
    }
    
    /**
     * Test the OCR service with a simple check
     * @return true if the service is available
     */
    public boolean testService() {
        try {
            // Create a simple test to check if the service is reachable
            // We'll just check if we can make a request (even if it fails due to no image)
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(OCR_API_URL);
                
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.addPart("apikey", new StringBody(API_KEY, ContentType.TEXT_PLAIN));
                
                HttpEntity multipart = builder.build();
                httpPost.setEntity(multipart);
                
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    // If we get any response, the service is reachable
                    return response.getStatusLine().getStatusCode() != 0;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "OCR service test failed", e);
            return false;
        }
    }
}
