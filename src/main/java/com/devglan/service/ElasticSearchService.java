package com.devglan.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import com.devglan.config.ElasticProperties;
import com.devglan.dto.request.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Service
public class ElasticSearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ElasticProperties elasticProperties;

    public List<SearchResult> fullTextSearch(String query) {
        try {
            SearchResponse<SearchResult> response = elasticsearchClient.search(s -> s
                            .index(elasticProperties.getIndices())
                            .query(fullTextQuery(query))
                            .size(20),
                    SearchResult.class
            );

            if (response == null || response.hits() == null || response.hits().hits() == null || response.hits().hits().isEmpty()) {
                return Collections.emptyList();
            }

            return response.hits()
                    .hits()
                    .stream()
                    .filter(hit -> hit != null && hit.source() != null)
                    .map(hit -> {
                        SearchResult result = hit.source();
                        // Defensive null handling
                        result.setIndex(hit.index());
                        result.setScore(hit.score() != null ? hit.score() : 0.0);

                        return result;
                    })
                    .toList();

        } catch (Exception e) {
            // Replace with proper structured logging
            // log.error("Elasticsearch fullTextSearch failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    public List<SearchResult> fuzzyTextQuerySearch(String query) {
        try {
            SearchResponse<SearchResult> response = elasticsearchClient.search(s -> s
                            .index(elasticProperties.getIndices())
                            .query(fuzzyTextQuery(query))
                            .size(20),
                    SearchResult.class
            );

            if (response == null || response.hits() == null || response.hits().hits() == null || response.hits().hits().isEmpty()) {
                return Collections.emptyList();
            }

            return response.hits()
                    .hits()
                    .stream()
                    .filter(hit -> hit != null && hit.source() != null)
                    .map(hit -> {
                        SearchResult result = hit.source();
                        // Defensive null handling
                        result.setIndex(hit.index());
                        result.setScore(hit.score() != null ? hit.score() : 0.0);

                        return result;
                    })
                    .toList();

        } catch (Exception e) {
            // Replace with proper structured logging
            // log.error("Elasticsearch fullTextSearch failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    public List<SearchResult> knnQuerySearch(String query) {
        try {
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(elasticProperties.getIndices())
                    .size(20)
                    //.minScore(elasticProperties.getMinScore())
                    .knn(knn -> knn
                            .field(elasticProperties.getField())
                            .k(20)
                            .numCandidates(100)
                            .queryVectorBuilder(qvb -> qvb
                                    .textEmbedding(te -> te
                                            .modelId(elasticProperties.getModelId())
                                            .modelText(query)
                                    )
                            )
                    )
                    .build();
            log.info(searchRequest.toString());
            SearchResponse<SearchResult> response =
                    elasticsearchClient.search(searchRequest, SearchResult.class);

            if (response == null || response.hits() == null || response.hits().hits() == null || response.hits().hits().isEmpty()) {
                return Collections.emptyList();
            }

            return response.hits()
                    .hits()
                    .stream()
                    .filter(hit -> hit != null && hit.source() != null)
                    .map(hit -> {
                        SearchResult result = hit.source();
                        result.setIndex(hit.index());
                        result.setScore(hit.score() != null ? hit.score() : 0.0);
                        return result;
                    })
                    .toList();

        } catch (Exception e) {
            // log.error("kNN search failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    //kNN + filter(bool(must: BM25 + fuzzy))
    public List<SearchResult> similaritySearch(String query) {

        try {

            SearchRequest.Builder builder = new SearchRequest.Builder()
                    .index(elasticProperties.getIndices())
                    .size(20)
                    .minScore(elasticProperties.getMinScore())
                    .knn(knn -> knn
                            .field(elasticProperties.getField())
                            .k(20)
                            .numCandidates(100)
                            .queryVectorBuilder(qvb -> qvb
                                    .textEmbedding(te -> te
                                            .modelId(elasticProperties.getModelId())
                                            .modelText(query)
                                    )
                            )
                            .filter(f -> f
                                    .bool(b -> b
                                            .must(fullTextQuery(query))
                                            .must(fuzzyTextQuery(query))
                                            .minimumShouldMatch(elasticProperties.getSimilarityThreshold())
                                    )
                            )
                    );

            SearchRequest searchRequest = builder.build();

            log.debug("Executing similarity search for query: {}", query);

            SearchResponse<SearchResult> response =
                    elasticsearchClient.search(searchRequest, SearchResult.class);

            if (response == null
                    || response.hits() == null
                    || response.hits().hits() == null
                    || response.hits().hits().isEmpty()) {
                return Collections.emptyList();
            }

            return response.hits()
                    .hits()
                    .stream()
                    .filter(hit -> hit != null && hit.source() != null)
                    .map(hit -> {
                        SearchResult result = hit.source();
                        result.setIndex(hit.index());
                        result.setScore(hit.score() != null ? hit.score() : 0.0);
                        return result;
                    })
                    .toList();

        } catch (Exception e) {
            log.error("Similarity search failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    private Function<Query.Builder, ObjectBuilder<Query>> fullTextQuery(String text) {
        return q -> q.multiMatch(m -> m
                .query(text)
                .fields("title^3", "body^2", "text^2")
                .type(TextQueryType.MostFields)
        );
    }

    private Function<Query.Builder, ObjectBuilder<Query>> fuzzyTextQuery(String text) {
        return q -> q.multiMatch(m -> m
                .query(text)
                .fields("title", "body", "text")
                .fuzziness("1")
                .prefixLength(3)
                .analyzer("standard")
                .type(TextQueryType.MostFields)
        );
    }
}
