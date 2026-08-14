package com.metropolitan.VibeOn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.MessageReactionRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.ChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatFeaturesIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private MessageReactionRepository messageReactionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ChatService chatService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recipientOpeningChatMarksOnlyIncomingMessagesAsRead() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User outsider = createUser("outsider");
        Chat chat = createChat(alice, bob);

        authenticateAs(alice);
        Long aliceMessageId = chatService.sendMessage("alice", chat.getId(), "Hello Bob").getId();
        assertThat(messageRepository.findById(aliceMessageId).orElseThrow().isRead()).isFalse();

        mockMvc.perform(get("/api/chats/{username}", bob.getUsername())
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].read").value(false));

        mockMvc.perform(get("/api/chats/{username}", alice.getUsername())
                        .with(user(bob.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].read").value(true));
        assertThat(messageRepository.findById(aliceMessageId).orElseThrow().isRead()).isTrue();
        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/chat/" + chat.getId() + "/read"),
                any(Object.class)
        );

        mockMvc.perform(patch("/api/chats/{chatId}/read", chat.getId())
                        .with(user(outsider.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void membersCanAddChangeAndRemoveOneReactionPerMessage() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User outsider = createUser("outsider");
        Chat chat = createChat(alice, bob);
        authenticateAs(alice);
        Long messageId = chatService.sendMessage("alice", chat.getId(), "React to this").getId();
        SecurityContextHolder.clearContext();

        react(chat.getId(), messageId, bob, "👍")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions.length()").value(1))
                .andExpect(jsonPath("$.reactions[0].username").value("bob"))
                .andExpect(jsonPath("$.reactions[0].emoji").value("👍"));
        react(chat.getId(), messageId, bob, "😂")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions.length()").value(1))
                .andExpect(jsonPath("$.reactions[0].emoji").value("😂"));
        assertThat(messageReactionRepository.count()).isOne();

        react(chat.getId(), messageId, alice, "❤️").andExpect(status().isOk());
        assertThat(messageReactionRepository.count()).isEqualTo(2);

        mockMvc.perform(delete("/api/chats/{chatId}/messages/{messageId}/reaction", chat.getId(), messageId)
                        .with(user(bob.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions.length()").value(1))
                .andExpect(jsonPath("$.reactions[0].username").value("alice"));

        react(chat.getId(), messageId, outsider, "👍").andExpect(status().isForbidden());
        react(chat.getId(), messageId, bob, "not-allowed").andExpect(status().isBadRequest());
    }

    @Test
    void chatFeatureEndpointsRequireRoleUserAndMessageMustBelongToChat() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User charlie = createUser("charlie");
        Chat firstChat = createChat(alice, bob);
        Chat secondChat = createChat(alice, charlie);
        authenticateAs(alice);
        Long messageId = chatService.sendMessage("alice", firstChat.getId(), "Message").getId();
        SecurityContextHolder.clearContext();

        mockMvc.perform(patch("/api/chats/{chatId}/read", firstChat.getId()))
                .andExpect(status().isUnauthorized());
        react(firstChat.getId(), messageId, "admin", "ADMIN", "👍")
                .andExpect(status().isForbidden());
        react(secondChat.getId(), messageId, alice, "👍")
                .andExpect(status().isNotFound());

        assertThatThrownBy(() -> chatService.requireChatMembership(firstChat.getId(), "charlie"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private org.springframework.test.web.servlet.ResultActions react(
            Long chatId,
            Long messageId,
            User user,
            String emoji
    ) throws Exception {
        return react(chatId, messageId, user.getUsername(), "USER", emoji);
    }

    private org.springframework.test.web.servlet.ResultActions react(
            Long chatId,
            Long messageId,
            String username,
            String role,
            String emoji
    ) throws Exception {
        return mockMvc.perform(put("/api/chats/{chatId}/messages/{messageId}/reaction", chatId, messageId)
                .with(user(username).roles(role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("emoji", emoji))));
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

    private Chat createChat(User first, User second) {
        Chat chat = new Chat();
        chat.setUsers(Set.of(first, second));
        return chatRepository.saveAndFlush(chat);
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
