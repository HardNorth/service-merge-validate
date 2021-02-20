package net.hardnorth.github.merge.utils;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

public class IoUtils {
    private static final int KILOBYTE = 2 ^ 10;

    private static final int READ_BUFFER = 10 * KILOBYTE;

    /**
     * Reads an <code>InputStream</code> into a <code>String</code>. Uses UTF-8 encoding and 10 kilobytes buffer by
     * default.
     *
     * @param is a stream to read from
     * @return the result
     */
    @Nullable
    public static String readInputStreamToString(@Nullable InputStream is) {
        if(is == null) {
            return null;
        }
        byte[] bytes = readInputStreamToBytes(is);
        if (bytes.length <= 0) {
            return "";
        }

        try {
            return new String(bytes, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // Most likely impossible case unless you run these tests on embedded controllers
            return null;
        }
    }

    /**
     * Reads an <code>InputStream</code> into an array of bytes. Uses 10 kilobytes buffer by default.
     *
     * @param is a stream to read from
     * @return the result
     */
    public static byte[] readInputStreamToBytes(InputStream is) {
        return readInputStreamToBytes(is, READ_BUFFER);
    }

    /**
     * Reads an <code>InputStream</code> into an array of bytes.
     *
     * @param is         a stream to read from
     * @param bufferSize size of read buffer in bytes
     * @return the result
     */
    public static byte[] readInputStreamToBytes(InputStream is, int bufferSize) {
        ReadableByteChannel channel = Channels.newChannel(is);
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int read;
            while ((read = channel.read(buffer)) > 0) {
                baos.write(buffer.array(), 0, read);
                buffer.clear();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return baos.toByteArray();
    }
}
