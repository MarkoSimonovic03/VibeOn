package com.metropolitan.VibeOn;

import com.metropolitan.VibeOn.entity.Follow;
import com.metropolitan.VibeOn.entity.FollowRequest;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.repository.FollowRequestRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PrivateAccountFunctionalityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private FollowRepository followRepository;
    @Autowired
    private FollowRequestRepository followRequestRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void privateAccountCreatesOnePendingRequestAndRejectsSelfFollow() throws Exception {
        User owner = createUser("owner", true);
        User requester = createUser("requester", false);

        mockMvc.perform(get("/api/auth/user/{username}", owner.getUsername())
                        .with(user(requester.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrivate").value(true))
                .andExpect(jsonPath("$.private").doesNotExist())
                .andExpect(jsonPath("$.followRequestPending").value(false))
                .andExpect(jsonPath("$.canViewPosts").value(false));

        mockMvc.perform(post("/api/follows/{userId}", owner.getId())
                        .with(user(requester.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.requestPending").value(true));

        mockMvc.perform(post("/api/follows/{userId}", owner.getId())
                        .with(user(requester.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestPending").value(true));

        assertThat(followRequestRepository.count()).isEqualTo(1);
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(
                requester.getId(), owner.getId()
        )).isFalse();

        mockMvc.perform(get("/api/auth/user/{username}", owner.getUsername())
                        .with(user(requester.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followRequestPending").value(true))
                .andExpect(jsonPath("$.canViewPosts").value(false));

        mockMvc.perform(delete("/api/follows/{userId}/request", owner.getId())
                        .with(user(requester.getUsername()).roles("USER")))
                .andExpect(status().isNoContent());
        assertThat(followRequestRepository.count()).isZero();

        mockMvc.perform(get("/api/auth/user/{username}", owner.getUsername())
                        .with(user(requester.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followRequestPending").value(false))
                .andExpect(jsonPath("$.canViewPosts").value(false));

        mockMvc.perform(post("/api/follows/{userId}", requester.getId())
                        .with(user(requester.getUsername()).roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerCanViewApproveAndRejectPendingRequests() throws Exception {
        User owner = createUser("owner", true);
        User first = createUser("first", false);
        User second = createUser("second", false);
        FollowRequest firstRequest = createRequest(first, owner);
        FollowRequest secondRequest = createRequest(second, owner);

        mockMvc.perform(get("/api/follows/requests")
                        .with(user(owner.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("first"));

        mockMvc.perform(post("/api/follows/requests/{id}/approve", firstRequest.getId())
                        .with(user(owner.getUsername()).roles("USER")))
                .andExpect(status().isNoContent());
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(
                first.getId(), owner.getId()
        )).isTrue();
        assertThat(followRequestRepository.findById(firstRequest.getId())).isEmpty();

        mockMvc.perform(delete("/api/follows/requests/{id}", secondRequest.getId())
                        .with(user(owner.getUsername()).roles("USER")))
                .andExpect(status().isNoContent());
        assertThat(followRequestRepository.findById(secondRequest.getId())).isEmpty();
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(
                second.getId(), owner.getId()
        )).isFalse();
    }

    @Test
    void privatePostsAreHiddenUntilTheFollowRequestIsApproved() throws Exception {
        User owner = createUser("owner", true);
        User viewer = createUser("viewer", false);
        Post privatePost = createPost(owner);

        mockMvc.perform(get("/api/posts/user/{username}", owner.getUsername())
                        .with(user(viewer.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/posts/{id}", privatePost.getId())
                        .with(user(viewer.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/posts")
                        .with(user(viewer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        FollowRequest request = createRequest(viewer, owner);
        mockMvc.perform(post("/api/follows/requests/{id}/approve", request.getId())
                        .with(user(owner.getUsername()).roles("USER")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/user/{username}", owner.getUsername())
                        .with(user(viewer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(privatePost.getId()));
        mockMvc.perform(get("/api/posts/{id}", privatePost.getId())
                        .with(user(viewer.getUsername()).roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void accountOwnerCanChangePrivacyAndPublicAccountsStillFollowImmediately() throws Exception {
        User owner = createUser("owner", false);
        User follower = createUser("follower", false);

        mockMvc.perform(patch("/api/follows/privacy")
                        .with(user(owner.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPrivate\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrivate").value(true));
        assertThat(userRepository.findById(owner.getId()).orElseThrow().isPrivate()).isTrue();

        mockMvc.perform(patch("/api/follows/privacy")
                        .with(user(owner.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPrivate\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrivate").value(false));
        mockMvc.perform(post("/api/follows/{userId}", owner.getId())
                        .with(user(follower.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.requestPending").value(false));
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(
                follower.getId(), owner.getId()
        )).isTrue();
    }

    private User createUser(String username, boolean isPrivate) {
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
        user.setPrivate(isPrivate);
        user.setRoles(Set.of(role));
        return userRepository.saveAndFlush(user);
    }

    private FollowRequest createRequest(User requester, User target) {
        FollowRequest request = new FollowRequest();
        request.setRequester(requester);
        request.setTarget(target);
        request.setCreatedAt(LocalDateTime.now());
        return followRequestRepository.saveAndFlush(request);
    }

    private Post createPost(User author) {
        Post post = new Post();
        post.setUser(author);
        post.setImageUrl(author.getUsername() + "-" + System.nanoTime() + ".png");
        post.setDescription("Private post");
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.saveAndFlush(post);
    }
}
