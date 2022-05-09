package top.fallenangel.tools.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONValidator;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Data
@Slf4j
public class Response {
    private StatusLine status;
    private boolean haveBody;
    private String bodyString;
    private Map<String, Object> body;

    private Response(String errorMsg) {
        status = new BasicStatusLine(HttpVersion.HTTP_1_1, 500, "Internal Server Error");
        bodyString = errorMsg;
    }

    private Response(HttpResponse response) {
        status = response.getStatusLine();
        HttpEntity entity = response.getEntity();

        haveBody = entity != null;
        if (haveBody) {
            byte[] data;
            try {
                data = EntityUtils.toByteArray(entity);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            bodyString = new String(data, StandardCharsets.UTF_8);
            if (HttpUtil.contentTypeEquals(ContentType.get(entity), ContentType.APPLICATION_JSON)) {
                JSONValidator validator = JSONValidator.from(bodyString);
                if (validator.validate()) {
                    body = JSON.parseObject(data, new TypeReference<Map<String, String>>() {}.getType());
                }
                try {validator.close();} catch (IOException ignore) {}
            }
        }

        log.info("==========请求结果==========");
        log.info("状态：{}", status);
        log.info("响应体类型：{}", ContentType.get(entity));
        log.info("响应体：{}", body);
        log.info("响应体字符串：{}", bodyString);
        log.info("==========请求结果==========");
        log.warn("===============Http请求结束===============");
    }

    public static Response build(HttpResponse response) {
        return new Response(response);
    }

    public static Response build(String errorMsg) {
        return new Response(errorMsg);
    }
}
