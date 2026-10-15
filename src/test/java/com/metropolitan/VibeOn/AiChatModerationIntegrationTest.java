package com.metropolitan.VibeOn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.entity.*;
import com.metropolitan.VibeOn.repository.*;
import com.metropolitan.VibeOn.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AiChatModerationIntegrationTest {
    @Autowired private ChatService chatService;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ChatRepository chatRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private AiModerationFlagRepository flagRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void suspiciousMessageIsDeliveredNormallyThenFlaggedInBackgroundAndUsesExistingRemoval() throws Exception {
        User sender = createUser("ai-chat-sender");
        User recipient = createUser("ai-chat-recipient");
        User moderator = createUser("ai-chat-moderator");
        Chat chat = new Chat();
        chat.setUsers(new HashSet<>(Set.of(sender, recipient)));
        chat = chatRepository.saveAndFlush(chat);

        SingleMessageDto delivered = chatService.sendMessage(
                sender.getUsername(), chat.getId(), "I am selling cocaine and heroin"
        );

        assertThat(delivered.getId()).isNotNull();
        assertThat(delivered.getContent()).isEqualTo("I am selling cocaine and heroin");
        assertThat(messageRepository.findById(delivered.getId())).isPresent();
        assertThat(objectMapper.writeValueAsString(delivered))
                .doesNotContainIgnoringCase("flag", "moderation", "confidence");

        AiModerationFlag flag = awaitFlag(delivered.getId());
        assertThat(flag.getStatus()).isEqualTo(AiModerationFlagStatus.PENDING);
        assertThat(flag.getDrugsConfidence()).isGreaterThanOrEqualTo(0.90);

        mockMvc.perform(get("/api/moderation/ai-flags")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetType").value("MESSAGE"))
                .andExpect(jsonPath("$[0].messageId").value(delivered.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chat.getId()))
                .andExpect(jsonPath("$[0].authorUsername").value(sender.getUsername()))
                .andExpect(jsonPath("$[0].content").value("I am selling cocaine and heroin"))
                .andExpect(jsonPath("$[0].imageUrls").isEmpty())
                .andExpect(jsonPath("$[0].categoryConfidences.DRUGS").isNumber());

        mockMvc.perform(delete("/api/moderation/content/MESSAGE/{id}", delivered.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Illegal drug sales",
                                "aiFlagId", flag.getId()
                        ))))
                .andExpect(status().isOk());

        assertThat(messageRepository.findById(delivered.getId())).isEmpty();
        assertThat(flagRepository.findByMessageId(delivered.getId())).isEmpty();

        SingleMessageDto reviewedMessage = chatService.sendMessage(
                sender.getUsername(), chat.getId(), "A gun listed in a museum catalog"
        );
        AiModerationFlag reviewedFlag = awaitFlag(reviewedMessage.getId());
        mockMvc.perform(post("/api/moderation/ai-flags/{id}/approve", reviewedFlag.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Benign historical context"))))
                .andExpect(status().isNoContent());
        assertThat(messageRepository.findById(reviewedMessage.getId())).isPresent();
        assertThat(flagRepository.findById(reviewedFlag.getId()).orElseThrow().getStatus())
                .isEqualTo(AiModerationFlagStatus.DISMISSED);
    }

    private AiModerationFlag awaitFlag(Long messageId) throws Exception {
        LocalDateTime deadline = LocalDateTime.now().plusSeconds(5);
        while (LocalDateTime.now().isBefore(deadline)) {
            var flag = flagRepository.findByMessageId(messageId);
            if (flag.isPresent()) return flag.get();
            Thread.sleep(50);
        }
        throw new AssertionError("Message was not flagged by the background analyzer");
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
        user.setRoles(new HashSet<>(Set.of(role)));
        return userRepository.saveAndFlush(user);
    }
}
