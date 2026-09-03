package local.githubpanel;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
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
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

/** GitHub's website, with no DOM bridge, API credential, or cookie export. */
public final class GitHubPanel extends JPanel implements Disposable {
    private static final String PREFIX = "local.githubpanel.";
    private final Project project;
    private final PropertiesComponent preferences;
    private final JComboBox<String> repositories = new JComboBox<>();
    private final JLabel status = new JLabel("GitHub Web");
    private final JTextArea notice = textArea("");
    private final JPanel noticePanel = new JPanel(new BorderLayout());
    private final JComboBox<String> sections = new JComboBox<>(new String[]{"Issues", "PR", "Projects"});
    private final JButton externalLink = button("Open link in browser", "Open the blocked HTTPS link after confirmation");
    private final JPanel center = new JPanel(new CardLayout());
    private final JPanel findBar = new JPanel(new BorderLayout(4, 0));
    private final JTextField findText = new JTextField();
    private final JTextField address = new JTextField();
    private final JButton copyAddress = new JButton(AllIcons.Actions.Copy);
    private boolean canGoBack;
    private boolean canGoForward;
    private boolean updatingSection;
    private volatile boolean disposed;
    private volatile String currentUrl = "";
    private String blockedUrl;
    private String section;
    private boolean updatingRepositories;
    private boolean started;
    private final InitialNavigation initialNavigation = new InitialNavigation();
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
        repositories.setMinimumSize(new Dimension(0, repositories.getPreferredSize().height));
        repositoryRow.add(repositories, BorderLayout.CENTER);
        sections.getAccessibleContext().setAccessibleName("GitHub section");
        sections.setToolTipText("Open Issues, pull requests or Projects");
        selectSection();
        sections.addActionListener(e -> {
            if (updatingSection || !started) return;
            String selected = switch (sections.getSelectedIndex()) {
                case 1 -> "pulls";
                case 2 -> "projects";
                default -> "issues";
            };
            if ("projects".equals(selected) && repositoryForTest() == null) {
                selectSection();
                showNotice("Select a GitHub repository to open Projects.", null);
                return;
            }
            section = selected;
            preferences.setValue(PREFIX + "section", section);
            openSection();
        });
        repositoryRow.add(sections, BorderLayout.EAST);
        top.add(repositoryRow);
        top.add(addressBar());
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

    List<AnAction> titleActions() {
        BooleanSupplier ready = () -> browser != null && started && !disposed;
        DefaultActionGroup more = new DefaultActionGroup("More", true);
        more.getTemplatePresentation().setIcon(AllIcons.Actions.MoreHorizontal);
        more.add(action("Find on page", AllIcons.Actions.Find, ready, this::showFind));
        more.add(action("Open in browser", null, ready, () -> {
            if (GitHubUrls.isGitHubPage(currentUrl)) BrowserUtil.browse(currentUrl);
        }));
        more.add(action("Sign in to GitHub", null, ready, () -> navigate(GitHubUrls.signIn(repositoryForTest(), section))));
        more.addSeparator();
        DefaultActionGroup zoomMenu = new DefaultActionGroup("Page zoom", true);
        zoomMenu.add(action("Zoom in", null, ready, () -> setZoom(zoom + .1)));
        zoomMenu.add(action("Zoom out", null, ready, () -> setZoom(zoom - .1)));
        zoomMenu.add(action("Reset to 100%", null, ready, () -> setZoom(1)));
        more.add(zoomMenu);
        more.addSeparator();
        more.add(action("Rescan repositories", null, () -> !disposed, this::refreshRepositories));
        more.add(action("Help and privacy", null, () -> !disposed, this::showHelp));
        return List.of(
            action("Back", AllIcons.Actions.Back, () -> ready.getAsBoolean() && canGoBack, () -> browser.getCefBrowser().goBack()),
            action("Forward", AllIcons.Actions.Forward, () -> ready.getAsBoolean() && canGoForward, () -> browser.getCefBrowser().goForward()),
            action("Reload", AllIcons.Actions.Refresh, ready, () -> { clearNotice(); browser.getCefBrowser().reload(); }),
            more
        );
    }

