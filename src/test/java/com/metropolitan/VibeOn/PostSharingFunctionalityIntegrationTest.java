package com.metropolitan.VibeOn;

import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostSharingFunctionalityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void roleUserCanShareAvailablePostInTheirChatAndMessageIsBroadcast() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        Post post = createPost(bob);
        Chat chat = createChat(alice, bob);

        mockMvc.perform(post("/api/chats/{chatId}/posts/{postId}", chat.getId(), post.getId())
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.content").value("Shared a post"))
                .andExpect(jsonPath("$.sharedPost.id").value(post.getId()))
                .andExpect(jsonPath("$.sharedPost.imageUrl").value(post.getImageUrl()))
                .andExpect(jsonPath("$.sharedPost.description").value(post.getDescription()))
                .andExpect(jsonPath("$.sharedPost.username").value("bob"));

        Message savedMessage = messageRepository.findByChatIdOrderByCreatedAtDesc(chat.getId()).get(0);
        assertThat(savedMessage.getSharedPost().getId()).isEqualTo(post.getId());
        assertThat(chatRepository.findById(chat.getId()).orElseThrow().getLastMessage().getId())
                .isEqualTo(savedMessage.getId());
        verify(messagingTemplate).convertAndSend(
                eq("/topic/chat/" + chat.getId()),
                any(SingleMessageDto.class)
        );
    }

    @Test
    void userCannotSharePostIntoAChatTheyDoNotBelongTo() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User mallory = createUser("mallory");
        Post post = createPost(alice);
        Chat chat = createChat(alice, bob);

        mockMvc.perform(post("/api/chats/{chatId}/posts/{postId}", chat.getId(), post.getId())
                        .with(user(mallory.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());

        assertThat(messageRepository.findByChatIdOrderByCreatedAtDesc(chat.getId())).isEmpty();
        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/chat/" + chat.getId()),
                any(SingleMessageDto.class)
        );
    }

    @Test
    void deletedPostsCannotBeShared() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        Post post = createPost(alice);
        Chat chat = createChat(alice, bob);

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.saveAndFlush(post);
        entityManager.clear();

        mockMvc.perform(post("/api/chats/{chatId}/posts/{postId}", chat.getId(), post.getId())
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isNotFound());

        assertThat(messageRepository.findByChatIdOrderByCreatedAtDesc(chat.getId())).isEmpty();
    }

    @Test
    void sharingRequiresRoleUser() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        Post post = createPost(alice);
        Chat chat = createChat(alice, bob);

        mockMvc.perform(post("/api/chats/{chatId}/posts/{postId}", chat.getId(), post.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/chats/{chatId}/posts/{postId}", chat.getId(), post.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
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
        post.setDescription("Post to share");
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.saveAndFlush(post);
    }

    private Chat createChat(User firstUser, User secondUser) {
        Chat chat = new Chat();
        chat.setUsers(Set.of(firstUser, secondUser));
        return chatRepository.saveAndFlush(chat);
    }
}
