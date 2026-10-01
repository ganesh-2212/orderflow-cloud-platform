import pathlib
import sys

def create_web_config(service_path, package_name):
    content = f"""package {package_name};

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {{

    @Override
    public void addCorsMappings(CorsRegistry registry) {{
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept");
    }}
}}
"""
    p = pathlib.Path(service_path) / "CorsConfig.java"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding='utf-8')
    print(f"Created {p}")

create_web_config('backend/inventory-service/src/main/java/com/orderflow/inventory/config', 'com.orderflow.inventory.config')
create_web_config('backend/fulfillment-service/src/main/java/com/orderflow/fulfillment/config', 'com.orderflow.fulfillment.config')
create_web_config('backend/incident-service/src/main/java/com/orderflow/incident/config', 'com.orderflow.incident.config')
