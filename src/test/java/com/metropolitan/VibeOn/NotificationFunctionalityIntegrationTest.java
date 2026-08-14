package com.metropolitan.VibeOn;

import com.metropolitan.VibeOn.dto.FullChatDto;
import com.metropolitan.VibeOn.entity.Notification;
import com.metropolitan.VibeOn.entity.NotificationType;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.NotificationRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.ChatService;
import com.metropolitan.VibeOn.service.FollowService;
import com.metropolitan.VibeOn.service.ImageStorageService;
import com.metropolitan.VibeOn.service.PostCommentService;
import com.metropolitan.VibeOn.service.PostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationFunctionalityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private FollowService followService;
    @Autowired
    private PostService postService;
    @Autowired
    private PostCommentService postCommentService;
    @Autowired
    private ChatService chatService;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @BeforeEach
    void configureImageStorage() throws Exception {
        when(imageStorageService.save(any())).thenReturn("notification-test-image.png");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void existingActionsCreateNotificationsForTheirRecipients() throws Exception {
        User recipient = createUser("recipient");
        User actor = createUser("actor");
        Post recipientPost = createPost(recipient, "Recipient post");
        authenticateAs(actor);

        followService.toggleFollow(recipient.getId());
        postService.likePost(recipientPost.getId());
        postCommentService.addComment(
                recipientPost.getId(),
                "A comment mentioning @recipient"
        );
        FullChatDto chat = chatService.getChat(recipient.getUsername());
        chatService.sendMessage(actor.getUsername(), chat.getId(), "Hello in chat");
        postService.createPost(
                new MockMultipartFile(
                        "image", "post.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1}
                ),
                "A new post mentioning @recipient"
        );

        List<Notification> notifications = notificationRepository
                .findAllByRecipientIdOrderByCreatedAtDescIdDesc(recipient.getId());

        assertThat(notifications).hasSize(6);
        assertThat(notifications).extracting(Notification::getType).containsExactlyInAnyOrder(
                NotificationType.FOLLOW,
                NotificationType.POST_LIKE,
                NotificationType.POST_COMMENT,
                NotificationType.COMMENT_MENTION,
                NotificationType.CHAT_MESSAGE,
                NotificationType.POST_MENTION
        );
        assertThat(notifications).allSatisfy(notification -> {
            assertThat(notification.getActor()).isEqualTo(actor);
            assertThat(notification.isRead()).isFalse();
            assertThat(notification.getContextText()).isNotBlank();
            assertThat(notification.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void ownLikesCommentsAndMentionsDoNotCreateNotifications() throws Exception {
        User owner = createUser("owner");
        Post post = createPost(owner, "Post by @owner");
        authenticateAs(owner);

        postService.likePost(post.getId());
        postCommentService.addComment(post.getId(), "My own comment mentioning @owner");
        postService.createPost(
                new MockMultipartFile(
                        "image", "self.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1}
                ),
                "Another self mention @owner"
        );

        assertThat(notificationRepository.findAllByRecipientIdOrderByCreatedAtDescIdDesc(owner.getId()))
                .isEmpty();
    }

    @Test
    void notificationApiEnforcesOwnershipAndUpdatesReadState() throws Exception {
        User recipient = createUser("recipient");
        User actor = createUser("actor");
        authenticateAs(actor);
        followService.toggleFollow(recipient.getId());
        Notification notification = notificationRepository
                .findAllByRecipientIdOrderByCreatedAtDescIdDesc(recipient.getId()).get(0);
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/notifications").with(user(actor.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/notifications").with(user(recipient.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("FOLLOW"))
                .andExpect(jsonPath("$[0].actorUsername").value("actor"))
                .andExpect(jsonPath("$[0].read").value(false));
        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(user(recipient.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .with(user(actor.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .with(user(recipient.getUsername()).roles("USER")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(user(recipient.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        User secondActor = createUser("second-actor");
        authenticateAs(secondActor);
        followService.toggleFollow(recipient.getId());
        mockMvc.perform(patch("/api/notifications/read-all")
                        .with(user(recipient.getUsername()).roles("USER")))
                .andExpect(status().isNoContent());
        assertThat(notificationRepository.countByRecipientIdAndReadFalse(recipient.getId())).isZero();
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

    private Post createPost(User author, String description) {
        Post post = new Post();
        post.setUser(author);
        post.setImageUrl(author.getUsername() + "-" + System.nanoTime() + ".png");
        post.setDescription(description);
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.saveAndFlush(post);
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }
}
