package com.backend.commonelasticsearch.provision;

/**
 * 재색인 + alias 스왑 결과.
 *
 * @param alias       스왑된 alias 이름
 * @param sourceIndex 재색인 원본(이전에 alias 가 가리키던) 물리 인덱스
 * @param newIndex    새로 생성되어 alias 가 가리키게 된 물리 인덱스
 * @param documents   재색인으로 복사된 문서 수
 */
public record ReindexResult(String alias, String sourceIndex, String newIndex, long documents) {

}
