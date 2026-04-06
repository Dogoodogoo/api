package com.github.dogoodogoo.api.global.error;

import lombok.Getter;

@Getter
public enum ErrorCode {

    //Common
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력 값입니다."),
    MISSING_INPUT_VALUE(400, "C002", "필수 값이 누락되었습니다."),
    METHOD_NOT_ALLOWED(405, "C003", "지원하지 않는 HTTP 메소드입니다."),
    INTERNAL_SERVER_ERROR(500, "C004", "내부 서버 오류가 발생했습니다."),

    //Stroll
    STROLL_PATH_NOT_FOUND(404, "S001", "유효한 산책 경로를 찾을 수 없습니다."),
    MAP_API_EXTERNAL_ERROR(502, "S002", "외부 지도 API 통신 중 오류가 발생했습니다."),
    OUT_OF_SERVICE_AREA(400, "S003", "산책 가능 범위를 벗어난 위치입니다."),

    // Feature
    INVALID_SATISFACTION_SCORE(400, "F001", "유효하지 않은 만족도 점수입니다."),
    DATABASE_QUERY_ERROR(500, "F002", "데이터베이스 조회 중 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}