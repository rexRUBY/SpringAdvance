package org.example.expert.domain.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expert.config.AuthUserArgumentResolver;
import org.example.expert.config.GlobalExceptionHandler;
import org.example.expert.config.JwtUtil;
import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.service.CommentService;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private AuthUserArgumentResolver authUserArgumentResolver;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(commentService))
                .setControllerAdvice(new GlobalExceptionHandler()) // 예외 처리 핸들러 설정
                .setCustomArgumentResolvers(authUserArgumentResolver) // AuthUserArgumentResolver 설정
                .build();
    }

    @Test
    void save_comment_성공() throws Exception {
        // given
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("Test comment");
        UserResponse userResponse = new UserResponse(1L, "user@example.com");
        CommentSaveResponse response = new CommentSaveResponse(1L, "Test comment", userResponse);

        // 인증된 사용자 가짜 설정
        given(authUserArgumentResolver.supportsParameter(any())).willReturn(true);
        given(authUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(new AuthUser(1L, "user@example.com", UserRole.USER));
        given(commentService.saveComment(any(), anyLong(), any())).willReturn(response);

        // when & then
        ResultActions resultActions = mockMvc.perform(
                post("/todos/{todoId}/comments", todoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk());
    }

    @Test
    public void getComments_성공() throws Exception {
        // given
        long todoId = 1L;
        List<CommentResponse> commentResponses = List.of();

        // when
        given(commentService.getComments(anyLong())).willReturn(commentResponses);

        // then
        mockMvc.perform(get("/todos/{todoId}/comments", todoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());  // 상태 코드 200 확인
        }
}
