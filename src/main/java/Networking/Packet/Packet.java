package Networking.Packet;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

public class Packet {

    private final List<Byte> buffer;
    private byte[] readableBuffer;
    private int readPos;

    public Packet() {
        buffer = new ArrayList<>();
        readPos = 0;
    }

    public Packet(int id) {
        buffer = new ArrayList<>();
        readPos = 0;

        write(id);
    }

    public Packet(ServerPacket id) {
        buffer = new ArrayList<>();
        readPos = 0;

        write(id.ordinal());
    }

    public Packet(ClientPacket id) {
        buffer = new ArrayList<>();
        readPos = 0;

        write(id.ordinal());
    }

    public Packet(byte[] data) {
        buffer = new ArrayList<>();
        readPos = 0;

        setBytes(data);
    }

    public void setBytes(byte[] bytes) {
        write(bytes);
        readableBuffer = toByteArray(buffer.toArray());
    }

    public void reset() {
        buffer.clear();
        readableBuffer = null;
        readPos = 0;
    }

    public void reset(boolean shouldReset) {
        if (shouldReset) {
            reset();
        } else {
            readPos -= 4;
        }
    }

    public void write(byte[] data) {
        addRange(data);
    }

    public void write(int data) {
        addRange(ByteBuffer.allocate(4).putInt(data).array());
    }

    public void write(float data) {
        addRange(ByteBuffer.allocate(4).putFloat(data).array());
    }

    public void write(boolean data) {
        buffer.add((byte) (data ? 1 : 0));
    }

    public void write(String data) {
        write(data.length());
        write(data.getBytes());
    }

    public void write(Object data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(data);
        oos.flush();

        byte[] bytes = baos.toByteArray();
        write(bytes.length);
        write(bytes);
    }

    public byte[] readBytes(int length) {
        if (buffer.size() > readPos) {
            byte[] value = toByteArray(buffer.subList(readPos, readPos + length).toArray());
            readPos += length;

            return value;
        } else {
            System.err.println("Couldn't read type byte[] from packet");
            return null;
        }
    }

    public int readInt() {
        if (buffer.size() > readPos) {
            byte[] data = toByteArray(buffer.subList(readPos, readPos + 4).toArray());
            int value = new BigInteger(data).intValue();
            readPos += 4;

            return value;
        } else {
            System.err.println("Couldn't read type int from packet");
            return -1;
        }
    }

    public float readFloat() {
        if (buffer.size() > readPos) {
            byte[] data = toByteArray(buffer.subList(readPos, readPos + 4).toArray());
            float value = ByteBuffer.allocate(4).put(data).getFloat();
            readPos += 4;

            return value;
        } else {
            System.err.println("Couldn't read type int from packet");
            return -1;
        }
    }

    public boolean readBoolean() {
        if (buffer.size() > readPos) {
            boolean value = buffer.get(readPos) == 1;
            readPos++;

            return value;
        } else {
            System.err.println("Couldn't read type boolean from packet");
            return false;
        }
    }

    public String readString() {
        if (buffer.size() > readPos) {
            int length = readInt();
            byte[] data = toByteArray(buffer.subList(readPos, readPos + length).toArray());

            String value = new String(data, StandardCharsets.UTF_8);

            if (value.length() > 0) {
                readPos += length;
            }

            return value;
        } else {
            System.err.println("Couldn't read type String from packet");
            return null;
        }
    }

    public Object readObject() throws Exception, ClassNotFoundException {
        if (buffer.size() > readPos) {
            int length = readInt();
            byte[] data = toByteArray(buffer.subList(readPos, readPos + length).toArray());
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);

            return ois.readObject();
        } else {
            System.err.println("Couldn't read type Object from packet");
            return null;
        }
    }

    public void writeLength() {
        byte[] data = ByteBuffer.allocate(4).putInt(buffer.size()).array();
        insertRange(data, 0);
    }

    public boolean isType(ServerPacket packetType, int packetID) {
        return packetType.ordinal() == packetID;
    }

    public boolean isType(ClientPacket packetType, int packetID) {
        return packetType.ordinal() == packetID;
    }

    public int unreadLength() {
        return length() - readPos;
    }

    public int length() {
        return buffer.size();
    }

    public byte[] toArray() {
        readableBuffer = toByteArray(buffer.toArray());
        return readableBuffer;
    }

    private void addRange(byte[] data) {
        for (byte b : data) {
            buffer.add(b);
        }
    }

    private void insertRange(byte[] data, int index) {
        for (int i = data.length - 1; i >= 0; i--) {
            buffer.add(index, data[i]);
        }
    }

    private byte[] toByteArray(Object[] array) {
        byte[] byteArray = new byte[array.length];
        for (int i = 0; i < byteArray.length; i++) {
            byteArray[i] = (byte)array[i];
        }

        return byteArray;
    }

}