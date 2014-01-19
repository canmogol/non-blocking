package command;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * acm
 */
public class Response {
    private final SocketChannel socketChannel;
    private int status = 200;
    private String message = "";
    private byte[] content = new byte[0];

    public Response(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public byte[] toBytes() {
        return new byte[0];
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void write() {
        try {
            StringBuilder stringBuffer = new StringBuilder();
            stringBuffer.append("status: ").append(status).append("\nmessage: ").append(message).append("\n\n");
            byte[] headerBytes = stringBuffer.toString().getBytes();
            int size = headerBytes.length + content.length;
            byte[] bytesToWrite = new byte[size + 4];
            System.arraycopy(headerBytes, 0, bytesToWrite, 0, headerBytes.length);
            System.arraycopy(content, 0, bytesToWrite, headerBytes.length, content.length);
            bytesToWrite[size] = '\n';
            bytesToWrite[size + 1] = '\r';
            bytesToWrite[size + 2] = '\n';
            bytesToWrite[size + 3] = '\r';
            ByteBuffer writeByteBuffer = ByteBuffer.wrap(bytesToWrite);
            socketChannel.write(writeByteBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    byte[] getContent() {
        return content;
    }

    void setContent(byte[] content) {
        this.content = content;
    }
}
