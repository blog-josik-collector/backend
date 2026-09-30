package com.backend.integratedapi.elasticsearchindex.bootstrap;

import com.backend.commonelasticsearch.provision.ElasticsearchIndexProvisioner;
import com.backend.commonelasticsearch.provision.ProvisionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 자동 부트스트랩. 기동 직후 alias 가 없으면 물리 인덱스 + alias 를 생성하고, 이미 있으면 아무것도 하지 않는다. <br>
 * 부트스트랩 소유 서비스를 한 곳(integrated-api)으로 제한해 다중 서비스 동시 기동 시 인덱스 중복 생성 경쟁을 피한다. <br>
 * {@code elasticsearch.provisioning.enabled=true} 일 때만 활성(prod 프로필은 미설정 시 false, 운영은 신규 환경 초기 세팅용으로 .env 에서 활성화). <br>
 * ES 미기동 등으로 실패해도 애플리케이션 기동은 계속되도록 예외를 삼킨다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "elasticsearch.provisioning", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ElasticsearchAutoBootstrapRunner implements ApplicationRunner {

    private final ElasticsearchIndexProvisioner provisioner;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ProvisionResult result = provisioner.bootstrapIfAbsent();
            if (result.created()) {
                log.info("[ES] auto bootstrap: created index={} alias={}", result.index(), result.alias());
            } else {
                log.info("[ES] auto bootstrap: alias={} already exists (index={})", result.alias(), result.index());
            }
        } catch (Exception e) {
            log.error("[ES] auto bootstrap failed (continuing startup). index may be missing — "
                    + "check ES connectivity/auth or run manage-index.sh bootstrap", e);
        }
    }
}
