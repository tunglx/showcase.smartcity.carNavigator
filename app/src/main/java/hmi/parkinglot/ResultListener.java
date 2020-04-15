package hmi.parkinglot;

/**
 * Result listener for the different async tasks performed by the application
 */
public interface ResultListener<T> {
    public void onResultReady(T result);
}
