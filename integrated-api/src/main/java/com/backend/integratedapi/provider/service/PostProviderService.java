package com.backend.integratedapi.provider.service;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.commondataaccess.service.validator.ValidationFlow;
import com.backend.integratedapi.provider.repository.PostProviderQueryRepository;
import com.backend.integratedapi.provider.repository.PostProviderRepository;
import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import com.backend.integratedapi.provider.service.validator.PostProviderValidator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class PostProviderService {

    private final PostProviderRepository postProviderRepository;
    private final PostProviderQueryRepository queryRepository;

    public PostProviderDto create(PostProviderDto postProviderDto) {
        ValidationFlow.start(postProviderDto)
                      .next(PostProviderValidator.validateName())
                      .next(PostProviderValidator.validateBaseUrl())
                      .next(PostProviderValidator.validateDescription())
                      .end();

        PostProviderValidator.verifyDuplicateName(postProviderDto.name(), postProviderRepository::existsByName);

        PostProvider postProvider = PostProvider.builder()
                                                .name(postProviderDto.name())
                                                .baseUrl(postProviderDto.baseUrl())
                                                .description(postProviderDto.description())
                                                .isUsed(true)
                                                .build();

        PostProvider savedPostProvider = postProviderRepository.save(postProvider);

        return PostProviderDto.from(savedPostProvider);
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<PostProviderDto> getPostProviders(int page, int size) {
        return queryRepository.fetchPostProviders(page, size)
                              .map(PostProviderDto::from);
    }

    @Transactional(readOnly = true)
    public PostProviderDto getPostProviderDto(UUID id) {
        PostProviderValidator.validateId(id);

        return PostProviderDto.from(getPostProvider(id));
    }

    @Transactional(readOnly = true)
    public PostProvider getPostProvider(UUID id) {
        PostProviderValidator.validateId(id);

        return PostProviderValidator.getPostProviderOrThrow(id, queryRepository::fetchOneById);
    }

    public void update(PostProviderDto postProviderDto) {
        ValidationFlow.start(postProviderDto)
                      .next(PostProviderValidator.validateId())
                      .end();

        PostProvider provider = getPostProvider(postProviderDto.id());

        if (StringUtils.isNotBlank(postProviderDto.description())) {
            provider.updateDescription(postProviderDto.description());
        }

        if (StringUtils.isNotBlank(postProviderDto.baseUrl())) {
            provider.updateBaseUrl(postProviderDto.baseUrl());
        }

        provider.updateUsed(postProviderDto.isUsed());
    }

    public void delete(UUID id) {
        PostProviderValidator.validateId(id);
        PostProvider postProvider = getPostProvider(id);

        postProvider.updateUsed(false); // 삭제하는 postProvider는 isUsed 필드 false로 처리
        postProvider.delete();
    }
}
