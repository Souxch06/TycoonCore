/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Charsets
 *  com.google.common.io.CharStreams
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 *  org.bukkit.Bukkit
 */
package com.cryptomorin.xseries.profiles.mojang;

import com.cryptomorin.xseries.profiles.ProfilesCore;
import com.cryptomorin.xseries.profiles.exceptions.MojangAPIException;
import com.cryptomorin.xseries.profiles.exceptions.MojangAPIRetryException;
import com.cryptomorin.xseries.profiles.mojang.ProfileRequestConfiguration;
import com.cryptomorin.xseries.profiles.mojang.RateLimiter;
import com.cryptomorin.xseries.reflection.XReflection;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MinecraftClient {
    private static final AtomicInteger SESSION_ID = new AtomicInteger();
    private static final Proxy PROXY = ProfilesCore.PROXY == null ? Proxy.NO_PROXY : ProfilesCore.PROXY;
    private static final Gson GSON = new Gson();
    private static final RateLimiter TOTAL_REQUESTS = new RateLimiter(Integer.MAX_VALUE, Duration.ofMinutes(10L));
    private static final String USER_AGENT = "XSeries/11.2.0 (" + System.getProperty("os.name") + "; " + System.getProperty("os.version") + "; " + System.getProperty("java.vendor") + "; " + System.getProperty("java.version") + ") " + Bukkit.getName() + '/' + Bukkit.getBukkitVersion() + ' ' + Bukkit.getVersion();
    private final String method;
    private final URI baseURL;
    private final RateLimiter rateLimiter;

    public Session session(@Nullable ProfileRequestConfiguration profileRequestConfiguration) {
        Session session = new Session();
        if (profileRequestConfiguration != null) {
            profileRequestConfiguration.configure(session);
        }
        return session;
    }

    public MinecraftClient(@Pattern(value="GET|POST|PUT|DELETE") String string, String string2, RateLimiter rateLimiter) {
        this.method = string;
        try {
            this.baseURL = new URI(string2);
        }
        catch (URISyntaxException uRISyntaxException) {
            throw new RuntimeException(uRISyntaxException);
        }
        this.rateLimiter = rateLimiter;
    }

    private static String totalReq() {
        return " (total: " + TOTAL_REQUESTS.getEffectiveRequestsCount() + ')';
    }

    static /* synthetic */ AtomicInteger access$000() {
        return SESSION_ID;
    }

    public final class Session {
        private final int sessionId = MinecraftClient.access$000().getAndIncrement();
        private Duration connectTimeout = Duration.ofSeconds(10L);
        private Duration readTimeout = Duration.ofSeconds(10L);
        private Duration retryDelay = Duration.ofSeconds(5L);
        private int retries;
        private boolean waitInQueue = true;
        private Object body;
        private String append;
        private HttpURLConnection connection;
        private BiFunction<Session, Throwable, Boolean> errorHandler;

        private void debug(String string, Object ... objectArray) {
            Object[] objectArray2 = XReflection.concatenate(new Object[]{this.sessionId, this.append}, objectArray);
            ProfilesCore.debug("[MinecraftClient-{}][{}] " + string, objectArray2);
        }

        public Session exceptionally(BiFunction<Session, Throwable, Boolean> biFunction) {
            this.errorHandler = biFunction;
            return this;
        }

        public Session waitInQueue(boolean bl) {
            this.waitInQueue = bl;
            return this;
        }

        public Session retry(int n, Duration duration) {
            this.retries = n;
            this.retryDelay = duration;
            return this;
        }

        public Session body(Object object) {
            this.validateMethod("POST");
            this.body = Objects.requireNonNull(object);
            return this;
        }

        public Session append(@Nonnull String string) {
            this.append = Objects.requireNonNull(string);
            return this;
        }

        public Session timeout(Duration duration, Duration duration2) {
            this.connectTimeout = duration;
            this.readTimeout = duration2;
            return this;
        }

        private void validateMethod(String string) {
            if (!MinecraftClient.this.method.equals(string)) {
                throw new UnsupportedOperationException("Cannot " + string + " with a client using method " + MinecraftClient.this);
            }
        }

        @Nullable
        public JsonElement request() {
            try {
                JsonElement jsonElement = this.request0();
                this.debug("Received response: {}", jsonElement);
                return jsonElement == null ? null : (jsonElement.isJsonNull() ? null : jsonElement);
            }
            catch (Exception exception) {
                if (this.retries > 0) {
                    --this.retries;
                    if (!(exception instanceof MojangAPIRetryException) || ((MojangAPIRetryException)exception).getReason() != MojangAPIRetryException.Reason.RATELIMITED) {
                        try {
                            Thread.sleep(this.retryDelay.toMillis());
                        }
                        catch (InterruptedException interruptedException) {
                            throw new RuntimeException(interruptedException);
                        }
                    }
                    return this.request();
                }
                if (this.errorHandler == null) {
                    throw exception;
                }
                Boolean bl = this.errorHandler.apply(this, exception);
                if (bl == null || bl.booleanValue()) {
                    return this.request();
                }
                throw exception;
            }
        }

        @Nullable
        private JsonElement request0() {
            Object object;
            Object object2;
            if (this.waitInQueue) {
                MinecraftClient.this.rateLimiter.acquireOrWait();
            } else if (!MinecraftClient.this.rateLimiter.acquire()) {
                throw new MojangAPIRetryException(MojangAPIRetryException.Reason.RATELIMITED, "Rate limit has been hit! " + MinecraftClient.this.rateLimiter + MinecraftClient.totalReq());
            }
            this.connection = (HttpURLConnection)(this.append == null ? MinecraftClient.this.baseURL : MinecraftClient.this.baseURL.resolve(this.append)).toURL().openConnection(PROXY);
            this.connection.setRequestMethod(MinecraftClient.this.method);
            this.connection.setConnectTimeout((int)this.connectTimeout.toMillis());
            this.connection.setReadTimeout((int)this.readTimeout.toMillis());
            this.connection.setDoInput(true);
            this.connection.setUseCaches(false);
            this.connection.setAllowUserInteraction(false);
            this.connection.setRequestProperty("User-Agent", USER_AGENT);
            if (this.body != null) {
                this.connection.setDoOutput(true);
                String string = GSON.toJson(this.body);
                this.debug("Writing body {} to {}", string, this.connection.getURL());
                object2 = string.getBytes(StandardCharsets.UTF_8);
                this.connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                this.connection.setRequestProperty("Content-Length", String.valueOf(((byte[])object2).length));
                object = this.connection.getOutputStream();
                try {
                    ((OutputStream)object).write((byte[])object2);
                }
                finally {
                    if (object != null) {
                        ((OutputStream)object).close();
                    }
                }
            } else {
                this.connection.setDoOutput(false);
            }
            this.debug("Sending request to {}", this.connection.getURL());
            try {
                return this.connectionStreamToJson(false);
            }
            catch (Throwable throwable) {
                try {
                    switch (this.connection.getResponseCode()) {
                        case 404: {
                            return null;
                        }
                        case 429: {
                            object = MinecraftClient.this.rateLimiter.toString();
                            MinecraftClient.this.rateLimiter.instantRateLimit();
                            throw new MojangAPIRetryException(MojangAPIRetryException.Reason.RATELIMITED, "Rate limit has been hit (server confirmed): " + (String)object + " -> " + (String)object + MinecraftClient.totalReq());
                        }
                    }
                    if (throwable instanceof SocketException && throwable.getMessage().toLowerCase(Locale.ENGLISH).contains("connection reset")) {
                        throw new MojangAPIRetryException(MojangAPIRetryException.Reason.CONNECTION_RESET, "Connection was closed", throwable);
                    }
                    object = this.connectionStreamToJson(true);
                    object2 = new MojangAPIException(object == null ? "[NO ERROR RESPONSE]" : ((JsonElement)object).toString(), throwable);
                }
                catch (MojangAPIRetryException mojangAPIRetryException) {
                    throw mojangAPIRetryException;
                }
                catch (Throwable throwable2) {
                    object2 = new MojangAPIException("Failed to read both normal response and error response from '" + this.connection.getURL() + '\'');
                    ((Throwable)object2).addSuppressed(throwable);
                    ((Throwable)object2).addSuppressed(throwable2);
                }
                throw object2;
            }
        }

        private JsonElement connectionStreamToJson(boolean bl) {
            try (InputStream inputStream = bl ? this.connection.getErrorStream() : this.connection.getInputStream();){
                if (bl && inputStream == null) {
                    JsonElement jsonElement = null;
                    return jsonElement;
                }
                JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                try {
                    JsonElement jsonElement;
                    Exception exception = null;
                    try {
                        jsonElement = Streams.parse(jsonReader);
                    }
                    catch (Exception exception2) {
                        exception = exception2;
                        jsonElement = null;
                    }
                    if (jsonElement == null) {
                        String string = CharStreams.toString((Readable)new InputStreamReader(bl ? this.connection.getErrorStream() : this.connection.getInputStream(), Charsets.UTF_8));
                        throw new RuntimeException((bl ? "error response" : "normal response") + " is not a JSON object '" + this.connection.getResponseCode() + " - " + this.connection.getResponseMessage() + "': " + string, exception);
                    }
                    JsonElement jsonElement2 = jsonElement;
                    jsonReader.close();
                    return jsonElement2;
                }
                catch (Throwable throwable) {
                    try {
                        jsonReader.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                    throw throwable;
                }
            }
        }
    }
}

