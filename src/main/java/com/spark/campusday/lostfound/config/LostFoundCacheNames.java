package com.spark.campusday.lostfound.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LostFoundCacheNames {
    public static final String FEED = "lostFoundFeed";
    public static final String POSTS = "lostFoundPosts";
    public static final String COMMENTS = "lostFoundComments";
}
