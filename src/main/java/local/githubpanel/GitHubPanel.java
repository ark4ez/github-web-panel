package local.githubpanel;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.*;
import org.cef.network.CefRequest;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/** GitHub's website, with no DOM bridge, API credential, or cookie export. */
public final class GitHubPanel extends JPanel implements Disposable {
    private static final String PREFIX = "local.githubpanel.";
    private final Project project;
    private final PropertiesComponent preferences;
    private final JComboBox<String> repositories = new JComboBox<>();
    private final JLabel status = new JLabel("GitHub Web");
    private final JTextArea notice = textArea("");
    private final JPanel noticePanel = new JPanel(new BorderLayout());
    private final JPanel controls = new ResponsiveToolbar();
    private final JButton back = button("Back", "Previous page");
    private final JButton forward = button("Next", "Next page");
    private final JButton externalLink = button("Open link in browser", "Open the blocked HTTPS link after confirmation");
    private final JPanel center = new JPanel(new CardLayout());
    private final JPanel findBar = new JPanel(new BorderLayout(4, 0));
    private final JTextField findText = new JTextField();
    private final List<AbstractButton> browserButtons = new ArrayList<>();
    private final List<AbstractButton> sectionButtons = new ArrayList<>();
    private volatile boolean disposed;
    private volatile String currentUrl = "https://github.com/issues";
    private String blockedUrl;
    private String section;
    private boolean updatingRepositories;
    private boolean started;
    private boolean navigated;
    private boolean autoOpenWhenReady;
    private boolean initialLoadFailed;
    private double zoom;
    private JBCefBrowser browser;
    private JBCefClient client;
    private Timer refreshTimer;

    public GitHubPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        preferences = PropertiesComponent.getInstance(project);
        section = PanelPreferences.section(preferences.getValue(PREFIX + "section"));
        zoom = PanelPreferences.zoom(preferences.getValue(PREFIX + "zoom"));
        setPreferredSize(new Dimension(640, 800));
        setMinimumSize(new Dimension(220, 240));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel repositoryRow = new JPanel(new BorderLayout(4, 0));
        repositoryRow.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        repositories.getAccessibleContext().setAccessibleName("GitHub repository");
        repositories.setPrototypeDisplayValue("owner/repository");
        repositoryRow.add(repositories, BorderLayout.CENTER);
        JButton rescan = button("Rescan", "Refresh repositories from Git remotes");
        rescan.addActionListener(e -> refreshRepositories());
        repositoryRow.add(rescan, BorderLayout.EAST);
        top.add(repositoryRow);

