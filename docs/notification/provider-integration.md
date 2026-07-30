# Provider integration

`NotificationChannelSender` is the provider boundary. The resolver selects one sender per channel:

- Email uses Spring Mail/SMTP with bounded connect, read, and write timeouts.
- In-app writes the rendered notification to MySQL.
- SMS and Web Push are explicit local mocks until production adapters are configured.

Provider results classify failures as transient, rate-limited, permanent, payload, or template errors. Transient and rate-limited failures retry at approximately 30 seconds, 2 minutes, 10 minutes, and 30 minutes with jitter, up to five total attempts. Provider retry-after values are respected. Exhausted retryable failures enter the dead-letter table; permanent, payload, and template failures do not loop.

Production adapters should implement the same interface, return a provider message ID, never log destinations or credentials, and map provider codes into the shared failure categories.
