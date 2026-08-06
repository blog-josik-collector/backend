package com.backend.commonelasticsearch.provision;

/**
 * alias 부트스트랩 결과.
 *
 * @param alias   애플리케이션이 사용하는 alias 이름
 * @param index   alias 가 가리키는 물리 인덱스명(없으면 null)
 * @param created 이번 호출로 새로 생성했는지 여부(false 면 이미 존재)
 */
public record ProvisionResult(String alias, String index, boolean created) {

}
