package com.project.notificationservice.provider;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.exception.NotificationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationSenderResolver {

    private final Map<Channel, NotificationChannelSender> senders;

    public NotificationSenderResolver(List<NotificationChannelSender> values) {
        EnumMap<Channel, NotificationChannelSender> mapped = new EnumMap<>(Channel.class);
        for (NotificationChannelSender sender : values) {
            if (mapped.put(sender.supportedChannel(), sender) != null) {
                throw new IllegalStateException("Duplicate sender for " + sender.supportedChannel());
            }
        }
        this.senders = Map.copyOf(mapped);
    }

    public NotificationChannelSender resolve(Channel channel) {
        NotificationChannelSender sender = senders.get(channel);
        if (sender == null) {
            throw new NotificationException("CHANNEL_NOT_CONFIGURED",
                    "No sender is configured for " + channel, HttpStatus.SERVICE_UNAVAILABLE);
        }
        return sender;
    }
}
