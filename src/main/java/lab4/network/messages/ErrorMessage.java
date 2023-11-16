package lab4.network.messages;
import java.nio.charset.StandardCharsets;

public class ErrorMessage extends Message {
    private final byte[] buffer;
    private transient String errorMessage;

    public ErrorMessage(String errorMessage) {
        super(MessageType.ERROR);
        this.buffer = StandardCharsets.UTF_8.encode(errorMessage).array();
    }

    public String getErrorMessage() {
        if (errorMessage == null) {
            errorMessage = new String(buffer, StandardCharsets.UTF_8);
        }
        return errorMessage;
    }
}