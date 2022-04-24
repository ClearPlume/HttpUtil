package top.fallenangel.tools.http;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import top.fallenangel.tools.http.method.Method;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@SuppressWarnings("unused")
public class HttpUtil {
    public static final ContentType CONTENT_TYPE_STR = ContentType.create("text/plain", StandardCharsets.UTF_8);

    public static class HttpUtilBuilder {
        private final List<Pair<String, Object>> params = new ArrayList<>();
        private final Map<String, String> headers = new HashMap<>();
        private Method method = Method.GET;
        private ContentType contentType = ContentType.APPLICATION_JSON;
        private String url;
        private Object singleParam;

        public HttpUtilBuilder url(String url) {
            this.url = url;
            log.info("url: {}", url);
            return this;
        }

        public HttpUtilBuilder method(Method method) {
            this.method = method;
            log.info("method: {}", method);
            return this;
        }

        public HttpUtilBuilder addHeader(String name, String value) {
            headers.put(name, value);
            log.info("新增请求头: \"{}\" = {}", name, value);
            return this;
        }

        public HttpUtilBuilder addHeader(Map<String, String> header) {
            this.headers.putAll(header);
            log.info("新增请求头: {}", header);
            return this;
        }

        public HttpUtilBuilder contentType(ContentType contentType) {
            this.contentType = contentType;
            log.info("contentType: {}", contentType);
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public HttpUtilBuilder addParam(String name, Object value) {
            if (value == null || value.toString().equals("null")) {
                log.info("参数<{}>的值<{}>为null或者\"null\"，已将其忽略！", name, value);
            } else {
                this.params.add(Pair.of(name, value));
                log.info("新增参数: \"{}\" = {}", name, value);
            }
            return this;
        }

        public HttpUtilBuilder addParam(Map<String, Object> param) {
            param.forEach(this::addParam);
            return this;
        }

        public HttpUtilBuilder singleParam(Object param) {
            this.singleParam = param;
            return this;
        }

        public Response execute() {
            CloseableHttpResponse response;
            try {
                log.info("发起请求...");
                response = HttpClientBuilder.create().build().execute(buildRequest());
            } catch (IOException e) {
                e.printStackTrace();
                String errorMsg = e.toString();
                log.info("请求失败，错误信息如下：");
                log.info(errorMsg);
                return Response.build(errorMsg);
            }
            return Response.build(response);
        }

        /**
         * 构建Http请求
         */
        private HttpRequestBase buildRequest() {
            // 获取Http请求实例
            HttpRequestBase instance = Method.instance(method);

            // 设置URL
            instance.setURI(URI.create(url));

            // 添加Headers
            headers.forEach(instance::addHeader);
            // 不默认使用长连接
            if (!headers.containsKey("Connection")) {
                log.info("默认未指定Connection请求头，添加<Connection=close>以避免长连接！");
                instance.addHeader("Connection", "close");
            }

            if (instance instanceof HttpGet) { // 如果是Get请求，参数直接拼接在URL之后；否则，需要根据请求ContentType决定请求体设置方式
                StringBuilder urlBuilder = new StringBuilder(url).append('?');

                for (Pair<String, Object> p : params) {
                    String name = p.getKey();
                    Object value = handleValue(p.getValue());
                    // 参数有可能是集合
                    if (value instanceof ArrayList<?> values) {
                        values.forEach(o -> urlBuilder.append(name).append('=').append(o).append('&'));
                    } else {
                        urlBuilder.append(name).append('=').append(value).append('&');
                    }
                }
                // 删除最后一个多余的字符：'&'
                urlBuilder.deleteCharAt(urlBuilder.length() - 1);
                // 重新设置URL
                instance.setURI(URI.create(urlBuilder.toString()));
            } else {
                // 根据请求ContentType决定请求体设置方式
                // 注：也可能是JSON格式的单个对象，也就是直接设置一个对象，转成JSON
                // 注：也可能直接就是一个JSON字符串
                HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) instance;
                if (singleParam != null) {
                    String paramStr;
                    if (singleParam instanceof String) {
                        paramStr = ((String) singleParam);
                    } else {
                        paramStr = JSON.toJSONString(singleParam);
                    }
                    request.setEntity(new StringEntity(paramStr, StandardCharsets.UTF_8));
                    log.info("param: {}", paramStr);
                } else {
                    if (contentTypeEquals(contentType, ContentType.APPLICATION_JSON)) { // 如果是JSON，把参数装进Map转为json字符串，以StringEntity的形式发送
                        String param = JSON.toJSONString(collectParam());
                        request.setEntity(new StringEntity(param, StandardCharsets.UTF_8));
                        log.info("param: {}", param);
                    } else if (contentTypeEquals(contentType, ContentType.APPLICATION_FORM_URLENCODED)) { // 如果是普通表单
                        List<NameValuePair> params = new ArrayList<>();

                        for (Pair<String, Object> p : this.params) {
                            String name = p.getKey();
                            Object value = handleValue(p.getValue());

                            if (value instanceof ArrayList<?> values) {
                                values.forEach(o -> params.add(new BasicNameValuePair(name, o.toString())));
                            } else {
                                params.add(new BasicNameValuePair(name, value.toString()));
                            }
                        }
                        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
                        log.info("param: {}", collectParam());
                    } else { // 否则按照文件处理
                        MultipartEntityBuilder body = MultipartEntityBuilder.create()
                                                                            .setMode(HttpMultipartMode.RFC6532)
                                                                            .setContentType(contentType);

                        for (Pair<String, Object> p : params) {
                            String name = p.getKey();
                            Object value = handleValue(p.getValue());

                            if (value instanceof ArrayList<?> values) {
                                for (Object o : values) {
                                    if (o instanceof ContentBody) {
                                        body.addPart(name, (ContentBody) o);
                                    } else {
                                        body.addTextBody(name, o.toString(), CONTENT_TYPE_STR);
                                    }
                                }
                            } else {
                                if (value instanceof ContentBody) {
                                    body.addPart(name, (ContentBody) value);
                                } else {
                                    body.addTextBody(name, value.toString(), CONTENT_TYPE_STR);
                                }
                            }
                        }
                        request.setEntity(body.build());
                        log.info("param: {}", collectParam());
                    }
                }
            }

            log.info(instance.getURI().toString());
            return instance;
        }

