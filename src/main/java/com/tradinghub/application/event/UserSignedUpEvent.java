package com.tradinghub.application.event;

import com.tradinghub.domain.model.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 회원가입 완료 시 발생하는 이벤트
 */
@Getter
@RequiredArgsConstructor
public class UserSignedUpEvent {
    private final User user;
}