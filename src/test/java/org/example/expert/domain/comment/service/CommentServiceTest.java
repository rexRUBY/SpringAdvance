package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.common.exception.ServerException;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TodoRepository todoRepository;
    @Mock
    private ManagerRepository managerRepository;
    @InjectMocks
    private CommentService commentService;

    @Test
    void comment_등록_중_할일을_찾지_못해_에러가_발생한다() {
        // given
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);

        // 할일을 찾지 못한 경우, repository에서 Optional.empty()를 반환
        given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            commentService.saveComment(authUser, todoId, request);
        });

        // then
        assertEquals("Todo not found", exception.getMessage());
    }


    @Test
    void comment를_정상적으로_등록한다() {
        // given
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        Todo todo = new Todo("title", "contents", "weather", user);
        Comment comment = new Comment(request.getContents(), user, todo);

        // 매니저 리스트에 현재 유저가 포함되어 있는 경우를 모킹
        Manager manager = new Manager(user, todo); // 매니저 객체 생성
        List<Manager> managers = new ArrayList<>();
        managers.add(manager);

        // 할일을 찾고, 매니저 리스트에 유저가 포함되어 있으며, 댓글이 저장되는 상황을 모킹
        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo)); // 할일 찾기
        given(managerRepository.findByTodoIdWithUser(todo.getId())).willReturn(managers); // 매니저 리스트 반환
        given(commentRepository.save(any(Comment.class))).willReturn(comment); // 댓글 저장

        // when
        CommentSaveResponse result = commentService.saveComment(authUser, todoId, request);

        // then
        assertNotNull(result);
        assertEquals(comment.getContents(), result.getContents()); // 저장된 댓글의 내용이 일치하는지 확인
    }


    @Test
    void 댓글_목록_가져오기_성공() {
        // given
        User user = new User("test@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "id", 1L);

        Comment comment1 = new Comment("First comment", user, todo);
        ReflectionTestUtils.setField(comment1,"id", 1L);
        Comment comment2 = new Comment("Second comment", user, todo);
        ReflectionTestUtils.setField(comment2,"id",2L);

        List<Comment> comments = Arrays.asList(comment1, comment2);
        given(commentRepository.findByTodoIdWithUser(anyLong())).willReturn(comments);

        // when
        List<CommentResponse> commentResponses = commentService.getComments(todo.getId());

        // then
        assertNotNull(commentResponses);
    }
}
