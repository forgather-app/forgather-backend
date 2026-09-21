package com.forgather.domain.space.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record HostSpaceResponse(

    @Schema(description = "호스트 스페이스 목록", example = """
        [
            {
               "id": 1,
               "spaceCode": "1234567890",
               "name": "서양화 졸업 전시",
               "description": "나의 서양화 졸업 전시",
               "isPublic": true,
               "linkUrl": "https://forgather.app",
               "linkName": "포트폴리오",
               "spacePhotoPath": "images/prod/spaces/1234567890/product/UUID.webp",
               "guestBookCardCount": 15,
               "unreadGuestBookCount": 6,
               "isFeatured": true
            },
            {
              "id": 2,
              "spaceCode": "0987654321",
              "name": "동양화 졸업 전시",
              "description": "나의 동양화 졸업 전시",
              "isPublic": true,
              "linkUrl": "",
              "linkName": "",
              "spacePhotoPath": null,
              "guestBookCardCount": 0,
              "unreadGuestBookCount": 0,
              "isFeatured": false
            }
        ]
        """)
    List<HostSpaceItemResponse> spaces
) {
}
