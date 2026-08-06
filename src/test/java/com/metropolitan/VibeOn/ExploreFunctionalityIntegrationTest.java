package com.metropolitan.VibeOn;

import com.metropolitan.VibeOn.entity.Follow;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.PostBookmark;
import com.metropolitan.VibeOn.entity.PostComment;
import com.metropolitan.VibeOn.entity.PostLike;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.repository.PostBookmarkRepository;
import com.metropolitan.VibeOn.repository.PostCommentRepository;
import com.metropolitan.VibeOn.repository.PostLikeRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExploreFunctionalityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private FollowRepository followRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private PostCommentRepository postCommentRepository;
    @Autowired
    private PostBookmarkRepository postBookmarkRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    @Test
    void exploreRanksEngagingAndRecentPostsFromNonFollowedUsers() throws Exception {
        User currentUser = createUser("alice");
        User followedUser = createUser("followed");
        User popularAuthor = createUser("popular-author");
        User recentAuthor = createUser("recent-author");
        User staleAuthor = createUser("stale-author");
        User engagerOne = createUser("engager-one");
        User engagerTwo = createUser("engager-two");

        followRepository.saveAndFlush(new Follow(null, currentUser, followedUser));

        createPost(currentUser, "Own post", LocalDateTime.now());
        createPost(followedUser, "Followed post", LocalDateTime.now());
        Post popularPost = createPost(
                popularAuthor,
                "Popular recommendation",
                LocalDateTime.now().minusDays(10)
        );
        Post recentPost = createPost(
                recentAuthor,
                "Recent recommendation",
                LocalDateTime.now().minusHours(2)
        );
        Post stalePost = createPost(
                staleAuthor,
                "Stale recommendation",
                LocalDateTime.now().minusDays(40)
        );
        Post deletedPost = createPost(
                popularAuthor,
                "Deleted recommendation",
                LocalDateTime.now()
        );
        deletedPost.setDeleted(true);
        deletedPost.setDeletedAt(LocalDateTime.now());
        postRepository.saveAndFlush(deletedPost);

        addLike(popularPost, currentUser);
        addLike(popularPost, engagerOne);
        addLike(popularPost, engagerTwo);
        addComment(popularPost, engagerOne);
        addBookmark(popularPost, engagerOne);
        addBookmark(popularPost, engagerTwo);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/posts/explore")
                        .with(user(currentUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(popularPost.getId()))
                .andExpect(jsonPath("$[0].likeCount").value(3))
                .andExpect(jsonPath("$[0].likedByCurrentUser").value(true))
                .andExpect(jsonPath("$[1].id").value(recentPost.getId()))
                .andExpect(jsonPath("$[2].id").value(stalePost.getId()));
    }

    @Test
    void exploreRequiresRoleUser() throws Exception {
        User user = createUser("alice");
        createPost(createUser("author"), "Recommendation", LocalDateTime.now());

        mockMvc.perform(get("/api/posts/explore"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/posts/explore")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/posts/explore")
                        .with(user(user.getUsername()).roles("USER")))
                .andExpect(status().isOk());
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

    private Post createPost(User author, String description, LocalDateTime createdAt) {
        Post post = new Post();
        post.setUser(author);
        post.setImageUrl(author.getUsername() + "-" + System.nanoTime() + ".png");
        post.setDescription(description);
        post.setCreatedAt(createdAt);
        return postRepository.saveAndFlush(post);
    }

    private void addLike(Post post, User user) {
        PostLike like = new PostLike();
        like.setPost(post);
        like.setUser(user);
        postLikeRepository.save(like);
    }

    private void addComment(Post post, User user) {
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent("Comment");
        comment.setCreatedAt(LocalDateTime.now());
        postCommentRepository.save(comment);
    }

    private void addBookmark(Post post, User user) {
        PostBookmark bookmark = new PostBookmark();
        bookmark.setPost(post);
        bookmark.setUser(user);
        postBookmarkRepository.save(bookmark);
    }
}
