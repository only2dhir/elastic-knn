package com.devglan.controller;

import com.devglan.dto.request.SearchResult;
import com.devglan.service.ElasticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/")
public class SearchController {

    @Autowired
    private ElasticSearchService service;

    @PostMapping
    public ResponseEntity<List<SearchResult>> searchElastic(@RequestBody String text) {
        return ResponseEntity.of(Optional.ofNullable(service.knnQuerySearch(text)));
    }
}
