package com.hissab.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;
import java.util.HashSet;

/**
 * JAX-RS Application configuration for GlassFish 7
 */
@ApplicationPath("/api")
public class HissabRestApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(com.hissab.service.MathRestService.class);
        return classes;
    }
}
