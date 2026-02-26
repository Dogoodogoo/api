1. 프로젝트 개요
  - 서비스명: 어디가개 (DogooDogoo)
  - 목표: 반려견의 특성(체급, 나이)과 사용자의 희망 산책 거리를 분석하여, 필수 인프라(쓰레기통, 음수대)를 경유하는 최적의 순환형(Loop) 맞춤 산책 경로를 3가지로 추천하는 PWA 서비스.
  - 핵심 가치: 단순한 길찾기를 넘어선 '공간 지능(Spatial Intelligence)' 기반의 맞춤형 코스 제안 및 펫프렌들리 장소 정보 제공.


2. 기술 스택 (Tech Stack)
  - Backend: Java 21, Spring Boot 4.0.2, Spring Data JPA, Hibernate-Spatial
  - Database: PostgreSQL + PostGIS (공간 데이터베이스)
  - API Docs: Swagger (OpenAPI 3.0 / springdoc-openapi-starter-webmvc-ui 2.8.5)
  - Data Pipeline (ETL): Python, Pandas, Requests (멀티스레딩 API 호출)
  - External API: TMAP Pedestrian API(경로 탐색), Naver Geocoding(주소->좌표)


3.데이터 파이프라인 (ETL) 설계
  모든 공간 데이터는 PostGIS의 GEOMETRY(Point, 4326) 타입으로 변환되어 GIST 인덱스를 탑니다.
  - 반려견 동반 장소: 공공데이터포털 API (KorPetTourService2) 자동 수집.
  - 음수대: 서울 열린데이터 광장 API (TbViewGisArisu) 실시간 자동 수집.
  - 가로 휴지통: 서울 열린데이터 광장 엑셀 파일 다운로드 -> Naver Geocoding으로 좌표 변환 후 적재.


4. 핵심 알고리즘: 120도 방사형 경로 추천 전략사용자가 시작 마커를 찍고 산책 거리($D$)를 설정하면, 다음 로직을 통해 3개의 서로 다른 경로를 반환합니다.
  - 각도 분할: 시작점 기준 $0^\circ, 120^\circ, 240^\circ$ 방향 설정.
  - 동적 변수 (다양성 확보): 매 호출 시마다 기본 각도에 $\pm 10\sim 20^\circ$ 오프셋, 거리($D/3$)에 $\pm 10\sim 15\%$ 오프셋을 무작위로 주어 매번 새로운 경로 생성.
  - 가상 경유지 계산: PostGIS의 ST_Project를 사용하여 3개 섹터의 가상 경유지 좌표 계산.
  - 실제 POI 매칭: ST_DWithin을 사용해 가상 경유지 반경 내의 필수 시설물(쓰레기통/음수대) 랜덤 추출.
  - 경로 생성: 확정된 POI를 TMAP 보행자 API의 passList에 넣어 최종 Polyline 생성 (병렬 호출 최적화).


5. 법적 및 보안 아키텍처 (LBS 대응 전략)
  - 위치기반서비스사업자(LBS) 규제 우회: 사용자의 실시간 GPS를 서버가 자동 수집하지 않습니다.
  - 수동 마커 입력 방식: 사용자가 직접 지도상의 특정 지점을 클릭(Pin Drop)하여 전송한 좌표만 서버에서 '단순 계산 데이터'로 활용합니다.
  - On-Device 처리: 실시간 산책 동선 트래킹 등 프라이버시에 민감한 데이터는 서버에 전송하지 않고 브라우저의 IndexedDB에만 저장하는 Privacy by Design 채택.


6. 현재까지 개발된 API 명세 (RESTful)
  - GET /api/v1/pet-places: 뷰포트(minLat, maxLat, minLng, maxLng) 기반 반려견 동반 장소 조회.
  - GET /api/v1/fountains: 페이징(page, size) 기반 전체 음수대 조회.
  - GET /api/v1/trash-bins: 뷰포트 및 중심점 좌표(centerLat, centerLng) 기반 인접 가로 휴지통 정렬 조회.
