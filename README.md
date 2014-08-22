credit-offer-service
====================

A message driven service that listens for device registrations and credits accounts accordingly. 

Currently, this service only listens for Hudl 2 registrations and credits a users account upon registering that specific device.

## Requirements

### Development requirement
Credit Offer Service require SBT 0.13 and MySql

### User requirement
A connection to the Zuul authentication server is required. The service relies on a user that has the CSR privileges too.

To get a user with the CSR privileges:
 
1 - You must have a user already in Zuul. We assume in this case that the user is `credit-offer-service@blinkbox.com`
2 - Run the following SQL in Zuul

```sql
-- set these values
SET @username = 'credit-offer-service@blinkbox.com';
SET @role = 'csr';

-- don't change anything below here
SET @user_id = (SELECT id FROM users WHERE username = @username);
SET @role_id = (SELECT id FROM user_roles WHERE `name` = @role);

INSERT INTO user_privileges (created_at, user_id, user_role_id)
SELECT NOW(), @user_id, @role_id
FROM user_roles
WHERE @user_id IS NOT NULL AND @role_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM user_privileges WHERE user_id = @user_id AND user_role_id = @role_id)
LIMIT 1;

SELECT
  p.created_at,
  p.user_id,
  r.`name` as role_name,
  r.description
FROM
  user_privileges p
  INNER JOIN user_roles r ON r.id = p.user_role_id
WHERE
  p.user_id = @user_id;
```

The service also connects to RabbitMQ to process the registration messages. 

The service subscribes to events from the “Events” exchange, and stores these in the Credit.Offer.DeviceRegistration and Credit.Offer.DeviceRegistration.DLQ queues.

It publishes messages to the Mail.Sender.Exchange and Agora exchanges.

## Database

The schema for the database are held within the `schema` directory. 

```sql
-- ----------------------------
--  Table structure for `promotions`
-- ----------------------------
CREATE TABLE `promotions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `promotion_id` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL,
  `credited_amount` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

```