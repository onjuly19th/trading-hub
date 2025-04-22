package com.tradinghub.common.exception.portfolio;

import org.springframework.http.HttpStatus;
import com.tradinghub.common.exception.BusinessException;
import com.tradinghub.common.exception.ErrorCodes;

/**
 * 요청한 자산(코인)을 포트폴리오에서 찾을 수 없을 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 사용자가 해당 코인을 보유하고 있지 않을 때
 * 2. 존재하지 않는 코인 심볼로 자산을 조회할 때
 * 3. 자산이 삭제된 경우
 * 
 * HTTP 상태 코드: {@link HttpStatus#NOT_FOUND} (404)
 * 에러 코드: {@link ErrorCodes.Portfolio#ASSET_NOT_FOUND}
 */
public class AssetNotFoundException extends BusinessException {
    private static final String MESSAGE = "Asset not found";
    
    /**
     * 기본 메시지로 자산을 찾을 수 없음 예외 생성
     */
    public AssetNotFoundException() {
        super(MESSAGE, ErrorCodes.Portfolio.ASSET_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 상세 메시지로 자산을 찾을 수 없음 예외 생성
     * 
     * @param message 상세 에러 메시지 (코인 심볼 등 포함 가능)
     */
    public AssetNotFoundException(String message) {
        super(message, ErrorCodes.Portfolio.ASSET_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
} 