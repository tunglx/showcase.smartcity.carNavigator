package hmi.parkinglot.navigation;

/**
 * Speech message to be synthesized
 */
public class SpeechMessage {
    public String message;
    public long silence;
    public String transactionId;

    public SpeechMessage(String message, long silence, String transactionId) {
        this.message = message;
        this.silence = silence;
        this.transactionId = transactionId;
    }
}
