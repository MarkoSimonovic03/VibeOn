package com.metropolitan.VibeOn;

import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.PostLikeRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
class PostLikeFunctionalityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    @Test
    void roleUserCanLikeAndUnlikeAPost() throws Exception {
        User user = createUser("alice");
        Post post = createPost(createUser("author"));

        mockMvc.perform(post("/api/posts/{id}/likes", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(post.getId()))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByCurrentUser").value(true));

        mockMvc.perform(get("/api/posts/{id}", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByCurrentUser").value(true));

        mockMvc.perform(delete("/api/posts/{id}/likes", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByCurrentUser").value(false));

        assertThat(postLikeRepository.count()).isZero();
    }

    @Test
    void likingTheSamePostTwiceDoesNotCreateADuplicate() throws Exception {
        User user = createUser("alice");
        Post post = createPost(createUser("author"));

        mockMvc.perform(post("/api/posts/{id}/likes", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/posts/{id}/likes", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));

        assertThat(postLikeRepository.countByPostId(post.getId())).isOne();
    }

    @Test
    void likeCountAndCurrentUserStateAreCalculatedPerUser() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User charlie = createUser("charlie");
        Post post = createPost(createUser("author"));

        mockMvc.perform(post("/api/posts/{id}/likes", post.getId())
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/posts/{id}/likes", post.getId())
                        .with(user(bob.getUsername()).roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}", post.getId())
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(2))
                .andExpect(jsonPath("$.likedByCurrentUser").value(true));

        mockMvc.perform(get("/api/posts/{id}", post.getId())
                        .with(user(charlie.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(2))
                .andExpect(jsonPath("$.likedByCurrentUser").value(false));
    }

    @Test
    void likeEndpointsRequireRoleUserAndRejectMissingOrSoftDeletedPosts() throws Exception {
        User user = createUser("alice");
        Post post = createPost(createUser("author"));

        mockMvc.perform(post("/api/posts/{id}/likes", post.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/posts/{id}/likes", post.getId())
                        .with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/posts/{id}/likes", 999999)
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isNotFound());

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.saveAndFlush(post);
        entityManager.clear();

        mockMvc.perform(post("/api/posts/{id}/likes", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isNotFound());
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
}
