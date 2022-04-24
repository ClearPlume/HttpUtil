package top.fallenangel.tools.http.method;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

public enum Method {
    GET, POST, PUT, DELETE;

    public static HttpRequestBase instance(Method method) {
        return switch (method) {
            case GET -> new HttpGet();
            case POST -> new HttpPost();
            case PUT -> new HttpPut();
            case DELETE -> new HttpDelete();
        };
    }
}
