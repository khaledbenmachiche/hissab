-- HISSAB Database Initialization Script
-- This script creates the database schema and initial data

-- Use the hissab_db database
USE hissab_db;

-- Create the trace table for logging mathematical expressions and results
CREATE TABLE IF NOT EXISTS trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expression VARCHAR(255) NOT NULL,
    result VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_expression (expression),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert some sample data for testing
INSERT INTO trace (expression, result, timestamp) VALUES
('2+2', '4', NOW() - INTERVAL 1 HOUR),
('5*3', '15', NOW() - INTERVAL 30 MINUTE),
('10-3', '7', NOW() - INTERVAL 15 MINUTE),
('[OCR] 2+3', '5', NOW() - INTERVAL 5 MINUTE);

-- Create a view for recent calculations
CREATE OR REPLACE VIEW recent_calculations AS
SELECT 
    id,
    expression,
    result,
    timestamp,
    CASE 
        WHEN expression LIKE '[OCR]%' THEN 'OCR'
        ELSE 'Manual'
    END as input_type
FROM trace 
ORDER BY timestamp DESC 
LIMIT 100;

-- Grant necessary permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON trace TO 'hissab_user'@'%';
GRANT SELECT ON recent_calculations TO 'hissab_user'@'%';

-- Show table structure
DESCRIBE trace;

-- Show initial data
SELECT COUNT(*) as initial_record_count FROM trace;
