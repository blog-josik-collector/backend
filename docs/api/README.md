# OpenAPI 스냅샷

각 서비스가 런타임에 생성하는 OpenAPI 스펙을 JSON으로 커밋해 둔다.
PR에서 API 변경이 diff로 보이고, 프론트 codegen / Postman·Bruno import에 그대로 쓸 수 있다.

| 파일 | 서비스                           | 로컬 Swagger UI                                   | api-docs |
|---|-------------------------------|-------------------------------------------------|---|
| `user-service-openapi.json` | user-service (`:8080`)        | http://localhost:8080/users/swagger-ui          | `/users/v3/api-docs/user-v1` |
| `integrated-api-openapi.json` | integrated-api (`:8081`)      | http://localhost:8081/integrated-api/swagger-ui | `/integrated-api/v3/api-docs/integrated-v1` |
| `interaction-service-openapi.json` | interaction-service (`:8083`) | http://localhost:8083/interaction/swagger-ui    | `/interaction/v3/api-docs/interaction-v1` |

## 갱신 방법

1. 변경한 서비스를 로컬에서 기동한다.
2. 저장소 루트에서 스펙을 다시 덤프한다.

```bash
# 세 서비스 전부 (전부 떠 있을 때)
./scripts/dump-openapi.sh

# 특정 서비스만
./scripts/dump-openapi.sh user
./scripts/dump-openapi.sh integrated
./scripts/dump-openapi.sh interaction
```

3. 변경된 `docs/api/*-openapi.json` 을 API 코드 변경과 함께 커밋한다.

포트가 다르면 환경 변수로 덮어쓴다.

```bash
INTERACTION_SERVICE_PORT=18083 ./scripts/dump-openapi.sh interaction
```

## 규칙

- API 요청/응답 DTO나 엔드포인트를 바꾼 PR에는 해당 서비스 스냅샷 갱신을 포함한다.
- 스냅샷은 `json.tool --sort-keys` 로 정렬된 상태로 커밋한다. (키 순서 흔들림으로 인한 노이즈 diff 방지)