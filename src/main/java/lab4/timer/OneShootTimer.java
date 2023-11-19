package lab4.timer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;

import java.util.concurrent.TimeUnit;

public class OneShootTimer {

    private final long delay;
    private final Runnable task;
    private boolean cancelled = false;
    private Disposable completable;

    public OneShootTimer(long delay, Runnable task) {
        this.delay = delay;
        this.task = task;
    }

    public void start() {
        if (cancelled) {
            throw new IllegalStateException("Timer cancelled");
        }
        completable = Completable
                .complete()
                .delay(delay, TimeUnit.MILLISECONDS)
                .subscribe(task::run);
    }

    public void cancel() {
        if (completable != null) {
            cancelled = true;
            completable.dispose();
        }
    }
}