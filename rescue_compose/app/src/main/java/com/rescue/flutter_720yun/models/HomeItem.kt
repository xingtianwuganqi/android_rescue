package com.rescue.flutter_720yun.models

data class HomeItem(
    val id: Int?,
    val content: String?,
    val desc: String?,
    val images: List<String>?,
    val language: String?,
    val region: String?
)
/*
"id": 8,
            "createdAt": "2026-01-05T14:02:23.087Z",
            "updatedAt": "2026-01-12T13:53:48.08Z",
            "deletedAt": null,
            "user": {
                "id": 1,
                "createdAt": "2025-06-23T13:59:44.994Z",
                "updatedAt": "2025-11-25T13:52:58.676Z",
                "deletedAt": null,
                "phone": "13689242201",
                "email": "",
                "username": "阿飞",
                "avatar": "2025-09/sb4U19aI_1080x1920.png",
                "wx": "",
                "location": 0,
                "language": "",
                "region": "",
                "role": "admin"
            },
            "userId": 1,
            "content": "发帖",
            "images": [],
            "topic": null,
            "topicId": null,
            "likeNum": 1,
            "collectionNum": 1,
            "commentNum": 0,
            "likeStatus": 1,
            "collectionStatus": 0,
            "language": "zh",
            "region": "cn"
 */