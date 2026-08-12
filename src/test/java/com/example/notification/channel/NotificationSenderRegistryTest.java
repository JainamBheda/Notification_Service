package com.example.notification.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.notification.common.NotificationChannel;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationSenderRegistryTest {

    @Test
    void selectsSenderByChannel() {
        NotificationSender email = mock(NotificationSender.class);
        when(email.channel()).thenReturn(NotificationChannel.EMAIL);
        NotificationSender sms = mock(NotificationSender.class);
        when(sms.channel()).thenReturn(NotificationChannel.SMS);

        NotificationSenderRegistry registry = new NotificationSenderRegistry(List.of(email, sms));

        assertThat(registry.get(NotificationChannel.EMAIL)).isSameAs(email);
        assertThat(registry.get(NotificationChannel.SMS)).isSameAs(sms);
    }

    @Test
    void throwsWhenChannelMissing() {
        NotificationSenderRegistry registry = new NotificationSenderRegistry(List.of());
        assertThatThrownBy(() -> registry.get(NotificationChannel.PUSH))
                .hasMessageContaining("PUSH");
    }
}
