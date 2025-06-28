package com.hissab.client;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Simple test client to verify SOAP integration
 */
public class TestClient {
    
    private static final Logger logger = Logger.getLogger(TestClient.class.getName());
    
    public static void main(String[] args) {
        System.out.println("=== HISSAB SOAP Client Test ===");
        
        try {
            // Initialize SOAP client
            System.out.println("Initializing SOAP service client...");
            HissabServiceClient client = new HissabServiceClient();
            System.out.println("✓ SOAP client initialized successfully");
            
            // Test 1: Health Check
            System.out.println("\n--- Test 1: Health Check ---");
            String healthResult = client.healthCheck();
            System.out.println("Health Check Result: " + healthResult);
            
            // Test 2: Calculate simple expression
            System.out.println("\n--- Test 2: Calculate Expression ---");
            String expression = "2 + 3 * 4";
            System.out.println("Expression: " + expression);
            String calcResult = client.calculateFromString(expression);
            System.out.println("Calculation Result: " + calcResult);
            
            // Test 3: Calculate another expression
            System.out.println("\n--- Test 3: Calculate Complex Expression ---");
            String complexExpression = "(5 + 3) * 2 - 1";
            System.out.println("Expression: " + complexExpression);
            String complexResult = client.calculateFromString(complexExpression);
            System.out.println("Calculation Result: " + complexResult);
            
            System.out.println("\n=== All tests completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
