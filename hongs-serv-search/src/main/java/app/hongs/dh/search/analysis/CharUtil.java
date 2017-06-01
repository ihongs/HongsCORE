package app.hongs.dh.search.analysis;

import java.io.IOException;
import java.io.Reader;

/**
 * 字符工具
 * 从 lucene 5.5 的 org.apache.lucene.analyzer.util.CharacterUtils 中提取
 */
class CharUtil {

    CharUtil() {
    }

    public int codePointAt(Buffer buffer, int offset) {
        return Character.codePointAt(buffer.getBuffer(), offset, buffer.getLength());
    }

    public boolean fill(Buffer buffer, Reader reader) throws IOException {
        return fill(buffer, reader, buffer.buffer.length);
    }

    public boolean fill(final Buffer buffer, final Reader reader, int numChars) throws IOException {
        assert buffer.buffer.length >= 2;
        if (numChars < 2 || numChars > buffer.buffer.length) {
            throw new IllegalArgumentException("numChars must be >= 2 and <= the buffer size");
        }
        final char[] charBuffer = buffer.buffer;
        buffer.offset = 0;
        final int offset;

        // Install the previously saved ending high surrogate:
        if (buffer.lastTrailingHighSurrogate != 0) {
            charBuffer[0] = buffer.lastTrailingHighSurrogate;
            buffer.lastTrailingHighSurrogate = 0;
            offset = 1;
        } else {
            offset = 0;
        }

        final int read = readFully(reader, charBuffer, offset, numChars - offset);

        buffer.length = offset + read;
        final boolean result = buffer.length == numChars;
        if (buffer.length < numChars) {
            // We failed to fill the buffer. Even if the last char is a high
            // surrogate, there is nothing we can do
            return result;
        }

        if (Character.isHighSurrogate(charBuffer[buffer.length - 1])) {
            buffer.lastTrailingHighSurrogate = charBuffer[--buffer.length];
        }
        return result;
    }

    static int readFully(Reader reader, char[] dest, int offset, int len) throws IOException {
        int read = 0;
        while (read < len) {
            final int r = reader.read(dest, offset + read, len - read);
            if (r == -1) {
                break;
            }
            read += r;
        }
        return read;
    }

    /**
     * A simple IO buffer to use with
     * {@link CharacterUtils#fill(CharacterBuffer, Reader)}.
     */
    public static final class Buffer {

        private final char[] buffer;
        private int offset;
        private int length;
        // NOTE: not private so outer class can access without
        // $access methods:
        char lastTrailingHighSurrogate;

        Buffer(final int size) {
            if (size < 2) {
                throw new IllegalArgumentException("buffer size must be >= 2");
            }
            this.buffer = new char[size];
            this.offset = 0;
            this.length = 0;
        }

        /**
         * Returns the internal buffer
         *
         * @return the buffer
         */
        public char[] getBuffer() {
            return buffer;
        }

        /**
         * Returns the data offset in the internal buffer.
         *
         * @return the offset
         */
        public int getOffset() {
            return offset;
        }

        /**
         * Return the length of the data in the internal buffer starting at
         * {@link #getOffset()}
         *
         * @return the length
         */
        public int getLength() {
            return length;
        }

        /**
         * Resets the Buffer. All internals are reset to its default values.
         */
        public void reset() {
            offset = 0;
            length = 0;
            lastTrailingHighSurrogate = 0;
        }
    }

}
