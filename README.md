1. 프로젝트 개요 (Overview)

'어디가개'는 반려견주들이 산책 중 겪는 실질적인 불편함(배변 처리, 수분 공급 등)을 해결하기 위해 기획된 공간 지능(Spatial Intelligence) 기반 산책 지원 서비스입니다. 단순한 목적지 안내를 넘어 반려견의 나이와 체급에 맞는 최적의 보행 속도와 거리를 계산하고, 필수 시설물(쓰레기통, 음수대)을 경유하는 3가지 순환형(Loop) 산책로를 제안합니다.

2. 핵심 기술 스택 (Tech Stack)

Backend: Java 21 (Virtual Threads 활용), Spring Boot 3.5.4, Spring Data JPA, Hibernate-Spatial

Infrastructure: RestClient (외부 API 통신), Jackson (GeoJSON 파싱 최적화)

Database: PostgreSQL 17 + PostGIS (공간 인덱싱 및 지능형 쿼리)

API Docs: Swagger (OpenAPI 3.0)

Data Pipeline: Python (Pandas, Requests) 기반 ETL 자동화

External API: TMAP Pedestrian API (도보 경로), Naver Map SDK & Geocoding

3. 지능형 경로 추천 엔진 (Intelligence Engine)

단순한 지점 연결이 아닌 유전 알고리즘(Genetic Algorithm)의 적합도 평가 모델을 도입하여 경로의 질을 극대화했습니다.

🧬 알고리즘 워크플로우

데이터 정규화: 사용자의 '년/개월' 입력 단위를 실수형 나이(Year)로 변환하여 퍼피/성견/노견에 맞는 정밀 속도 제어 로직을 적용합니다.

섹터 기반 시뮬레이션: Java 21의 **가상 스레드(Virtual Threads)**를 활용하여 120도 간격의 3개 구역($0^\circ, 120^\circ, 240^\circ$)에 대해 수백 개의 경로 후보를 동시 생성합니다.

적합도 평가 (Fitness Function):

거리 정밀도 점수: 도심지 굴곡률(1.35)을 반영한 예측치와 사용자의 목표 거리 일치율 평가.

시설물 가중치: 경로 내 쓰레기통 및 음수대 배치의 유용성 평가.

최종 검증 및 가이드 추출: 선별된 '엘리트 경로'에 대해서만 TMAP API를 호출하여 실제 도로망 데이터를 확보하고, 실시간 회전 안내(Turn-by-turn) 정보를 파싱합니다.

4. 주요 기능 (Key Features)

지능형 경로 카드: 생성된 3종 경로의 예상 시간, 거리, 테마별 정보를 카드 UI로 제공.

실시간 내비게이션 모드: 산책 시작 시 상단 배너를 통해 "[지점명]에서 왼쪽/오른쪽", "직진" 등 정제된 방향 정보 제공.

통합 공간 맵: 반려견 동반 장소, 음수대, 쓰레기통의 위치를 클러스터링 기술로 시각화. 동일 좌표 시설물의 지능적 병합(Merging) 처리.

5. 법적/보안 아키텍처 (Privacy by Design)

LBS(위치기반서비스) 규제 대응 및 사용자 프라이버시 보호를 위해 다음 전략을 채택했습니다.

수동 마커 방식: 서버가 실시간 GPS를 자동 수집하지 않고, 사용자가 지도에 직접 찍은 핀(Pin) 좌표만 계산 데이터로 활용합니다.

On-Device 처리: 민감한 트래킹 데이터는 브라우저 내부에 저장하여 위치정보법 리스크를 지능적으로 우회합니다.

6. API명세
http://jhin.iptime.org:8080/swagger-ui/index.html
