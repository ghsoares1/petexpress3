package com.petexpress.petexpress_site_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private final String frontendDirectory;

    public StaticResourceConfig(@Value("${app.frontend-directory:../..}") String frontendDirectory) {
        this.frontendDirectory = Path.of(frontendDirectory).toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/*.html", "/*.js", "/*.css")
                .addResourceLocations("classpath:/public/", frontendDirectory, "classpath:/static/");
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/public/img/", frontendDirectory + "img/", "classpath:/static/img/");
    }
}
