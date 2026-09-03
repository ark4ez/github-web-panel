package local.githubpanel;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Account-free regression suite; no network or IDE launch. */
public final class RegressionSuite {
    private static int checks;
    public static void main(String[] args) throws Exception {
        GitHubUrlsTest.main(args);
        initialNavigation();
        for (String value : new String[]{null, "", "NaN", "Infinity", "-Infinity", "garbage"}) check(PanelPreferences.zoom(value) == 1.0, "invalid zoom");
        check(PanelPreferences.zoom("0.1") == .5, "lower bound");
        check(PanelPreferences.zoom("9") == 2, "upper bound");
        check(PanelPreferences.zoom("1.3") == 1.3, "saved zoom");
        check(PanelPreferences.section("evil").equals("issues"), "invalid saved section");
        check(PanelPreferences.section(null).equals("issues"), "missing section");
        check(PanelPreferences.section("pulls").equals("pulls"), "saved PR section");
        String login = GitHubUrls.signIn("ark4ez/my.repo", "pulls");
        check(GitHubUrls.isGitHubPage(login), "login stays on GitHub");
        check(URLDecoder.decode(URI.create(login).getRawQuery().substring(10), StandardCharsets.UTF_8).equals("/ark4ez/my.repo/pulls"), "login return path");
        check(GitHubUrls.signIn(null, "projects").endsWith("%2Fissues"), "no repo fallback");
        try { GitHubUrls.signIn("../bad", "issues"); throw new AssertionError("unsafe login return"); } catch (IllegalArgumentException expected) { checks++; }
        check(GitHubUrls.isExternalWebPage("https://example.org/help?q=test"), "explicit external HTTPS link");
        check(GitHubUrls.isAttachment("https://github.com/user-attachments/files/123/test.txt"), "attachment link");
        for (String value : new String[]{null, "https://github.com/login", "https://github.com/user-attachments/files/1/..", "https://github.com/user-attachments/files/1/test.txt?redirect=x", "https://github.com/user-attachments/files/1/%2fsecret", "https://github.com.evil.test/user-attachments/files/1/test.txt", "http://github.com/user-attachments/files/1/test.txt"}) check(!GitHubUrls.isAttachment(value), "unsafe attachment link");
        check(GitHubUrls.sectionForPage("https://github.com/Owner/repo/pull/42", "Owner/repo").orElseThrow().equals("pulls"), "PR detail section");
        check(GitHubUrls.sectionForPage("https://github.com/Owner/repo/issues", "Owner/repo").orElseThrow().equals("issues"), "history section");
        check(GitHubUrls.sectionForPage("https://github.com/Other/repo/issues", "Owner/repo").isEmpty(), "different repository navigation");
        check(GitHubUrls.sectionForPage("https://github.com/login", "Owner/repo").isEmpty(), "login keeps section");
        for (String value : new String[]{null, "file:///C:/data", "javascript:alert(1)", "data:text/html,x", "http://example.org", "https://user:pass@example.org/", "https://example.org:8443/", "https://github.com/login", "https://example.org/\nnext"}) check(!GitHubUrls.isExternalWebPage(value), "unsafe external destination");
        SwingUtilities.invokeAndWait(RegressionSuite::layout);
        nestedResize();
        System.out.println("PASS: " + checks + " product regressions (plus URL suite)");
    }
    private static void initialNavigation() {
        Runnable[] ready = new Runnable[1];
        String[] repository = new String[1], opened = new String[1];
        int[] count = {0};
        InitialNavigation initial = new InitialNavigation();
        initial.schedule(callback -> ready[0] = callback, () -> { opened[0] = repository[0]; count[0]++; });
        check(count[0] == 0, "do not open personal page before Git initialization");
        repository[0] = "owner/repository";
        ready[0].run();
        check("owner/repository".equals(opened[0]), "use repository discovered during delayed initialization");
        ready[0].run();
        check(count[0] == 1, "initial navigation happens only once");
        InitialNavigation noGit = new InitialNavigation();
        noGit.schedule(Runnable::run, () -> count[0]++);
        check(count[0] == 2, "completed no-Git initialization still opens fallback");
        for (String reason : new String[]{"manual page choice", "panel disposal"}) {
            InitialNavigation cancelled = new InitialNavigation();
            cancelled.schedule(callback -> ready[0] = callback, () -> count[0]++);
            cancelled.cancel();
            ready[0].run();
            check(count[0] == 2, reason + " prevents late startup navigation");
        }
    }
    private static void nestedResize() throws Exception {
        JPanel[] root = new JPanel[1], top = new JPanel[1], toolbar = new JPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            root[0] = new JPanel(new BorderLayout());
            top[0] = new JPanel();
            top[0].setLayout(new BoxLayout(top[0], BoxLayout.Y_AXIS));
            toolbar[0] = new ResponsiveToolbar();
            for (int i = 0; i < 13; i++) { JButton button = new JButton("Button " + i); button.setPreferredSize(new Dimension(80, 28)); toolbar[0].add(button); }
            top[0].add(toolbar[0]); root[0].add(top[0], BorderLayout.NORTH);
            root[0].setSize(700, 800); root[0].doLayout(); top[0].doLayout(); toolbar[0].doLayout();
        });
        for (int width : new int[]{220, 640, 320}) {
            SwingUtilities.invokeAndWait(() -> { root[0].setSize(width, 800); root[0].doLayout(); top[0].doLayout(); });
            // Event queue flush: width-change invalidation precedes the next validation pass.
            SwingUtilities.invokeAndWait(() -> { root[0].doLayout(); top[0].doLayout(); toolbar[0].doLayout();
                for (Component child : toolbar[0].getComponents()) check(child.getY() + child.getHeight() <= toolbar[0].getHeight(), "nested toolbar clipped after resize to " + width);
            });
        }
    }
    private static void layout() {
        for (int width : new int[]{180, 220, 320, 480, 640}) {
            JPanel panel = new JPanel(new WrapLayout());
            for (String text : new String[]{"Back", "Next", "Reload", "Issues", "PR", "Projects", "Sign in", "Browser", "Find", "−", "100%", "+", "Help"}) panel.add(new JButton(text));
            panel.setSize(width, 1);
            panel.setSize(width, panel.getPreferredSize().height);
            panel.doLayout();
            for (Component child : panel.getComponents()) {
                check(child.getY() >= 0 && child.getY() + child.getHeight() <= panel.getHeight(), "toolbar button vertically clipped at " + width);
                check(child.getX() >= 0 && child.getX() + child.getWidth() <= width, "toolbar button horizontally clipped at " + width);
                check(child.isFocusable(), "keyboard focus");
            }
        }
    }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
}
