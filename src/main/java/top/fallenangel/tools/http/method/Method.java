package top.fallenangel.tools.http.method;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

public enum Method {
    GET, POST, PUT, DELETE;

    public static HttpRequestBase instance(Method method) {
        HttpRequestBase request = null;
        switch (method) {
            case GET:
                request = new HttpGet();
                break;
            case POST:
                request = new HttpPost();
                break;
            case PUT:
                request = new HttpPut();
                break;
            case DELETE:
                request = new HttpDelete();
                break;
        }
        return request;
    }
}
