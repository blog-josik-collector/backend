package com.backend.integratedworker.collectsource.service;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.integratedworker.collectsource.repository.CollectSourceQueryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class CollectSourceService {

    private final CollectSourceQueryRepository queryRepository;

    @Transactional(readOnly = true)
    public List<CollectSource> getActiveCronCollectSources() {
        return queryRepository.findActiveCronCollectSources();
    }
}
