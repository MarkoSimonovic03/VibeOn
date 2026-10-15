package com.metropolitan.VibeOn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.PostComment;
import com.metropolitan.VibeOn.entity.Report;
import com.metropolitan.VibeOn.entity.ReportTargetType;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.Story;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.ModerationActionRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.PostCommentRepository;
import com.metropolitan.VibeOn.repository.ReportRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.StoryRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private PostCommentRepository postCommentRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ModerationActionRepository moderationActionRepository;
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
    void commentAndStoryReportsUseTheExistingReviewActionAndDismissalWorkflow() throws Exception {
        User reporter = createUser("comment-story-reporter");
        User moderator = createUser("comment-story-moderator");
        User author = createUser("comment-story-author");
        PostComment comment = createComment(createPost(author), author);
        Story story = createStory(author);

        performReport(reporter, "COMMENT", comment.getId(), "HARASSMENT", "Abusive reply")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("COMMENT"));
        performReport(reporter, "STORY", story.getId(), "SEXUAL_CONTENT", "Story concern")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("STORY"));

        mockMvc.perform(get("/api/reports/moderation")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.targetType == 'COMMENT')].targetContent").value("Comment"))
                .andExpect(jsonPath("$[?(@.targetType == 'COMMENT')].targetUsername").value(author.getUsername()))
                .andExpect(jsonPath("$[?(@.targetType == 'COMMENT')].targetProfileImageUrl").value(author.getProfileImageUrl()))
                .andExpect(jsonPath("$[*].reporterProfileImageUrl").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(reporter.getProfileImageUrl()))
                ))
                .andExpect(jsonPath("$[?(@.targetType == 'STORY')].targetImageUrl").value(story.getImageUrl()))
                .andExpect(jsonPath("$[*].targetAvailable").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))
                ));

        Report commentReport = reportRepository.findAll().stream()
                .filter(report -> report.getTargetType() == ReportTargetType.COMMENT)
                .findFirst().orElseThrow();
        Report storyReport = reportRepository.findAll().stream()
                .filter(report -> report.getTargetType() == ReportTargetType.STORY)
                .findFirst().orElseThrow();

        mockMvc.perform(delete("/api/moderation/content/COMMENT/{commentId}", comment.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Confirmed abusive content",
                                "reportId", commentReport.getId()
                        ))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/moderation/reports/{reportId}/dismiss", storyReport.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Story does not violate the rules"
                        ))))
                .andExpect(status().isOk());

        assertThat(postCommentRepository.existsById(comment.getId())).isFalse();
        assertThat(storyRepository.existsById(story.getId())).isTrue();
        assertThat(reportRepository.findAll()).allMatch(report -> !report.isActive());
        assertThat(moderationActionRepository.findAll())
                .extracting(action -> action.getTargetType().name())
                .containsExactlyInAnyOrder("COMMENT", "REPORT_DISMISSAL");
    }

    @Test
    void usersCannotReportTheirOwnCommentsOrStories() throws Exception {
        User reporter = createUser("own-comment-story-reporter");
        PostComment comment = createComment(createPost(reporter), reporter);
        Story story = createStory(reporter);

        performReport(reporter, "COMMENT", comment.getId(), "OTHER", null)
                .andExpect(status().isForbidden());
        performReport(reporter, "STORY", story.getId(), "OTHER", null)
                .andExpect(status().isForbidden());

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void moderatorCanResolveCommentReportByRemovingOnlyMentions() throws Exception {
        User reporter = createUser("mention-report-reporter");
        User moderator = createUser("mention-report-moderator");
        User author = createUser("mention-report-author");
        createUser("mention-report-other");
        PostComment comment = createComment(createPost(author), author);
        comment.setContent("Context stays @mention-report-reporter @mention-report-reporter and @mention-report-other");
        postCommentRepository.saveAndFlush(comment);

        performReport(reporter, "COMMENT", comment.getId(), "MENTION_ABUSE", "Mass tagging")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value("MENTION_ABUSE"))
                .andExpect(jsonPath("$.affectedUserId").value(reporter.getId()))
                .andExpect(jsonPath("$.affectedUsername").value(reporter.getUsername()));
        Report report = reportRepository.findAll().get(0);

        mockMvc.perform(get("/api/reports/moderation/{id}", report.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("MENTION_ABUSE"))
                .andExpect(jsonPath("$.affectedUserId").value(reporter.getId()))
                .andExpect(jsonPath("$.affectedUsername").value(reporter.getUsername()));

        mockMvc.perform(post("/api/moderation/mentions/COMMENT/{commentId}/remove", comment.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Mass-tagging harassment",
                                "reportId", report.getId()
                        ))))
                .andExpect(status().isOk());

        assertThat(postCommentRepository.findById(comment.getId()).orElseThrow().getContent())
                .isEqualTo("Context stays mention-report-reporter mention-report-reporter and @mention-report-other");
        assertThat(reportRepository.findById(report.getId()).orElseThrow().isActive()).isFalse();
        assertThat(moderationActionRepository.findAll()).singleElement().satisfies(action -> {
            assertThat(action.getTargetType().name()).isEqualTo("COMMENT_MENTION_REMOVAL");
            assertThat(action.getReport().getId()).isEqualTo(report.getId());
            assertThat(action.getTargetContent()).contains("@mention-report-reporter");
            assertThat(action.getResultContent()).doesNotContain("@mention-report-reporter");
            assertThat(action.getResultContent()).contains("@mention-report-other");
        });

        mockMvc.perform(get("/api/moderation/history")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("MENTION_MODERATION"))
                .andExpect(jsonPath("$[0].affectedUserId").value(reporter.getId()))
                .andExpect(jsonPath("$[0].affectedUsername").value(reporter.getUsername()));

        mockMvc.perform(get("/api/reports/moderation")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void mentionAbuseReportRequiresReporterToBeMentionedAndCannotTargetOtherContentTypes() throws Exception {
        User reporter = createUser("unmentioned-reporter");
        User author = createUser("unmentioned-author");
        Post post = createPost(author);

        performReport(reporter, "POST", post.getId(), "MENTION_ABUSE", "I was not mentioned")
                .andExpect(status().isForbidden());
        performReport(reporter, "USER", author.getId(), "MENTION_ABUSE", "Wrong target type")
                .andExpect(status().isBadRequest());

        assertThat(reportRepository.findAll()).isEmpty();
    }

    @Test
    void mentionedUserCanCreateMentionAbuseReportForPost() throws Exception {
        User reporter = createUser("mentioned-post-reporter");
        User author = createUser("mentioned-post-author");
        Post post = createPost(author);
        post.setDescription("A mass tag aimed at @mentioned-post-reporter");
        postRepository.saveAndFlush(post);

        performReport(reporter, "POST", post.getId(), "MENTION_ABUSE", "Unwanted targeted tag")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("POST"))
                .andExpect(jsonPath("$.reason").value("MENTION_ABUSE"))
                .andExpect(jsonPath("$.affectedUserId").value(reporter.getId()))
                .andExpect(jsonPath("$.affectedUsername").value(reporter.getUsername()));
    }

    @Test
    void unrelatedReportCannotUseMentionRemovalAction() throws Exception {
        User reporter = createUser("ordinary-report-reporter");
        User moderator = createUser("ordinary-report-moderator");
        User author = createUser("ordinary-report-author");
        PostComment comment = createComment(createPost(author), author);
        comment.setContent("Hello @ordinary-report-reporter");
        postCommentRepository.saveAndFlush(comment);

        performReport(reporter, "COMMENT", comment.getId(), "HARASSMENT", "Unrelated harassment report")
                .andExpect(status().isCreated());
        Report report = reportRepository.findAll().get(0);

        mockMvc.perform(post("/api/moderation/mentions/COMMENT/{commentId}/remove", comment.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Should not be available",
                                "reportId", report.getId()
                        ))))
                .andExpect(status().isBadRequest());

        assertThat(reportRepository.findById(report.getId()).orElseThrow().isActive()).isTrue();
        assertThat(postCommentRepository.findById(comment.getId()).orElseThrow().getContent())
                .isEqualTo("Hello @ordinary-report-reporter");
        assertThat(moderationActionRepository.findAll()).isEmpty();
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
                        .with(user("guest").roles("GUEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("USER", target.getId(), "SPAM", null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("USER", target.getId(), "SPAM", null)))
                .andExpect(status().isUnauthorized());

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void moderatorsCanSeeAllActiveReportsWithResolvedTargetsAndFilterByType() throws Exception {
        User reporter = createUser("reporter");
        User secondReporter = createUser("second-reporter");
        User target = createUser("target");
        Post post = createPost(target);
        Message message = createMessage(target, reporter);

        performReport(reporter, "POST", post.getId(), "SPAM", "Repeated advertising")
                .andExpect(status().isCreated());
        performReport(reporter, "MESSAGE", message.getId(), "HARASSMENT", "Threatening message")
                .andExpect(status().isCreated());
        performReport(reporter, "USER", target.getId(), "OTHER", "Suspicious account")
                .andExpect(status().isCreated());
        performReport(secondReporter, "USER", target.getId(), "SPAM", "Old report")
                .andExpect(status().isCreated());

        Report inactiveReport = reportRepository.findAll().stream()
                .filter(report -> report.getReporter().equals(secondReporter))
                .findFirst()
                .orElseThrow();
        inactiveReport.setActive(false);
        reportRepository.saveAndFlush(inactiveReport);

        mockMvc.perform(get("/api/reports/moderation")
                        .with(user("moderator").roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.targetType == 'POST')].reporterUsername").value("reporter"))
                .andExpect(jsonPath("$[?(@.targetType == 'POST')].targetUsername").value("target"))
                .andExpect(jsonPath("$[?(@.targetType == 'POST')].targetContent").value("Post"))
                .andExpect(jsonPath("$[?(@.targetType == 'POST')].description").value("Repeated advertising"))
                .andExpect(jsonPath("$[?(@.targetType == 'POST')].createdAt").exists())
                .andExpect(jsonPath("$[?(@.targetType == 'MESSAGE')].targetContent").value("Message"))
                .andExpect(jsonPath("$[?(@.targetType == 'MESSAGE')].targetChatId").value(
                        message.getChat().getId().intValue()
                ))
                .andExpect(jsonPath("$[?(@.targetType == 'USER')].targetUsername").value("target"))
                .andExpect(jsonPath("$[*].targetAvailable").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))
                ));

        mockMvc.perform(get("/api/reports/moderation")
                        .param("targetType", "MESSAGE")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].targetType").value("MESSAGE"))
                .andExpect(jsonPath("$[0].reason").value("HARASSMENT"));
    }

    @Test
    void moderatorReportQueueRejectsUsersAndAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/reports/moderation")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/moderation")
                        .with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/moderation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void moderatorsCanOpenAnActiveReportButNotAClosedOne() throws Exception {
        User reporter = createUser("reporter");
        User target = createUser("target");
        performReport(reporter, "USER", target.getId(), "OTHER", "Account report")
                .andExpect(status().isCreated());
        Report report = reportRepository.findAll().get(0);

        mockMvc.perform(get("/api/reports/moderation/{reportId}", report.getId())
                        .with(user("moderator").roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(report.getId()))
                .andExpect(jsonPath("$.targetType").value(ReportTargetType.USER.name()))
                .andExpect(jsonPath("$.targetUsername").value("target"));

        report.setActive(false);
        reportRepository.saveAndFlush(report);
        mockMvc.perform(get("/api/reports/moderation/{reportId}", report.getId())
                        .with(user("moderator").roles("MODERATOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void moderatorCanDismissPostMessageAndUserReportsWithoutSanctioningTargets() throws Exception {
        User moderator = createUser("dismiss-moderator");
        User reporter = createUser("dismiss-reporter");
        User target = createUser("dismiss-target");
        Post post = createPost(target);
        Message message = createMessage(target, reporter);

        performReport(reporter, "POST", post.getId(), "SPAM", "Looks repetitive")
                .andExpect(status().isCreated());
        performReport(reporter, "MESSAGE", message.getId(), "HARASSMENT", "Read in context")
                .andExpect(status().isCreated());
        performReport(reporter, "USER", target.getId(), "OTHER", "Profile concern")
                .andExpect(status().isCreated());

        for (Report report : reportRepository.findAll()) {
            mockMvc.perform(post("/api/moderation/reports/{reportId}/dismiss", report.getId())
                            .with(user(moderator.getUsername()).roles("MODERATOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "reason", "Reviewed in context; no rule violation"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reportId").value(report.getId()))
                    .andExpect(jsonPath("$.targetType").value(report.getTargetType().name()))
                    .andExpect(jsonPath("$.reason").value("Reviewed in context; no rule violation"));
        }

        assertThat(reportRepository.findAll()).allMatch(report -> !report.isActive());
        assertThat(postRepository.findById(post.getId())).get().extracting(Post::isDeleted).isEqualTo(false);
        assertThat(messageRepository.findById(message.getId())).isPresent();
        assertThat(userRepository.findById(target.getId())).get().extracting(User::getSuspendedUntil).isNull();
        assertThat(moderationActionRepository.findAll())
                .hasSize(3)
                .allMatch(action -> action.getTargetType() == com.metropolitan.VibeOn.entity.ModerationTargetType.REPORT_DISMISSAL)
                .allMatch(action -> action.getReport() != null)
                .allMatch(action -> action.getReason().equals("Reviewed in context; no rule violation"));

        mockMvc.perform(get("/api/reports/moderation")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/moderation/history")
                        .with(user(moderator.getUsername()).roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reportTargetType == 'POST')].reason")
                        .value("Reviewed in context; no rule violation"))
                .andExpect(jsonPath("$[?(@.reportTargetType == 'MESSAGE')].reason")
                        .value("Reviewed in context; no rule violation"))
                .andExpect(jsonPath("$[?(@.reportTargetType == 'USER')].reason")
                        .value("Reviewed in context; no rule violation"));
    }

    @Test
    void reportDismissalRequiresModeratorValidReasonAndAnActiveReport() throws Exception {
        User moderator = createUser("dismiss-validation-moderator");
        User reporter = createUser("dismiss-validation-reporter");
        User target = createUser("dismiss-validation-target");
        performReport(reporter, "USER", target.getId(), "OTHER", null)
                .andExpect(status().isCreated());
        Report report = reportRepository.findAll().get(0);

        mockMvc.perform(post("/api/moderation/reports/{reportId}/dismiss", report.getId())
                        .with(user("regular").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No violation"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/moderation/reports/{reportId}/dismiss", report.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", " "))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/moderation/reports/{reportId}/dismiss", report.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No violation"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/moderation/reports/{reportId}/dismiss", report.getId())
                        .with(user(moderator.getUsername()).roles("MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Again"))))
                .andExpect(status().isNotFound());
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
}
