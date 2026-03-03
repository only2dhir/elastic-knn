package com.devglan.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.RequiredArgsConstructor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Configuration
@RequiredArgsConstructor
public class ElasticSearchConfig {

    private final ElasticProperties elasticProperties;

    @Bean
    public ElasticsearchClient elasticsearchClient() throws Exception {

        // 1. Create SSLContext that trusts all certificates (bypass validation)
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (chain, authType) -> true) // trust all
                .build();

        // 2. Basic authentication
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(
                        elasticProperties.getUsername(),
                        elasticProperties.getPassword()
                )
        );

        // 3. Build RestClient
        RestClient restClient = RestClient.builder(HttpHost.create(elasticProperties.getUrl()))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE) // bypass hostname check
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setMaxConnTotal(100)
                        .setMaxConnPerRoute(20)
                )
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(5_000)
                        .setSocketTimeout(60_000)
                )
                .build();

        // 4. Create Elasticsearch client
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}