package com.metropolitan.VibeOn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metropolitan.VibeOn.dto.FullChatDto;
import com.metropolitan.VibeOn.dto.LoginDto;
import com.metropolitan.VibeOn.dto.RegisterDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.dto.SinglePostDto;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.security.JwtTokenProvider;
import com.metropolitan.VibeOn.service.ChatService;
import com.metropolitan.VibeOn.service.FollowService;
import com.metropolitan.VibeOn.service.ImageStorageService;
import com.metropolitan.VibeOn.service.PostService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CoreFunctionalityIntegrationTest {

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
    private FollowRepository followRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private PostService postService;
    @Autowired
    private FollowService followService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @BeforeEach
    void configureImageStorage() throws Exception {
        when(imageStorageService.save(any())).thenAnswer(invocation -> {
            org.springframework.web.multipart.MultipartFile image = invocation.getArgument(0);
            return "stored-" + image.getOriginalFilename();
        });
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginReturnsValidJwtAndRejectsBadCredentials() throws Exception {
        createUser("alice", "alice@example.com", "correct-password");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginDto("alice", "correct-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("accessToken").asText();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("alice");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginDto("alice", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireAValidJwt() throws Exception {
        User alice = createUser("alice", "alice@example.com", "password");
        String token = loginToken(alice.getUsername(), "password");

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/posts").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/posts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void registrationValidatesInputAndStoresEncodedPasswordWithUserRole() throws Exception {
        RegisterDto invalid = validRegistration("new-user", "new@example.com");
        invalid.setEmail("not-an-email");
        invalid.setPassword("short");

        mockMvc.perform(multipart("/api/auth/register")
                        .file(imagePart())
                        .file(jsonPart("registerDto", invalid)))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.existsByUsername("new-user")).isFalse();

        RegisterDto valid = validRegistration("new-user", "new@example.com");
        mockMvc.perform(multipart("/api/auth/register")
                        .file(imagePart())
                        .file(jsonPart("registerDto", valid)))
                .andExpect(status().isOk());

        User saved = userRepository.findByUsername("new-user").orElseThrow();
        assertThat(passwordEncoder.matches(valid.getPassword(), saved.getPassword())).isTrue();
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("ROLE_USER");
        assertThat(saved.getProfileImageUrl()).isEqualTo("stored-image.png");
    }

    @Test
    void postCreationEditAuthorizationAndSoftDeleteAreEnforced() throws Exception {
        User owner = createUser("owner", "owner@example.com", "password");
        User other = createUser("other", "other@example.com", "password");

        authenticateAs(owner);
        SinglePostDto created = postService.createPost(
                imagePart(),
                "Original description for @other and @missing-user #project"
        );
        assertThat(created.getDescription()).contains("@other", "#project");
        assertThat(created.getMentionedUsernames()).containsExactly("other");
        assertThat(postRepository.findPostByIdAsDto(created.getId())).isPresent();

        authenticateAs(other);
        assertThatThrownBy(() -> postService.updatePost(created.getId(), "Unauthorized edit"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> postService.softDeletePost(created.getId()))
                .isInstanceOf(AccessDeniedException.class);

        authenticateAs(owner);
        SinglePostDto updated = postService.updatePost(
                created.getId(),
                "Updated description mentioning @owner"
        );
        assertThat(updated.getDescription()).isEqualTo("Updated description mentioning @owner");
        assertThat(updated.getMentionedUsernames()).containsExactly("owner");

        postService.softDeletePost(created.getId());
        postRepository.flush();

        Boolean deleted = jdbcTemplate.queryForObject(
                "select is_deleted from posts where id = ?",
                Boolean.class,
                created.getId()
        );
        assertThat(deleted).isTrue();
        assertThat(postRepository.findPostByIdAsDto(created.getId())).isEmpty();
        assertThat(postRepository.findAllPostsAsDto())
                .extracting(SinglePostDto::getId)
                .doesNotContain(created.getId());
    }

    @Test
    void postRequestValidationRejectsBlankDescriptions() throws Exception {
        User owner = createUser("owner", "owner@example.com", "password");
        String token = loginToken(owner.getUsername(), "password");
        MockMultipartFile postPart = new MockMultipartFile(
                "post",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"description\":\"  \"}".getBytes()
        );

        mockMvc.perform(multipart("/api/posts")
                        .file(imagePart())
                        .file(postPart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        assertThat(postRepository.count()).isZero();
    }

    @Test
    void postCreationAcceptsMultipleImagesAndKeepsLegacyImageUrl() throws Exception {
        User owner = createUser("owner", "owner@example.com", "password");
        String token = loginToken(owner.getUsername(), "password");
        MockMultipartFile firstImage = new MockMultipartFile(
                "images", "first.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1}
        );
        MockMultipartFile secondImage = new MockMultipartFile(
                "images", "second.png", MediaType.IMAGE_PNG_VALUE, new byte[]{2}
        );
        MockMultipartFile postPart = new MockMultipartFile(
                "post", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"description\":\"A gallery post\"}".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/posts")
                        .file(firstImage)
                        .file(secondImage)
                        .file(postPart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("stored-first.png"))
                .andExpect(jsonPath("$.imageUrls.length()").value(2))
                .andExpect(jsonPath("$.imageUrls[0]").value("stored-first.png"))
                .andExpect(jsonPath("$.imageUrls[1]").value("stored-second.png"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long postId = objectMapper.readTree(response).get("id").asLong();
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("stored-first.png"))
                .andExpect(jsonPath("$.imageUrls.length()").value(2));
    }

    @Test
    void followToggleCreatesAndThenRemovesTheRelationship() {
        User follower = createUser("follower", "follower@example.com", "password");
        User followee = createUser("followee", "followee@example.com", "password");
        authenticateAs(follower);

        followService.toggleFollow(followee.getId());
        assertThat(followRepository.findByFollowerIdAndFolloweeId(follower.getId(), followee.getId()))
                .isPresent();

        followService.toggleFollow(followee.getId());
        assertThat(followRepository.findByFollowerIdAndFolloweeId(follower.getId(), followee.getId()))
                .isEmpty();
    }

    @Test
    void chatCreationAndMessageSendingRequireChatMembership() {
        User alice = createUser("alice", "alice@example.com", "password");
        User bob = createUser("bob", "bob@example.com", "password");
        createUser("mallory", "mallory@example.com", "password");
        authenticateAs(alice);

        FullChatDto chat = chatService.getChat(bob.getUsername());
        assertThat(chatRepository.existsByIdAndUsersUsername(chat.getId(), "alice")).isTrue();
        assertThat(chatRepository.existsByIdAndUsersUsername(chat.getId(), "bob")).isTrue();
        entityManager.flush();
        entityManager.clear();

        SingleMessageDto message = chatService.sendMessage("alice", chat.getId(), "Hello");
        assertThat(message.getContent()).isEqualTo("Hello");
        assertThat(message.getUsername()).isEqualTo("alice");
        assertThat(messageRepository.findMessagesByChatIdAsDto(chat.getId())).hasSize(1);

        assertThatThrownBy(() -> chatService.sendMessage("mallory", chat.getId(), "Unauthorized"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not authorized");
        assertThat(messageRepository.findMessagesByChatIdAsDto(chat.getId())).hasSize(1);
    }

    private User createUser(String username, String email, String rawPassword) {
        Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setName(username);
        user.setLastName("Test");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setProfileImageUrl(username + ".png");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setCreatedAt(LocalDate.now());
        user.setRoles(Set.of(role));
        return userRepository.saveAndFlush(user);
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

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginDto(username, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private RegisterDto validRegistration(String username, String email) {
        return new RegisterDto(
                username,
                "New",
                "User",
                email,
                "valid-password",
                LocalDate.of(2000, 1, 1),
                false
        );
    }

    private MockMultipartFile imagePart() {
        return new MockMultipartFile("image", "image.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
    }

    private MockMultipartFile jsonPart(String name, Object value) throws Exception {
        return new MockMultipartFile(
                name,
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(value)
        );
    }
}
