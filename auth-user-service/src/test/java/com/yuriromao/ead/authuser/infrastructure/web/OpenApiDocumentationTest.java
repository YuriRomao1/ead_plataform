package com.yuriromao.ead.authuser.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldExposeOpenApiJsonForUsersApi() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("EAD Auth/User Service API"))
        .andExpect(jsonPath("$.paths['/users'].post.summary").value("Create a public user account"))
        .andExpect(content().string(containsString("USER_EMAIL_ALREADY_EXISTS")));
  }
}
