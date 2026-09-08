package net.fabricmc.fabric.api.client.event.lifecycle.v1;
import java.util.function.Consumer;
public final class ClientTickEvents {
    public static final Event END_CLIENT_TICK = new Event();
    public static final class Event {
        private Consumer<Object> listener;
        public void register(Consumer<Object> listener) { this.listener = listener; }
        public void fire() { if (listener != null) listener.accept(null); }
    }
}
