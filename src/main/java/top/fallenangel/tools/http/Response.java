package top.fallenangel.tools.http;

import com.alibaba.fastjson.JSON;
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
    private String bodyString = "";
    private Map<String, Object> body;

    private Response() {
        status = new BasicStatusLine(HttpVersion.HTTP_1_1, 500, "Internal Server Error");
    }

    private Response(HttpResponse response) {
        status = response.getStatusLine();
        HttpEntity entity = response.getEntity();

        if (status.getStatusCode() == 200) {
            haveBody = entity != null;
            if (haveBody) {
                byte[] data;
                try {
                    data = EntityUtils.toByteArray(entity);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                if (HttpUtil.contentTypeEquals(ContentType.get(entity), ContentType.APPLICATION_JSON)) {
                    body = JSON.parseObject(data, new TypeReference<Map<String, String>>() {}.getType());
                    bodyString = JSON.toJSONString(body);
                } else {
                    bodyString = new String(data, StandardCharsets.UTF_8);
                }
            }
        }

        log.info("==========请求结果==========");
        log.info("状态：{}", status);
        log.info("响应体：{}", body);
        log.info("响应体字符串：{}", bodyString);
        log.info("==========请求结果==========");
        log.warn("===============Http请求结束===============");
    }

    public static Response build(HttpResponse response) {
        return response == null ? new Response() : new Response(response);
    }
}