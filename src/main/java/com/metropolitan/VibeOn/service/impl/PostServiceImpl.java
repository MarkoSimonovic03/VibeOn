package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.PostBookmarkResponseDto;
import com.metropolitan.VibeOn.dto.PostLikeResponseDto;
import com.metropolitan.VibeOn.dto.SinglePostDto;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.PostBookmark;
import com.metropolitan.VibeOn.entity.PostLike;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.PostBookmarkRepository;
import com.metropolitan.VibeOn.repository.PostCommentRepository;
import com.metropolitan.VibeOn.repository.PostLikeRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.service.ImageStorageService;
import com.metropolitan.VibeOn.service.PostService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private static final int EXPLORE_RESULT_LIMIT = 30;
    private static final int MAX_IMAGES_PER_POST = 10;

    private final PostRepository postRepository;
    private final UserLookupService userLookupService;
    private final ImageStorageService imageStorageService;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final PostCommentRepository postCommentRepository;

    @Override
    @Transactional
    public SinglePostDto createPost(MultipartFile image, String description) throws IOException {
        return createPost(image == null ? List.of() : List.of(image), description);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public SinglePostDto createPost(List<MultipartFile> images, String description) throws IOException {
        if (images == null || images.isEmpty() || images.stream().anyMatch(image -> image == null || image.isEmpty())) {
            throw new IllegalArgumentException("At least one non-empty image is required");
        }
        if (images.size() > MAX_IMAGES_PER_POST) {
            throw new IllegalArgumentException("A post can contain at most " + MAX_IMAGES_PER_POST + " images");
        }
        User currentUser = userLookupService.getCurrentUser();
        List<String> storedImages = new java.util.ArrayList<>();
        for (MultipartFile image : images) {
            storedImages.add(imageStorageService.save(image));
        }

        Post post = new Post();
        post.setDescription(description);
        post.setUser(currentUser);
        post.setCreatedAt(LocalDateTime.now());
        post.setImageUrl(storedImages.get(0));
        post.setImageUrls(storedImages);

        Post savedPost = postRepository.save(post);

        return withCurrentUserState(SinglePostDto.fromPost(savedPost), currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SinglePostDto getPostById(Long id) {
        User currentUser = userLookupService.getCurrentUser();
        SinglePostDto post = postRepository.findPostByIdAsDto(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));
        return withCurrentUserState(post, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SinglePostDto> getAllPosts() {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        return withCurrentUserState(postRepository.findAllPostsAsDto(), currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SinglePostDto> getPostsByUsername(String username) {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        return withCurrentUserState(postRepository.findAllPostsByUsernameAsDto(username), currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SinglePostDto> getAllPostsByFolloweeUser() {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        return withCurrentUserState(postRepository.findAllPostsByFolloweeUserAsDto(currentUserId), currentUserId);
    }

    @Override
    @Transactional
    public SinglePostDto updatePost(Long id, String newDescription) throws AccessDeniedException {
        User currentUser = userLookupService.getCurrentUser();

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        if (!post.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to update this post.");
        }

        post.setDescription(newDescription);
        postRepository.save(post);

        return withCurrentUserState(SinglePostDto.fromPost(post), currentUser.getId());
    }

    @Override
    @Transactional
    public void softDeletePost(Long id) throws AccessDeniedException {
        User currentUser = userLookupService.getCurrentUser();

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        if (!post.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to delete this post.");
        }

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public PostLikeResponseDto likePost(Long id) {
        User currentUser = userLookupService.getCurrentUser();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        if (!postLikeRepository.existsByPostIdAndUserId(id, currentUser.getId())) {
            PostLike postLike = new PostLike();
            postLike.setPost(post);
            postLike.setUser(currentUser);
            postLikeRepository.save(postLike);
        }

        return getLikeStatus(id, currentUser.getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public PostLikeResponseDto unlikePost(Long id) {
        User currentUser = userLookupService.getCurrentUser();
        postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        postLikeRepository.findByPostIdAndUserId(id, currentUser.getId())
                .ifPresent(postLikeRepository::delete);

        return getLikeStatus(id, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<SinglePostDto> getBookmarkedPosts() {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        return withCurrentUserState(
                postRepository.findAllBookmarkedPostsAsDto(currentUserId),
                currentUserId
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public PostBookmarkResponseDto bookmarkPost(Long id) {
        User currentUser = userLookupService.getCurrentUser();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        if (!postBookmarkRepository.existsByPostIdAndUserId(id, currentUser.getId())) {
            PostBookmark bookmark = new PostBookmark();
            bookmark.setPost(post);
            bookmark.setUser(currentUser);
            postBookmarkRepository.save(bookmark);
        }

        return getBookmarkStatus(id, currentUser.getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public PostBookmarkResponseDto removeBookmark(Long id) {
        User currentUser = userLookupService.getCurrentUser();
        postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        postBookmarkRepository.findByPostIdAndUserId(id, currentUser.getId())
                .ifPresent(postBookmarkRepository::delete);

        return getBookmarkStatus(id, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<SinglePostDto> searchPostsByHashtag(String hashtag) {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        String normalizedHashtag = hashtag.trim().toLowerCase(Locale.ROOT);
        Pattern exactHashtag = Pattern.compile(
                "(?iu)(?<![\\p{L}\\p{N}_])#" + Pattern.quote(normalizedHashtag)
                        + "(?![\\p{L}\\p{N}_])"
        );

        List<SinglePostDto> matchingPosts = postRepository
                .findHashtagCandidates(normalizedHashtag)
                .stream()
                .filter(post -> exactHashtag.matcher(post.getDescription()).find())
                .toList();

        return withCurrentUserState(matchingPosts, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<SinglePostDto> getExplorePosts() {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        LocalDateTime now = LocalDateTime.now();

        List<SinglePostDto> recommendations = postRepository
                .findExploreCandidates(currentUserId)
                .stream()
                .map(post -> new RankedPost(post, recommendationScore(post, now)))
                .sorted(Comparator
                        .comparingLong(RankedPost::score)
                        .reversed()
                        .thenComparing(
                                rankedPost -> rankedPost.post().getCreatedAt(),
                                Comparator.reverseOrder()
                        ))
                .limit(EXPLORE_RESULT_LIMIT)
                .map(RankedPost::post)
                .toList();

        return withCurrentUserState(recommendations, currentUserId);
    }

    private List<SinglePostDto> withCurrentUserState(List<SinglePostDto> posts, Long currentUserId) {
        posts.forEach(post -> withCurrentUserState(post, currentUserId));
        return posts;
    }

    private SinglePostDto withCurrentUserState(SinglePostDto post, Long currentUserId) {
        postRepository.findById(post.getId())
                .ifPresent(entity -> post.setImageUrls(entity.getEffectiveImageUrls()));
        post.setLikeCount(postLikeRepository.countByPostId(post.getId()));
        post.setLikedByCurrentUser(
                postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId)
        );
        post.setBookmarkedByCurrentUser(
                postBookmarkRepository.existsByPostIdAndUserId(post.getId(), currentUserId)
        );
        return post;
    }

    private PostLikeResponseDto getLikeStatus(Long postId, Long currentUserId) {
        return new PostLikeResponseDto(
                postId,
                postLikeRepository.countByPostId(postId),
                postLikeRepository.existsByPostIdAndUserId(postId, currentUserId)
        );
    }

    private PostBookmarkResponseDto getBookmarkStatus(Long postId, Long currentUserId) {
        return new PostBookmarkResponseDto(
                postId,
                postBookmarkRepository.existsByPostIdAndUserId(postId, currentUserId)
        );
    }

    private long recommendationScore(SinglePostDto post, LocalDateTime now) {
        long engagementScore =
                postLikeRepository.countByPostId(post.getId()) * 3
                + postCommentRepository.countByPostId(post.getId()) * 2
                + postBookmarkRepository.countByPostId(post.getId()) * 2;

        long ageInDays = Math.max(0, Duration.between(post.getCreatedAt(), now).toDays());
        long recencyBonus;
        if (ageInDays < 1) {
            recencyBonus = 6;
        } else if (ageInDays < 7) {
            recencyBonus = 3;
        } else if (ageInDays < 30) {
            recencyBonus = 1;
        } else {
            recencyBonus = 0;
        }

        return engagementScore + recencyBonus;
    }

    private record RankedPost(SinglePostDto post, long score) {
    }
}
