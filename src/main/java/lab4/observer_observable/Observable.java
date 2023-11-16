package lab4.observer_observable;
public interface Observable {
    void registerObserver(Observer gameObserver);

    void removeObserver(Observer gameObserver);

    void notifyObservers();
}