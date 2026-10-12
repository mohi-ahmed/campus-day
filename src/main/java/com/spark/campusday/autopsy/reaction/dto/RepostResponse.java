package com.spark.campusday.autopsy.reaction.dto;

public record RepostResponse(
        String postId,
        boolean reposted,
        long reposts
) {
}
