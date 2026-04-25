package com.javajava.project.global.sse;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseTicketService {

    private record SseTicket(Long memberNo, long expiresAt) {}

    private final Map<String, SseTicket> tickets = new ConcurrentHashMap<>();

    private static final long TICKET_TTL_MS = 10_000; // 10초

    public String generateTicket(Long memberNo) {
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, new SseTicket(memberNo, System.currentTimeMillis() + TICKET_TTL_MS));
        return ticket;
    }

    public Long validateAndConsume(String ticket) {
        SseTicket t = tickets.remove(ticket);
        if (t == null || System.currentTimeMillis() > t.expiresAt()) return null;
        return t.memberNo();
    }
}
