package local.githubpanel;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

/** Parsing only: never executes git, resolves DNS, or handles credentials. */
public final class GitHubUrls {
    private static final Pattern REPO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9_.-]{1,100}");
    private GitHubUrls() {}

    public static Optional<String> repository(String remote) {
        if (remote == null) return Optional.empty();
        String value = remote.trim();
        String path;
        try {
            if (value.startsWith("git@github.com:")) {
                path = value.substring("git@github.com:".length());
            } else {
                URI uri = URI.create(value);
                if (!"github.com".equalsIgnoreCase(uri.getHost()) || uri.getQuery() != null || uri.getFragment() != null) return Optional.empty();
                boolean https = "https".equalsIgnoreCase(uri.getScheme()) && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null;
                boolean ssh = "ssh".equalsIgnoreCase(uri.getScheme()) && (uri.getPort() == -1 || uri.getPort() == 22) && "git".equals(uri.getUserInfo());
                if (!https && !ssh) return Optional.empty();
                path = uri.getRawPath();
                if (path == null || !path.startsWith("/")) return Optional.empty();
                path = path.substring(1);
            }
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
            if (!REPO.matcher(path).matches() || path.endsWith("/.") || path.endsWith("/..")) return Optional.empty();
            return Optional.of(path);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static boolean isGitHubPage(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && "github.com".equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null;
        } catch (RuntimeException ignored) { return false; }
    }

    /** Recognize attachment routes for an explicit external-browser fallback. */
    public static boolean isAttachment(String url) {
        if (!isGitHubPage(url)) return false;
        URI uri = URI.create(url);
        String path = uri.getRawPath();
        return uri.getRawQuery() == null && uri.getRawFragment() == null && path != null
            && path.matches("/user-attachments/files/[0-9]+/[A-Za-z0-9_.-]+")
            && !path.endsWith("/.") && !path.endsWith("/..");
    }

    /** External links are opened only after a deliberate, separate user action. */
    public static boolean isExternalWebPage(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
                && !isGitHubPage(url);
        } catch (RuntimeException ignored) { return false; }
    }

    public static String signIn(String repository, String section) {
        String destination = repository == null ? "/issues" : "/" + repository + "/" + PanelPreferences.section(section);
        if (repository != null) page(repository, PanelPreferences.section(section));
        return "https://github.com/login?return_to=" + URLEncoder.encode(destination, StandardCharsets.UTF_8);
    }

    public static Optional<String> sectionForPage(String url, String repository) {
        if (!isGitHubPage(url)) return Optional.empty();
        String prefix = repository == null ? "/" : "/" + repository + "/";
        String path = URI.create(url).getPath();
        if (path == null || !path.regionMatches(true, 0, prefix, 0, prefix.length())) return Optional.empty();
        String first = path.substring(prefix.length()).split("/", 2)[0];
        return switch (first) {
            case "pull", "pulls" -> Optional.of("pulls");
            case "issues" -> Optional.of("issues");
            case "projects" -> Optional.of("projects");
            default -> Optional.empty();
        };
    }

    public static String page(String repository, String section) {
        if (!REPO.matcher(repository).matches() || repository.endsWith("/.") || repository.endsWith("/..")) throw new IllegalArgumentException("Invalid repository");
        if (!section.equals("issues") && !section.equals("pulls") && !section.equals("projects")) throw new IllegalArgumentException("Invalid section");
        return "https://github.com/" + repository + "/" + section;
    }

    /** Only a host is shown in notices, never query parameters or callback tokens. */
    public static String displayHost(String url) {
        try { String host = URI.create(url).getHost(); return host == null ? "unsupported address" : host; }
        catch (RuntimeException ignored) { return "unsupported address"; }
    }
}
