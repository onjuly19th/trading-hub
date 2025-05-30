package com.tradinghub.application.event;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tradinghub.application.usecase.portfolio.CreatePortfolioUseCase;
import com.tradinghub.domain.model.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignupEventListener {
    private final CreatePortfolioUseCase createPortfolioUseCase;
    /**
     * UserSignedUpEvent 발생 시 포트폴리오를 생성합니다.
     * 회원가입 트랜잭션이 성공적으로 커밋된 후에 실행됩니다.
     * 실패 시 최대 3번 재시도합니다. (총 3번 실행 시도)
     * @param event 회원가입 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        retryFor = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public void handleUserSignup(UserSignedUpEvent event) {
        User user = event.getUser();
        log.info("Portfolio creation try (User ID: {}, Username: {})...", user.getId(), user.getUsername());

        createPortfolioUseCase.execute(user, "USD", new java.math.BigDecimal("1000000"));

        log.info("User {} portfolio created successfully", user.getUsername());
    }
}