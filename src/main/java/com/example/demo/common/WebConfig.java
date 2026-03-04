package com.example.demo.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 💡 暫時留空即可。
    // 未來如果你有需要設定「靜態資源路徑映射」(例如本地存圖片)
    // 或「自定義參數解析器」，才會用到這份檔案。
}