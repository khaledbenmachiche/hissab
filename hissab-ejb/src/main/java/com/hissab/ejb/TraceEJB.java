package com.hissab.ejb;

import com.hissab.entity.Trace;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

@Stateless
public class TraceEJB implements TraceEJBLocal {
    
    private static final Logger logger = Logger.getLogger(TraceEJB.class.getName());
    
    @PersistenceContext(unitName = "hissabPU")
    private EntityManager entityManager;
    
    /**
     * Logs a calculation trace to the database
     * @param expression The mathematical expression
     * @param result The calculation result
     * @return The persisted Trace entity
     */
    public Trace logTrace(String expression, String result) {
        try {
            Trace trace = new Trace(expression, result);
            entityManager.persist(trace);
            entityManager.flush(); // Ensure it's immediately written to DB
            
            logger.log(Level.INFO, "Successfully logged trace: " + trace);
            return trace;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error logging trace for expression: " + expression, e);
            throw new RuntimeException("Failed to log calculation trace", e);
        }
    }
    
    /**
     * Retrieves all traces from the database
     * @return List of all traces
     */
    public List<Trace> getAllTraces() {
        try {
            TypedQuery<Trace> query = entityManager.createQuery(
                "SELECT t FROM Trace t ORDER BY t.timestamp DESC", Trace.class);
            
            List<Trace> traces = query.getResultList();
            logger.log(Level.INFO, "Retrieved " + traces.size() + " traces from database");
            return traces;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving traces from database", e);
            throw new RuntimeException("Failed to retrieve calculation traces", e);
        }
    }
    
    /**
     * Retrieves traces by expression pattern
     * @param expressionPattern The expression pattern to search for
     * @return List of matching traces
     */
    public List<Trace> getTracesByExpression(String expressionPattern) {
        try {
            TypedQuery<Trace> query = entityManager.createQuery(
                "SELECT t FROM Trace t WHERE t.expression LIKE :pattern ORDER BY t.timestamp DESC", 
                Trace.class);
            query.setParameter("pattern", "%" + expressionPattern + "%");
            
            List<Trace> traces = query.getResultList();
            logger.log(Level.INFO, "Retrieved " + traces.size() + " traces matching pattern: " + expressionPattern);
            return traces;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving traces by expression pattern: " + expressionPattern, e);
            throw new RuntimeException("Failed to retrieve traces by expression", e);
        }
    }
    
    /**
     * Retrieves the most recent N traces
     * @param limit Maximum number of traces to retrieve
     * @return List of recent traces
     */
    public List<Trace> getRecentTraces(int limit) {
        try {
            TypedQuery<Trace> query = entityManager.createQuery(
                "SELECT t FROM Trace t ORDER BY t.timestamp DESC", Trace.class);
            query.setMaxResults(limit);
            
            List<Trace> traces = query.getResultList();
            logger.log(Level.INFO, "Retrieved " + traces.size() + " recent traces");
            return traces;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving recent traces", e);
            throw new RuntimeException("Failed to retrieve recent traces", e);
        }
    }
    
    /**
     * Counts the total number of traces in the database
     * @return Total count of traces
     */
    public long getTraceCount() {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(t) FROM Trace t", Long.class);
            
            Long count = query.getSingleResult();
            logger.log(Level.INFO, "Total trace count: " + count);
            return count;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error counting traces", e);
            throw new RuntimeException("Failed to count traces", e);
        }
    }
    
    /**
     * Deletes all traces from the database (for testing/cleanup purposes)
     * @return Number of deleted traces
     */
    public int deleteAllTraces() {
        try {
            int deletedCount = entityManager.createQuery("DELETE FROM Trace t").executeUpdate();
            logger.log(Level.INFO, "Deleted " + deletedCount + " traces from database");
            return deletedCount;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deleting all traces", e);
            throw new RuntimeException("Failed to delete traces", e);
        }
    }
}
