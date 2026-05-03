package com.backend.integratedapi.collectsourcepost.service;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.integratedapi.collectsourcepost.repository.CollectSourcePostQueryRepository;
import com.backend.integratedapi.collectsourcepost.service.dto.CollectSourcePostDto;
import com.backend.integratedapi.collectsourcepost.service.validator.CollectSourcePostValidator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class CollectSourcePostService {

    private final CollectSourcePostQueryRepository queryRepository;

    @Transactional(readOnly = true)
    public CollectSourcePostDto getCollectSourcePostDto(UUID id) {
        CollectSourcePostValidator.validateId(id);
        CollectSourcePost collectSourcePost = getCollectSourcePost(id);

        return CollectSourcePostDto.from(collectSourcePost);
    }

    @Transactional(readOnly = true)
    public CollectSourcePost getCollectSourcePost(UUID id) {
        return CollectSourcePostValidator.getCollectSourcePostOrThrow(id, queryRepository::fetchOneById);
    }
}
