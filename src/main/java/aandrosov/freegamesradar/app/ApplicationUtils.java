package aandrosov.freegamesradar.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ApplicationUtils {

    public static Path saveTempFileFromUrl(String url, String prefix, String suffix) throws IOException {
        try (InputStream in = new URL(url).openStream()) {
            return saveTempFile(in, prefix, suffix);
        }
    }

    public static Path saveTempFile(InputStream in, String prefix, String suffix) throws IOException {
        Path path = Files.createTempFile(prefix, suffix);
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        return path;
    }
}
