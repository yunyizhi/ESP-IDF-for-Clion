package org.btik.espidf.toolwindow.tasks.web;

import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.util.I18nMessage;
import org.jetbrains.ide.HttpRequestHandler;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.btik.espidf.toolwindow.tasks.web.HttpMeta.*;

public class EspIdfHttpHandler extends HttpRequestHandler {
    private static final Logger LOG = Logger.getInstance(EspIdfHttpHandler.class);
    private static final Gson GSON = new Gson();

    @Override
    public boolean process(@NonNull QueryStringDecoder queryStringDecoder,
                           @NonNull FullHttpRequest fullHttpRequest,
                           @NonNull ChannelHandlerContext channelHandlerContext) throws IOException {
        String path = queryStringDecoder.path();

        if (!path.startsWith(SIZE_ANALYSIS_PREFIX)) {
            return false;
        }

        if (path.equals(API_SETTINGS)) {
            return handleApiSettings(channelHandlerContext);
        }

        if (path.equals(API_DATA)) {
            Map<String, List<String>> params = queryStringDecoder.parameters();
            return handleApiData(channelHandlerContext, params);
        }

        if (path.startsWith(ASSETS_PREFIX)) {
            return handleAsset(channelHandlerContext, path);
        }

        return handleIndexHtml(channelHandlerContext);
    }

    private boolean handleApiSettings(ChannelHandlerContext ctx) {
        String lang = I18nMessage.getMsg("web.lang");
        if (StringUtils.isBlank(lang)) lang = "en";

        boolean dark = EditorColorsManager.getInstance().isDarkEditor();

        sendJson(ctx, GSON.toJson(new WebSettings(lang, dark)), HttpResponseStatus.OK);
        return true;
    }

    private boolean handleApiData(ChannelHandlerContext ctx, Map<String, List<String>> params) {
        List<String> pathList = params.get("path");
        if (pathList == null || pathList.isEmpty()) {
            sendJson(ctx, GSON.toJson(new WebError("Missing path parameter.")), HttpResponseStatus.BAD_REQUEST);
            return true;
        }

        String filePath = URLDecoder.decode(pathList.get(0), StandardCharsets.UTF_8);

        try {
            byte[] bytes = Files.readAllBytes(Path.of(filePath));
            sendBytes(ctx, bytes, CT_JSON, HttpResponseStatus.OK);
        } catch (IOException e) {
            LOG.error("Failed to read size JSON: " + filePath, e);
            sendJson(ctx, GSON.toJson(new WebError("Failed to read size data.")), HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return true;
    }

    private boolean handleAsset(ChannelHandlerContext ctx, String path) {
        String fileName = path.substring(ASSETS_PREFIX.length());
        if (fileName.contains("..") || fileName.contains("/")) {
            sendText(ctx, "Forbidden", HttpResponseStatus.FORBIDDEN);
            return true;
        }
        String resourcePath = "web/assets/" + fileName;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                sendText(ctx, "Asset not found", HttpResponseStatus.NOT_FOUND);
                return true;
            }
            byte[] bytes = is.readAllBytes();
            String contentType = fileName.endsWith(".js") ? CT_JS
                    : fileName.endsWith(".css") ? CT_CSS
                      : CT_OCTET;
            sendBytes(ctx, bytes, contentType, HttpResponseStatus.OK);
        } catch (IOException e) {
            LOG.error("Failed to read asset: " + resourcePath, e);
            sendText(ctx, "Internal error", HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return true;
    }

    private boolean handleIndexHtml(ChannelHandlerContext ctx) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("web/index.html")) {
            if (is == null) {
                sendText(ctx, "Size analysis page not found", HttpResponseStatus.NOT_FOUND);
                return true;
            }
            sendBytes(ctx, is.readAllBytes(), CT_HTML, HttpResponseStatus.OK);
        } catch (IOException e) {
            LOG.error("Failed to read web/index.html", e);
            sendText(ctx, "Internal error", HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return true;
    }

    private void sendJson(ChannelHandlerContext ctx, String json, HttpResponseStatus status) {
        sendBytes(ctx, json.getBytes(StandardCharsets.UTF_8), CT_JSON, status);
    }

    private void sendText(ChannelHandlerContext ctx, String text, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer(text, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, CT_TEXT);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendBytes(ChannelHandlerContext ctx, byte[] data, String contentType, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer(data));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
