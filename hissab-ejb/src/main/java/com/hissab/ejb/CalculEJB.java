package com.hissab.ejb;

import jakarta.ejb.Stateless;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.logging.Logger;
import java.util.logging.Level;

@Stateless
public class CalculEJB implements CalculEJBLocal {
    
    private static final Logger logger = Logger.getLogger(CalculEJB.class.getName());
    private final ScriptEngine engine;
    
    public CalculEJB() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("JavaScript");
        
        if (this.engine == null) {
            logger.log(Level.WARNING, "JavaScript engine not available - will use fallback evaluator");
        } else {
            logger.log(Level.INFO, "JavaScript engine initialized successfully");
        }
    }
    
    /**
     * Evaluates a mathematical expression string
     * @param expression The mathematical expression (e.g., "2+3*4")
     * @return The result as a string, or error message if evaluation fails
     */
    public String evaluateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            logger.log(Level.WARNING, "Empty or null expression provided");
            return "Error: Empty expression";
        }
        
        try {
            // Clean the expression - remove spaces and validate characters
            String cleanExpression = cleanExpression(expression);
            
            if (!isValidExpression(cleanExpression)) {
                logger.log(Level.WARNING, "Invalid expression: " + expression);
                return "Error: Invalid expression";
            }
            
            // Try using JavaScript engine first, fallback to simple evaluator
            Object result;
            if (engine != null) {
                logger.log(Level.INFO, "Using JavaScript engine to evaluate: " + cleanExpression);
                result = engine.eval(cleanExpression);
            } else {
                logger.log(Level.INFO, "Using fallback evaluator for: " + cleanExpression);
                result = evaluateSimpleExpression(cleanExpression);
            }
            
            // Format the result
            if (result instanceof Double) {
                Double doubleResult = (Double) result;
                // If it's a whole number, display as integer
                if (doubleResult == Math.floor(doubleResult)) {
                    return String.valueOf(doubleResult.intValue());
                }
            }
            
            logger.log(Level.INFO, "Successfully evaluated: " + expression + " = " + result);
            return result.toString();
            
        } catch (ScriptException e) {
            logger.log(Level.SEVERE, "Error evaluating expression: " + expression, e);
            return "Error: Invalid mathematical expression";
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error evaluating expression: " + expression, e);
            return "Error: Calculation failed";
        }
    }
    
    /**
     * Cleans the expression by removing unnecessary characters
     */
    private String cleanExpression(String expression) {
        return expression.replaceAll("\\s+", ""); // Remove all whitespace
    }
    
    /**
     * Validates that the expression contains only allowed mathematical characters
     */
    private boolean isValidExpression(String expression) {
        // Allow digits, operators, parentheses, and decimal points
        return expression.matches("[0-9+\\-*/().]+");
    }
    
    /**
     * Alternative method for more complex validation if needed
     */
    public boolean validateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        String clean = cleanExpression(expression);
        
        // Check for balanced parentheses
        int openParens = 0;
        for (char c : clean.toCharArray()) {
            if (c == '(') openParens++;
            else if (c == ')') openParens--;
            if (openParens < 0) return false; // More closing than opening
        }
        
        return openParens == 0 && isValidExpression(clean);
    }
    
    /**
     * Advanced expression evaluator that handles complex mathematical expressions
     * with proper operator precedence and parentheses
     */
    private double evaluateSimpleExpression(String expression) throws Exception {
        return evaluateExpression(expression, 0).value;
    }
    
    /**
     * Recursive descent parser for mathematical expressions
     * Supports: +, -, *, /, parentheses, and floating point numbers
     */
    private ParseResult evaluateExpression(String expression, int startIndex) throws Exception {
        return parseAddSubtract(expression, startIndex);
    }
    
    /**
     * Parse addition and subtraction (lowest precedence)
     */
    private ParseResult parseAddSubtract(String expression, int index) throws Exception {
        ParseResult left = parseMultiplyDivide(expression, index);
        
        while (left.nextIndex < expression.length()) {
            char op = expression.charAt(left.nextIndex);
            if (op == '+' || op == '-') {
                ParseResult right = parseMultiplyDivide(expression, left.nextIndex + 1);
                if (op == '+') {
                    left = new ParseResult(left.value + right.value, right.nextIndex);
                } else {
                    left = new ParseResult(left.value - right.value, right.nextIndex);
                }
            } else {
                break;
            }
        }
        
        return left;
    }
    
    /**
     * Parse multiplication and division (higher precedence)
     */
    private ParseResult parseMultiplyDivide(String expression, int index) throws Exception {
        ParseResult left = parseFactor(expression, index);
        
        while (left.nextIndex < expression.length()) {
            char op = expression.charAt(left.nextIndex);
            if (op == '*' || op == '/') {
                ParseResult right = parseFactor(expression, left.nextIndex + 1);
                if (op == '*') {
                    left = new ParseResult(left.value * right.value, right.nextIndex);
                } else {
                    if (right.value == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    left = new ParseResult(left.value / right.value, right.nextIndex);
                }
            } else {
                break;
            }
        }
        
        return left;
    }
    
    /**
     * Parse factors: numbers, parenthesized expressions, and unary minus
     */
    private ParseResult parseFactor(String expression, int index) throws Exception {
        // Skip whitespace
        while (index < expression.length() && Character.isWhitespace(expression.charAt(index))) {
            index++;
        }
        
        if (index >= expression.length()) {
            throw new IllegalArgumentException("Unexpected end of expression");
        }
        
        char ch = expression.charAt(index);
        
        // Handle unary minus
        if (ch == '-') {
            ParseResult result = parseFactor(expression, index + 1);
            return new ParseResult(-result.value, result.nextIndex);
        }
        
        // Handle unary plus
        if (ch == '+') {
            return parseFactor(expression, index + 1);
        }
        
        // Handle parenthesized expressions
        if (ch == '(') {
            ParseResult result = parseAddSubtract(expression, index + 1);
            
            // Skip whitespace
            while (result.nextIndex < expression.length() && 
                   Character.isWhitespace(expression.charAt(result.nextIndex))) {
                result.nextIndex++;
            }
            
            if (result.nextIndex >= expression.length() || 
                expression.charAt(result.nextIndex) != ')') {
                throw new IllegalArgumentException("Missing closing parenthesis");
            }
            
            return new ParseResult(result.value, result.nextIndex + 1);
        }
        
        // Handle numbers
        if (Character.isDigit(ch) || ch == '.') {
            return parseNumber(expression, index);
        }
        
        throw new IllegalArgumentException("Unexpected character: " + ch);
    }
    
    /**
     * Parse a number from the expression starting at the given index
     */
    private ParseResult parseNumber(String expression, int index) throws Exception {
        int start = index;
        
        // Parse integer part
        while (index < expression.length() && Character.isDigit(expression.charAt(index))) {
            index++;
        }
        
        // Parse decimal part if present
        if (index < expression.length() && expression.charAt(index) == '.') {
            index++;
            while (index < expression.length() && Character.isDigit(expression.charAt(index))) {
                index++;
            }
        }
        
        if (start == index) {
            throw new IllegalArgumentException("Expected number");
        }
        
        String numberStr = expression.substring(start, index);
        double value = Double.parseDouble(numberStr);
        
        return new ParseResult(value, index);
    }
    
    /**
     * Helper class to hold parsing results
     */
    private static class ParseResult {
        final double value;
        int nextIndex;
        
        ParseResult(double value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }

}
