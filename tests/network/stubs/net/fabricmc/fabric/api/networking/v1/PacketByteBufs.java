package net.fabricmc.fabric.api.networking.v1;

public final class PacketByteBufs {
    public static Buffer create() { return new Buffer(); }
    public static final class Buffer {
        private String value;
        public Buffer writeString(String value, int limit) {
            if (value.length() > limit) throw new IllegalArgumentException("length");
            this.value = value;
            return this;
        }
        public String readString(int limit) {
            if (value.length() > limit) throw new IllegalArgumentException("length");
            return value;
        }
    }
}
