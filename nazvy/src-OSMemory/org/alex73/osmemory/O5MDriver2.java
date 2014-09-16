package org.alex73.osmemory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;

public class O5MDriver2 {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static final byte MARK_BOF = (byte) 0xFF;
    public static final byte MARK_EOF = (byte) 0xFE;
    public static final byte MARK_RESET = (byte) 0xFF;
    public static final byte MARK_DATASET_NODE = (byte) 0x10;
    public static final byte MARK_DATASET_WAY = (byte) 0x11;
    public static final byte MARK_DATASET_RELATION = (byte) 0x12;
    public static final byte MARK_DATASET_BOUNDINGBOX = (byte) 0xDB;
    public static final byte MARK_DATASET_FILETIMESTAMP = (byte) 0xDC;
    public static final byte MARK_DATASET_HEADER = (byte) 0xE0;
    public static final byte MARK_DATASET_SYNC = (byte) 0xEE;
    public static final byte MARK_DATASET_JUMP = (byte) 0xEF;

    final O5MReader2 handler;
    private ByteBuffer buffer;

    private DeltaCoder deltaId = new DeltaCoder();
    private DeltaCoder deltaTimestamp = new DeltaCoder();
    private DeltaCoder deltaChangeset = new DeltaCoder();
    private DeltaCoder deltaLongitude = new DeltaCoder();
    private DeltaCoder deltaLatitude = new DeltaCoder();
    private DeltaCoder deltaReferenceNode = new DeltaCoder();

    private int[] stringPairPositions = new int[15001];
    int stringPairPos;

    private int datasetEndPos;
    int objectTagsCount;
    int[] objectTagPositions = new int[1024];
    long[] memberIds = new long[8192];
    byte[] memberTypes = new byte[8192];
    int[] memberRolePositions = new int[8192];

    private void resetDeltas() {
        deltaId.value = 0;
        deltaTimestamp.value = 0;
        deltaChangeset.value = 0;
        deltaLongitude.value = 0;
        deltaLatitude.value = 0;
        deltaReferenceNode.value = 0;
    }

    public O5MDriver2(O5MReader2 handler) {
        this.handler = handler;
    }

    public void read(File file) throws Exception {
        try (RandomAccessFile aFile = new RandomAccessFile(file, "r")) {
            FileChannel inChannel = aFile.getChannel();
            buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            if (buffer.get() != MARK_BOF) {
                throw new IOException("This is not a .o5m file");
            }
            while (buffer.remaining() > 0) {
                byte datasetType = buffer.get();
                if (datasetType == MARK_RESET) {
                    resetDeltas();
                    continue;
                }
                if (datasetType == MARK_EOF && buffer.remaining() == 0) {
                    break;
                }
                long datasetLength = readUnsignedNumberAbsolute();
                datasetEndPos = (int) (buffer.position() + datasetLength);
                switch (datasetType) {
                case MARK_DATASET_HEADER:
                    // should follow the first 0xff in the file; contents: 0x04 0x6f 0x35 0x6d 0x32 ("o5m2"),
                    // or 0x04 0x6f 0x35 0x63 0x32 ("o5c2") for .o5m change files
                    System.out.println("MARK_DATASET_HEADER");
                    break;
                case MARK_DATASET_FILETIMESTAMP:
                    System.out.println("MARK_DATASET_FILETIMESTAMP");
                    break;
                case MARK_DATASET_BOUNDINGBOX:
                    System.out.println("MARK_DATASET_BOUNDINGBOX");
                    break;
                case MARK_DATASET_NODE:
                    readNode();
                    break;
                case MARK_DATASET_WAY:
                    readWay();
                    break;
                case MARK_DATASET_RELATION:
                    readRelation();
                    break;
                default:
                    System.out.println("Unknown dataset : " + Integer.toHexString(datasetType));
                }
                objectTagsCount = 0;
                buffer.position(datasetEndPos);
            }
        }
    }

    void readNode() {
        long id = readSignedNumber(deltaId);

        if (buffer.position() >= datasetEndPos) {
            return;
        }
        readVersion();

        if (buffer.position() >= datasetEndPos) {
            return;
        }
        long longitude = readSignedNumber(deltaLongitude);
        long latitude = readSignedNumber(deltaLatitude);

        while (buffer.position() < datasetEndPos) {
            readObjectTag();
        }
        handler.createNode(this, id, latitude, longitude);
    }

    void readWay() {
        long id = readSignedNumber(deltaId);

        if (buffer.position() >= datasetEndPos) {
            return;
        }
        readVersion();

        if (buffer.position() >= datasetEndPos) {
            return;
        }

        int nodesListStart = buffer.position();
        int refSectionLength = (int) readUnsignedNumberAbsolute();
        int refSectionEnd = buffer.position() + refSectionLength;
        buffer.position(refSectionEnd);

        while (buffer.position() < datasetEndPos) {
            readObjectTag();
        }
        handler.createWay(this, id, nodesListStart);
    }

    void readRelation() {
        long id = readSignedNumber(deltaId);

        if (buffer.position() >= datasetEndPos) {
            return;
        }
        readVersion();

        if (buffer.position() >= datasetEndPos) {
            return;
        }

        deltaReferenceNode.value = 0;
        int refSectionLength = (int) readUnsignedNumberAbsolute();
        int refSectionEnd = buffer.position() + refSectionLength;

        int c = 0;
        while (buffer.position() < refSectionEnd) {
            memberIds[c] = readSignedNumber(deltaReferenceNode);
            memberTypes[c] = readMemberInfo(c);
            c++;
            if (c >= memberTypes.length) {
                throw new RuntimeException("Too many members in relation");
            }
        }

        while (buffer.position() < datasetEndPos) {
            readObjectTag();
        }
        handler.createRelation(this, id, Arrays.copyOf(memberIds, c), Arrays.copyOf(memberTypes, c),
                Arrays.copyOf(memberRolePositions, c));
    }

