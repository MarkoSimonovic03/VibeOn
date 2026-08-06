package com.metropolitan.VibeOn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.Report;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.ReportRepository;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportFunctionalityIntegrationTest {

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
    private ChatRepository chatRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void roleUserCanReportAPostMessageAndUserAccount() throws Exception {
        User reporter = createUser("reporter");
        User target = createUser("target");
        Post targetPost = createPost(target);
        Message targetMessage = createMessage(target, reporter);

        performReport(reporter, "POST", targetPost.getId(), "SPAM", "Repeated advertising")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("POST"))
                .andExpect(jsonPath("$.description").value("Repeated advertising"));

        performReport(reporter, "MESSAGE", targetMessage.getId(), "HARASSMENT", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("MESSAGE"));

        performReport(reporter, "USER", target.getId(), "OTHER", "Account report")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("USER"));

        assertThat(reportRepository.count()).isEqualTo(3);
        assertThat(reportRepository.findAll()).allMatch(report -> report.isActive());
    }

    @Test
    void duplicateActiveReportForTheSameTargetIsRejectedButAClosedReportCanBeSubmittedAgain() throws Exception {
        User reporter = createUser("reporter");
        Post post = createPost(createUser("author"));

        performReport(reporter, "POST", post.getId(), "SPAM", null)
                .andExpect(status().isCreated());
        performReport(reporter, "POST", post.getId(), "OTHER", "A second reason")
                .andExpect(status().isConflict());

        assertThat(reportRepository.count()).isOne();

        Report closedReport = reportRepository.findAll().get(0);
        closedReport.setActive(false);
        reportRepository.saveAndFlush(closedReport);

        performReport(reporter, "POST", post.getId(), "OTHER", "A later report")
                .andExpect(status().isCreated());
        assertThat(reportRepository.count()).isEqualTo(2);
    }

    @Test
    void usersCannotReportTheirOwnAccountPostOrMessage() throws Exception {
        User reporter = createUser("reporter");
        User otherMember = createUser("other");
        Post ownPost = createPost(reporter);
        Message ownMessage = createMessage(reporter, otherMember);

        performReport(reporter, "USER", reporter.getId(), "OTHER", null)
                .andExpect(status().isForbidden());
        performReport(reporter, "POST", ownPost.getId(), "OTHER", null)
                .andExpect(status().isForbidden());
        performReport(reporter, "MESSAGE", ownMessage.getId(), "OTHER", null)
                .andExpect(status().isForbidden());

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void messageReportsRequireMembershipInTheChat() throws Exception {
        User outsider = createUser("outsider");
        User author = createUser("author");
        User chatMember = createUser("member");
        Message message = createMessage(author, chatMember);

        performReport(outsider, "MESSAGE", message.getId(), "HARASSMENT", null)
                .andExpect(status().isForbidden());

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void reportRequestIsValidatedAndEndpointRequiresRoleUser() throws Exception {
        User reporter = createUser("reporter");
        User target = createUser("target");

        mockMvc.perform(post("/api/reports")
                        .with(user(reporter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "USER",
                                "targetId", 0,
                                "reason", "SPAM"
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/reports")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("USER", target.getId(), "SPAM", null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("USER", target.getId(), "SPAM", null)))
                .andExpect(status().isUnauthorized());

        assertThat(reportRepository.count()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions performReport(
            User reporter,
            String targetType,
            Long targetId,
            String reason,
            String description
    ) throws Exception {
        return mockMvc.perform(post("/api/reports")
                .with(user(reporter.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportJson(targetType, targetId, reason, description)));
    }

    private String reportJson(String targetType, Long targetId, String reason, String description)
            throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "targetType", targetType,
                "targetId", targetId,
                "reason", reason,
                "description", description == null ? "" : description
        ));
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

    private Message createMessage(User author, User otherMember) {
        Chat chat = new Chat();
        chat.setUsers(new HashSet<>(Set.of(author, otherMember)));
        chat = chatRepository.saveAndFlush(chat);

        Message message = new Message();
        message.setUser(author);
        message.setChat(chat);
        message.setContent("Message");
        message.setCreatedAt(LocalDateTime.now());
        return messageRepository.saveAndFlush(message);
    }
}