        /**
         * 把List<Pair>转为Map，有名字相同的参数则转为List
         */
        private Map<String, Object> collectParam() {
            Map<String, Object> allParam = new HashMap<>();
            this.params.forEach(param -> {
                String name = param.getKey();
                Object value = handleValue(param.getValue());

                if (allParam.containsKey(name)) { // 如果已经添加过参数，则将value转为List再添加
                    // 已经添加完成的value
                    Object builtInValue = allParam.get(name);

                    if (builtInValue instanceof ArrayList) {
                        //noinspection unchecked
                        ArrayList<Object> inValue = (ArrayList<Object>) builtInValue;

                        if (value instanceof ArrayList) {
                            inValue.addAll((ArrayList<?>) value);
                        } else {
                            inValue.add(value);
                        }
                    } else {
                        allParam.put(name, new ArrayList<>() {{
                            add(builtInValue);

                            if (value instanceof ArrayList) {
                                addAll((ArrayList<?>) value);
                            } else {
                                add(value);
                            }
                        }});
                    }
                } else { // 否则直接添加
                    allParam.put(name, value);
                }
            });
            return allParam;
        }

        /**
         * 处理参数值，如果它是集合、数组，统一转成ArrayList，否则原样返回
         *
         * @return 处理后的参数，可能是ArrayList，可能是原样
         */
        private Object handleValue(Object value) {
            if (value != null) {
                if (value.getClass().isArray()) {
                    List<Object> values = new ArrayList<>();
                    for (int i = 0; i < Array.getLength(value); i++) {
                        values.add(Array.get(value, i));
                    }
                    return values;
                } else if (value instanceof Collection<?> values) {
                    return new ArrayList<>(values);
                }
            }

            return value;
        }
    }

    public static HttpUtilBuilder configurer() {
        log.warn("===============开始构建Http请求===============");
        return new HttpUtilBuilder();
    }

    public static boolean contentTypeEquals(ContentType ct1, ContentType ct2) {
        if (ct1 == ct2) {
            return true;
        }

        return Objects.equals(ct1.getMimeType(), ct2.getMimeType());
    }
}
