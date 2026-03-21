package com.javajava.project.util;

import com.javajava.project.entity.ProductImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FileStore {

    @Value("${file.dir:C:/fullstack/javajavaproject/FullStack-JavaBootCamp-BackEnd/uploads/}")
    private String fileDir;

    public String getFullPath(String filename) {
        return fileDir + filename;
    }

    public List<ProductImage> storeFiles(List<MultipartFile> multipartFiles, Long productNo) throws IOException {
        List<ProductImage> storeFileResult = new ArrayList<>();
        if (multipartFiles == null || multipartFiles.isEmpty()) {
            return storeFileResult;
        }

        for (int i = 0; i < multipartFiles.size(); i++) {
            MultipartFile multipartFile = multipartFiles.get(i);
            if (!multipartFile.isEmpty()) {
                // 첫 번째 인덱스는 대표 사진(isMain = 1)
                storeFileResult.add(storeFile(multipartFile, productNo, i == 0 ? 1 : 0));
            }
        }
        return storeFileResult;
    }

    public ProductImage storeFile(MultipartFile multipartFile, Long productNo, Integer isMain) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String uuidName = createStoreFileName(originalFilename);
        
        // 디렉토리가 없으면 생성
        File dir = new File(fileDir);
        if(!dir.exists()) {
             dir.mkdirs();
        }

        multipartFile.transferTo(new File(getFullPath(uuidName)));

        return ProductImage.builder()
                .productNo(productNo)
                .originalName(originalFilename)
                .uuidName(uuidName)
                .imagePath(getFullPath(uuidName))
                .isMain(isMain)
                .build();
    }

    private String createStoreFileName(String originalFilename) {
        String ext = extractExt(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + ext;
    }

    private String extractExt(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
             return "jpg"; // 기본 확장자 처리
        }
        int pos = originalFilename.lastIndexOf(".");
        return originalFilename.substring(pos + 1);
    }
}
