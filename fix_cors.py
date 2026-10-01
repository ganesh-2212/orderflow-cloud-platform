import pathlib
import sys

def update_security_config():
    p = pathlib.Path('backend/order-service/src/main/java/com/orderflow/order/security/SecurityConfig.java')
    content = p.read_text(encoding='utf-8')
    
    # Add imports if not present
    imports = """import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import org.springframework.security.config.Customizer;"""
    if "CorsConfigurationSource" not in content:
        content = content.replace('import org.springframework.context.annotation.Bean;', imports + '\nimport org.springframework.context.annotation.Bean;')
        
    # Add CorsConfigurationSource bean
    bean = """
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean"""
    if "corsConfigurationSource" not in content:
        content = content.replace('    @Bean\n    public SecurityFilterChain', bean + '\n    public SecurityFilterChain')
        
    # Add .cors(Customizer.withDefaults())
    if ".cors(Customizer.withDefaults())" not in content:
        content = content.replace('        http\n            .csrf', '        http\n            .cors(Customizer.withDefaults())\n            .csrf')
        
    p.write_text(content, encoding='utf-8')
    print("Updated SecurityConfig.java")

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
    p.write_text(content, encoding='utf-8')
    print(f"Created {p}")

update_security_config()
create_web_config('backend/inventory-service/src/main/java/com/orderflow/inventory/config', 'com.orderflow.inventory.config')
create_web_config('backend/fulfillment-service/src/main/java/com/orderflow/fulfillment/config', 'com.orderflow.fulfillment.config')
create_web_config('backend/incident-service/src/main/java/com/orderflow/incident/config', 'com.orderflow.incident.config')
