package Lab1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import static java.nio.channels.FileChannel.MapMode.*;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ByteBuffer utility class.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class ByteBufferUtils {

    public static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    private static final int DIRECT_THRESHOLD = 10240; // 10 KB

    private ByteBufferUtils() {
    }

    private static int truncateLength(FileChannel fc, int length) throws IOException {
        return (int) Math.min(length > 0 ? length : fc.size(), Integer.MAX_VALUE);
    }

    public static ByteBuffer allocate(int size) {
        // allocateDirect is pretty slow when used frequently, use it for larger
        // buffers only
        if (size > DIRECT_THRESHOLD) {
            return ByteBuffer.allocateDirect(size);
        } else {
            try {
                return ByteBuffer.allocate(size);
            } catch (OutOfMemoryError ex) {
                // not enough space in the heap, try direct allocation instead
                return ByteBuffer.allocateDirect(size);
            }
        }
    }

    public static ShortBuffer allocateDirectShort(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 2);
        bb.order(ByteOrder.nativeOrder());
        return bb.asShortBuffer();
    }

    public static CharBuffer allocateDirectChar(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 2);
        bb.order(ByteOrder.nativeOrder());
        return bb.asCharBuffer();
    }

    public static IntBuffer allocateDirectInt(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 4);
        bb.order(ByteOrder.nativeOrder());
        return bb.asIntBuffer();
    }

    public static LongBuffer allocateDirectLong(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 8);
        bb.order(ByteOrder.nativeOrder());
        return bb.asLongBuffer();
    }

    public static FloatBuffer allocateDirectFloat(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 4);
        bb.order(ByteOrder.nativeOrder());
        return bb.asFloatBuffer();
    }

    public static DoubleBuffer allocateDirectDouble(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 8);
        bb.order(ByteOrder.nativeOrder());
        return bb.asDoubleBuffer();
    }

    /**
     * Reads a file and puts its content into a byte buffer, using the given length
     * and offset.
     *
     * @param path
     * @param offset
     * @param length
     * @return
     * @throws IOException
     */
    public static ByteBuffer load(Path path, int offset, int length) throws IOException {
        try (FileChannel fc = FileChannel.open(path, READ)) {
            int size = truncateLength(fc, length);
            ByteBuffer bb = allocate(size);

            fc.position(offset);
            fc.read(bb);
            bb.flip();

            return bb;
        }
    }

    /**
     * Reads a file and puts its whole content into a byte buffer. If the file is
     * larger than {@link java.lang.Integer#MAX_VALUE}, then all bytes beyond
     * that limit are omitted.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static ByteBuffer load(Path path) throws IOException {
        return load(path, 0, -1);
    }

    /**
     * Reads a list of files and puts their contents into a byte buffer. The
     * combined size of all files must not exceed {@link java.lang.Integer#MAX_VALUE}.
     *
     * @param paths
     * @return
     * @throws IOException
     */
    public static ByteBuffer load(List<Path> paths) throws IOException {
        List<ByteBuffer> bbs = new ArrayList<>();

        for (Path path : paths) {
            bbs.add(openReadOnly(path));
        }

        return concat(bbs);
    }

    /**
     * Writes the remaining bytes of a byte buffer to the given file.
     * If the file already exists, it will be overwritten and its size will be
     * truncated to the amount of remaining bytes in the given buffer.
     *
     * @param path
     * @param bb
     * @throws IOException
     */
    public static void save(Path path, ByteBuffer bb) throws IOException {
        try (FileChannel fc = FileChannel.open(path, WRITE, CREATE)) {
            fc.truncate(bb.remaining());
            fc.write(bb);
        }
    }

    /**
     * Maps a portion of a file to memory and returns the mapped read-only byte
     * buffer using the given offset and length.
     *
     * @param path
     * @param offset
     * @param length
     * @return
     * @throws IOException
     */
    public static MappedByteBuffer openReadOnly(Path path, int offset, int length) throws IOException {
        try (FileChannel fc = FileChannel.open(path, READ)) {
            return fc.map(READ_ONLY, offset, truncateLength(fc, length));
        }
    }

    /**
     * Maps the whole file to memory and returns the mapped read-only byte buffer.
     * If the file is larger than {@link java.lang.Integer#MAX_VALUE}, then all
     * bytes beyond that limit are omitted.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static MappedByteBuffer openReadOnly(Path path) throws IOException {
        return openReadOnly(path, 0, -1);
    }

    /**
     * Maps a portion of a file to memory and returns the mapped writable byte
     * buffer using the given offset and length. If the file doesn't exist, it
     * will be created.
     *
     * @param path
     * @param offset
     * @param length
     * @return
     * @throws IOException
     */
    public static MappedByteBuffer openReadWrite(Path path, int offset, int length) throws IOException {
        try (FileChannel fc = FileChannel.open(path, READ, WRITE, CREATE)) {
            return fc.map(READ_WRITE, offset, truncateLength(fc, length));
        }
    }

    /**
     * Maps the whole file to memory and returns the mapped writable byte buffer.
     * If the file is larger than {@link java.lang.Integer#MAX_VALUE}, then all
     * bytes beyond that limit are omitted. If the file doesn't exist, it will be
     * created.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static MappedByteBuffer openReadWrite(Path path) throws IOException {
        return openReadWrite(path, 0, -1);
    }

    /**
     * Returns the subset of a byte buffer, using the given offset and length.
     * The position and limit of the original buffer won't change after this
     * operation.
     *
     * @param bb
     * @param offset
     * @param length
     * @return
     */
    public static ByteBuffer getSlice(ByteBuffer bb, int offset, int length) {
        if (length == 0) {
            // very funny
            return EMPTY;
        }

        ByteOrder order = bb.order();

        // create duplicate so the position/limit of the original won't change
        bb = bb.duplicate();

        // go to offset
        bb.position(offset);

        // set new limit if length is provided, use current limit otherwise
        if (length > 0) {
            bb.limit(offset + length);
        }

        // do the actual slicing
        ByteBuffer bbSlice = bb.slice();

        // set same byte order
        bbSlice.order(order);

        return bbSlice;
    }

    /**
     * Returns the subset of a byte buffer, starting from the given offset up to
     * the current limit.
     * The position and limit of the original buffer won't change after this
     * operation.
     *
     * @param bb
     * @param offset
     * @return
     */
    public static ByteBuffer getSlice(ByteBuffer bb, int offset) {
        return getSlice(bb, offset, -1);
    }

    /**
     * Concatenates one or more byte buffers to one large buffer. The combined
     * size of all buffers must not exceed {@link java.lang.Integer#MAX_VALUE}.
     *
     * @param bbs list of byte buffers to combine
     * @return byte buffer containing the combined content of the supplied byte
     *         buffers
     */
    public static ByteBuffer concat(List<ByteBuffer> bbs) {
        long length = 0;

        // get amount of remaining bytes from all buffers
        for (ByteBuffer bb : bbs) {
            bb.rewind();
            length += bb.remaining();
        }

        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Buffers are too large for concatenation");
        }

        if (length == 0) {
            // very funny
            System.out.println(" lenght is zero");
            return EMPTY;
        }

        ByteBuffer bbNew = ByteBuffer.allocateDirect((int) length);

        // put all buffers from list
        for (ByteBuffer bb : bbs) {
            bb.rewind();
            bbNew.put(bb);
        }

        bbNew.rewind();

        return bbNew;
    }

    /**
     * Concatenates one or more byte buffers to one large buffer. The combined
     * size of all buffers must not exceed {@link java.lang.Integer#MAX_VALUE}.
     *
     * @param bb one or more byte buffers to combine
     * @return byte buffer containing the combined content of the supplied byte
     *         buffers
     */
    public static ByteBuffer concat(ByteBuffer... bb) {
        return concat(Arrays.asList(bb));
    }

    /**
     * Performs a deep copy on a byte buffer. The resulting byte buffer will have
     * the same position, byte order and visible bytes as the original byte buffer.
     * If the source buffer is direct, the copied buffer will be direct, too.
     *
     * Any changes in one buffer won't be visible to the other, i.e. the two
     * buffers will be entirely independent from another.
     *
     * The position and limit of the original buffer won't change after this
     * operation.
     *
     * @param bb source byte buffer
     * @param forceDirect force copy to be a direct ByteBuffer
     * @return deep copy of source buffer
     */
    public static ByteBuffer copy(ByteBuffer bb, boolean forceDirect) {
        int capacity = bb.limit();
        int pos = bb.position();
        ByteOrder order = bb.order();
        ByteBuffer copy;

        if (bb.isDirect() || forceDirect) {
            copy = ByteBuffer.allocateDirect(capacity);
        } else {
            copy = ByteBuffer.allocate(capacity);
        }

        bb.rewind();

        copy.order(order);
        copy.put(bb);
        copy.position(pos);

        bb.position(pos);

        return copy;
    }

    public static ByteBuffer copy(ByteBuffer bb) {
        return copy(bb, false);
    }

    public static int transfer(ByteBuffer src, ByteBuffer dst) {
        int numBytes = Math.min(src.remaining(), dst.remaining());
        if (numBytes > 0) {
            int oldLimit = src.limit();
            src.limit(src.position() + numBytes);
            dst.put(src);
            src.limit(oldLimit);
        }
        return numBytes;
    }

    public static boolean isEmpty(ByteBuffer bb) {
        return bb == null || bb.capacity() == 0;
    }
}
