package lab4.timer;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

import java.util.concurrent.TimeUnit;

public class InfiniteShootsTimer {
    private final long delayMs;
    private final Runnable task;

    private boolean cancelled = false;
    private Disposable disposable;

    public InfiniteShootsTimer(long delayMs, Runnable task) {
        this.delayMs = delayMs;
        this.task = task;
    }

    public void start() {
        if (cancelled) {
            throw new IllegalStateException("Timer cancelled");
        }
        disposable = Observable
                .interval(delayMs, TimeUnit.MILLISECONDS)
                .subscribe(ignored -> task.run());
    }

    public void cancel() {
        if (disposable != null) {
            cancelled = true;
            disposable.dispose();
        }
    }
}