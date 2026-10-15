package com.metropolitan.VibeOn;

import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SearchFunctionalityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    @Test
    void roleUserCanSearchOtherUsersByUsernameOrNameWithoutPrivateData() throws Exception {
        User alice = createUser("alice", "Alice", "Current");
        createUser("bobby", "Robert", "Stone");
        createUser("charlie", "Boris", "Green");

        mockMvc.perform(get("/api/search/users")
                        .param("query", "BO")
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("bobby"))
                .andExpect(jsonPath("$[0].name").value("Robert"))
                .andExpect(jsonPath("$[0].profileImageUrl").value("bobby.png"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());

        mockMvc.perform(get("/api/search/users")
                        .param("query", "alice")
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void hashtagSearchIsCaseInsensitiveAndMatchesCompleteHashtagsOnly() throws Exception {
        User alice = createUser("alice", "Alice", "Test");
        User author = createUser("author", "Post", "Author");
        Post exactUppercase = createPost(author, "A day outside #Travel");
        Post exactPunctuation = createPost(author, "#travel, plans");
        createPost(author, "Try #traveller");
        createPost(author, "Embeddedword#travel");

        mockMvc.perform(get("/api/search/posts")
                        .param("hashtag", "travel")
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(
                        exactPunctuation.getId().intValue(),
                        exactUppercase.getId().intValue()
                )))
                .andExpect(jsonPath("$[0].likeCount").value(0))
                .andExpect(jsonPath("$[0].likedByCurrentUser").value(false))
                .andExpect(jsonPath("$[0].bookmarkedByCurrentUser").value(false));
    }

    @Test
    void softDeletedPostsAreNotReturnedByHashtagSearch() throws Exception {
        User alice = createUser("alice", "Alice", "Test");
        Post deletedPost = createPost(createUser("author", "Post", "Author"), "#hidden");
        deletedPost.setDeleted(true);
        deletedPost.setDeletedAt(LocalDateTime.now());
        postRepository.saveAndFlush(deletedPost);
        entityManager.clear();

        mockMvc.perform(get("/api/search/posts")
                        .param("hashtag", "hidden")
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchRequiresRoleUserAndValidParameters() throws Exception {
        User alice = createUser("alice", "Alice", "Test");

        mockMvc.perform(get("/api/search/users").param("query", "alice"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/search/users")
                        .param("query", "alice")
                        .with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/search/users")
                        .param("query", "   ")
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/search/posts")
                        .param("hashtag", "not valid!")
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isBadRequest());
    }

    private User createUser(String username, String name, String lastName) {
        Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setLastName(lastName);
        user.setEmail(username + "@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setProfileImageUrl(username + ".png");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setCreatedAt(LocalDate.now());
        user.setRoles(Set.of(role));
        return userRepository.saveAndFlush(user);
    }

    private Post createPost(User author, String description) {
        Post post = new Post();
        post.setUser(author);
        post.setImageUrl(author.getUsername() + "-" + System.nanoTime() + ".png");
        post.setDescription(description);
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.saveAndFlush(post);
    }
}
