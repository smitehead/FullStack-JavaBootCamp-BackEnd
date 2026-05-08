package com.javajava.project.domain.chat.service;

import com.javajava.project.domain.chat.entity.ChatRoom;
import com.javajava.project.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatRoomHelper {

    private final ChatRoomRepository chatRoomRepository;

    // REQUIRES_NEW: 독립 트랜잭션에서 INSERT를 즉시 실행한다.
    // 유니크 제약 위반 시 이 트랜잭션만 롤백되고, 호출자 트랜잭션은 그대로 유지된다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatRoom tryInsert(Long buyerNo, Long sellerNo, Long productNo) {
        ChatRoom newRoom = ChatRoom.builder()
                .buyerNo(buyerNo)
                .sellerNo(sellerNo)
                .productNo(productNo)
                .build();
        return chatRoomRepository.saveAndFlush(newRoom);
    }
}
