# LiveBid 기능 전체 정리

> 최종 업데이트: 2026-04-28

---

## 목차
1. [인증 / 회원](#1-인증--회원)
2. [상품 / 경매](#2-상품--경매)
3. [입찰](#3-입찰)
4. [낙찰 / 거래](#4-낙찰--거래)
5. [포인트 / 결제](#5-포인트--결제)
6. [리뷰](#6-리뷰)
7. [위시리스트](#7-위시리스트)
8. [알림 (SSE)](#8-알림-sse)
9. [채팅 (WebSocket)](#9-채팅-websocket)
10. [고객센터 - 공지사항](#10-고객센터---공지사항)
11. [고객센터 - 문의](#11-고객센터---문의)
12. [고객센터 - FAQ](#12-고객센터---faq)
13. [신고](#13-신고)
14. [관리자 - 회원 관리](#14-관리자---회원-관리)
15. [관리자 - 경매 관리](#15-관리자---경매-관리)
16. [관리자 - 신고 관리](#16-관리자---신고-관리)
17. [관리자 - 알림 발송](#17-관리자---알림-발송)
18. [관리자 - 활동 로그](#18-관리자---활동-로그)
19. [관리자 - 매너온도 이력](#19-관리자---매너온도-이력)
20. [관리자 - 배너 관리](#20-관리자---배너-관리)
21. [관리자 - 공지 관리](#21-관리자---공지-관리)
22. [관리자 - 문의 관리](#22-관리자---문의-관리)
23. [관리자 - 출금 관리](#23-관리자---출금-관리)
24. [관리자 - 수익 통계](#24-관리자---수익-통계)
25. [관리자 - 대시보드](#25-관리자---대시보드)
26. [스케줄러](#26-스케줄러)
27. [미구현 항목 요약](#27-미구현-항목-요약)
28. [운영 배포 전 체크리스트](#28-운영-배포-전-체크리스트)

---

## 1. 인증 / 회원

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/auth/login` | 로그인 (JWT 발급, 중복 로그인 방지) |
| POST | `/api/auth/logout` | 로그아웃 (DB 토큰 null) |
| POST | `/api/auth/send-email-code` | 이메일 인증번호 발송 |
| POST | `/api/auth/verify-email-code` | 이메일 인증번호 검증 |
| POST | `/api/auth/find-id` | 아이디 찾기 |
| POST | `/api/auth/send-reset-code` | 비밀번호 재설정 코드 발송 |
| POST | `/api/auth/reset-pw` | 임시 비밀번호 발급 |
| POST | `/api/members` | 회원가입 (BCrypt, Bean Validation, 만 14세 제한) |
| GET | `/api/members/{id}` | 회원 단건 조회 (포인트, 매너온도 포함) |
| GET | `/api/members/check-userid` | 아이디 중복확인 |
| GET | `/api/members/check-nickname` | 닉네임 중복확인 |
| GET | `/api/members/check-email` | 이메일 중복확인 |
| GET | `/api/members/me` | 내 프로필 조회 |
| PUT | `/api/members/me/profile` | 프로필 수정 |
| PUT | `/api/members/me/email` | 이메일 수정 |
| PUT | `/api/members/me/password` | 비밀번호 변경 |
| PUT | `/api/members/me/notification` | 알림 설정 변경 |
| PUT | `/api/members/{memberNo}/profile-image-url` | 프로필 이미지 URL 저장 |
| DELETE | `/api/members/me` | 회원 탈퇴 |
| POST | `/api/members/me/blocked/{targetMemberNo}` | 사용자 차단 |
| GET | `/api/members/me/blocked/{targetMemberNo}` | 차단 여부 확인 |
| GET | `/api/members/me/blocked` | 차단 사용자 목록 |
| DELETE | `/api/members/me/blocked/{targetMemberNo}` | 차단 해제 |
| GET | `/api/members/{memberNo}/seller-profile` | 판매자 공개 프로필 조회 |

### 구현 방식
- JWT HS256, 24시간 만료
- DB에 currentToken 저장 → 중복 로그인 시 SSE `forceLogout` 이벤트 발송
- CORS: `corsConfigurationSource()` 빈 등록

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| Login.tsx | `/login` | ✅ API 연동 완료 |
| Signup.tsx | `/signup` | ✅ API 연동 완료 |
| FindAccount.tsx | `/find-account` | ✅ API 연동 완료 |
| Settings.tsx | `/mypage/settings` | ✅ API 연동 완료 |
| SellerProfile.tsx | `/seller/:id` | ✅ API 연동 완료 |

---

## 2. 상품 / 경매

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/products` | 상품 등록 (multipart/form-data, 이미지 다중 업로드) |
| GET | `/api/products` | 상품 목록 (필터/정렬/무한스크롤/페이징) |
| GET | `/api/products/{id}` | 상품 상세 조회 |
| GET | `/api/products/{id}/bids` | 상품별 입찰 기록 |
| DELETE | `/api/products/{id}` | 상품 삭제 (soft delete) |
| POST | `/api/products/{id}/cancel` | 판매자 경매 취소 |
| GET | `/api/products/my-selling` | 내 판매 상품 목록 |
| GET | `/api/products/my-bidding` | 내 입찰 상품 목록 |
| GET | `/api/products/my-purchased` | 구매 완료 상품 목록 |
| POST | `/api/images/upload` | 이미지 업로드 |
| GET | `/api/images/{filename}` | 이미지 서빙 (7일 캐시) |
| GET | `/api/products/{productNo}/qna` | QnA 목록 조회 |
| POST | `/api/products/{productNo}/qna` | QnA 작성 |
| DELETE | `/api/products/{productNo}/qna/{qnaNo}` | QnA 삭제 |
| POST | `/api/products/{productNo}/qna/{qnaNo}/answer` | 판매자 답변 등록/수정 |
| DELETE | `/api/products/{productNo}/qna/{qnaNo}/answer` | 판매자 답변 삭제 |

### 구현 방식
- 이미지: `FileStore.storeFiles()` — 다중 업로드, 첫 번째가 대표 이미지
- 페이징: Oracle 11g `OracleLegacyDialect` (ROWNUM 기반)
- 필터: 카테고리, 상태, 가격 범위, 검색어, 정렬(인기/최신/마감임박)

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| Home.tsx | `/` | ✅ API 연동 완료 (배너 + 인기 상품) |
| ProductList.tsx | `/products` | ✅ API 연동 완료 |
| ProductDetail.tsx | `/products/:id` | ✅ API 연동 완료 |
| ProductRegister.tsx | `/sell` | ✅ API 연동 완료 (이미지 다중 업로드 포함) |
| MyPage.tsx | `/mypage` | ✅ API 연동 완료 |

---

## 3. 입찰

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/bids` | 실시간 입찰 |
| POST | `/api/bids/buyout` | 즉시 구매 |
| GET | `/api/bids/product/{productNo}` | 상품별 입찰 기록 조회 |
| POST | `/api/bids/{productId}/cancel` | 최고 입찰자 본인 취소 (5% 위약금) |
| PATCH | `/api/bids/{bidNo}/cancel` | 관리자 입찰 취소 |
| POST | `/api/auto-bid` | 자동입찰 등록/수정 |
| DELETE | `/api/auto-bid/{productNo}` | 자동입찰 취소 |
| GET | `/api/auto-bid/active` | 활성 자동입찰 조회 |

### 구현 방식
- 비관적 락: `memberNo` 오름차순으로 락 순서 고정 → 데드락 방지
- 입찰 취소: 5% 위약금 차감, 차순위 승계, SSE `priceUpdate` 발송
- 취소 차단: `CANCEL_BLOCK_HOURS = 12` — 입찰 후 12시간 내 취소 불가 (⚠️ 현재 테스트용 `0`으로 설정)
- SSE 격리: try-catch로 SSE 실패가 트랜잭션 롤백 유발 방지

### 프론트엔드
| 컴포넌트 | 상태 |
|---------|------|
| ProductDetail.tsx (입찰 모달) | ✅ API 연동 완료 |
| ProductDetail.tsx (즉시 구매) | ✅ API 연동 완료 |
| ProductDetail.tsx (자동입찰) | ✅ API 연동 완료 |

---

## 4. 낙찰 / 거래

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/auction-results/product/{productNo}` | 낙찰 결과 조회 (구매자) |
| GET | `/api/auction-results/seller/product/{productNo}` | 낙찰 결과 조회 (판매자) |
| POST | `/api/auction-results/{resultNo}/confirm` | 구매 확정 (에스크로 정산) |
| POST | `/api/auction-results/{resultNo}/cancel` | 강제 승계 낙찰자 취소 |
| POST | `/api/auction-results/{resultNo}/request-cancel` | 구매자 취소 요청 |
| POST | `/api/auction-results/{resultNo}/cancel-approve` | 판매자 구매자 취소 승인 |
| POST | `/api/auction-results/{resultNo}/seller-request-cancel` | 판매자 취소 요청 |
| POST | `/api/auction-results/{resultNo}/buyer-approve-cancel` | 구매자 판매자 취소 승인 |
| PATCH | `/api/auction-results/seller/product/{productNo}/delivery-address` | 배송지 업데이트 |

### 구현 방식
- 에스크로 정산: 구매 확정 시 낙찰가를 판매자 포인트로 지급
- 7일 자동 구매 확정: `AuctionAutoConfirmScheduler` + `AuctionAutoConfirmProcessor`
- 취소 협의: 구매자↔판매자 쌍방 요청/승인 구조

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| WonProductDetail.tsx | `/won/:productNo` | ✅ API 연동 완료 |
| SellerAuctionResult.tsx | `/seller/auction/:productNo` | ✅ API 연동 완료 |

---

## 5. 포인트 / 결제

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/points/billing-key` | 카드 등록 (빌링키 저장) |
| GET | `/api/points/billing-key` | 등록된 카드 조회 |
| DELETE | `/api/points/billing-key` | 카드 삭제 |
| POST | `/api/points/charge` | 포인트 충전 (PortOne 연동) |
| GET | `/api/points/history` | 포인트 내역 조회 (페이징) |
| GET | `/api/points/accounts` | 계좌 목록 조회 |
| POST | `/api/points/accounts` | 계좌 추가 |
| DELETE | `/api/points/accounts/{accountNo}` | 계좌 삭제 |
| POST | `/api/points/withdraw` | 출금 신청 |

### 구현 방식
- PortOne 연동: 결제 후 `PaymentReconciliationScheduler`(5분마다)로 PENDING 보정
- 10분 이상 PENDING → PortOne 재확인 → 성공 시 포인트 지급 / 실패 시 FAILED 처리

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| Points.tsx | `/mypage/points` | ✅ API 연동 완료 |
| PointCharge.tsx | `/mypage/points/charge` | ✅ API 연동 완료 |
| PointWithdraw.tsx | `/mypage/points/withdraw` | ✅ API 연동 완료 |
| CardRegistration.tsx | `/mypage/card` | ✅ API 연동 완료 |
| AccountRegistration.tsx | `/mypage/account` | ✅ API 연동 완료 |

---

## 6. 리뷰

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/reviews` | 리뷰 작성 |
| GET | `/api/reviews/target/{memberNo}` | 특정 회원이 받은 리뷰 목록 |
| GET | `/api/reviews/my` | 내가 작성한 리뷰 목록 |
| GET | `/api/reviews/exists/{resultNo}` | 리뷰 작성 여부 확인 |
| PUT | `/api/reviews/{reviewNo}/hide` | 리뷰 숨김 처리 |

### 기능 구조
- 리뷰 작성: 태그 선택 + 텍스트 후기 (별점 없음)
- 태그 목록: `응답이 빨라요`, `친절하고 매너가 좋아요`, `시간 약속을 잘 지켜요`, `상품 상태가 설명과 같아요`
- 매너온도와 리뷰 자동 연동 없음 (관리자 수동 조정)

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| WonProductDetail.tsx (리뷰 모달) | `/won/:productNo` | ✅ API 연동 완료 |
| SellerProfile.tsx (받은 리뷰 탭) | `/seller/:id` | ✅ API 연동 완료 |
| MyPage.tsx (리뷰 탭) | `/mypage` | ✅ API 연동 완료 |
| ReviewCreate.tsx | `/review/:orderId` | ⚠️ 데드코드 (WonProductDetail 모달로 대체됨) |

### DB 적용 필요
```sql
ALTER TABLE REVIEW DROP COLUMN RATING;
```

---

## 7. 위시리스트

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/wishlists/toggle` | 찜 토글 |
| GET | `/api/wishlists/my` | 내 찜 목록 (페이징) |

### 프론트엔드
| 컴포넌트 | 상태 |
|---------|------|
| ProductCard.tsx (찜 버튼) | ✅ API 연동 완료 |
| MyPage.tsx (찜 목록 탭) | ✅ API 연동 완료 |

---

## 8. 알림 (SSE)

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/sse/ticket` | SSE 연결용 일회용 티켓 발급 |
| GET | `/api/sse/subscribe` | SSE 구독 (text/event-stream) |
| GET | `/api/notifications` | 알림 목록 조회 |
| GET | `/api/notifications/unread-count` | 미읽음 알림 개수 |
| PATCH | `/api/notifications/{notiNo}/read` | 단일 알림 읽음 처리 |
| PATCH | `/api/notifications/read-all` | 전체 알림 읽음 처리 |
| DELETE | `/api/notifications/{notiNo}` | 알림 삭제 |

### SSE 이벤트 종류
| 이벤트 | 설명 |
|--------|------|
| `notification` | 알림 (Map 구조: notiNo, type, content, linkUrl, isRead, createdAt) |
| `pointUpdate` | 포인트 변경 |
| `priceUpdate` | 입찰가 실시간 업데이트 |
| `forceLogout` | 중복 로그인 강제 로그아웃 |

### 알림 type 규칙
| type | 사용 시점 |
|------|----------|
| `bid` | 입찰 / 낙찰 / 유찰 / 경매취소 |
| `activity` | 결제 / 배송 / 구매확정 / 리뷰 / Q&A |
| `system` | 정지 / 포인트충전 |

### 프론트엔드
| 위치 | 상태 |
|------|------|
| Layout.tsx (SSE 백그라운드 구독) | ✅ API 연동 완료 |
| Inbox.tsx (알림 탭) | ✅ API 연동 완료 |

---

## 9. 채팅 (WebSocket)

### 백엔드 — REST API
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/chat/rooms` | 채팅방 생성 또는 기존 방 반환 |
| GET | `/api/chat/rooms` | 내 채팅방 목록 조회 |
| GET | `/api/chat/rooms/{roomId}/messages` | 메시지 내역 (커서 페이징) |
| GET | `/api/chat/rooms/{roomId}/messages/after` | 누락 메시지 조회 (재연결용) |
| PATCH | `/api/chat/rooms/{roomId}/read` | 읽음 벌크 처리 |
| POST | `/api/chat/rooms/{roomId}/images` | 채팅 이미지 업로드 |
| DELETE | `/api/chat/rooms/{roomId}` | 채팅방 soft delete |

### 백엔드 — STOMP WebSocket
| 발행 | 구독 | 설명 |
|------|------|------|
| `/pub/chat/message` | `/sub/chat/room/{roomId}` | 메시지 전송/수신 |
| `/pub/chat/read` | `/sub/chat/room/{roomId}/read` | 실시간 읽음 상태 브로드캐스트 |

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| chat.tsx | `/chat/:roomId` | ✅ STOMP WebSocket 연동 완료 (자동 스크롤, 말풍선 개행 포함) |
| Inbox.tsx (채팅 탭) | `/inbox` | ✅ API 연동 완료 |

---

## 10. 고객센터 - 공지사항

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/notices` | 공지사항 목록 (카테고리 필터, 검색, 페이징) |
| GET | `/api/notices/{noticeNo}` | 공지사항 상세 조회 |

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| NoticeList.tsx | `/cs/notices` | ✅ API 연동 완료 |
| NoticeDetail.tsx | `/cs/notices/:id` | ✅ API 연동 완료 |

---

## 11. 고객센터 - 문의

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/inquiries` | 문의 등록 (multipart/form-data, 이미지 최대 5개) |
| GET | `/api/inquiries/my` | 내 문의 목록 (페이징) |
| GET | `/api/inquiries/{inquiryNo}` | 문의 상세 조회 |

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| InquiryList.tsx | `/cs/inquiries` | ✅ API 연동 완료 |
| InquiryCreate.tsx | `/cs/inquiries/create` | ✅ API 연동 완료 |
| InquiryDetail.tsx | `/cs/inquiries/:id` | ✅ API 연동 완료 |

---

## 12. 고객센터 - FAQ

### 백엔드
| 상태 | 내용 |
|------|------|
| ❌ 미구현 | FAQ 관련 API 없음 |

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| FAQ.tsx | `/cs/faq` | ❌ FAQ_DATA 하드코딩 (정적 콘텐츠) |

---

## 13. 신고

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/reports` | 신고 제출 (multipart/form-data, 이미지 최대 5개) |

### 프론트엔드
| 페이지 | 경로 | 상태 |
|--------|------|------|
| Report.tsx | `/cs/report` | ✅ API 연동 완료 |

---

## 14. 관리자 - 회원 관리

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/admin/members` | 회원 목록 조회 / 검색 (페이징) |
| PUT | `/api/admin/members/{memberNo}/suspend` | 회원 정지 (IS_SUSPENDED + SUSPEND_UNTIL + 알림) |
| PUT | `/api/admin/members/{memberNo}/unsuspend` | 회원 정지 해제 |
| PUT | `/api/admin/members/{memberNo}/manner-temp` | 매너온도 수동 변경 + 사유 기록 |
| PUT | `/api/admin/members/{memberNo}/points` | 포인트 변경 |
| PUT | `/api/admin/members/{memberNo}/role` | 권한 변경 |
| GET | `/api/admin/members/manner-history` | 매너온도 변동 이력 전체 조회 |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| UserManagement.tsx | ✅ API 연동 완료 |
| MannerHistoryManagement.tsx | ✅ API 연동 완료 |

---

## 15. 관리자 - 경매 관리

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/admin/products` | 전체 상품 목록 조회 (페이징) |
| GET | `/api/admin/products/category-stats` | 대분류별 상품 건수 통계 |
| PUT | `/api/admin/products/{productNo}/cancel` | 경매 강제 종료 |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| AuctionManagement.tsx | ✅ API 연동 완료 |

---

## 16. 관리자 - 신고 관리

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/admin/reports` | 신고 목록 조회 (상태 필터, 페이징) |
| GET | `/api/admin/reports/{reportNo}` | 신고 상세 조회 (이미지 포함) |
| PUT | `/api/admin/reports/{reportNo}/resolve` | 신고 처리 (상태 변경 + 제재 메시지 + 알림 발송) |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| ReportManagement.tsx | ✅ API 연동 완료 |

---

## 17. 관리자 - 알림 발송

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/admin/notifications/broadcast` | 전체 회원 알림 발송 |
| GET | `/api/admin/notifications/recent` | 최근 발송 알림 목록 |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| NotificationManagement.tsx | ✅ API 연동 완료 |

---

## 18. 관리자 - 활동 로그

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/admin/activity-logs` | 활동 로그 조회 (targetType 필터) |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| ActivityLogManagement.tsx | ✅ API 연동 완료 |

---

## 19. 관리자 - 매너온도 이력

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/admin/members/manner-history` | 매너온도 변동 이력 전체 조회 |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| MannerHistoryManagement.tsx | ✅ API 연동 완료 |

---

## 20. 관리자 - 배너 관리

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/banners` | 활성 배너 조회 (type 필터) |
| GET | `/api/banners/all` | 전체 배너 조회 (활성/비활성 포함) |
| POST | `/api/banners` | 배너 등록 (bannerType 포함) |
| PUT | `/api/banners/{bannerNo}` | 배너 수정 |
| DELETE | `/api/banners/{bannerNo}` | 배너 삭제 |
| PATCH | `/api/banners/{bannerNo}/toggle` | 활성화/비활성화 토글 |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| BannerManagement.tsx | ✅ API 연동 완료 |

---

## 21. 관리자 - 공지 관리

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/notices/all` | 전체 공지 조회 (삭제 포함) |
| POST | `/api/notices` | 공지 등록 |
| PUT | `/api/notices/{noticeNo}` | 공지 수정 |
| DELETE | `/api/notices/{noticeNo}` | 공지 삭제 (soft delete) |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| NoticeManagement.tsx | ✅ API 연동 완료 |

### DB 적용 필요
```sql
ALTER TABLE NOTICE DROP COLUMN DESCRIPTION;
```

---

## 22. 관리자 - 문의 관리

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/admin/inquiries` | 전체 문의 목록 (상태 필터, 페이징) |
| GET | `/api/admin/inquiries/{inquiryNo}` | 문의 상세 조회 |
| PATCH | `/api/admin/inquiries/{inquiryNo}/answer` | 답변 등록 |
| DELETE | `/api/admin/inquiries/{inquiryNo}` | 문의 삭제 |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| InquiryManagement.tsx | ⚠️ 부분 구현 (API 호출은 되나 닉네임 표시에 MOCK_USERS 사용 중) |

---

## 23. 관리자 - 출금 관리

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/admin/withdraws` | 출금 신청 목록 조회 (상태 필터, 페이징) |
| PATCH | `/api/admin/withdraws/{withdrawNo}` | 출금 처리 (상태 변경) |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| WithdrawManagement.tsx | ✅ API 연동 완료 |

---

## 24. 관리자 - 수익 통계

### 백엔드
| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/admin/revenue/stats` | 수익 통계 요약 |
| GET | `/api/admin/revenue` | 수익 내역 목록 (필터/페이징) |

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| RevenueManagement.tsx | ✅ API 연동 완료 |

---

## 25. 관리자 - 대시보드

### 프론트엔드
| 페이지 | 상태 |
|--------|------|
| AdminDashboard.tsx | ✅ API 연동 완료 (출금 대기, 공지 현황, 문의 현황, 카테고리별 상품 통계) |

---

## 26. 스케줄러

| 파일 | 실행 주기 | 역할 |
|------|---------|------|
| `AuctionScheduler` | 30초마다 | 경매 종료 폴링 → 낙찰/유찰 처리 |
| `BannerScheduler` | 1분마다 (fixedRate) | 만료 배너(endAt 기준) 자동 비활성화 |
| `NotificationScheduler` | 매일 새벽 3시 | 30일 이상 오래된 알림 자동 삭제 |
| `PaymentReconciliationScheduler` | 5분마다 | PENDING 10분 초과 결제 PortOne 재확인 후 포인트 지급 or FAILED 처리 |
| `AuctionAutoConfirmScheduler` | ⚠️ 현재 30초마다 (테스트 모드) → 운영 시 매시 정각으로 변경 필요 | 7일 경과 낙찰 건 자동 구매 확정 |
| `AuctionAutoConfirmScheduler` (D-1/D-2 알림) | 매일 오전 10시 | 자동 확정 1일/2일 전 구매자 사전 알림 발송 |
| `AuctionAutoConfirmProcessor` | 스케줄러 호출 시 (REQUIRES_NEW) | 자동 확정 단건 처리: 에스크로 정산 → 상태 변경 → 매너온도 +0.2 → 알림 |

---

## 27. 미구현 항목 요약

| 항목 | 상태 | 내용 |
|------|------|------|
| FAQ 백엔드 | ❌ 미구현 | API 없음, 프론트 하드코딩 |
| 문의관리 닉네임 | ⚠️ 부분 | MOCK_USERS로 닉네임 매핑 중 |
| ReviewCreate.tsx | ⚠️ 데드코드 | 라우트만 등록, WonProductDetail 모달로 대체됨 |
| PaymentExpiryProcessor | ⚠️ 구현 완료, 미사용 | 12시간 미결제 승계 처리 로직 — 향후 활성화 필요 |

---

## 28. 운영 배포 전 체크리스트

| 항목 | 파일 | 현재 값 | 운영 값 |
|------|------|---------|---------|
| 자동 구매 확정 주기 | `AuctionAutoConfirmScheduler.java:55` | `"0/30 * * * * *"` (30초) | `"0 0 * * * *"` (매시 정각) |
| 입찰 취소 차단 시간 | `BidCancelService.java` | `CANCEL_BLOCK_HOURS = 0L` | `12L` |
