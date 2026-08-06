package com.metropolitan.VibeOn.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageStorageService {
    private static final String IMAGE_DIRECTORY = "images";
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif"
    );

    public String save(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image is required");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image is too large");
        }

        String extension = ALLOWED_IMAGE_TYPES.get(image.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Unsupported image type");
        }

        byte[] imageBytes = image.getBytes();
        try (ByteArrayInputStream input = new ByteArrayInputStream(imageBytes)) {
            if (ImageIO.read(input) == null) {
                throw new IllegalArgumentException("Invalid image file");
            }
        }

        Path uploadDirectory = Paths.get(IMAGE_DIRECTORY);
        Files.createDirectories(uploadDirectory);
        String imageName = UUID.randomUUID() + extension;
        Files.write(uploadDirectory.resolve(imageName), imageBytes);
        return imageName;
    }
}
