package com.devglan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "devglan.elastic")
public class ElasticProperties {

    private String url;
    //private String apiKey;
    private String username;
    private String password;
    private String similarityThreshold;
    private Double minScore;
    private String modelId;
    private String field;
    private List<String> indices;
}
