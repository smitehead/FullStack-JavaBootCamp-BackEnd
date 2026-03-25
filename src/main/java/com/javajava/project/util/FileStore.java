package com.javajava.project.util;

import com.javajava.project.entity.ProductImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class FileStore {

    // 허용하는 이미지 MIME 타입 목록
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif",
            "image/webp", "image/bmp", "image/svg+xml"
    );

    // 허용하는 이미지 확장자 목록
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg"
    );

    // 최대 파일 크기: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    @Value("${file.dir:uploads/}")
    private String fileDir;

    /**
     * 업로드 디렉토리의 절대 경로를 반환합니다.
     * 상대 경로인 경우, 현재 실행 위치(user.dir) 기준 절대 경로로 변환합니다.
     */
    public String getUploadDir() {
        Path path = Paths.get(fileDir);
        if (path.isAbsolute()) {
            return path.toString();
        }
        // 상대 경로면 프로젝트 루트 기준 절대 경로로 변환
        return Paths.get(System.getProperty("user.dir"), fileDir).toAbsolutePath().normalize().toString();
    }

    /**
     * UUID 파일명으로 서버 저장 경로(절대 경로)를 반환합니다.
     * Path Traversal 공격 방지: 파일명에 경로 구분자가 있으면 제거
     */
    public String getFullPath(String filename) {
        // 경로 구분자가 포함된 파일명 차단 (Path Traversal 방지)
        String safeFilename = new File(filename).getName();
        return Paths.get(getUploadDir(), safeFilename).toAbsolutePath().normalize().toString();
    }

    /**
     * 여러 파일을 한꺼번에 저장합니다.
     * - 첫 번째 파일은 대표 이미지(isMain=1)로 저장됩니다.
     */
    public List<ProductImage> storeFiles(List<MultipartFile> multipartFiles, Long productNo) throws IOException {
        List<ProductImage> storeFileResult = new ArrayList<>();
        if (multipartFiles == null || multipartFiles.isEmpty()) {
            return storeFileResult;
        }

        for (int i = 0; i < multipartFiles.size(); i++) {
            MultipartFile multipartFile = multipartFiles.get(i);
            if (multipartFile != null && !multipartFile.isEmpty()) {
                storeFileResult.add(storeFile(multipartFile, productNo, i == 0 ? 1 : 0));
            }
        }
        return storeFileResult;
    }

    /**
     * 단일 파일을 저장합니다.
     * - 파일 크기 및 MIME 타입 검증
     * - UUID 기반 고유 파일명 생성
     * - 저장 디렉토리 자동 생성
     */
    public ProductImage storeFile(MultipartFile multipartFile, Long productNo, Integer isMain) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        // 1. 파일 크기 검증
        if (multipartFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다: " + multipartFile.getOriginalFilename());
        }

        // 2. 파일 타입 검증 (Content-Type)
        String contentType = multipartFile.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. 이미지 파일만 업로드 가능합니다: " + contentType);
        }

        // 3. 확장자 검증 (이중 체크)
        String originalFilename = multipartFile.getOriginalFilename();
        String ext = extractExt(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다: " + ext);
        }

        // 4. UUID 기반 고유 파일명 생성
        String uuidName = UUID.randomUUID().toString() + "." + ext.toLowerCase();

        // 5. 디렉토리 생성 (없으면 자동 생성)
        File dir = new File(getUploadDir());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 6. 파일 저장
        File targetFile = new File(dir, uuidName);
        multipartFile.transferTo(targetFile);

        return ProductImage.builder()
                .productNo(productNo)
                .originalName(originalFilename != null ? originalFilename : "unknown")
                .uuidName(uuidName)
                .imagePath(targetFile.getAbsolutePath())
                .isMain(isMain)
                .build();
    }

    /**
     * 물리적 파일을 삭제합니다. (상품 삭제 등에 활용)
     * @return 삭제 성공 여부
     */
    public boolean deleteFile(String uuidName) {
        try {
            Path filePath = Paths.get(getFullPath(uuidName));
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 파일명에서 확장자를 추출합니다.
     */
    private String extractExt(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "jpg";
        }
        int pos = originalFilename.lastIndexOf(".");
        String ext = originalFilename.substring(pos + 1);
        // 확장자의 쿼리스트링 제거 (예: foo.jpg?v=1 → jpg)
        int queryIdx = ext.indexOf('?');
        if (queryIdx > 0) {
            ext = ext.substring(0, queryIdx);
        }
        return ext;
    }
}
