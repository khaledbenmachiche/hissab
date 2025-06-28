package com.hissab.ejb;

import jakarta.ejb.Local;

/**
 * Local business interface for CalculEJB
 */
@Local
public interface CalculEJBLocal {
    
    /**
     * Evaluates a mathematical expression string
     * @param expression The mathematical expression (e.g., "2+3*4")
     * @return The result as a string, or error message if evaluation fails
     */
    String evaluateExpression(String expression);
    
    /**
     * Validates if the given expression is mathematically valid
     * @param expression The expression to validate
     * @return true if valid, false otherwise
     */
    boolean validateExpression(String expression);
}