package org.example.expert.domain.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j(topic = "Admin Access Log")
@Component
@Aspect
public class AdminAccessLogging {
    @Pointcut("execution(* org.example.expert.domain..*AdminController.*(..))")
    public void adminControllerMethods() {}

    @Around("adminControllerMethods()")
    public Object logging(ProceedingJoinPoint joinPoint) throws Throwable {
        // HttpServletRequest 가져오기
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = (attributes != null) ? attributes.getRequest() : null;

        if (request != null) {
            // 사용자 ID, 요청 시간, URL 정보 가져오기
            Long userId = (Long) request.getAttribute("userId");// 필터나 세션에서 설정된 userId
            String requestTimestamp = LocalDateTime.now().toString();
            String requestUrl = request.getRequestURL().toString();

            // 로그 기록
            log.info("User ID: " + userId);
            log.info("Request Time: " + requestTimestamp);
            log.info("Request URL: " + requestUrl);
        }

        // 원래 메서드 실행
        Object result = joinPoint.proceed();
        return result;
    }
}
