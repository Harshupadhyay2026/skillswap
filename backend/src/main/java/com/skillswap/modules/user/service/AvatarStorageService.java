package com.skillswap.modules.user.service;

import com.skillswap.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores avatar images on local disk.
 *
 * Validation is deliberately strict: a bounded size, an allow-listed image
 * type, and a magic-byte check so a renamed HTML/JS file cannot be served back
 * from the public /api/files route. File names are generated, never taken from
 * the client, which removes path traversal from the picture entirely.
 */
@Service
public class AvatarStorageService {

    private static final Logger log = LoggerFactory.getLogger(AvatarStorageService.class);

    /** 2 MB — enough for a profile photo, small enough to keep storage tidy. */
    public static final long MAX_BYTES = 2L * 1024 * 1024;

    private static final String PUBLIC_PREFIX = "/api/files/avatars/";

    private final Path avatarDir;

    public AvatarStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.avatarDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("avatars");
        try {
            Files.createDirectories(this.avatarDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create avatar directory: " + this.avatarDir, e);
        }
    }

    /**
     * @return the public URL path, e.g. {@code /api/files/avatars/12-abcd.png}
     */
    public String store(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose an image to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Image is too large — the limit is 2 MB");
        }

        String extension = detectExtension(file);
        String filename = userId + "-" + UUID.randomUUID().toString().substring(0, 12) + "." + extension;
        Path target = avatarDir.resolve(filename).normalize();

        if (!target.startsWith(avatarDir)) {
            throw new BadRequestException("Invalid file name");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the uploaded image", e);
        }

        return PUBLIC_PREFIX + filename;
    }

    /** Best-effort cleanup of a previous avatar belonging to this user. */
    public void deleteIfOwned(String avatarUrl, Long userId) {
        if (avatarUrl == null || !avatarUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String filename = avatarUrl.substring(PUBLIC_PREFIX.length());
        if (!filename.startsWith(userId + "-")) {
            return;
        }
        try {
            Files.deleteIfExists(avatarDir.resolve(filename).normalize());
        } catch (IOException e) {
            log.warn("Could not delete old avatar {}: {}", filename, e.getMessage());
        }
    }

    /**
     * Reads the first bytes of the upload and accepts only real PNG / JPEG /
     * GIF / WebP images. Content-Type alone is attacker-controlled.
     */
    private String detectExtension(MultipartFile file) {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(header);
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        if (read >= 8
                && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return "png";
        }
        if (read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (read >= 6 && ascii(header, 0, 3).equals("GIF")
                && (ascii(header, 3, 3).equals("87a") || ascii(header, 3, 3).equals("89a"))) {
            return "gif";
        }
        if (read >= 12 && ascii(header, 0, 4).equals("RIFF") && ascii(header, 8, 4).equals("WEBP")) {
            return "webp";
        }

        String contentType = file.getContentType() == null
                ? "unknown" : file.getContentType().toLowerCase(Locale.ROOT);
        throw new BadRequestException(
                "Unsupported image format (" + contentType + "). Use PNG, JPEG, GIF or WebP.");
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = offset; i < offset + length; i++) {
            sb.append((char) (bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
