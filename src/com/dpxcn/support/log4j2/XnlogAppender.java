package com.dpxcn.support.log4j2;

import com.alibaba.fastjson.JSON;
import okhttp3.*;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义日志appender log4j2
 * Created by liang
 * on 2017/12/25.
 */
@Plugin(name = "Custom", category = "Core", elementType = "appender", printObject = true)
public class XnlogAppender extends AbstractAppender {

    private String serverName;
    private String endpoint;
    private String ip;


    public XnlogAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                         boolean ignoreExceptions, String serverName, String endpoint, String ip) {
        super(name, filter, layout, ignoreExceptions);
        this.serverName = serverName;
        this.endpoint = endpoint;
        this.ip = ip;
    }

    @Override
    public void append(LogEvent event) {

        if (null == serverName) {
            serverName = getLocalHostName();
        }
        MediaType jsonType = MediaType.parse("application/json; charset=utf-8");
        String ticket = RequestUtils.getHeader("xn-ticket-id");
        String userId = RequestUtils.getHeader("xn-user-id");
        String userName = RequestUtils.getHeader("xn-user-name");

        String level = event.getLevel().name();
        String content = event.getMessage().getFormat();


        String jsonstring = toJson(serverName, ip, ticket, userName, userId, level, content);
        RequestBody body = RequestBody.create(jsonType, jsonstring);

        Request request = new Request.Builder()
                .url(endpoint)
                .post(body)
                .build();

        enqueue(request);
    }


    @PluginFactory
    public static XnlogAppender createAppender(@PluginAttribute("name") String name,
                                               @PluginAttribute("serverName") String serverName,
                                               @PluginAttribute("endpoint") String endpoint,
                                               @PluginAttribute("ip") String ip,
                                               @PluginElement("Filter") final Filter filter,
                                               @PluginElement("Layout") Layout<? extends Serializable> layout,
                                               @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
        if (name == null) {
            LOGGER.error("no name defined in conf.");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        return new XnlogAppender(name, filter, layout, ignoreExceptions, serverName, endpoint, ip);
    }


    private static String getLocalHostName() {
        String hostName;
        try {

            InetAddress addr = InetAddress.getLocalHost();

            hostName = addr.getHostName();
        } catch (Exception ex) {
            hostName = "";
        }

        return hostName;
    }

    private static final OkHttpClient mOkHttpClient = new OkHttpClient();

    public static void enqueue(Request request) {
        mOkHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
    }


    private String toJson(String serverName, String ip, String ticket,
                         String userName, String userId,
                         String level, String content) {

        String tag = "";
        String pattern = "(?<=\\#)[\\s\\S]*?(?=\\#)";

        Pattern r = Pattern.compile(pattern);

        Matcher m = r.matcher(content);
        if (m.find()) {
            tag = m.group(0);
            content = content.replace("#"+tag+"#","");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("ticketId", ticket);
        map.put("userName", userName);
        map.put("userId", userId);
        map.put("serverName", serverName);
        map.put("fromIp", ip);
        map.put("tag", tag);
        map.put("level", level);
        map.put("content", content);


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = sdf.format(new Date());
        map.put("sendTime", now);

        String json = JSON.toJSONString(map);
        return json;

    }


    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
