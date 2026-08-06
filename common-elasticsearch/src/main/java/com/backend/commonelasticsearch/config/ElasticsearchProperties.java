package com.backend.commonelasticsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Elasticsearch 접속/프로비저닝 설정. <br>
 * {@code indexAlias} 는 애플리케이션이 읽고/쓰는 <b>alias</b> 이름이다(예: {@code techblog-posts}). <br>
 * 실제 물리 인덱스는 프로비저너가 {@code <alias>-<yyMMddHHmmss>} 형식으로 생성하고 이 alias 로 연결한다.
 */
@ConfigurationProperties(prefix = "elasticsearch")
public record ElasticsearchProperties(String host,
                                      int port,
                                      String scheme,
                                      String username,
                                      String password,
                                      String fingerprint,
                                      String indexAlias,
                                      Provisioning provisioning) {

    /**
     * 인덱스 자동 생성(부트스트랩) 관련 설정.
     *
     * @param enabled            기동 시 alias 부재하면 물리 인덱스 + alias 자동 생성 여부(로컬 편의용)
     * @param definitionLocation 매핑/세팅 정의 JSON 리소스 위치(Spring {@code Resource} 표기)
     * @param numberOfReplicas   생성 시 적용할 복제본 수(로컬 단일 노드는 0 권장)
     */
    public record Provisioning(boolean enabled,
                               String definitionLocation,
                               int numberOfReplicas) {

    }
}
