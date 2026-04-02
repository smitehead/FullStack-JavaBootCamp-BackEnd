//PortOne API 호출 전담
package com.javajava.project.service;

import com.javajava.project.config.PortOneProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneClient {
        private final PortOneProperties portOneProperties;

        // portone rest api 호출용 webclient(매번 생성하지 않도록 지연 초기화함.)
        private WebClient webClient() {
                return WebClient.builder()
                                .baseUrl(portOneProperties.getBaseUrl())
                                .build();
        }

        // <1> portone 액세스 토큰 발급
        // 모든 api 호출시 필요함.
        public String getAccessToken() {
                Map<?, ?> response = webClient().post()
                                .uri("/users/getToken")
                                .bodyValue(Map.of(
                                                "imp_key", portOneProperties.getApiKey(),
                                                "imp_secret", portOneProperties.getApiSecret()))
                                .retrieve()
                                .bodyToMono(Map.class)
                                .block();

                Map<?, ?> responseBody = (Map<?, ?>) response.get("response");
                return (String) responseBody.get("access_token");
        }

        // <2> 빌링키 기반 자동결제 요청
        // 결제창 없이 서버에서 바로 결제 실행
        public Map<?, ?> requestBillingCharge(String accessToken, String customerUid, String merchantUid, Long amount) {
                Map<?, ?> response = webClient().post()
                                .uri("/subscribe/payments/again")
                                .header("Authorization", accessToken)
                                .bodyValue(Map.of(
                                                "customer_uid", customerUid,
                                                "merchant_uid", merchantUid,
                                                "amount", amount,
                                                "name", "포인트 충전"))
                                .retrieve()
                                .bodyToMono(Map.class)
                                .block();

                return (Map<?, ?>) response.get("response");
        }

        // <3> 결제 단건 조회 - 서버 검증
        // portone 서버에서 실제 결제 정보를 가져와서 금액과 상태를 검증
        public Map<?, ?> getPayment(String accessToken, String impUid) {
                Map<?, ?> response = webClient().get()
                                .uri("/payments/{impUid}", impUid)
                                .header("Authorization", accessToken)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .block();

                return (Map<?, ?>) response.get("response");
        }

        // <4> 결제 취소
        // 금액 불일치 또는 상태 비정상 시 반드시 호출됨.
        // 호출안하면 고객 돈만 빠지고 포인트는 안오르는 상황 발생
        public void cancelPayment(String accessToken, String impUid, String reason) {
                try {
                        webClient().post()
                                        .uri("/payments/cancel")
                                        .header("Authorization", accessToken)
                                        .bodyValue(Map.of(
                                                        "imp_uid", impUid,
                                                        "reason", reason))
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .block();
                } catch (Exception e) {
                        // 취소 실패 — 로그 남기고 수동 처리되게 함.
                        log.error("[PortOne] 결제 취소 실패 impUid={}, reason={}, error={}",
                                        impUid, reason, e.getMessage());
                }
        }

        //API 방식 빌링키 발급
        //고객 카드 정보를 직접 받아 portone 서버에 빌링키 발급 요청
        //pg사 결제창 없이 서버간 통신으로 처리
        public Map<?, ?> issueBillingKey(String accessToken, String customerUid,
                                  String cardNumber, String expiry,
                                  String birth, String pwd2digit) {
            Map<?, ?> response = webClient().post()
                .uri("/subscribe/customers/{customerUid}", customerUid)
                .header("Authorization", accessToken)
                .bodyValue(Map.of(
                        "card_number", cardNumber,   // 카드번호 (하이픈 제거, 16자리)
                        "expiry",      expiry,        // 유효기간 YYYY-MM 형식
                        "birth",       birth,         // 생년월일 6자리 또는 사업자번호 10자리
                        "pwd_2digit",  pwd2digit       // 비밀번호 앞 2자리
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
                
            log.info("[PortOne] issueBillingKey 응답: {}", response);

            // PortOne 응답 코드 확인 (0이 아니면 실패)
            Integer code = (Integer) response.get("code");
            if (code != null && code != 0) {
                String message = (String) response.get("message");
                log.error("[PortOne] 빌링키 발급 실패 코드: {}, 메시지: {}", code, message);
                throw new IllegalStateException("빌링키 발급 실패: " + message);
            }

            return (Map<?, ?>) response.get("response");
        }
}
