package com.javajava.project.controller;

import com.javajava.project.util.FileStore;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final FileStore fileStore;

    /**
     * 이미지 파일 서빙 엔드포인트
     *
     * 【개선사항】
     * 1. Cache-Control 헤더 추가: 브라우저가 7일간 캐시하여 반복 요청 불필요
     * 2. 파일명 Path Traversal 방지 (FileStore.getFullPath에서 처리)
     * 3. MIME 타입 감지 실패 시 image/jpeg 기본값 적용 (octet-stream 대신)
     * 4. 예외 타입별 명확한 상태 코드 반환
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> downloadImage(@PathVariable("filename") String filename) {
        try {
            // FileStore가 Path Traversal을 차단하고 안전한 절대 경로를 반환
            Path filePath = Paths.get(fileStore.getFullPath(filename));
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // MIME 타입 감지
            String mimeType;
            try {
                mimeType = Files.probeContentType(filePath);
            } catch (IOException e) {
                mimeType = null;
            }
            // 감지 실패 시 파일명 확장자로 추론, 그래도 없으면 image/jpeg 기본값 사용
            if (mimeType == null) {
                String lowerName = filename.toLowerCase();
                if (lowerName.endsWith(".png")) mimeType = "image/png";
                else if (lowerName.endsWith(".gif")) mimeType = "image/gif";
                else if (lowerName.endsWith(".webp")) mimeType = "image/webp";
                else if (lowerName.endsWith(".svg")) mimeType = "image/svg+xml";
                else mimeType = "image/jpeg"; // 기본값
            }

            return ResponseEntity.ok()
                    // 이미지는 7일 캐시 (604800초) - 반복 요청 방지로 성능 향상
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            // Path Traversal 등 잘못된 파일명
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
