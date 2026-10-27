package com.skillswap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves uploaded files (currently avatars) from disk.
 *
 * The URL lives under /api so the Vite dev proxy and any future reverse proxy
 * pick it up with the rest of the API. Nothing stored here is executable.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebConfig(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(root.toUri().toString())
                .setCachePeriod(3600);
    }
}
