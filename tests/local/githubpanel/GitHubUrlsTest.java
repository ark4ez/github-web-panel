package local.githubpanel;

public final class GitHubUrlsTest {
    private static int checks;
    public static void main(String[] args) {
        repo("git@github.com:Owner/repo.git", "Owner/repo");
        repo("https://github.com/Owner/repo.git", "Owner/repo");
        repo("ssh://git@github.com/Owner/repo.git", "Owner/repo");
        repo("https://GITHUB.com/Owner/repo/", "Owner/repo");
        for (String value : new String[]{"https://github.com.evil.test/Owner/repo", "https://token@github.com/Owner/repo", "https://github.com/Owner/../repo", "https://github.com/Owner/%2e%2e", "https://github.com/Owner/repo?token=secret", "http://github.com/Owner/repo", "https://github.com:8443/Owner/repo", "git@elsewhere:Owner/repo.git", "https://github.com/Owner/..", "file:///C:/repo", "Owner/repo", "https://github.com/Owner/repo/issues"}) {
            check(GitHubUrls.repository(value).isEmpty(), "reject remote: " + value);
        }
        for (String value : new String[]{"https://github.com/login", "https://github.com:443/Owner/repo/issues?state=open", "https://github.com/orgs/Owner/projects"}) check(GitHubUrls.isGitHubPage(value), "allow page");
        for (String value : new String[]{"http://github.com/", "https://github.com.evil.test/", "https://github.com@evil.test/", "https://evil.test@github.com/", "javascript:alert(1)", "file:///C:/secrets", "https://github.com:8443/", "data:text/html,hello", "https://github.com\\@evil.test/", "https://github.com./"}) check(!GitHubUrls.isGitHubPage(value), "reject page: " + value);
        check(GitHubUrls.page("Owner/repo", "issues").equals("https://github.com/Owner/repo/issues"), "issue URL");
        check(GitHubUrls.page("Owner/repo", "pulls").endsWith("/pulls"), "PR URL");
        check(GitHubUrls.page("Owner/repo", "projects").endsWith("/projects"), "projects URL");
        check(GitHubUrls.displayHost("https://example.com/callback?token=secret").equals("example.com"), "do not expose query");
        try { GitHubUrls.page("Owner/repo", "../settings"); throw new AssertionError("section escape"); } catch (IllegalArgumentException expected) { checks++; }
        System.out.println("PASS: " + checks + " URL and navigation-boundary checks");
    }
    private static void repo(String value, String expected) { check(GitHubUrls.repository(value).orElseThrow().equals(expected), "remote parsing"); }
    private static void check(boolean result, String label) { if (!result) throw new AssertionError(label); checks++; }
}
