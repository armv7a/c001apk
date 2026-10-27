package cn.shuzilm.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Converts endpoint configuration to the host[:port] grammar understood by libdu. */
public final class DigitalUnionEndpoint {
    private DigitalUnionEndpoint() {}

    public static String toNativeAuthority(String endpoint) {
        if (endpoint == null) return null;
        String value = endpoint.trim();
        if (value.isEmpty()) throw invalidEndpoint();
        try {
            URI uri = new URI(value.contains("://") ? value : "//" + value);
            String scheme = uri.getScheme();
            if (scheme != null && !scheme.equalsIgnoreCase("https")
                    && !scheme.equalsIgnoreCase("http")) throw invalidEndpoint();
            if (uri.getRawUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) throw invalidEndpoint();
            String path = uri.getRawPath();
            if (path != null && !path.isEmpty() && !path.equals("/")) throw invalidEndpoint();
            String host = uri.getHost();
            // libdu splits on the first colon and cannot represent IPv6 literals.
            if (host == null || host.length() > 253 || host.contains(":")
                    || uri.getRawAuthority().endsWith(":")) throw invalidEndpoint();
            for (String label : host.split("\\.", -1)) {
                if (!label.matches("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?")) {
                    throw invalidEndpoint();
                }
            }
            int port = uri.getPort();
            if (port == -1 && scheme != null) port = scheme.equalsIgnoreCase("https") ? 443 : 80;
            if (port != -1 && (port < 1 || port > 65535)) throw invalidEndpoint();
            return host.toLowerCase(Locale.ROOT) + (port == -1 ? "" : ":" + port);
        } catch (URISyntaxException failure) {
            throw invalidEndpoint();
        }
    }

    private static IllegalArgumentException invalidEndpoint() {
        // Do not include user-provided URLs or credentials in errors.
        return new IllegalArgumentException("Unsupported Digital Union endpoint");
    }
}