    private AnAction action(String title, Icon icon, BooleanSupplier enabled, Runnable command) {
        return new DumbAwareAction(title, title, icon) {
            @Override public ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.EDT; }
            @Override public void update(AnActionEvent event) { event.getPresentation().setEnabled(enabled.getAsBoolean()); }
            @Override public void actionPerformed(AnActionEvent event) {
                if (!disposed && enabled.getAsBoolean()) command.run();
            }
        };
    }

    private void selectSection() {
        updatingSection = true;
        try { sections.setSelectedIndex("pulls".equals(section) ? 1 : "projects".equals(section) ? 2 : 0); }
        finally { updatingSection = false; }
    }

    private JPanel addressBar() {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        address.setEditable(false);
        address.getAccessibleContext().setAccessibleName("Current page URL");
        address.setToolTipText("Current page URL — select and copy, or use Copy URL");
        address.setMinimumSize(new Dimension(0, address.getPreferredSize().height));
        address.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { address.selectAll(); }
        });
        copyAddress.setToolTipText("Copy URL");
        copyAddress.getAccessibleContext().setAccessibleName("Copy URL");
        copyAddress.setPreferredSize(new Dimension(28, address.getPreferredSize().height));
        copyAddress.setEnabled(false);
        copyAddress.addActionListener(event -> {
            String url = address.getText();
            if (!disposed && GitHubUrls.isGitHubPage(url)) {
                CopyPasteManager.getInstance().setContents(new StringSelection(url));
                status.setText("URL copied");
            }
        });
        row.add(address, BorderLayout.CENTER);
        row.add(copyAddress, BorderLayout.EAST);
        return row;
    }

    private void showHelp() {
        Messages.showInfoMessage(project,
            "Sign in directly on GitHub; no personal access token is needed.\n\n"
            + "The session is separate from Chrome/Edge, but may be shared with other embedded browsers in Rider. "
            + "Sign out on GitHub before using another account. Uninstalling does not clear cookies.\n\n"
            + "This Windows beta supports github.com. External SSO and GitHub Enterprise are unsupported. "
            + "Downloads use your usual browser and its separate login. Passkeys and account switching still need testing.\n\n"
            + "A 404 can mean a missing page or insufficient access. Use More > Sign in, then choose Issues or PR.\n\n"
            + "Use the title bar for Back, Forward and Reload. More contains search, zoom, sign-in and repository rescan. "
            + "The address bar shows the current page URL; use Copy URL to paste it elsewhere. "
            + "Alt+Left/Right navigates history from panel controls. Find: Enter for next match; Escape to close.",
            "GitHub Web — Help and Privacy");
    }

    private JPanel welcome() {
        JPanel welcome = new JPanel(new BorderLayout(0, 12));
        welcome.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));
        JTextArea heading = textArea("GitHub, beside your code.");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        welcome.add(heading, BorderLayout.NORTH);
        JTextArea explanation = textArea("Issues, pull requests and Projects — using GitHub's own website and your project's Git remote.\n\n"
            + "Open GitHub to connect. Sign in directly on GitHub for private repositories. No personal access token is needed.\n\n"
            + "Your session is separate from your usual browser and may be shared with other embedded Rider browsers.\n\n"
            + "Windows beta · github.com only. External SSO is unsupported; downloads open in your usual browser. More > Help and privacy has the details.");
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
            browser.setZoomLevel(zoom);
            setBrowserControls(true);
            status.setText("Waiting for project repositories…");
            // The VCS barrier includes initial repository mappings. A fixed delay can
            // mistake an unfinished Git model for a project without GitHub remotes.
            initialNavigation.schedule(
                ready -> ProjectLevelVcsManager.getInstance(project).runAfterInitialization(() -> ui(ready)),
                () -> { refreshRepositories(); openSection(); });
        } catch (RuntimeException | LinkageError error) {
            if (browser != null) { Disposer.dispose(browser); browser = null; }
            if (client != null) { Disposer.dispose(client); client = null; }
            started = false;
            setBrowserControls(false);
            ((CardLayout) center.getLayout()).show(center, "welcome");
            showNotice("The embedded browser could not start. Check Rider's runtime and JCEF plugin, then retry. (" + error.getClass().getSimpleName() + ")", null);
        }
    }

    private void configureFind() {
        findBar.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
        findText.getAccessibleContext().setAccessibleName("Find text on page");
        findText.setToolTipText("Enter to find the next occurrence");
        findText.setMinimumSize(new Dimension(0, findText.getPreferredSize().height));
        findBar.add(findText, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 2, 0));
        JButton next = button("↓", "Next match");
        next.addActionListener(e -> find(true));
        JButton close = button("×", "Close page search");
        close.addActionListener(e -> closeFind());
        actions.add(next);
        actions.add(close);
        findBar.add(actions, BorderLayout.EAST);
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
        sections.setEnabled(enabled);
        if (!enabled) { canGoBack = false; canGoForward = false; }
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
        // Only update the selector: never replace an active page or a comment draft.
    }

    private void openSection() {
        String repository = repositoryForTest();
        String url = repository == null ? "https://github.com/" + (section.equals("pulls") ? "pulls" : "issues") : GitHubUrls.page(repository, section);
        navigate(url);
    }

    void navigate(String url) {
        if (disposed || browser == null || !GitHubUrls.isGitHubPage(url)) return;
        initialNavigation.cancel();
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
                    ui(() -> {
                        address.setText(url);
                        address.setCaretPosition(0);
                        copyAddress.setEnabled(true);
                        GitHubUrls.sectionForPage(url, repositoryForTest()).ifPresent(active -> {
                            section = active;
                            preferences.setValue(PREFIX + "section", active);
                            selectSection();
                        });
                    });
                }
            }
        });
        cef.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override public void onLoadingStateChange(CefBrowser b, boolean loading, boolean canBack, boolean canForward) {
                ui(() -> {
                    canGoBack = canBack;
                    canGoForward = canForward;
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
        initialNavigation.cancel();
        blockedUrl = null;
        currentUrl = "";
        if (refreshTimer != null) refreshTimer.stop();
    }
}
