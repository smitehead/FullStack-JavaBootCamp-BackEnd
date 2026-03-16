package com.javajava.project.service;

import com.javajava.project.dto.MemberRequestDto;
import com.javajava.project.entity.Member;

public interface MemberService {
    Long join(MemberRequestDto dto);
    Member findOne(Long memberNo);
}