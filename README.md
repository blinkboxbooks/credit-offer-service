credit-offer-service
====================

A message driven service that listens for device registrations and credits accounts accordingly. 

Currently, this service only listens for Hudl 2 registrations and credits a users account upon registering that specific device.

## Requirements

Credit Offer Service require SBT 0.13 and MySql

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