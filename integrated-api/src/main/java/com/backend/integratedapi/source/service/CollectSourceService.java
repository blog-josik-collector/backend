package com.backend.integratedapi.source.service;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.commondataaccess.persistence.source.CollectSource;
import com.backend.commondataaccess.persistence.source.enums.ScheduleType;
import com.backend.commondataaccess.service.validator.ValidationFlow;
import com.backend.integratedapi.provider.service.PostProviderService;
import com.backend.integratedapi.source.repository.CollectSourceQueryRepository;
import com.backend.integratedapi.source.repository.CollectSourceRepository;
import com.backend.integratedapi.source.service.dto.CollectSourceDto;
import com.backend.integratedapi.source.service.validator.CollectSourceValidator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class CollectSourceService {

    private final CollectSourceRepository collectSourceRepository;
    private final CollectSourceQueryRepository queryRepository;
    private final PostProviderService postProviderService;

    public CollectSourceDto create(CollectSourceDto collectSourceDto) {
        ValidationFlow.start(collectSourceDto)
                      .next(CollectSourceValidator.validateProviderId())
                      .next(CollectSourceValidator.validateScheduleType())
                      .end();

        CollectSourceValidator.validateScheduleTypeAndCronExpressionPair(collectSourceDto.scheduleType(), collectSourceDto.cronExpression());

        PostProvider postProvider = postProviderService.getPostProvider(collectSourceDto.providerId());

        CollectSource collectSource = CollectSource.builder()
                                                   .postProvider(postProvider)
                                                   .url(collectSourceDto.url())
                                                   .scheduleType(collectSourceDto.scheduleType())
                                                   .cronExpression(collectSourceDto.cronExpression())
                                                   .isUsed(collectSourceDto.isUsed())
                                                   .build();

        CollectSource savedCollectSource = collectSourceRepository.save(collectSource);

        return CollectSourceDto.from(savedCollectSource);
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<CollectSourceDto> getCollectSources(int page, int size) {
        return queryRepository.fetchCollectSources(page, size)
                              .map(CollectSourceDto::from);
    }

    @Transactional(readOnly = true)
    public CollectSourceDto getCollectSourceDto(UUID id) {
        CollectSourceValidator.validateId(id);
        return CollectSourceDto.from(getCollectSource(id));
    }

    @Transactional(readOnly = true)
    public CollectSource getCollectSource(UUID id) {
        return CollectSourceValidator.getCollectSourceOrThrow(id, queryRepository::fetchOneById);
    }

    public void update(CollectSourceDto collectSourceDto) {
        ValidationFlow.start(collectSourceDto)
                      .next(CollectSourceValidator.validateId())
                      .end();

        CollectSource collectSource = getCollectSource(collectSourceDto.id());

        if (StringUtils.isNotBlank(collectSourceDto.url())) {
            collectSource.updateUrl(collectSourceDto.url());
        }

        if (ObjectUtils.isNotEmpty(collectSourceDto.scheduleType())) {
            ScheduleType scheduleType = collectSourceDto.scheduleType();
            CollectSourceValidator.validateScheduleTypeAndCronExpressionPair(scheduleType, collectSourceDto.cronExpression());
            collectSource.updateScheduleType(collectSourceDto.scheduleType());

            if (scheduleType.equals(ScheduleType.MANUAL)) {
                collectSource.updateCronExpression(StringUtils.EMPTY);
            }
        }

        if (StringUtils.isNotBlank(collectSourceDto.cronExpression())) {
            collectSource.updateCronExpression(collectSourceDto.cronExpression());
        }

        collectSource.updateUsed(collectSourceDto.isUsed());
    }

    public void delete(UUID id) {
        CollectSourceValidator.validateId(id);
        CollectSource collectSource = getCollectSource(id);

        collectSource.updateUsed(false); // 삭제하는 collectSource는 isUsed 필드 false로 처리
        collectSource.delete();
    }
}
