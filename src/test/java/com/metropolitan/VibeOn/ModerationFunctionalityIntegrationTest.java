package com.metropolitan.VibeOn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metropolitan.VibeOn.entity.*;
import com.metropolitan.VibeOn.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModerationFunctionalityIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ChatRepository chatRepository;
    @Autowired private PostCommentRepository postCommentRepository;
    @Autowired private StoryRepository storyRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private ModerationActionRepository moderationActionRepository;
    @Autowired private FollowRepository followRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void moderatorCanRemoveOtherUsersPostsMessagesCommentsAndStoriesWithAReason() throws Exception {
        User moderator = createUser("moderator");
        User author = createUser("author");
        User chatMember = createUser("member");
        Post post = createPost(author);
        Message message = createMessage(author, chatMember);
        PostComment comment = createComment(post, author);
        Story story = createStory(author);

        remove("POST", post.getId(), moderator, "Spam post", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Spam post"));
        remove("MESSAGE", message.getId(), moderator, "Harassing message", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetChatId").value(message.getChat().getId().intValue()));
        remove("COMMENT", comment.getId(), moderator, "Abusive comment", null)
                .andExpect(status().isOk());
        remove("STORY", story.getId(), moderator, "Inappropriate story", null)
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "select is_deleted from posts where id = ?", Boolean.class, post.getId()
        )).isTrue();
        assertThat(messageRepository.existsById(message.getId())).isFalse();
        assertThat(postCommentRepository.existsById(comment.getId())).isFalse();
        assertThat(storyRepository.existsById(story.getId())).isFalse();
        assertThat(chatRepository.findById(message.getChat().getId()).orElseThrow().getLastMessage()).isNull();
        assertThat(moderationActionRepository.findAll())
                .hasSize(4)
                .allMatch(action -> action.getModerator().equals(moderator))
                .allMatch(action -> action.getTargetCreatedAt() != null)
                .extracting(ModerationAction::getReason)
                .containsExactlyInAnyOrder("Spam post", "Harassing message", "Abusive comment", "Inappropriate story");
        assertThat(moderationActionRepository.findAll())
                .filteredOn(action -> action.getTargetType() == ModerationTargetType.COMMENT)
                .singleElement().extracting(ModerationAction::getTargetContent).isEqualTo("Comment");
        assertThat(moderationActionRepository.findAll())
                .filteredOn(action -> action.getTargetType() == ModerationTargetType.STORY)
                .singleElement().extracting(ModerationAction::getTargetImageUrl).isEqualTo(story.getImageUrl());
    }

    @Test
    void removingReportedContentClosesMatchingActiveReportsAndLinksTheAction() throws Exception {
        User moderator = createUser("moderator");
        User reporter = createUser("reporter");
        Post post = createPost(createUser("author"));
        Report report = createReport(reporter, ReportTargetType.POST, post.getId());
        createReport(createUser("second-reporter"), ReportTargetType.POST, post.getId());

        remove("POST", post.getId(), moderator, "Confirmed spam", report.getId())
                .andExpect(status().isOk());

        assertThat(reportRepository.findAll()).allMatch(existing -> !existing.isActive());
        ModerationAction action = moderationActionRepository.findAll().get(0);
        assertThat(action.getReport().getId()).isEqualTo(report.getId());
        assertThat(action.getReason()).isEqualTo("Confirmed spam");
    }

    @Test
    void moderationRemovalRequiresANonBlankReasonAndMatchingReport() throws Exception {
        User moderator = createUser("moderator");
        Post post = createPost(createUser("author"));
        Report unrelated = createReport(createUser("reporter"), ReportTargetType.USER, createUser("target").getId());

        remove("POST", post.getId(), moderator, "   ", null)
                .andExpect(status().isBadRequest());
        remove("POST", post.getId(), moderator, "Reason", unrelated.getId())
                .andExpect(status().isBadRequest());

        assertThat(postRepository.findById(post.getId())).isPresent();
        assertThat(moderationActionRepository.count()).isZero();
    }

    @Test
    void usersAndAnonymousAccountsCannotUseModeratorRemoval() throws Exception {
        Post post = createPost(createUser("author"));

        mockMvc.perform(delete("/api/moderation/content/POST/{id}", post.getId())
                        .with(user("regular").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removalJson("Reason", null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/moderation/content/POST/{id}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removalJson("Reason", null)))
                .andExpect(status().isUnauthorized());

        assertThat(postRepository.findById(post.getId())).isPresent();
    }

    @Test
    void moderatorRemovalIsLimitedToOtherUsersContent() throws Exception {
        User moderator = createUser("moderator");
        Post ownPost = createPost(moderator);

        remove("POST", ownPost.getId(), moderator, "Reason", null)
                .andExpect(status().isForbidden());

        assertThat(postRepository.findById(ownPost.getId())).isPresent();
        assertThat(moderationActionRepository.count()).isZero();
    }

    @Test
    void adminInheritsModeratorRemovalPermission() throws Exception {
        User admin = createUser("admin");
        Post post = createPost(createUser("author"));

        mockMvc.perform(delete("/api/moderation/content/POST/{id}", post.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removalJson("Policy violation", null)))
                .andExpect(status().isOk());

        assertThat(moderationActionRepository.count()).isOne();
    }

    @Test
    void moderatorCanTemporarilySuspendReportedUserWithReason() throws Exception {
        User moderator = createUser("sanctions-moderator");
        User target = createUser("suspended-user");
        Report report = createReport(createUser("sanctions-reporter"), ReportTargetType.USER, target.getId());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/moderation/users/{id}/suspension", target.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Repeated harassment",
                                "durationHours", 48,
                                "reportId", report.getId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(target.getId()))
                .andExpect(jsonPath("$.reason").value("Repeated harassment"))
                .andExpect(jsonPath("$.suspendedUntil").isNotEmpty());

        User suspended = userRepository.findById(target.getId()).orElseThrow();
        assertThat(suspended.getSuspendedUntil()).isAfter(LocalDateTime.now().plusHours(47));
        assertThat(suspended.getSuspensionReason()).isEqualTo("Repeated harassment");
        assertThat(reportRepository.findById(report.getId()).orElseThrow().isActive()).isFalse();
        ModerationAction action = moderationActionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.USER_SUSPENSION);
        assertThat(action.getReport().getId()).isEqualTo(report.getId());
        assertThat(action.getExpiresAt()).isEqualTo(suspended.getSuspendedUntil());
    }

    @Test
    void suspensionBlocksNewLoginAndExistingTokenUntilItExpires() throws Exception {
        User moderator = createUser("login-moderator");
        User target = createUser("login-target");
        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", target.getUsername(), "password", "password"
        ));
        String loginResponse = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/moderation/users/{id}/suspension", target.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Temporary restriction", "durationHours", 2
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        target = userRepository.findById(target.getId()).orElseThrow();
        target.setSuspendedUntil(LocalDateTime.now().minusMinutes(1));
        userRepository.saveAndFlush(target);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk());
    }

    @Test
    void moderatorCanRemoveAnExactFollowRelationshipWithReason() throws Exception {
        User moderator = createUser("follow-moderator");
        User follower = createUser("inappropriate-follower");
        User followee = createUser("follow-target");
        User another = createUser("other-follow-target");
        Follow selected = new Follow(null, follower, followee);
        followRepository.saveAndFlush(selected);
        followRepository.saveAndFlush(new Follow(null, follower, another));

        mockMvc.perform(delete("/api/moderation/follows/{followerId}/{followeeId}", follower.getId(), followee.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Abusive follow behavior"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerUsername").value(follower.getUsername()))
                .andExpect(jsonPath("$.followeeUsername").value(followee.getUsername()));

        assertThat(followRepository.findByFollowerIdAndFolloweeId(follower.getId(), followee.getId())).isEmpty();
        assertThat(followRepository.findByFollowerIdAndFolloweeId(follower.getId(), another.getId())).isPresent();
        ModerationAction action = moderationActionRepository.findAll().get(0);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.FOLLOW_RELATIONSHIP);
        assertThat(action.getTargetId()).isEqualTo(follower.getId());
        assertThat(action.getSecondaryTargetId()).isEqualTo(followee.getId());
        assertThat(action.getReason()).isEqualTo("Abusive follow behavior");
    }

    @Test
    void sanctionsRequireModeratorAccessAndNonBlankReasons() throws Exception {
        User moderator = createUser("validation-moderator");
        User target = createUser("validation-target");
        User followee = createUser("validation-followee");
        followRepository.saveAndFlush(new Follow(null, target, followee));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/moderation/users/{id}/suspension", target.getId())
                        .with(user("regular").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Reason", "durationHours", 1))))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/moderation/users/{id}/suspension", target.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", " ", "durationHours", 1))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/moderation/follows/{followerId}/{followeeId}", target.getId(), followee.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", " "))))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findById(target.getId()).orElseThrow().getSuspendedUntil()).isNull();
        assertThat(followRepository.findByFollowerIdAndFolloweeId(target.getId(), followee.getId())).isPresent();
        assertThat(moderationActionRepository.count()).isZero();
    }

    @Test
    void moderatorHistoryShowsActionsTargetsModeratorReasonsAndTimesNewestFirst() throws Exception {
        User moderator = createUser("history-moderator");
        User affectedUser = createUser("history-target");
        User followee = createUser("history-followee");

        ModerationAction older = new ModerationAction();
        older.setModerator(moderator);
        older.setTargetType(ModerationTargetType.POST);
        older.setTargetId(41L);
        older.setTargetUsername(affectedUser.getUsername());
        older.setReason("Removed spam");
        older.setCreatedAt(LocalDateTime.now().minusHours(1));
        moderationActionRepository.saveAndFlush(older);

        ModerationAction newer = new ModerationAction();
        newer.setModerator(moderator);
        newer.setTargetType(ModerationTargetType.FOLLOW_RELATIONSHIP);
        newer.setTargetId(affectedUser.getId());
        newer.setTargetUsername(affectedUser.getUsername());
        newer.setSecondaryTargetId(followee.getId());
        newer.setSecondaryTargetUsername(followee.getUsername());
        newer.setReason("Removed inappropriate relationship");
        newer.setCreatedAt(LocalDateTime.now());
        moderationActionRepository.saveAndFlush(newer);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/moderation/history")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("FOLLOW_REMOVAL"))
                .andExpect(jsonPath("$[0].targetUsername").value(affectedUser.getUsername()))
                .andExpect(jsonPath("$[0].secondaryTargetUsername").value(followee.getUsername()))
                .andExpect(jsonPath("$[0].moderatorUsername").value(moderator.getUsername()))
                .andExpect(jsonPath("$[0].moderatorProfileImageUrl").value(moderator.getProfileImageUrl()))
                .andExpect(jsonPath("$[0].targetProfileImageUrl").value(affectedUser.getProfileImageUrl()))
                .andExpect(jsonPath("$[0].reason").value("Removed inappropriate relationship"))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[1].actionType").value("CONTENT_REMOVAL"))
                .andExpect(jsonPath("$[1].targetType").value("POST"));
    }

    @Test
    void moderatorCanRestoreASoftDeletedPostAndBothActionsRemainInHistory() throws Exception {
        User moderator = createUser("restore-moderator");
        User author = createUser("restore-author");
        Post post = createPost(author);

        remove("POST", post.getId(), moderator, "Removed after review", null)
                .andExpect(status().isOk());
        ModerationAction removal = moderationActionRepository.findAll().get(0);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/moderation/history")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("CONTENT_REMOVAL"))
                .andExpect(jsonPath("$[0].targetContent").value("Post"))
                .andExpect(jsonPath("$[0].targetImageUrl").value(post.getImageUrl()))
                .andExpect(jsonPath("$[0].targetProfileImageUrl").value(author.getProfileImageUrl()))
                .andExpect(jsonPath("$[0].targetCreatedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].canRestore").value(true));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/moderation/history/{actionId}/restore", removal.getId())
                        .with(user("regular").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Not authorized"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/moderation/history/{actionId}/restore", removal.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Decision overturned on appeal"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(post.getId()))
                .andExpect(jsonPath("$.originalActionId").value(removal.getId()));

        assertThat(jdbcTemplate.queryForObject(
                "select is_deleted from posts where id = ?", Boolean.class, post.getId()
        )).isFalse();
        assertThat(moderationActionRepository.findAll()).hasSize(2);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/moderation/history")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("CONTENT_RESTORATION"))
                .andExpect(jsonPath("$[0].originalActionId").value(removal.getId()))
                .andExpect(jsonPath("$[0].reason").value("Decision overturned on appeal"))
                .andExpect(jsonPath("$[1].actionType").value("CONTENT_REMOVAL"))
                .andExpect(jsonPath("$[1].restoredByActionId").value(moderationActionRepository.findAll().stream()
                        .filter(action -> action.getOriginalAction() != null).findFirst().orElseThrow().getId()))
                .andExpect(jsonPath("$[1].canRestore").value(false));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/moderation/history/{actionId}/restore", removal.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Restore twice"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moderationHistoryRequiresModeratorAccessAndIsInheritedByAdmin() throws Exception {
        User admin = createUser("history-admin");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/moderation/history")
                        .with(user("regular").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/moderation/history"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/moderation/history")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions remove(
            String type, Long targetId, User moderator, String reason, Long reportId
    ) throws Exception {
        return mockMvc.perform(delete("/api/moderation/content/{type}/{id}", type, targetId)
                .with(user(moderator.getUsername()).roles("MODERATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(removalJson(reason, reportId)));
    }

    private String removalJson(String reason, Long reportId) throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("reason", reason);
        if (reportId != null) body.put("reportId", reportId);
        return objectMapper.writeValueAsString(body);
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

    private Post createPost(User author) {
        Post post = new Post();
        post.setUser(author);
        post.setImageUrl(author.getUsername() + "-" + System.nanoTime() + ".png");
        post.setDescription("Post");
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.saveAndFlush(post);
    }

    private Message createMessage(User author, User member) {
        Chat chat = new Chat();
        chat.setUsers(new HashSet<>(Set.of(author, member)));
        chat = chatRepository.saveAndFlush(chat);
        Message message = new Message();
        message.setUser(author);
        message.setChat(chat);
        message.setContent("Message");
        message.setCreatedAt(LocalDateTime.now());
        message = messageRepository.saveAndFlush(message);
        chat.setLastMessage(message);
        chatRepository.saveAndFlush(chat);
        return message;
    }

    private PostComment createComment(Post post, User author) {
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(author);
        comment.setContent("Comment");
        comment.setCreatedAt(LocalDateTime.now());
        return postCommentRepository.saveAndFlush(comment);
    }

    private Story createStory(User author) {
        Story story = new Story();
        story.setUser(author);
        story.setImageUrl(author.getUsername() + "-story-" + System.nanoTime() + ".png");
        story.setCreatedAt(LocalDateTime.now());
        story.setExpiresAt(LocalDateTime.now().plusHours(24));
        return storyRepository.saveAndFlush(story);
    }

    private Report createReport(User reporter, ReportTargetType type, Long targetId) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(type);
        report.setTargetId(targetId);
        report.setReason(ReportReason.OTHER);
        report.setCreatedAt(LocalDateTime.now());
        return reportRepository.saveAndFlush(report);
    }
}
