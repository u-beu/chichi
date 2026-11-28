package com.example.chichi.domain.bot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PlaybackFailureStatus {
    NOT_IN_VOICE("not_in_voice", "음성 채널에 먼저 입장해 주세요."),
    VIDEO_TOO_LONG("video_too_long", "영상이 너무 길어 재생할 수 없습니다. (최대 2시간)"),
    SEARCH_FAILED("search_failed", "곡 정보를 찾지 못했습니다."),
    NO_PERMISSION("no_permission", "봇에게 음성 채널 입장/발언 권한이 없습니다."),
    CONNECT_FAILED("connect_failed", "음성 채널 연결에 실패했습니다."),
    PLAYBACK_FAILED("playback_failed", "재생 중 오류가 발생했습니다."),
    UNKNOWN("unknown", "재생에 실패했습니다.");

    private final String code;
    private final String message;

    public static PlaybackFailureStatus from(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
