package com.backend.integratedapi.indexedpost.service;

import com.backend.integratedapi.indexedpost.repository.IndexedPostElasticsearchRepository;
import com.backend.integratedapi.indexedpost.service.dto.IndexedPost;
import com.backend.integratedapi.indexedpost.service.validator.IndexedPostValidator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IndexedPostService {

    private final IndexedPostElasticsearchRepository indexedPostElasticsearchRepository;

    public IndexedPost getIndexedPost(UUID id) {
        return IndexedPostValidator.getIndexedPostOrThrow(id, indexedPostElasticsearchRepository::fetchOneById);
    }
}
