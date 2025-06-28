package com.hissab.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "trace")
public class Trace {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "expression", nullable = false, length = 255)
    private String expression;
    
    @Column(name = "result", nullable = false, length = 255)
    private String result;
    
    @Column(name = "timestamp", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    
    // Default constructor
    public Trace() {
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }
    
    // Constructor with parameters
    public Trace(String expression, String result) {
        this();
        this.expression = expression;
        this.result = result;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getExpression() {
        return expression;
    }
    
    public void setExpression(String expression) {
        this.expression = expression;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Trace{" +
                "id=" + id +
                ", expression='" + expression + '\'' +
                ", result='" + result + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
