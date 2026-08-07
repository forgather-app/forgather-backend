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

    public static String generateSpacePhotoFilePath(
        String rootDirectory,
        String fileName
    ) {
        return "%s/spaces/photos/%s".formatted(
            rootDirectory,
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
