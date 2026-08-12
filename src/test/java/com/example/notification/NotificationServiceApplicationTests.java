package com.example.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notification.template.TemplateResolver;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationServiceApplicationTests {

    @Test
    void templateResolverWorksStandalone() {
        TemplateResolver resolver = new TemplateResolver();
        assertThat(resolver.resolve("Hi {{name}}", Map.of("name", "A"))).isEqualTo("Hi A");
    }
}
