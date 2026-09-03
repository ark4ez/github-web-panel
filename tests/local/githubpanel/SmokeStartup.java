package local.githubpanel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.jcef.JBCefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.Timer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

/** Included only in the separately built smoke archive, never in the deliverable plugin. */
public final class SmokeStartup implements StartupActivity.DumbAware {
    private Path directory;
    @Override public void runActivity(Project project) {
        String target = System.getProperty("github.panel.smoke.directory");
        if (target == null || !project.getName().equals("github-panel-smoke-project")) return;
        directory = Path.of(target);
        record("Startup activity loaded");
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("GitHub Web");
            if (toolWindow == null) { record("FAIL: tool window missing"); return; }
            toolWindow.activate(() -> {
                try {
                    GitHubPanel panel = (GitHubPanel) toolWindow.getContentManager().getContent(0).getComponent();
                    panel.startForTest();
                    if (panel.browserForTest() == null) { record("FAIL: JCEF unsupported"); return; }
                    record("Panel created; selected=" + panel.repositoryForTest());
                    var settings = JBCefApp.getInstance().getCefSettings();
                    record("JCEF disk cache configured=" + (settings != null && settings.cache_path != null && !settings.cache_path.isBlank()));
                    AtomicBoolean captured = new AtomicBoolean();
                    // Observe without replacing the plugin's native load handlers.
                    long deadline = System.currentTimeMillis() + 60000;
                    Timer observer = new Timer(1000, event -> {
                        Timer timer = (Timer) event.getSource();
                        if (project.isDisposed() || System.currentTimeMillis() > deadline) {
                            timer.stop();
                            if (!captured.get()) record("INCOMPLETE: public page readiness timed out");
                            return;
                        }
                        CefBrowser cefBrowser = panel.browserForTest().getCefBrowser();
                        if (cefBrowser.isLoading() || !"https://github.com/JetBrains/intellij-community/issues".equals(cefBrowser.getURL())) return;
                        cefBrowser.getText(text -> {
                            if ((text.contains("All issues") || text.contains("No results")) && captured.compareAndSet(false, true)) {
                                record("PASS: rendered public GitHub Issues content");
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    record("Selected after Git model loaded=" + panel.repositoryForTest());
                                    timer.stop();
                                });
                            }
                        });
                    });
                    observer.start();
                    panel.navigate("https://github.com/JetBrains/intellij-community/issues");
                } catch (Throwable error) { record("FAIL: " + error.getClass().getName() + ": " + error.getMessage()); }
            }, false);
        });
    }
    private synchronized void record(String text) {
        try { Files.createDirectories(directory); Files.writeString(directory.resolve("smoke.log"), text + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (Exception ignored) { }
    }
}
