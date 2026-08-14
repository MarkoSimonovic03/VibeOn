package com.metropolitan.VibeOn;

import com.metropolitan.VibeOn.entity.Follow;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.Story;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.StoryRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StoryFunctionalityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private FollowRepository followRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @BeforeEach
    void configureImageStorage() throws Exception {
        when(imageStorageService.save(any())).thenAnswer(invocation -> {
            MultipartFile image = invocation.getArgument(0);
            if (image == null || image.isEmpty()) {
                throw new IllegalArgumentException("Image is required");
            }
            if (!Set.of(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/gif")
                    .contains(image.getContentType())) {
                throw new IllegalArgumentException("Unsupported image type");
            }
            return "stored-story-" + System.nanoTime() + ".png";
        });
    }

    @Test
    void roleUserCanCreateAnImageStoryThatExpiresAfterTwentyFourHours() throws Exception {
        User alice = createUser("alice");

        String response = mockMvc.perform(multipart("/api/stories")
                        .file(imagePart())
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.profileImageUrl").value("alice.png"))
                .andExpect(jsonPath("$.ownStory").value(true))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Story savedStory = storyRepository.findAll().get(0);
        assertThat(savedStory.getUser().getId()).isEqualTo(alice.getId());
        assertThat(Duration.between(savedStory.getCreatedAt(), savedStory.getExpiresAt()))
                .isEqualTo(Duration.ofHours(24));
        assertThat(response).contains(savedStory.getImageUrl());
    }

    @Test
    void listingReturnsOnlyCurrentlyActiveOwnAndOtherUserStories() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        LocalDateTime now = LocalDateTime.now();

        Story ownStory = createStory(alice, now.minusHours(2), now.plusHours(22));
        Story otherStory = createStory(bob, now.minusHours(1), now.plusHours(23));
        createStory(bob, now.minusHours(25), now.minusHours(1));
        createStory(bob, now.plusMinutes(5), now.plusHours(24));

        mockMvc.perform(get("/api/stories")
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(otherStory.getId()))
                .andExpect(jsonPath("$[0].ownStory").value(false))
                .andExpect(jsonPath("$[1].id").value(ownStory.getId()))
                .andExpect(jsonPath("$[1].ownStory").value(true));
    }

    @Test
    void privateStoriesAreVisibleOnlyToTheOwnerAndApprovedFollowers() throws Exception {
        User privateOwner = createUser("private-owner", true);
        User viewer = createUser("viewer");
        LocalDateTime now = LocalDateTime.now();
        Story privateStory = createStory(
                privateOwner,
                now.minusMinutes(5),
                now.plusHours(23)
        );

        mockMvc.perform(get("/api/stories")
                        .with(user(viewer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/stories")
                        .with(user(privateOwner.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(privateStory.getId()))
                .andExpect(jsonPath("$[0].ownStory").value(true));

        followRepository.saveAndFlush(new Follow(null, viewer, privateOwner));

        mockMvc.perform(get("/api/stories")
                        .with(user(viewer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(privateStory.getId()))
                .andExpect(jsonPath("$[0].ownStory").value(false));
    }

    @Test
    void storyCreationRejectsEmptyAndUnsupportedUploads() throws Exception {
        User alice = createUser("alice");

        mockMvc.perform(multipart("/api/stories")
                        .file(new MockMultipartFile(
                                "image",
                                "empty.png",
                                MediaType.IMAGE_PNG_VALUE,
                                new byte[0]
                        ))
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/stories")
                        .file(new MockMultipartFile(
                                "image",
                                "story.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "not an image".getBytes()
                        ))
                        .with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isBadRequest());

        assertThat(storyRepository.count()).isZero();
    }

    @Test
    void storyEndpointsRequireRoleUser() throws Exception {
        User alice = createUser("alice");

        mockMvc.perform(get("/api/stories"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/stories").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/stories")
                        .file(imagePart())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/stories").with(user(alice.getUsername()).roles("USER")))
                .andExpect(status().isOk());
    }

    private User createUser(String username) {
        return createUser(username, false);
    }

    private User createUser(String username, boolean isPrivate) {
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
        user.setPrivate(isPrivate);
        user.setRoles(Set.of(role));
        return userRepository.saveAndFlush(user);
    }

    private Story createStory(User user, LocalDateTime createdAt, LocalDateTime expiresAt) {
        Story story = new Story();
        story.setUser(user);
        story.setImageUrl(user.getUsername() + "-" + System.nanoTime() + ".png");
        story.setCreatedAt(createdAt);
        story.setExpiresAt(expiresAt);
        return storyRepository.saveAndFlush(story);
    }

    private MockMultipartFile imagePart() {
        return new MockMultipartFile(
                "image",
                "story.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );
    }
}
