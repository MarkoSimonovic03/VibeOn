package com.metropolitan.VibeOn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.PostComment;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.PostCommentRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostCommentFunctionalityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostCommentRepository postCommentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    @Test
    void roleUserCanAddAndViewCommentsWithAuthorInformation() throws Exception {
        User author = createUser("author");
        User commenter = createUser("commenter");
        Post post = createPost(author);

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .with(user(commenter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("  A useful comment  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(post.getId()))
                .andExpect(jsonPath("$.username").value("commenter"))
                .andExpect(jsonPath("$.profileImageUrl").value("commenter.png"))
                .andExpect(jsonPath("$.content").value("A useful comment"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
                        .with(user(author.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("commenter"))
                .andExpect(jsonPath("$[0].content").value("A useful comment"));
    }

    @Test
    void commentAuthorCanDeleteTheirComment() throws Exception {
        User author = createUser("author");
        User commenter = createUser("commenter");
        Post post = createPost(author);
        PostComment comment = createComment(post, commenter, "Comment");

        mockMvc.perform(delete(
                        "/api/posts/{postId}/comments/{commentId}",
                        post.getId(),
                        comment.getId()
                ).with(user(commenter.getUsername()).roles("USER")))
                .andExpect(status().isNoContent());

        assertThat(postCommentRepository.findById(comment.getId())).isEmpty();
    }

    @Test
    void usersCannotDeleteCommentsWrittenByOtherUsersOrFromAnotherPostPath() throws Exception {
        User author = createUser("author");
        User commenter = createUser("commenter");
        User other = createUser("other");
        Post post = createPost(author);
        Post anotherPost = createPost(other);
        PostComment comment = createComment(post, commenter, "Comment");

        mockMvc.perform(delete(
                        "/api/posts/{postId}/comments/{commentId}",
                        post.getId(),
                        comment.getId()
                ).with(user(other.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(
                        "/api/posts/{postId}/comments/{commentId}",
                        anotherPost.getId(),
                        comment.getId()
                ).with(user(commenter.getUsername()).roles("USER")))
                .andExpect(status().isNotFound());

        assertThat(postCommentRepository.findById(comment.getId())).isPresent();
    }

    @Test
    void commentContentIsValidated() throws Exception {
        User commenter = createUser("commenter");
        Post post = createPost(createUser("author"));

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .with(user(commenter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("   ")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .with(user(commenter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("x".repeat(501))))
                .andExpect(status().isBadRequest());

        assertThat(postCommentRepository.count()).isZero();
    }

    @Test
    void commentEndpointsRequireRoleUserAndRejectSoftDeletedPosts() throws Exception {
        User commenter = createUser("commenter");
        Post post = createPost(createUser("author"));

        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.saveAndFlush(post);
        entityManager.clear();

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .with(user(commenter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("Comment")))
                .andExpect(status().isNotFound());
    }

    private String commentJson(String content) throws Exception {
        return objectMapper.writeValueAsString(Map.of("content", content));
    }

    private User createUser(String username) {
        Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setName(username);
        user.setLastName("Test");
        user.setEmail(username + "@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setProfileImageUrl(username + ".png");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setCreatedAt(LocalDate.now());
        user.setRoles(Set.of(role));
        return userRepository.saveAndFlush(user);
    }

    private Post createPost(User author) {
        Post post = new Post();
        post.setUser(author);
        post.setImageUrl(author.getUsername() + "-" + System.nanoTime() + ".png");
        post.setDescription("Post");
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.saveAndFlush(post);
    }

    private PostComment createComment(Post post, User commenter, String content) {
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(commenter);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        return postCommentRepository.saveAndFlush(comment);
    }
}
