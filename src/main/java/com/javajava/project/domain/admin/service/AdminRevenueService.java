package com.javajava.project.domain.admin.service;

import com.javajava.project.domain.platform.dto.PlatformRevenueResponseDto;
import com.javajava.project.domain.platform.entity.PlatformRevenue;
import com.javajava.project.domain.platform.repository.PlatformRevenueRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRevenueService {

    private final PlatformRevenueRepository revenueRepository;

    public Map<String, Object> getRevenueStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue",   revenueRepository.sumTotalRevenue());
        stats.put("monthlyRevenue", revenueRepository.sumRevenueInPeriod(monthStart, now));
        stats.put("todayRevenue",   revenueRepository.sumRevenueInPeriod(todayStart, todayEnd));
        stats.put("totalCount",     revenueRepository.count());
        return stats;
    }

    public Page<PlatformRevenueResponseDto> getRevenueList(
            String reason,
            Long sourceMemberNo,
            Long relatedProductNo,
            String startDate,
            String endDate,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<PlatformRevenue> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (reason != null && !reason.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("reason")),
                        "%" + reason.toLowerCase() + "%"));
            }
            if (sourceMemberNo != null) {
                predicates.add(cb.equal(root.get("sourceMemberNo"), sourceMemberNo));
            }
            if (relatedProductNo != null) {
                predicates.add(cb.equal(root.get("relatedProductNo"), relatedProductNo));
            }
            if (startDate != null && !startDate.isBlank()) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        LocalDate.parse(startDate).atStartOfDay()));
            }
            if (endDate != null && !endDate.isBlank()) {
                predicates.add(cb.lessThan(root.get("createdAt"),
                        LocalDate.parse(endDate).plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return revenueRepository.findAll(spec, pageable)
                .map(PlatformRevenueResponseDto::from);
    }
}
