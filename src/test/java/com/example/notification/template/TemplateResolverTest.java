package com.example.notification.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateResolverTest {

    private final TemplateResolver resolver = new TemplateResolver();

    @Test
    void resolvesPlaceholders() {
        String result = resolver.resolve("Hello {{name}}, welcome to {{app}}.",
                Map.of("name", "Jainam", "app", "Acme"));
        assertThat(result).isEqualTo("Hello Jainam, welcome to Acme.");
    }

    @Test
    void missingPlaceholderBecomesEmpty() {
        String result = resolver.resolve("Hello {{name}}!", Map.of());
        assertThat(result).isEqualTo("Hello !");
    }

    @Test
    void nullTemplateReturnsNull() {
        assertThat(resolver.resolve(null, Map.of("a", "b"))).isNull();
    }
}
