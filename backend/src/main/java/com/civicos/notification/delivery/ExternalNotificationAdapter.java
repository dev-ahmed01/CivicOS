package com.civicos.notification.delivery;

public interface ExternalNotificationAdapter {

	Channel channel();

	void deliver(NotificationDelivery delivery);

	enum Channel { EMAIL, SMS, PUSH }
}
