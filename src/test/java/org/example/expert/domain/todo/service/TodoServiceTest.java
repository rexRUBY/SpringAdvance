package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private WeatherClient weatherClient;

    @InjectMocks
    private TodoService todoService;

    @Test
    void saveTodo_성공() {
        // given
        AuthUser authUser = new AuthUser(1L, "user@example.com", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        TodoSaveRequest todoSaveRequest = new TodoSaveRequest("Test Title", "Test Contents");

        given(weatherClient.getTodayWeather()).willReturn("Sunny");

        Todo newTodo = new Todo("Test Title", "Test Contents", "Sunny", user);
        given(todoRepository.save(any(Todo.class))).willReturn(newTodo);

        // when
        TodoSaveResponse response = todoService.saveTodo(authUser, todoSaveRequest);

        // then
        assertEquals("Test Title", response.getTitle());
        assertEquals("Test Contents", response.getContents());
        assertEquals("Sunny", response.getWeather());
        assertEquals(authUser.getId(), response.getUser().getId());
        assertEquals(authUser.getEmail(), response.getUser().getEmail());
    }

    @Test
    void getTodos_성공() {
        // given
        User user = new User("user@example.com", "password", UserRole.USER);
        Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Todo> todosPage = new PageImpl<>(List.of(todo), pageable, 1);

        given(todoRepository.findAllByOrderByModifiedAtDesc(pageable)).willReturn(todosPage);

        // when
        Page<TodoResponse> todos = todoService.getTodos(1, 10);

        // then
        assertEquals(1, todos.getTotalElements());
        assertEquals("Test Title", todos.getContent().get(0).getTitle());
        assertEquals("Test Contents", todos.getContent().get(0).getContents());
        assertEquals("Sunny", todos.getContent().get(0).getWeather());
        assertEquals(user.getEmail(), todos.getContent().get(0).getUser().getEmail());
    }

    @Test
    void getTodo_성공() {
        // given
        User user = new User("user@example.com", "password", UserRole.USER);
        Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);
        ReflectionTestUtils.setField(todo, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(todo, "modifiedAt", LocalDateTime.now());

        given(todoRepository.findByIdWithUser(anyLong())).willReturn(Optional.of(todo));

        // when
        TodoResponse response = todoService.getTodo(1L);

        // then
        assertEquals("Test Title", response.getTitle());
        assertEquals("Test Contents", response.getContents());
        assertEquals("Sunny", response.getWeather());
        assertEquals(user.getEmail(), response.getUser().getEmail());
    }
}
