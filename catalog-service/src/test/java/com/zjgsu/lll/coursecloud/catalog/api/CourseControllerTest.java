package com.zjgsu.lll.coursecloud.catalog.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjgsu.lll.coursecloud.catalog.controller.CourseRequest;
import com.zjgsu.lll.coursecloud.catalog.controller.DayOfWeekValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndListCourse() throws Exception {
        CourseRequest request = new CourseRequest(
                "CST101",
                "Introduction to Microservices",
                "inst-001",
                "Dr. Zhang",
                "zhang@example.com",
                DayOfWeekValue.MONDAY,
                "09:00",
                "11:00",
                80,
                60
        );

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CST101"));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Introduction to Microservices"));
    }
}
