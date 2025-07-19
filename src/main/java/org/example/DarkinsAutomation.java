package org.example;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DarkinsAutomation {

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setArgs(List.of("--start-maximized"))
            );

            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(null));
            Page page = context.newPage();

            page.navigate("https://darkins.in/");
            page.waitForLoadState(LoadState.LOAD);

            page.navigate("https://darkins.in/collections/experiences-tours/experiences");
            page.waitForLoadState(LoadState.LOAD);
            page.waitForTimeout(2000);
            System.out.println("Navigated to 'Experiences' filtered page.");

            page.waitForSelector("select#SortBy");
            page.selectOption("select#SortBy", new SelectOption().setLabel("Price, low to high"));
            System.out.println("Sorted by Price: Low to High");
            page.waitForTimeout(1000);

            Path downloadDir = Paths.get("src/test/Images");
            if (!Files.exists(downloadDir)) Files.createDirectories(downloadDir);

            List<String> imageUrls = page.locator(".grid-view-item__image").elementHandles().stream()
                    .map(e -> {
                        String src = e.getAttribute("src");
                        if (src == null || src.isEmpty()) {
                            src = e.getAttribute("data-src");
                        }
                        if ((src == null || src.isEmpty())) {
                            String srcset = e.getAttribute("srcset");
                            if (srcset == null || srcset.isEmpty()) {
                                srcset = e.getAttribute("data-srcset");
                            }
                            if (srcset != null && !srcset.isEmpty()) {
                                String[] parts = srcset.split(",");
                                String largest = parts[parts.length - 1].trim().split(" ")[0];
                                src = largest;
                            }
                        }
                        if (src != null && src.startsWith("//")) {
                            src = "https:" + src;
                        } else if (src != null && src.startsWith("/")) {
                            src = "https://darkins.in" + src;
                        } else if (src != null && !(src.startsWith("http://") || src.startsWith("https://"))) {
                            src = "https://darkins.in/" + src;
                        }
                        return src;
                    })
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.toList());

            System.out.println("Found " + imageUrls.size() + " images.");
            for (String url : imageUrls) {
                System.out.println("Image URL: " + url);
            }

            int index = 1;
            for (String url : imageUrls) {
                Path outputPath = downloadDir.resolve("image_" + index + ".jpg");
                System.out.println("[DEBUG] Attempting to download: " + url);
                System.out.println("[DEBUG] Output path: " + outputPath.toAbsolutePath());
                downloadImage(url, outputPath);
                index++;
            }

            System.out.println("Image download complete!");
            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadImage(String imageUrl, Path outputPath) {
        System.out.println("[DEBUG] downloadImage called with URL: " + imageUrl + ", outputPath: " + outputPath.toAbsolutePath());
        try {
            URI uri = URI.create(imageUrl);
            URL url = uri.toURL();
            try (InputStream in = url.openStream()) {
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image downloaded: " + outputPath.getFileName());
            }
        } catch (Exception e) {
            System.err.println("Failed to download: " + imageUrl);
            System.err.println("[DEBUG] Exception: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
