package com.hissab.ejb;

import com.hissab.entity.Trace;
import jakarta.ejb.Local;
import java.util.List;

/**
 * Local business interface for TraceEJB
 */
@Local
public interface TraceEJBLocal {
    
    /**
     * Logs a calculation trace to the database
     * @param expression The mathematical expression
     * @param result The calculation result
     * @return The persisted Trace entity
     */
    Trace logTrace(String expression, String result);
    
    /**
     * Retrieves all traces from the database
     * @return List of all traces
     */
    List<Trace> getAllTraces();
    
    /**
     * Retrieves traces by expression pattern
     * @param expressionPattern The expression pattern to search for
     * @return List of matching traces
     */
    List<Trace> getTracesByExpression(String expressionPattern);
    
    /**
     * Retrieves the most recent N traces
     * @param limit Maximum number of traces to retrieve
     * @return List of recent traces
     */
    List<Trace> getRecentTraces(int limit);
    
    /**
     * Counts the total number of traces in the database
     * @return Total count of traces
     */
    long getTraceCount();
    
    /**
     * Deletes all traces from the database (for testing/cleanup purposes)
     * @return Number of deleted traces
     */
    int deleteAllTraces();
}