    void readVersion() {
        long version = readUnsignedNumberAbsolute();
        if (version == 0) {
            return;
        }
        long timestamp = readSignedNumber(deltaTimestamp);
        if (timestamp == 0) {
            return;
        }
        long changeset = readSignedNumber(deltaChangeset);
        readUidUserPair();
    }

    /**
     * To store numbers of different lengths we abandon bit 7 (most significant bit) of every byte and use
     * this bit as a length indicator. This indicator – when set to 1 – tells us that the next byte belongs to
     * the same number. The first byte of such a long number contains the least significant 7 bits, the last
     * byte the most significant 7 bits.
     */
    long readUnsignedNumberAbsoluteo() {
        long value = 0;

        for (int i = 0, b = 0x80; (b & 0x80) != 0; i++) {
            b = buffer.get();
            long part = ((long) (b & 0x7F)) << (i * 7);
            value |= part;
        }

        return value;
    }
    long readUnsignedNumberAbsolute() {
        long value = 0;
short b=0x80;
        for (int i = 0; (b & 0x80) != 0; i+=7) {
            b = buffer.get();
            long part = ((long)(b & 0x7F)) << i ;
            value |= part;
        }

        return value;
    }

    /**
     * If a number is stored as "signed", we will need 1 bit for the sign. For this purpose, the least
     * significant bit of the least significant byte is taken as sign bit. 0 means positive, 1 means negative.
     * We do not need the number -0, of course, so we can shift the range of negative numbers by one.
     */
    long readSignedNumber(DeltaCoder delta) {
        long value = readUnsignedNumberAbsolute();

        if ((value & 1) != 0) {
            // negative
            value = -(value >> 1) - 1;
        } else {
            // positive
            value = (value >> 1);
        }

        long result = delta.value + value;
        delta.value = result;

        return result;
    }

    void readUidUserPair() {
        long v = readUnsignedNumberAbsolute();
        if (v != 0) {
            // refer to string pair
        } else {
            int sz = storeStringPair(stringPairPos);
            if (sz <= 250) {
                // store for future
                stringPairPos++;
                if (stringPairPos >= stringPairPositions.length) {
                    stringPairPos -= stringPairPositions.length;
                }
            }
        }
    }

    byte readMemberInfo(int i) {
        long v = readUnsignedNumberAbsolute();
        int pairPos;
        if (v != 0) {
            // refer to string pair
            pairPos = (int) (stringPairPos - v);
            if (pairPos < 0) {
                pairPos += stringPairPositions.length;
            }
        } else {
            pairPos = stringPairPos;
            int sz = storeStringOnes(stringPairPos);
            if (sz <= 250) {
                // store for future
                stringPairPos++;
                if (stringPairPos >= stringPairPositions.length) {
                    stringPairPos -= stringPairPositions.length;
                }
            }
        }
        memberRolePositions[i] = stringPairPositions[pairPos] + 1;
        switch (buffer.get(stringPairPositions[pairPos])) {
        case '0':
            return NodeObject2.TYPE_NODE;
        case '1':
            return NodeObject2.TYPE_WAY;
        case '2':
            return NodeObject2.TYPE_RELATION;
        default:
            throw new RuntimeException("Unknown member type");
        }
    }

    void readObjectTag() {
        long v = readUnsignedNumberAbsolute();
        int pairPos;
        if (v != 0) {
            // refer to string pair
            pairPos = (int) (stringPairPos - v);
            if (pairPos < 0) {
                pairPos += stringPairPositions.length;
            }
        } else {
            pairPos = stringPairPos;
            int sz = storeStringPair(stringPairPos);
            if (sz <= 250) {
                // store for future
                stringPairPos++;
                if (stringPairPos >= stringPairPositions.length) {
                    stringPairPos -= stringPairPositions.length;
                }
            }
        }
        objectTagPositions[objectTagsCount] = stringPairPositions[pairPos];
        objectTagsCount++;
        if (objectTagsCount >= objectTagPositions.length) {
            throw new RuntimeException("Too many tags in object");
        }
    }

    int storeStringPair(int pos) {
        stringPairPositions[pos] = buffer.position();
        int size = 0;
        while (buffer.get() != 0) {
            size++;
        }
        while (buffer.get() != 0) {
            size++;
        }
        return size;
    }

    int storeStringOnes(int pos) {
        stringPairPositions[pos] = buffer.position();
        int size = 0;
        while (buffer.get() != 0) {
            size++;
        }
        return size;
    }

    void skip(long count) {
        for (int i = 0; i < count; i++) {
            buffer.get();
        }
    }

    public int skipString(int pos) {
        int p = buffer.position();

        buffer.position(pos);

        while (buffer.get() != 0)
            ;
        int result = buffer.position();

        buffer.position(p);

        return result;
    }

    public String getString(int pos) {
        int p = buffer.position();

        buffer.position(pos);
        int size = 0;
        while (buffer.get() != 0) {
            size++;
        }
        buffer.position(pos);
        byte[] result = new byte[size];
        buffer.get(result);

        buffer.position(p);

        return new String(result, UTF8);
    }

    static class DeltaCoder {
        long value;
    }
}
