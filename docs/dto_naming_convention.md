🏷️ DTO 명명 규칙 및 설계 철학 가이드
본 문서는 도메인의 기능 확장을 고려하여, 명확하고 일관성 있는 DTO 명명 규칙을 정의합니다.

1. 확장성 기반의 명명 원칙 (Principle of Explicitness)
도메인 내 기능이 단일하지 않고 지속적으로 추가될 예정인 경우, **[도메인][행위][객체타입]**의 형식을 준수하여 이름만으로도 그 용도를 즉시 파악할 수 있도록 합니다.

다중 기능 대응: StrollRouteRequest, StrollLogRequest, StrollStatsResponse 등

2. 기능별 객체 명칭 정의
향후 추가될 기능들을 고려한 stroll 도메인의 객체 네이밍 맵입니다.

① 산책 경로 추천 (현재 작업 중)
Service: StrollRouteService
Request DTO: StrollRouteRequest (경로 생성을 위한 좌표 및 조건 전달)
Response DTO: StrollRouteResponse 또는 StrollPath (추천된 경로 상세 정보)

② 산책 일지 기록 (추가 예정)
Service: StrollLogService
Request DTO: StrollLogRequest (실제 산책한 경로, 시간, 사진 정보 등 전달)
Response DTO: StrollLogResponse

③ 산책 통계 조회 (추가 예정)
Service: StrollStatsService
Request DTO: StrollStatsRequest
Response DTO: StrollStatsResponse

3. 기술적 이점
의미론적 명확성: StrollRouteRequest라는 이름만 보고도 "이 객체는 경로 알고리즘을 태우기 위한 데이터구나"라고 즉시 인지할 수 있습니다.
타입 안전성: 컨트롤러나 서비스 레이어에서 파라미터를 받을 때, 잘못된 객체가 주입되는 실수를 컴파일 타임에 방지할 수 있습니다.
API 명세 자동화(Swagger): API 문서에서 각 기능별 요청 규격이 명확히 구분되어 프론트엔드 개발자와의 소통 비용이 감소합니다.