package lab4.javafx;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Scheduler;

public class FxSchedulers extends Scheduler {
    private FxSchedulers() {
    }

    public static Scheduler get() {
        return new FxSchedulers();
    }

    @Override
    public @NonNull Worker createWorker() {
        return new FxWorker();
    }
}