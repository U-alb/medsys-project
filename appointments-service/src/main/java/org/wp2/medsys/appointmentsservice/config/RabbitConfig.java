package org.wp2.medsys.appointmentsservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String APPOINTMENTS_EXCHANGE = "medsys.appointments.exchange";
    public static final String NOTIFICATIONS_QUEUE = "medsys.notifications.queue";
    public static final String NOTIFICATIONS_ROUTING_KEY = "appointments.notifications";

    @Bean
    public TopicExchange appointmentsExchange() {
        return new TopicExchange(APPOINTMENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationsQueue() {
        // durable queue so it survives broker restarts
        return new Queue(NOTIFICATIONS_QUEUE, true);
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue,
                                        TopicExchange appointmentsExchange) {
        return BindingBuilder
                .bind(notificationsQueue)
                .to(appointmentsExchange)
                .with(NOTIFICATIONS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
