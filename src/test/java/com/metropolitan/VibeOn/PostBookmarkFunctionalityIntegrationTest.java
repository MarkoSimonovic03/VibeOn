package com.metropolitan.VibeOn;

import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.PostBookmarkRepository;
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
class PostBookmarkFunctionalityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostBookmarkRepository postBookmarkRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    @Test
    void roleUserCanSaveAndUnsaveAPost() throws Exception {
        User user = createUser("alice");
        Post post = createPost(createUser("author"));

        mockMvc.perform(post("/api/posts/{id}/bookmarks", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(post.getId()))
                .andExpect(jsonPath("$.bookmarkedByCurrentUser").value(true));

        mockMvc.perform(get("/api/posts/{id}", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarkedByCurrentUser").value(true));

        mockMvc.perform(delete("/api/posts/{id}/bookmarks", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarkedByCurrentUser").value(false));

        assertThat(postBookmarkRepository.count()).isZero();
    }

    @Test
    void savingTheSamePostTwiceDoesNotCreateADuplicate() throws Exception {
        User user = createUser("alice");
        Post post = createPost(createUser("author"));

        mockMvc.perform(post("/api/posts/{id}/bookmarks", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/posts/{id}/bookmarks", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarkedByCurrentUser").value(true));

        assertThat(postBookmarkRepository.count()).isOne();
    }

    @Test
    void savedPostsPageOnlyReturnsTheCurrentUsersBookmarks() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        Post firstPost = createPost(createUser("first-author"));
        Post secondPost = createPost(createUser("second-author"));

        mockMvc.perform(post("/api/posts/{id}/bookmarks", firstPost.getId())
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/posts/{id}/bookmarks", secondPost.getId())
                        .with(user(bob.getUsername()).roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/bookmarks")
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstPost.getId()))
                .andExpect(jsonPath("$[0].bookmarkedByCurrentUser").value(true));

        mockMvc.perform(get("/api/posts/bookmarks")
                        .with(user(bob.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(secondPost.getId()));
    }

    @Test
    void bookmarkEndpointsRequireRoleUserAndIgnoreSoftDeletedPosts() throws Exception {
        User user = createUser("alice");
        Post post = createPost(createUser("author"));

        mockMvc.perform(post("/api/posts/{id}/bookmarks", post.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/posts/{id}/bookmarks", post.getId())
                        .with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/posts/{id}/bookmarks", 999999)
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/posts/{id}/bookmarks", post.getId())
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk());

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.saveAndFlush(post);
        entityManager.clear();

        mockMvc.perform(get("/api/posts/bookmarks")
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/posts/{id}/bookmarks", post.getId())
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
