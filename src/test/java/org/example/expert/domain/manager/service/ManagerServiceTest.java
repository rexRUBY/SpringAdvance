package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private ManagerService managerService;

    @Nested
    @DisplayName("Manager 저장 기능 테스트")
    class ManagerSaveTests {

        @Test
        void todo의_user가_null인_경우_예외가_발생한다() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerUserId = 2L;

            Todo todo = new Todo();
            ReflectionTestUtils.setField(todo, "user", null);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            assertEquals("담당자를 등록하려고 하는 유저가 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }

        @Test
        void todo가_정상적으로_등록된다() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

            long todoId = 1L;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            long managerUserId = 2L;
            User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
            ReflectionTestUtils.setField(managerUser, "id", managerUserId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));
            given(managerRepository.save(any(Manager.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ManagerSaveResponse response = managerService.saveManager(authUser, todoId, managerSaveRequest);

            // then
            assertNotNull(response);
            assertEquals(managerUser.getId(), response.getUser().getId());
            assertEquals(managerUser.getEmail(), response.getUser().getEmail());
        }

        @Test
        void 일정_작성자가_본인을_담당자로_등록할_때_예외발생() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정 작성자와 AuthUser가 동일
            long todoId = 1L;

            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            long managerUserId = authUser.getId();
            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(managerUserId)).willReturn(Optional.of(user));

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            // then
            assertEquals("일정 작성자는 본인을 담당자로 등록할 수 없습니다.", exception.getMessage());
        }

    }

    @Nested
    @DisplayName("Manager 조회 기능 테스트")
    class ManagerGetTests {

        @Test
        void manager_목록_조회_시_Todo가_없다면_InvalidRequestException_에러를_던진다() {
            // given
            long todoId = 1L;
            given(todoRepository.findById(todoId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> managerService.getManagers(todoId));
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test
        void manager_목록_조회에_성공한다() {
            // given
            long todoId = 1L;
            User user = new User("user1@example.com", "password", UserRole.USER);
            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Manager mockManager = new Manager(todo.getUser(), todo);
            List<Manager> managerList = List.of(mockManager);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(managerList);

            // when
            List<ManagerResponse> managerResponses = managerService.getManagers(todoId);

            // then
            assertEquals(1, managerResponses.size());
            assertEquals(mockManager.getId(), managerResponses.get(0).getId());
            assertEquals(mockManager.getUser().getEmail(), managerResponses.get(0).getUser().getEmail());
        }
    }

    @Nested
    @DisplayName("매니저 삭제기능 테스트")
    class ManagerDeleteTests {
        @Test
        void todo가_없으면_예외_발생() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerId = 1L;

            given(todoRepository.findById(todoId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> managerService.deleteManager(authUser, todoId, managerId));
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test
        void manager_삭제_성공() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerId = 1L;

            User user = new User("a@a.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", authUser.getId());

            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Manager manager = new Manager(user, todo);
            ReflectionTestUtils.setField(manager, "id", managerId);

            // when
            given(todoRepository.findById(todo.getId())).willReturn(Optional.of(todo));
            given(managerRepository.findById(manager.getId())).willReturn(Optional.of(manager));
            managerService.deleteManager(authUser, todoId, managerId);

            // then
            verify(managerRepository, times(1)).delete(manager);
        }

        @Test
        void 일정의_작성자와_삭제_요청_유저가_다를_때_예외발생() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerId = 1L;

            // 다른 유저가 생성한 Todo
            User differentUser = new User("other@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(differentUser, "id", 2L);  // 다른 유저 ID 설정

            Todo todo = new Todo("Title", "Contents", "Sunny", differentUser);  // 다른 유저가 작성한 일정
            ReflectionTestUtils.setField(todo, "id", todoId);

            // Todo가 존재하는 경우
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.deleteManager(authUser, todoId, managerId);
            });

            assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());  // 예외 메시지 검증
        }

        @Test
        void 매니저가_해당_일정과_연관이_없을_때_예외발생() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerId = 1L;

            User user = new User("a@a.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", authUser.getId());

            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            // 다른 Todo와 연관된 Manager
            Todo differentTodo = new Todo("Other Title", "Other Contents", "Cloudy", user);
            ReflectionTestUtils.setField(differentTodo, "id", 2L);

            Manager manager = new Manager(user, differentTodo);  // 다른 Todo와 연관된 Manager
            ReflectionTestUtils.setField(manager, "id", managerId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.deleteManager(authUser, todoId, managerId);
            });

            assertEquals("해당 일정에 등록된 담당자가 아닙니다.", exception.getMessage());
        }

        @Test
        void 일정의_작성자가_null일_때_예외발생() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerId = 1L;

            // Todo 객체의 작성자(User)가 null인 경우
            Todo todo = new Todo();
            ReflectionTestUtils.setField(todo, "id", todoId);
            ReflectionTestUtils.setField(todo, "user", null);  // 작성자 null 설정

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.deleteManager(authUser, todoId, managerId)
            );

            assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }

    }
}
