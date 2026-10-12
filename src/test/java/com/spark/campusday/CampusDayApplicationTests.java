package com.spark.campusday;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.mongodb.uri=mongodb://localhost:27017",
        "spring.data.mongodb.auto-index-creation=false"
})
class CampusDayApplicationTests {

    @Test
    void contextLoads() {
    }

}
