package local.githubpanel;

import java.util.function.Consumer;

/** One initial navigation on the UI thread; explicit navigation or disposal wins. */
final class InitialNavigation {
    private boolean finished;

    void schedule(Consumer<Runnable> whenReady, Runnable navigate) {
        whenReady.accept(() -> {
            if (finished) return;
            finished = true;
            navigate.run();
        });
    }

    void cancel() { finished = true; }
}
