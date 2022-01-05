package top.fallenangel.tools.http.method;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

public class HttpDelete extends HttpEntityEnclosingRequestBase {
    public final static String METHOD_NAME = "DELETE";

    public HttpDelete() {
        super();
    }

    public HttpDelete(final URI uri) {
        super();
        setURI(uri);
    }

    /**
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpDelete(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
