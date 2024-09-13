package org.example.expert.domain.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expert.config.AuthUserArgumentResolver;
import org.example.expert.config.GlobalExceptionHandler;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private AuthUserArgumentResolver authUserArgumentResolver;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TodoController(todoService))
                .setControllerAdvice(new GlobalExceptionHandler()) // 예외 처리 핸들러 설정
                .setCustomArgumentResolvers(authUserArgumentResolver) // AuthUserArgumentResolver 설정
                .build();
    }

    @Test
    void saveTodo_성공() throws Exception {
        // given
        TodoSaveRequest todoSaveRequest = new TodoSaveRequest("New Todo", "This is a new task.");
        UserResponse userResponse = new UserResponse(2L, "b@b.com");
        TodoSaveResponse todoSaveResponse = new TodoSaveResponse(1L,"title", "contnet", "SUNNY", userResponse);

        // AuthUserArgumentResolver 모킹
        given(authUserArgumentResolver.supportsParameter(any())).willReturn(true);
        given(authUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(new AuthUser(1L, "user@example.com", UserRole.USER));

        // TodoService 모킹
        given(todoService.saveTodo(any(AuthUser.class), any(TodoSaveRequest.class))).willReturn(todoSaveResponse);

        // when & then
        mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(todoSaveRequest))
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoSaveResponse.getId()));
    }

    @Test
    void getTodos_성공() throws Exception {
        // given
        List<TodoResponse> todoList = List.of();
        Page<TodoResponse> todoPage = new PageImpl<>(todoList);

        // TodoService 모킹
        given(todoService.getTodos(anyInt(), anyInt())).willReturn(null);

        // when & then
        mockMvc.perform(get("/todos")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void getTodo_성공() throws Exception {
        // given
        long todoId = 1L;

        // when & then
        ResultActions resultActions = mockMvc.perform(get("/todos/{todoId}", todoId))
                .andExpect(status().isOk());
    }
}
