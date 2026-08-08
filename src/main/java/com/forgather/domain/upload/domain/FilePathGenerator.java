package com.forgather.domain.upload.domain;

public class FilePathGenerator {

    private FilePathGenerator() {
    }

    public static String generateContentsFilePath(
        String rootDirectory,
        String spaceCode,
        UploadCategory category,
        String fileName
    ) {
        return "%s/%s/%s/%s/%s".formatted(
            rootDirectory,
            "spaces",
            spaceCode,
            category.toString(),
            fileName
        );
    }

    /**
     * 스페이스 생성 시점에는 spaceCode가 없으므로 인증된 hostId로 경로를 격리한다.
     * 격리가 없으면 클라이언트가 지정한 파일명만으로 다른 호스트의 객체 키를 겨냥해 덮어쓸 수 있다.
     */
    public static String generateSpacePhotoFilePath(
        String rootDirectory,
        Long hostId,
        String fileName
    ) {
        return "%s/spaces/photos/%d/%s".formatted(
            rootDirectory,
            hostId,
            fileName
        );
    }

    public static String generateExhibitionContentsFilePath(
        String rootDirectory,
        String fileName
    ) {
        return "%s/exhibitions/%s".formatted(
            rootDirectory,
            fileName
        );
    }

    public static String generateHostProfileFilePath(
        String rootDirectory,
        Long hostId,
        String fileName
    ) {
        return "%s/hosts/%d/profile/%s".formatted(
            rootDirectory,
            hostId,
            fileName
        );
    }
}
