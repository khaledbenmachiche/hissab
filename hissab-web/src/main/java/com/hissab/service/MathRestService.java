package com.hissab.service;

import com.hissab.ejb.CalculEJBLocal;
import com.hissab.ejb.TraceEJBLocal;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.PostConstruct;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * REST service for mathematical calculations
 */
@Path("/math")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MathRestService {
    
    private static final Logger logger = Logger.getLogger(MathRestService.class.getName());
    
    @EJB
    private CalculEJBLocal calculEJB;
    
    @EJB
    private TraceEJBLocal traceEJB;
    
    @PostConstruct
    public void init() {
        logger.log(Level.INFO, "MathRestService initialized for GlassFish 7");
    }
    
    /**
     * Calculate mathematical expression from text
     */
    @POST
    @Path("/calculate")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response calculateExpression(String expression) {
        logger.log(Level.INFO, "REST: Received calculation request for expression: " + expression);
        
        try {
            // Validate input
            if (expression == null || expression.trim().isEmpty()) {
                String errorMsg = "Error: Expression cannot be empty";
                logger.log(Level.WARNING, errorMsg);
                return Response.status(Response.Status.BAD_REQUEST).entity(errorMsg).build();
            }
            
            // Check if EJB is injected
            if (calculEJB == null) {
                String errorMsg = "Error: CalculEJB is not available";
                logger.log(Level.SEVERE, errorMsg);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMsg).build();
            }
            
            // Calculate the result using CalculEJB
            String result = calculEJB.evaluateExpression(expression);
            
            // Log the trace using TraceEJB (optional - don't fail if database is not available)
            try {
                if (traceEJB != null) {
                    traceEJB.logTrace(expression.trim(), result);
                    logger.log(Level.INFO, "Successfully logged trace for expression: " + expression);
                }
            } catch (Exception e) {
                // Log warning but don't fail the calculation
                logger.log(Level.WARNING, "Failed to log trace to database (calculation still succeeded): " + e.getMessage());
            }
            
            logger.log(Level.INFO, "Successfully calculated: " + expression + " = " + result);
            return Response.ok(result).build();
            
        } catch (Exception e) {
            String errorMsg = "Error: Calculation failed - " + e.getMessage();
            logger.log(Level.SEVERE, "Error processing calculation request", e);
            
            // Try to log the error trace
            try {
                if (traceEJB != null) {
                    traceEJB.logTrace(expression != null ? expression.trim() : "null", errorMsg);
                }
            } catch (Exception traceError) {
                logger.log(Level.SEVERE, "Failed to log error trace", traceError);
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMsg).build();
        }
    }
    
    /**
     * Health check endpoint
     */
    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public Response healthCheck() {
        try {
            // Check if EJB is injected
            if (calculEJB == null) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                              .entity("Service health check failed: CalculEJB is not available")
                              .build();
            }
            
            // Test basic functionality
            String testResult = calculEJB.evaluateExpression("1+1");
            logger.log(Level.INFO, "Health check successful");
            String healthMsg = "Service is healthy. Test calculation: 1+1 = " + testResult;
            return Response.ok(healthMsg).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Health check failed", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                          .entity("Service health check failed: " + e.getMessage())
                          .build();
        }
    }
    
    /**
     * Get service information
     */
    @GET
    @Path("/info")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getServiceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("HISSAB Math Learning REST API\n");
        info.append("Available endpoints:\n");
        info.append("POST /api/math/calculate - Calculate mathematical expression (text/plain)\n");
        info.append("GET /api/math/health - Health check\n");
        info.append("GET /api/math/info - This information\n");
        info.append("\nExample usage:\n");
        info.append("curl -X POST -H \"Content-Type: text/plain\" -d \"2+3*4\" http://localhost:8085/hissab-web-1.0-SNAPSHOT/api/math/calculate\n");
        
        return Response.ok(info.toString()).build();
    }
}
