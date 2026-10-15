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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiCommentStoryModerationIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private PostCommentRepository commentRepository;
    @Autowired private StoryRepository storyRepository;
    @Autowired private AiModerationFlagRepository flagRepository;
    @Autowired private ModerationActionRepository actionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void abusiveCommentMentionsAreFlaggedAndCanBeApprovedAsFalsePositive() throws Exception {
        User author = createUser("comment-post-author");
        User commenter = createUser("mention-abuser");
        User victim = createUser("mention-target");
        User moderator = createUser("comment-reviewer");
        Post post = createPost(author);

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .with(user(commenter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "Hey @mention-target @mention-target @mention-target"
                        ))))
                .andExpect(status().isCreated());

        PostComment comment = commentRepository.findAll().get(0);
        AiModerationFlag flag = flagRepository.findByCommentId(comment.getId()).orElseThrow();
        assertThat(flag.getMentionAbuseConfidence()).isGreaterThanOrEqualTo(0.90);
        assertThat(commentRepository.findById(comment.getId())).isPresent();

        mockMvc.perform(get("/api/moderation/ai-flags")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetType").value("COMMENT"))
                .andExpect(jsonPath("$[0].commentId").value(comment.getId()))
                .andExpect(jsonPath("$[0].parentPostId").value(post.getId()))
                .andExpect(jsonPath("$[0].authorUserId").value(commenter.getId()))
                .andExpect(jsonPath("$[0].categoryConfidences.MENTION_ABUSE").value(0.9));

        mockMvc.perform(post("/api/moderation/ai-flags/{id}/approve", flag.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Conversation context shows no harassment"
                        ))))
                .andExpect(status().isNoContent());

        assertThat(flagRepository.findById(flag.getId()).orElseThrow().getStatus())
                .isEqualTo(AiModerationFlagStatus.DISMISSED);
        assertThat(commentRepository.findById(comment.getId())).isPresent();
        ModerationAction action = actionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.AI_COMMENT_FLAG_REVIEW);
        assertThat(action.getSecondaryTargetId()).isEqualTo(post.getId());
        assertThat(victim.getId()).isNotNull();
    }

    @Test
    void suspiciousStoryImageUsesSharedQueueAndExistingRemovalWorkflow() throws Exception {
        User author = createUser("story-flag-author");
        User moderator = createUser("story-reviewer");

        mockMvc.perform(multipart("/api/stories")
                        .file(imagePart(new Color(210, 10, 10)))
                        .with(user(author.getUsername()).roles("USER")))
                .andExpect(status().isCreated());

        Story story = storyRepository.findAll().get(0);
        AiModerationFlag flag = flagRepository.findByStoryId(story.getId()).orElseThrow();
        assertThat(flag.getViolenceBloodConfidence()).isGreaterThanOrEqualTo(0.90);

        mockMvc.perform(get("/api/moderation/ai-flags")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetType").value("STORY"))
                .andExpect(jsonPath("$[0].storyId").value(story.getId()))
                .andExpect(jsonPath("$[0].imageUrls[0]").value(story.getImageUrl()))
                .andExpect(jsonPath("$[0].categoryConfidences.VIOLENCE_BLOOD").value(0.99));

        mockMvc.perform(delete("/api/moderation/content/STORY/{id}", story.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Graphic violent image",
                                "aiFlagId", flag.getId()
                        ))))
                .andExpect(status().isOk());

        assertThat(storyRepository.findById(story.getId())).isEmpty();
        assertThat(flagRepository.findById(flag.getId())).isEmpty();
        ModerationAction action = actionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.STORY);
        assertThat(action.getReason()).isEqualTo("Graphic violent image");
    }

    @Test
    void ordinaryCommentMentionsAreNotFlagged() throws Exception {
        User author = createUser("normal-post-author");
        User commenter = createUser("normal-commenter");
        createUser("normal-friend");
        Post post = createPost(author);

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .with(user(commenter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "Thanks for the photo @normal-friend"
                        ))))
                .andExpect(status().isCreated());

        PostComment comment = commentRepository.findAll().get(0);
        assertThat(flagRepository.findByCommentId(comment.getId())).isEmpty();
        assertThat(comment.getContent()).contains("@normal-friend");
    }

    @Test
    void moderatorCanNeutralizeAbusiveCommentMentionsWithoutRemovingComment() throws Exception {
        User author = createUser("targeted-post-author");
        User commenter = createUser("targeted-commenter");
        createUser("targeted-victim");
        User moderator = createUser("targeted-reviewer");
        Post post = createPost(author);

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .with(user(commenter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "Stop this @targeted-victim @targeted-victim @targeted-victim but keep this context"
                        ))))
                .andExpect(status().isCreated());

        PostComment comment = commentRepository.findAll().get(0);
        AiModerationFlag flag = flagRepository.findByCommentId(comment.getId()).orElseThrow();

        mockMvc.perform(post("/api/moderation/mentions/COMMENT/{id}/remove", comment.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Repeated targeted tagging",
                                "aiFlagId", flag.getId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetType").value("COMMENT"))
                .andExpect(jsonPath("$.originalContent").value("Stop this @targeted-victim @targeted-victim @targeted-victim but keep this context"))
                .andExpect(jsonPath("$.moderatedContent").value("Stop this targeted-victim targeted-victim targeted-victim but keep this context"));

        PostComment preserved = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(preserved.getContent()).isEqualTo(
                "Stop this targeted-victim targeted-victim targeted-victim but keep this context"
        );
        assertThat(flagRepository.findById(flag.getId()).orElseThrow().getStatus())
                .isEqualTo(AiModerationFlagStatus.ACTIONED);

        ModerationAction action = actionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.COMMENT_MENTION_REMOVAL);
        assertThat(action.getTargetContent()).contains("@targeted-victim");
        assertThat(action.getResultContent()).doesNotContain("@targeted-victim");
        assertThat(action.getTargetContextId()).isEqualTo(post.getId());
        assertThat(action.getReason()).isEqualTo("Repeated targeted tagging");

        mockMvc.perform(get("/api/moderation/history")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("MENTION_MODERATION"))
                .andExpect(jsonPath("$[0].targetContent").value(action.getTargetContent()))
                .andExpect(jsonPath("$[0].resultContent").value(action.getResultContent()));
    }

    @Test
    void targetedMentionModerationRequiresModeratorReasonAndActualMention() throws Exception {
        User author = createUser("mention-validation-author");
        User commenter = createUser("mention-validation-commenter");
        User moderator = createUser("mention-validation-reviewer");
        Post post = createPost(author);
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(commenter);
        comment.setContent("A comment with no tagging");
        comment.setCreatedAt(LocalDateTime.now());
        commentRepository.saveAndFlush(comment);

        mockMvc.perform(post("/api/moderation/mentions/COMMENT/{id}/remove", comment.getId())
                        .with(user(commenter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Not allowed"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/moderation/mentions/COMMENT/{id}/remove", comment.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", " "))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/moderation/mentions/COMMENT/{id}/remove", comment.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No mention exists"))))
                .andExpect(status().isBadRequest());

        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getContent())
                .isEqualTo("A comment with no tagging");
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void flaggedCommentCanBeResolvedThroughExistingUserSanctionWorkflow() throws Exception {
        User author = createUser("sanction-post-author");
        User commenter = createUser("sanction-commenter");
        createUser("sanction-target");
        User moderator = createUser("sanction-reviewer");
        Post post = createPost(author);

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .with(user(commenter.getUsername()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "@sanction-target kill yourself"
                        ))))
                .andExpect(status().isCreated());
        PostComment comment = commentRepository.findAll().get(0);
        AiModerationFlag flag = flagRepository.findByCommentId(comment.getId()).orElseThrow();

        mockMvc.perform(post("/api/moderation/users/{id}/suspension", commenter.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Targeted harassment",
                                "durationHours", 24,
                                "aiFlagId", flag.getId()
                        ))))
                .andExpect(status().isOk());

        assertThat(flagRepository.findById(flag.getId()).orElseThrow().getStatus())
                .isEqualTo(AiModerationFlagStatus.ACTIONED);
        assertThat(commentRepository.findById(comment.getId())).isPresent();
        ModerationAction action = actionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.USER_SUSPENSION);
        assertThat(action.getTargetId()).isEqualTo(commenter.getId());
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

    private MockMultipartFile imagePart(Color color) throws Exception {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("image", "story.png", "image/png", output.toByteArray());
    }
}
