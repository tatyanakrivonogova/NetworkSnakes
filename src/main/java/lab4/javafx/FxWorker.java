package lab4.javafx;


import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import javafx.application.Platform;

import java.util.concurrent.TimeUnit;

public class FxWorker extends Scheduler.Worker {

    @Override
    public @NonNull Disposable schedule(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
        Platform.runLater(run);
        return new FxDisposable();
    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean isDisposed() {
        return true;
    }
}