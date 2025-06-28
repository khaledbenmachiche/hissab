package com.hissab.client;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * SOAP Service Client for HISSAB application.
 * This implementation uses direct HTTP calls to communicate with the SOAP service.
 */
public class HissabServiceClient {
    
    private static final Logger logger = Logger.getLogger(HissabServiceClient.class.getName());
    private static final String SOAP_ENDPOINT = "http://localhost:8085/hissab/HissabService";
    private static final String TARGET_NAMESPACE = "http://service.hissab.com/";
    
    // OCR service instance
    private WebOCRService ocrService;
    
    public HissabServiceClient() throws Exception {
        logger.log(Level.INFO, "Initializing SOAP service client for endpoint: " + SOAP_ENDPOINT);
        // Initialize OCR service (lazy initialization - will be created when needed)
        this.ocrService = null;
        // Test connection
        testConnection();
    }
    
    private void testConnection() throws Exception {
        try {
            // Test with a simple health check - but avoid recursive call during initialization
            String soapRequest = createHealthCheckRequest();
            String soapResponse = sendSOAPRequest(soapRequest);
            extractResult(soapResponse);
            logger.log(Level.INFO, "SOAP service connection test successful");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to connect to SOAP service", e);
            throw new Exception("Cannot connect to web service at " + SOAP_ENDPOINT + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate result from string expression
     */
    public String calculateFromString(String expression) throws Exception {
        try {
            logger.log(Level.INFO, "Calling calculateFromString with expression: " + expression);
            
            String soapRequest = createCalculateFromStringRequest(expression);
            String soapResponse = sendSOAPRequest(soapRequest);
            String result = extractResult(soapResponse);
            
            logger.log(Level.INFO, "calculateFromString result: " + result);
            return result;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error calling calculateFromString", e);
            throw new Exception("Web service call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Health check
     */
    public String healthCheck() throws Exception {
        try {
            logger.log(Level.INFO, "Calling healthCheck");
            
            String soapRequest = createHealthCheckRequest();
            String soapResponse = sendSOAPRequest(soapRequest);
            String result = extractResult(soapResponse);
            
            logger.log(Level.INFO, "healthCheck result: " + result);
            return result;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error calling healthCheck", e);
            throw new Exception("Web service call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get or initialize the OCR service (lazy initialization)
     */
    private WebOCRService getOCRService() throws Exception {
        if (ocrService == null) {
            logger.log(Level.INFO, "Initializing Web OCR service");
            ocrService = new WebOCRService();
        }
        return ocrService;
    }

    /**
     * Calculate result from image using local OCR and then string calculation
     * This method processes the image locally with OCR, extracts the text,
     * and then sends it to the backend using calculateFromString
     */
    public String calculateFromImageWithLocalOCR(byte[] imageData, String filename) throws Exception {
        try {
            logger.log(Level.INFO, "Starting local OCR processing for image: " + filename);
            
            // Step 1: Extract text from image using local OCR
            String extractedText = getOCRService().extractTextFromImage(imageData, filename);
            logger.log(Level.INFO, "OCR extracted text: " + extractedText);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new Exception("No text could be extracted from the image");
            }
            
            // Step 2: Send extracted text to backend for calculation using string method
            logger.log(Level.INFO, "Sending extracted text to backend for calculation: " + extractedText);
            String result = calculateFromString(extractedText);
            
            logger.log(Level.INFO, "Local OCR + backend calculation result: " + result);
            return result;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in local OCR processing", e);
            throw new Exception("Local OCR processing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate result from image file using local OCR and then string calculation
     */
    public String calculateFromImageWithLocalOCR(java.io.File imageFile) throws Exception {
        try {
            logger.log(Level.INFO, "Starting local OCR processing for file: " + imageFile.getName());
            
            // Step 1: Extract text from image using local OCR
            String extractedText = getOCRService().extractTextFromImage(imageFile);
            logger.log(Level.INFO, "OCR extracted text: " + extractedText);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new Exception("No text could be extracted from the image");
            }
            
            // Step 2: Send extracted text to backend for calculation
            logger.log(Level.INFO, "Sending extracted text to backend for calculation: " + extractedText);
            String result = calculateFromString(extractedText);
            
            logger.log(Level.INFO, "Local OCR + backend calculation result: " + result);
            return result;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in local OCR processing", e);
            throw new Exception("Local OCR processing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create SOAP request for calculateFromString operation
     */
    private String createCalculateFromStringRequest(String expression) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
               "xmlns:tns=\"" + TARGET_NAMESPACE + "\">\n" +
               "  <soap:Body>\n" +
               "    <tns:calculateFromString>\n" +
               "      <tns:expression>" + escapeXml(expression) + "</tns:expression>\n" +
               "    </tns:calculateFromString>\n" +
               "  </soap:Body>\n" +
               "</soap:Envelope>";
    }
    
    /**
     * Create SOAP request for healthCheck operation
     */
    private String createHealthCheckRequest() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
               "xmlns:tns=\"" + TARGET_NAMESPACE + "\">\n" +
               "  <soap:Body>\n" +
               "    <tns:healthCheck/>\n" +
               "  </soap:Body>\n" +
               "</soap:Envelope>";
    }
    
    /**
     * Send SOAP request to the service
     */
    private String sendSOAPRequest(String soapRequest) throws Exception {
        URL url = new URL(SOAP_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set request properties
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        connection.setRequestProperty("SOAPAction", "");
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000); // 30 seconds
        connection.setReadTimeout(30000); // 30 seconds
        
        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            os.write(soapRequest.getBytes("UTF-8"));
        }
        
        // Read response
        int responseCode = connection.getResponseCode();
        InputStream inputStream = (responseCode == 200) ? 
            connection.getInputStream() : connection.getErrorStream();
            
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
        }
        
        if (responseCode != 200) {
            throw new Exception("HTTP " + responseCode + ": " + response.toString());
        }
        
        return response.toString();
    }
    
    /**
     * Extract result from SOAP response
     */
    private String extractResult(String soapResponse) {
        // Simple XML parsing to extract the return value
        String startTag = "<tns:return>";
        String endTag = "</tns:return>";
        
        int startIndex = soapResponse.indexOf(startTag);
        if (startIndex == -1) {
            // Try without namespace prefix
            startTag = "<return>";
            endTag = "</return>";
            startIndex = soapResponse.indexOf(startTag);
        }
        
        if (startIndex != -1) {
            startIndex += startTag.length();
            int endIndex = soapResponse.indexOf(endTag, startIndex);
            if (endIndex != -1) {
                return unescapeXml(soapResponse.substring(startIndex, endIndex));
            }
        }
        
        // If extraction failed, return error message
        return "Could not extract result from response: " + soapResponse;
    }
    
    /**
     * Escape XML special characters
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
    
    /**
     * Unescape XML special characters
     */
    private String unescapeXml(String text) {
        if (text == null) return "";
        return text.replace("&amp;", "&")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&quot;", "\"")
                  .replace("&apos;", "'");
    }
}
