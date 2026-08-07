package com.metropolitan.VibeOn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Where(clause = "is_deleted = false")
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", unique = true)
    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image_url", nullable = false, unique = true)
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "description")
    private String description;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public List<String> getEffectiveImageUrls() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return List.copyOf(imageUrls);
        }
        return imageUrl == null ? List.of() : List.of(imageUrl);
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls == null ? new ArrayList<>() : new ArrayList<>(imageUrls);
    }
}
