package com.example.notification.config;

import com.example.notification.kafka.event.NotificationEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, NotificationEvent> notificationEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        // Instance deserializers are supplied below — remove class + spring.json.* props
        // to avoid: "JsonDeserializer must be configured with property setters, or via
        // configuration properties; not both"
        props.remove(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
        props.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        props.keySet().removeIf(key -> key.startsWith("spring.json."));

        JsonDeserializer<NotificationEvent> deserializer =
                new JsonDeserializer<>(NotificationEvent.class, false);
        deserializer.addTrustedPackages("com.example.notification.kafka.event");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>
            kafkaListenerContainerFactory(ConsumerFactory<String, NotificationEvent> consumerFactory,
                                          NotificationProperties properties) {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(properties.getKafka().getConsumer().getConcurrency());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>
            priorityKafkaListenerContainerFactory(
                    ConsumerFactory<String, NotificationEvent> consumerFactory,
                    NotificationProperties properties) {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(properties.getKafka().getConsumer().getPriorityConcurrency());
        return factory;
    }

    @Bean
    public NewTopic notificationRequestedTopic(NotificationProperties properties) {
        return TopicBuilder.name(properties.getKafka().getTopics().getRequested())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationRequestedPriorityTopic(NotificationProperties properties) {
        return TopicBuilder.name(properties.getKafka().getTopics().getRequestedPriority())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationRetryTopic(NotificationProperties properties) {
        return TopicBuilder.name(properties.getKafka().getTopics().getRetry())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationDeadLetterTopic(NotificationProperties properties) {
        return TopicBuilder.name(properties.getKafka().getTopics().getDeadLetter())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationStatusTopic(NotificationProperties properties) {
        return TopicBuilder.name(properties.getKafka().getTopics().getStatus())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
