package com.devglan.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.RequiredArgsConstructor;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ElasticSearchConfig {

    private final ElasticProperties elasticProperties;

    @Bean
    public ElasticsearchClient elasticsearchClient() {

        Header[] defaultHeaders = new Header[]{
                new BasicHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + elasticProperties.getApiKey())
        };

        RestClient restClient = RestClient.builder(
                        HttpHost.create(elasticProperties.getUrl())
                )
                .setDefaultHeaders(defaultHeaders)
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(5_000)
                                .setSocketTimeout(60_000)
                )
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setMaxConnTotal(100)
                                .setMaxConnPerRoute(20)
                )
                .build();

        ElasticsearchTransport transport =
                new RestClientTransport(restClient, new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }
}