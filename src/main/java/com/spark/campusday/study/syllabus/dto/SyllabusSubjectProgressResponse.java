package com.spark.campusday.study.syllabus.dto;

import java.util.List;

public record SyllabusSubjectProgressResponse(
        SyllabusSubjectResponse subject,
        int completedTopics,
        int totalTopics,
        List<SyllabusTopicResponse> topics
) {
}
