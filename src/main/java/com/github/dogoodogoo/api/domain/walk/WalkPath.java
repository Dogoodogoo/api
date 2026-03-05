package com.github.dogoodogoo.api.domain.walk;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class WalkPath {

    private String routeId;
    private String routeName;
    private Double totalDistance;   //산책 경로 총거리
    private Integer estimatedTime;  //산책 예상 시간(단위: 분)
    private List<Coordinate> pathCoordinates;
    private List<Waypoint> waypoints;   //경유지 목록
    private Integer trashBinCount;  //쓰레기통 총 개수
    private Integer fountainCount;  //음수대 총 개수

    //지도 선(Line)을 그리기 위한 단순 위경도.
    @Getter
    @Builder
    public static class Coordinate {
        private Double latitude;    //위도
        private Double longitude;   //경도

    }

    //경로 내 주요 방문 지점 상세 정보를 담는 객체.
    @Getter
    @Builder
    public static class Waypoint {
        private String name;        //지점 명
        private String category;    //지점 카테고리(start, trash_bin, fountain, pet_place 등)
        private Double latitude;    //위도
        private Double longitude;   //경도
        private Integer sequence;   //경로내 방문 순서.
    }
}
