package com.metropolitan.VibeOn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metropolitan.VibeOn.entity.*;
import com.metropolitan.VibeOn.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiModerationFunctionalityIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private AiModerationFlagRepository flagRepository;
    @Autowired private ModerationActionRepository actionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void suspiciousPostIsFlaggedWithConfidencesButNotAutomaticallyRemoved() throws Exception {
        User author = createUser("ai-author");
        createPost(author, "Selling cocaine and heroin", Color.WHITE)
                .andExpect(status().isCreated());

        Post post = postRepository.findAll().get(0);
        assertThat(post.isDeleted()).isFalse();
        AiModerationFlag flag = flagRepository.findByPostId(post.getId()).orElseThrow();
        assertThat(flag.getStatus()).isEqualTo(AiModerationFlagStatus.PENDING);
        assertThat(flag.getDrugsConfidence()).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    void moderatorQueueShowsTextImageCategoriesAndConfidence() throws Exception {
        User author = createUser("queue-author");
        User moderator = createUser("queue-moderator");
        createPost(author, "Graphic blood", new Color(210, 10, 10)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/moderation/ai-flags")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").isNumber())
                .andExpect(jsonPath("$[0].targetType").value("POST"))
                .andExpect(jsonPath("$[0].authorUserId").value(author.getId()))
                .andExpect(jsonPath("$[0].authorUsername").value(author.getUsername()))
                .andExpect(jsonPath("$[0].content").value("Graphic blood"))
                .andExpect(jsonPath("$[0].imageUrls[0]").isNotEmpty())
                .andExpect(jsonPath("$[0].textConfidence").isNumber())
                .andExpect(jsonPath("$[0].categoryConfidences.NSFW").isNumber())
                .andExpect(jsonPath("$[0].categoryConfidences.VIOLENCE_BLOOD").value(0.99))
                .andExpect(jsonPath("$[0].categoryConfidences.WEAPONS").isNumber())
                .andExpect(jsonPath("$[0].categoryConfidences.DRUGS").isNumber());
    }

    @Test
    void moderatorCanDismissFalsePositiveWithReasonAndActionIsAudited() throws Exception {
        User author = createUser("dismiss-author");
        User moderator = createUser("dismiss-moderator");
        createPost(author, "A gun museum exhibit", Color.WHITE).andExpect(status().isCreated());
        AiModerationFlag flag = flagRepository.findAll().get(0);

        mockMvc.perform(post("/api/moderation/ai-flags/{id}/approve", flag.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Historical context; safe"))))
                .andExpect(status().isNoContent());

        assertThat(flagRepository.findById(flag.getId()).orElseThrow().getStatus())
                .isEqualTo(AiModerationFlagStatus.DISMISSED);
        assertThat(postRepository.findById(flag.getPost().getId())).isPresent();
        ModerationAction action = actionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.AI_FLAG_REVIEW);
        assertThat(action.getReason()).isEqualTo("Historical context; safe");
    }

    @Test
    void moderatorCanRemoveFlaggedContentAndResolveReviewThroughExistingHistory() throws Exception {
        User author = createUser("remove-flag-author");
        User moderator = createUser("remove-flag-moderator");
        createPost(author, "Selling cocaine", Color.WHITE).andExpect(status().isCreated());
        AiModerationFlag flag = flagRepository.findAll().get(0);

        mockMvc.perform(delete("/api/moderation/content/POST/{id}", flag.getPost().getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Illegal drug sales",
                                "aiFlagId", flag.getId()
                        ))))
                .andExpect(status().isOk());

        assertThat(postRepository.findById(flag.getPost().getId()).orElseThrow().isDeleted()).isTrue();
        assertThat(flagRepository.findById(flag.getId()).orElseThrow().getStatus())
                .isEqualTo(AiModerationFlagStatus.ACTIONED);
        ModerationAction action = actionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.POST);
        assertThat(action.getReason()).isEqualTo("Illegal drug sales");
    }

    @Test
    void moderatorCanNeutralizeMassTaggingInPostWithoutRemovingPostOrImage() throws Exception {
        User author = createUser("mention-post-author");
        createUser("mention-post-target");
        User moderator = createUser("mention-post-reviewer");
        createPost(
                author,
                "Look @mention-post-target @mention-post-target @mention-post-target and keep the photo",
                Color.WHITE
        ).andExpect(status().isCreated());

        Post post = postRepository.findAll().stream()
                .filter(candidate -> candidate.getUser().getId().equals(author.getId()))
                .findFirst().orElseThrow();
        AiModerationFlag flag = flagRepository.findByPostId(post.getId()).orElseThrow();
        String imageUrl = post.getImageUrl();

        mockMvc.perform(post("/api/moderation/mentions/POST/{id}/remove", post.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Mass-tagging harassment",
                                "aiFlagId", flag.getId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderatedContent")
                        .value("Look mention-post-target mention-post-target mention-post-target and keep the photo"));

        Post preserved = postRepository.findById(post.getId()).orElseThrow();
        assertThat(preserved.isDeleted()).isFalse();
        assertThat(preserved.getImageUrl()).isEqualTo(imageUrl);
        assertThat(preserved.getDescription()).doesNotContain("@mention-post-target");
        assertThat(flagRepository.findById(flag.getId()).orElseThrow().getStatus())
                .isEqualTo(AiModerationFlagStatus.ACTIONED);
        ModerationAction action = actionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.POST_MENTION_REMOVAL);
        assertThat(action.getTargetImageUrl()).isEqualTo(imageUrl);
        assertThat(action.getTargetContent()).contains("@mention-post-target");
        assertThat(action.getResultContent()).isEqualTo(preserved.getDescription());
    }

    @Test
    void moderatorCanSuspendFlaggedContentAuthorAndResolveReviewThroughSanctions() throws Exception {
        User author = createUser("sanction-flag-author");
        User moderator = createUser("sanction-flag-moderator");
        createPost(author, "Selling heroin", Color.WHITE).andExpect(status().isCreated());
        AiModerationFlag flag = flagRepository.findAll().get(0);

        mockMvc.perform(post("/api/moderation/users/{id}/suspension", author.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Repeated illegal sales",
                                "durationHours", 24,
                                "aiFlagId", flag.getId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(author.getUsername()));

        assertThat(userRepository.findById(author.getId()).orElseThrow().getSuspendedUntil()).isNotNull();
        assertThat(flagRepository.findById(flag.getId()).orElseThrow().getStatus())
                .isEqualTo(AiModerationFlagStatus.ACTIONED);
        ModerationAction action = actionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.USER_SUSPENSION);
        assertThat(action.getReason()).isEqualTo("Repeated illegal sales");
    }

    @Test
    void aiReviewActionMustMatchTheFlaggedTarget() throws Exception {
        User firstAuthor = createUser("flag-first-author");
        User secondAuthor = createUser("flag-second-author");
        User moderator = createUser("flag-match-moderator");
        createPost(firstAuthor, "Selling cocaine", Color.WHITE).andExpect(status().isCreated());
        AiModerationFlag flag = flagRepository.findAll().get(0);
        createPost(secondAuthor, "Ordinary landscape", Color.WHITE).andExpect(status().isCreated());
        Post otherPost = postRepository.findAll().stream()
                .filter(post -> post.getUser().getId().equals(secondAuthor.getId()))
                .findFirst().orElseThrow();

        mockMvc.perform(delete("/api/moderation/content/POST/{id}", otherPost.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Mismatched review",
                                "aiFlagId", flag.getId()
                        ))))
                .andExpect(status().isBadRequest());

        assertThat(otherPost.isDeleted()).isFalse();
        assertThat(flagRepository.findById(flag.getId()).orElseThrow().getStatus())
                .isEqualTo(AiModerationFlagStatus.PENDING);
    }

    @Test
    void aiQueueAndReviewRequireModeratorPermissionAndAdminInheritsIt() throws Exception {
        User admin = createUser("ai-admin");
        mockMvc.perform(get("/api/moderation/ai-flags").with(user("regular").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/moderation/ai-flags/1/approve")
                        .with(user("regular").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Not allowed"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/moderation/ai-flags"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/moderation/ai-flags").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions createPost(User author, String description, Color color) throws Exception {
        MockMultipartFile postJson = new MockMultipartFile(
                "post", "", "application/json",
                objectMapper.writeValueAsBytes(Map.of("description", description))
        );
        MockMultipartFile image = new MockMultipartFile("images", "post.png", "image/png", imageBytes(color));
        return mockMvc.perform(multipart("/api/posts")
                .file(image).file(postJson)
                .with(user(author.getUsername()).roles("USER")));
    }

    private byte[] imageBytes(Color color) throws Exception {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, 20, 20);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
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
