package org.btik.espidf.toolwindow.tasks.web;

/**
 * HTTP 路径、ContentType 常量及响应数据 record。
 *
 * @author lustre
 * @since 2025/6/21
 */
public interface HttpMeta {

    // ── URL 路径 ──

    String SIZE_ANALYSIS_PREFIX = "/esp-idf-size-analysis/";
    String API_DATA  = SIZE_ANALYSIS_PREFIX + "api/data";
    String API_SETTINGS = SIZE_ANALYSIS_PREFIX + "api/settings";
    String ASSETS_PREFIX = SIZE_ANALYSIS_PREFIX + "assets/";

    // ── Content-Type ──

    String CT_JSON  = "application/json; charset=UTF-8";
    String CT_JS    = "application/javascript; charset=UTF-8";
    String CT_CSS   = "text/css; charset=UTF-8";
    String CT_HTML  = "text/html; charset=UTF-8";
    String CT_TEXT  = "text/plain; charset=UTF-8";
    String CT_OCTET = "application/octet-stream";

    // ── 响应数据 record ──

    record WebSettings(String lang, boolean dark) {}
    record WebError(String error) {}
}
