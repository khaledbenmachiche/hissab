package com.hissab.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(
    name = "HissabService",
    targetNamespace = "http://service.hissab.com/"
)
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface HissabService {
    
    /**
     * Calculates the result of a mathematical expression provided as a string
     * 
     * @param expression The mathematical expression to evaluate (e.g., "2+3*4")
     * @return The result of the calculation as a string
     */
    @WebMethod
    String calculateFromString(@WebParam(name = "expression") String expression);
    
    /**
     * Calculates the result of a mathematical expression extracted from an image using OCR
     * Note: This is a simulated OCR implementation that returns a fixed expression
     * 
     * @param image The image data as byte array
     * @return The OCR result and calculation as a string
     */
    @WebMethod
    String calculateFromImage(@WebParam(name = "image") byte[] image);
    
    /**
     * Health check method to verify the service is operational
     * 
     * @return Health status message
     */
    @WebMethod
    String healthCheck();
}