        back.setEnabled(false);
        forward.setEnabled(false);
        controls.add(back);
        controls.add(forward);
        addControl("Reload", "Reload current page", () -> { clearNotice(); browser.getCefBrowser().reload(); });
        ButtonGroup sections = new ButtonGroup();
        for (String[] tab : new String[][]{{"Issues", "issues"}, {"PR", "pulls"}, {"Projects", "projects"}}) {
            JToggleButton tabButton = new JToggleButton(tab[0]);
            tabButton.setMargin(new Insets(3, 7, 3, 7));
            tabButton.setSelected(tab[1].equals(section));
            tabButton.setToolTipText("Open " + tab[0] + " for the selected repository");
            tabButton.addActionListener(e -> {
                section = tab[1];
                preferences.setValue(PREFIX + "section", section);
                openSection();
            });
            sections.add(tabButton);
            sectionButtons.add(tabButton);
            browserButtons.add(tabButton);
            controls.add(tabButton);
        }
        addControl("Sign in", "Sign in on github.com; your usual browser has a separate session", () -> navigate(GitHubUrls.signIn(repositoryForTest(), section)));
        addControl("Browser", "Open the current GitHub page in your default browser", () -> {
            if (GitHubUrls.isGitHubPage(currentUrl)) BrowserUtil.browse(currentUrl);
        });
        addControl("Find", "Find text on the current page", this::showFind);
        addControl("−", "Zoom out", () -> setZoom(zoom - 0.1));
        addControl("100%", "Reset page zoom", () -> setZoom(1));
        addControl("+", "Zoom in", () -> setZoom(zoom + 0.1));
        JButton help = button("Help", "Session, supported sign-in methods and keyboard controls");
        help.addActionListener(e -> Messages.showInfoMessage(project,
            "Use your GitHub website session; no personal access token is needed.\n\n"
            + "This session is separate from Chrome/Edge, but may be shared with other embedded browsers in Rider. "
            + "Sign out on GitHub before using another account. Uninstalling this plugin does not clear browser cookies.\n\n"
            + "Only github.com is supported. External SSO and GitHub Enterprise are not supported. "
            + "A 404 may indicate a missing page or insufficient access; try Sign in or Browser.\n\n"
            + "Tab moves through controls. Find searches within the page. Alt+Left/Right navigates history. "
            + "The external browser has a separate session and cannot transfer its login back here.", "GitHub Web Help"));
        controls.add(help);
        top.add(controls);
        controls.setVisible(false);
        configureFind();
        top.add(findBar);
        noticePanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        noticePanel.add(notice, BorderLayout.CENTER);
        JPanel noticeActions = new JPanel(new WrapLayout());
        noticeActions.add(externalLink);
        JButton dismiss = button("Dismiss", "Dismiss this message");
        dismiss.addActionListener(e -> clearNotice());
        noticeActions.add(dismiss);
        noticePanel.add(noticeActions, BorderLayout.SOUTH);
        externalLink.addActionListener(e -> openBlockedLink());
        noticePanel.setVisible(false);
        top.add(noticePanel);
        add(top, BorderLayout.NORTH);
        center.add(welcome(), "welcome");
        add(center, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        add(status, BorderLayout.SOUTH);
        setBrowserControls(false);
        repositories.addActionListener(e -> {
            if (updatingRepositories) return;
            String choice = repositoryForTest();
            if (choice != null) preferences.setValue(PREFIX + "repository", choice);
            if (started) openSection();
        });
        refreshRepositories();
        // Git models are cached. No fetch, subprocess, or polling while the panel is hidden.
        refreshTimer = new Timer(5000, e -> refreshRepositories());
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing() && !disposed) { refreshRepositories(); refreshTimer.start(); }
                else refreshTimer.stop();
            }
        });
        if (isShowing()) refreshTimer.start();
        project.getMessageBus().connect(this).subscribe(GitRepository.GIT_REPO_CHANGE, repository -> ui(() -> {
            if (isShowing()) refreshRepositories();
        }));
        if (PropertiesComponent.getInstance().getBoolean(PREFIX + "welcomeAccepted.v1", false)) {
            startBrowsing();
        }
        installShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "back", () -> { if (browser != null) browser.getCefBrowser().goBack(); });
        installShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), "forward", () -> { if (browser != null) browser.getCefBrowser().goForward(); });
    }

    private JPanel welcome() {
        JPanel welcome = new JPanel(new BorderLayout(0, 12));
        welcome.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));
        JTextArea heading = textArea("Your repository. GitHub's own interface.");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        welcome.add(heading, BorderLayout.NORTH);
        JTextArea explanation = textArea("Keep Issues, pull requests and Projects beside your code. The repository is selected from your Git remotes.\n\n"
            + "Opening the panel connects directly to GitHub. Sign in on GitHub when you need private repositories. No personal access token is required.\n\n"
            + "Your session may be shared with other embedded browsers in Rider. It is separate from your usual browser.\n\n"
            + "This beta supports github.com. External SSO and GitHub Enterprise are not supported. Download attachments in your usual browser; passkeys and account switching still need testing.\n\n"
            + "If a page shows 404, it may be missing or require access. Use Sign in, then select Issues or PR again.");
        JScrollPane scroll = new JScrollPane(explanation);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        welcome.add(scroll, BorderLayout.CENTER);
        JButton start = button("Open GitHub", "Connect to GitHub and open the selected repository");
        start.addActionListener(e -> {
            PropertiesComponent.getInstance().setValue(PREFIX + "welcomeAccepted.v1", true);
            startBrowsing();
        });
        welcome.add(start, BorderLayout.SOUTH);
        return welcome;
    }

    private void startBrowsing() {
        if (started || disposed) return;
        if (!SystemInfo.isWindows) {
            showNotice("This beta has been validated only on Windows. Other platforms are not enabled yet.", null);
            return;
        }
        if (!JBCefApp.isSupported()) {
            showNotice("Embedded browser unavailable. Use Rider's bundled runtime and enable Web Browser (JCEF), then reopen the project.", null);
            return;
        }
        try {
            client = JBCefApp.getInstance().createClient();
            Disposer.register(this, client);
            browser = JBCefBrowser.createBuilder().setClient(client).setUrl("about:blank")
                .setEnableOpenDevToolsMenuItem(false).build();
            Disposer.register(this, browser);
            installHandlers();
            browser.setOpenLinksInExternalBrowser(false);
            center.add(browser.getComponent(), "browser");
            ((CardLayout) center.getLayout()).show(center, "browser");
            started = true;
            controls.setVisible(true);
            browser.setZoomLevel(zoom);
            back.addActionListener(e -> browser.getCefBrowser().goBack());
            forward.addActionListener(e -> browser.getCefBrowser().goForward());
            setBrowserControls(true);
            autoOpenWhenReady = true;
            // Give the cached Git model time to initialize before choosing a landing page.
            Timer initial = new Timer(800, e -> {
                if (!disposed && !navigated) { refreshRepositories(); if (!navigated) openSection(); }
            });
            initial.setRepeats(false);
            Disposer.register(this, () -> initial.stop());
            initial.start();
            if (repositories.getItemCount() > 0) openSection();
        } catch (RuntimeException | LinkageError error) {
            if (browser != null) { Disposer.dispose(browser); browser = null; }
            if (client != null) { Disposer.dispose(client); client = null; }
            started = false;
            controls.setVisible(false);
            setBrowserControls(false);
            ((CardLayout) center.getLayout()).show(center, "welcome");
            showNotice("The embedded browser could not start. Check Rider's runtime and JCEF plugin, then retry. (" + error.getClass().getSimpleName() + ")", null);
        }
    }

    private void configureFind() {
        findBar.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
        findText.getAccessibleContext().setAccessibleName("Find text on page");
        findText.setToolTipText("Enter to find the next occurrence");
        findBar.add(findText, BorderLayout.CENTER);
        JPanel actions = new JPanel(new WrapLayout());
        JButton next = button("Next", "Next match");
        next.addActionListener(e -> find(true));
        JButton close = button("Close", "Close page search");
        close.addActionListener(e -> closeFind());
        actions.add(next);
        actions.add(close);
        findBar.add(actions, BorderLayout.SOUTH);
        findBar.setVisible(false);
        findText.addActionListener(e -> find(true));
        findText.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { find(false); }
            public void removeUpdate(DocumentEvent e) { find(false); }
            public void changedUpdate(DocumentEvent e) { find(false); }
        });
        findText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeSearch");
        findText.getActionMap().put("closeSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { closeFind(); }
        });
    }

    private void installShortcut(KeyStroke key, String name, Runnable action) {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, name);
        getActionMap().put(name, new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { action.run(); } });
    }
    private void showFind() { findBar.setVisible(true); revalidate(); findText.requestFocusInWindow(); }
    private void closeFind() { findBar.setVisible(false); if (browser != null) browser.getCefBrowser().stopFinding(true); revalidate(); }
    private void find(boolean next) {
        if (browser == null) return;
        if (findText.getText().isEmpty()) browser.getCefBrowser().stopFinding(true);
        else browser.getCefBrowser().find(findText.getText(), true, false, next);
    }
    private void setZoom(double value) {
        zoom = PanelPreferences.zoom(Double.toString(value));
        browser.setZoomLevel(zoom);
        preferences.setValue(PREFIX + "zoom", Double.toString(zoom));
        status.setText("github.com · " + Math.round(zoom * 100) + "%");
    }
    private void addControl(String title, String tooltip, Runnable action) {
        JButton button = button(title, tooltip);
        button.addActionListener(e -> { if (browser != null && !disposed) action.run(); });
        browserButtons.add(button);
        controls.add(button);
    }
    private static JButton button(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setMargin(new Insets(3, 7, 3, 7));
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }
    private static JTextArea textArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(UIManager.getFont("Label.font"));
        return area;
    }
    private void setBrowserControls(boolean enabled) {
        for (AbstractButton button : browserButtons) button.setEnabled(enabled);
        for (AbstractButton button : sectionButtons) {
            if ("Projects".equals(button.getText())) button.setEnabled(enabled && repositories.getItemCount() > 0);
        }
        if (!enabled) { back.setEnabled(false); forward.setEnabled(false); }
    }

    private void refreshRepositories() {
        if (disposed || project.isDisposed()) return;
        LinkedHashSet<String> found = new LinkedHashSet<>();
        for (GitRepository repository : GitRepositoryManager.getInstance(project).getRepositories()) {
            repository.getRemotes().stream()
                .sorted(Comparator.comparing(remote -> !remote.getName().equals("origin")))
                .forEach(remote -> remote.getUrls().forEach(url -> GitHubUrls.repository(url).ifPresent(found::add)));
        }
        List<String> existing = new ArrayList<>();
        for (int i = 0; i < repositories.getItemCount(); i++) existing.add(repositories.getItemAt(i));
        if (!existing.equals(new ArrayList<>(found))) {
            String selected = repositoryForTest();
            if (selected == null) selected = preferences.getValue(PREFIX + "repository");
            updatingRepositories = true;
            try {
                repositories.removeAllItems();
                found.forEach(repositories::addItem);
                if (selected != null && found.contains(selected)) repositories.setSelectedItem(selected);
            } finally { updatingRepositories = false; }
        }
        repositories.setEnabled(!found.isEmpty());
        repositories.setToolTipText(found.isEmpty() ? "No github.com remote found. Use your personal Issues and PR pages." : "GitHub repository from this project's Git remotes");
        setBrowserControls(started);
        // Never replace an active page or a comment draft when Git remotes change.
        if (autoOpenWhenReady && !navigated && !found.isEmpty()) openSection();
    }

    private void openSection() {
        String repository = repositoryForTest();
        String url = repository == null ? "https://github.com/" + (section.equals("pulls") ? "pulls" : "issues") : GitHubUrls.page(repository, section);
        navigate(url);
    }

    void navigate(String url) {
        if (disposed || browser == null || !GitHubUrls.isGitHubPage(url)) return;
        navigated = true;
        autoOpenWhenReady = false;
        clearNotice();
        browser.loadURL(url);
    }
    private void clearNotice() {
        blockedUrl = null;
        noticePanel.setVisible(false);
        notice.setText("");
        revalidate();
    }
    private void showNotice(String message, String external) {
        blockedUrl = external;
        notice.setText(message);
        externalLink.setText(GitHubUrls.isAttachment(external) ? "Open attachment in browser" : "Open link in browser");
        externalLink.setVisible(GitHubUrls.isExternalWebPage(external) || GitHubUrls.isAttachment(external));
        noticePanel.setVisible(true);
        revalidate();
    }
    private void blocked(String url, boolean userGesture) {
        ui(() -> showNotice("This panel stays on github.com. Destination: " + GitHubUrls.displayHost(url)
            + ". External sign-in providers cannot complete login in this panel. Your usual browser has a separate session.",
            userGesture && GitHubUrls.isExternalWebPage(url) ? url : null));
    }
    private void offerAttachment(String url) {
        ui(() -> showNotice("Attachment downloads need your usual browser in this beta. Open the original GitHub attachment there; you may need to sign in separately. Your current page stays here.", url));
    }
    private void openBlockedLink() {
        String target = blockedUrl;
        if (!GitHubUrls.isExternalWebPage(target) && !GitHubUrls.isAttachment(target)) return;
        if (Messages.showYesNoDialog(project, "Open an external HTTPS link to " + GitHubUrls.displayHost(target)
            + " in your default browser?\n\nOnly proceed if this is the destination you intended. It uses a separate login session.",
            "Open External Link", "Open Browser", "Cancel", Messages.getQuestionIcon()) == Messages.YES) {
            BrowserUtil.browse(target);
        }
    }

    private void installHandlers() {
        var cef = client.getCefClient();
        cef.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override public boolean onBeforeBrowse(CefBrowser b, CefFrame frame, CefRequest request, boolean userGesture, boolean redirect) {
                if (!frame.isMain()) return false;
                String url = request.getURL();
                if (GitHubUrls.isAttachment(url)) { offerAttachment(url); return true; }
                if ("about:blank".equals(url) || GitHubUrls.isGitHubPage(url)) return false;
                blocked(url, userGesture && !redirect);
                return true;
            }
            @Override public boolean onOpenURLFromTab(CefBrowser b, CefFrame frame, String url, boolean gesture) {
                if (GitHubUrls.isAttachment(url)) offerAttachment(url); else if (GitHubUrls.isGitHubPage(url)) ui(() -> navigate(url)); else blocked(url, gesture);
                return true;
            }
        });
        cef.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override public boolean onBeforePopup(CefBrowser b, CefFrame frame, String url, String target) {
                if (GitHubUrls.isAttachment(url)) offerAttachment(url); else if (GitHubUrls.isGitHubPage(url)) ui(() -> navigate(url)); else blocked(url, false);
                return true;
            }
        });
        cef.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override public void onAddressChange(CefBrowser b, CefFrame frame, String url) {
                if (frame.isMain() && GitHubUrls.isGitHubPage(url)) {
                    currentUrl = url; // In-memory only. Never log or persist redirects or query strings.
                    ui(() -> GitHubUrls.sectionForPage(url, repositoryForTest()).ifPresent(active -> {
                        section = active;
                        preferences.setValue(PREFIX + "section", active);
                        for (AbstractButton button : sectionButtons) {
                            String target = "PR".equals(button.getText()) ? "pulls" : button.getText().toLowerCase(java.util.Locale.ROOT);
                            button.setSelected(target.equals(active));
                        }
                    }));
                }
            }
        });
        cef.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override public void onLoadingStateChange(CefBrowser b, boolean loading, boolean canBack, boolean canForward) {
                ui(() -> {
                    back.setEnabled(canBack);
                    forward.setEnabled(canForward);
                    status.setText(loading ? "Loading github.com…" : initialLoadFailed ? "Page could not load" : "github.com");
                });
            }
            @Override public void onLoadStart(CefBrowser b, CefFrame frame, CefRequest.TransitionType transition) {
                if (frame.isMain()) ui(() -> { initialLoadFailed = false; clearNotice(); });
            }
            @Override public void onLoadEnd(CefBrowser b, CefFrame frame, int httpStatus) {
                if (!frame.isMain()) return;
                ui(() -> {
                    if (httpStatus == 404 || httpStatus == 403) showNotice("GitHub returned " + httpStatus
                        + ". This page may be missing or your account may need access. Try Sign in, then reopen Issues or PR. Browser uses a separate session.", null);
                    else if (httpStatus >= 500) showNotice("GitHub is temporarily unavailable (" + httpStatus + "). Try Reload later.", null);
                });
            }
            @Override public void onLoadError(CefBrowser b, CefFrame frame, CefLoadHandler.ErrorCode code, String text, String url) {
                if (frame.isMain() && code != CefLoadHandler.ErrorCode.ERR_ABORTED) ui(() -> {
                    initialLoadFailed = true;
                    status.setText("Page could not load");
                    showNotice("Page could not load (" + code + "). Check your connection and use Reload. Certificate errors are not bypassed.", null);
                });
            }
        });
    }

    private void ui(Runnable action) {
        ApplicationManager.getApplication().invokeLater(() -> { if (!disposed && !project.isDisposed()) action.run(); });
    }
    JBCefBrowser browserForTest() { return browser; }
    String repositoryForTest() { return (String) repositories.getSelectedItem(); }
    void startForTest() { startBrowsing(); }
    @Override public void dispose() {
        disposed = true;
        blockedUrl = null;
        currentUrl = "";
        if (refreshTimer != null) refreshTimer.stop();
    }
}
