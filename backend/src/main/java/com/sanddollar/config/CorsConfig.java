package com.sanddollar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
  @Value("${cors.allowed-origins:}")
  private String allowedOriginsCsv;

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowCredentials(true);
    List<String> origins = StringUtils.hasText(allowedOriginsCsv)
        ? Arrays.stream(allowedOriginsCsv.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList()
        : List.of("http://localhost:5173", "http://localhost:5177");
    c.setAllowedOrigins(origins);
    c.addAllowedHeader("*");
    c.addAllowedMethod("*");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return new CorsFilter(source);
  }
}
