package org.example.expert.domain.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expert.config.AuthUserArgumentResolver;
import org.example.expert.config.GlobalExceptionHandler;
import org.example.expert.config.JwtUtil;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.service.ManagerService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerController.class)
class ManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManagerService managerService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private AuthUserArgumentResolver authUserArgumentResolver;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ManagerController(managerService, jwtUtil))
                .setControllerAdvice(new GlobalExceptionHandler()) // 예외 처리 핸들러 설정
                .setCustomArgumentResolvers(authUserArgumentResolver) // AuthUserArgumentResolver 설정
                .build();
    }

    @Test
    void saveManager_성공() throws Exception {
        // given
        long todoId = 1L;
        ManagerSaveRequest request = new ManagerSaveRequest(2L);
        UserResponse userResponse = new UserResponse(2L, "b@b.com");
        ManagerSaveResponse response = new ManagerSaveResponse(1L, userResponse);

        // ManagerService 모킹
        given(authUserArgumentResolver.supportsParameter(any())).willReturn(true);
        given(authUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(new AuthUser(1L, "a@a.com", UserRole.USER));
        given(managerService.saveManager(any(),anyLong(),any())).willReturn(response);

        // when & then
        ResultActions resultActions = mockMvc.perform
                (post("/todos/{todoId}/managers", todoId, request)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void getMembers_성공() throws Exception {
        // given
        long todoId = 1L;
        List<ManagerResponse> managerResponses = List.of(
                new ManagerResponse(1L, new UserResponse(1L, "manager1@example.com")),
                new ManagerResponse(2L, new UserResponse(2L, "manager2@example.com"))
        );

        // ManagerService 모킹
        given(managerService.getManagers(todoId)).willReturn(managerResponses);

        // when & then
        mockMvc.perform(get("/todos/{todoId}/managers", todoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteManager_성공() throws Exception {
        // given
        long todoId = 1L;
        long managerId = 2L;

        // AuthUserArgumentResolver 모킹
        given(authUserArgumentResolver.supportsParameter(any())).willReturn(true);
        given(authUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(new AuthUser(1L, "user@example.com", UserRole.USER));

        // ManagerService의 deleteManager 메서드 모킹
        Mockito.doNothing().when(managerService).deleteManager(any(AuthUser.class), anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/todos/{todoId}/managers/{managerId}", todoId, managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

}
