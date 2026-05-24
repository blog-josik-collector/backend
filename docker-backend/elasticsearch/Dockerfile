# 현재 프로젝트에서 사용 중인 Elasticsearch 베이스 이미지 버전 지정
FROM docker.elastic.co/elasticsearch/elasticsearch:8.18.0

# Nori 플러그인 설치 (배치 모드로 사용자 확인 인터랙션 생략)
RUN elasticsearch-plugin install --batch analysis-nori