-- ----------------------------
--  Table structure for `promotions`
--  We assume that a user will not be credited more than 100 pounds
-- ----------------------------
CREATE TABLE `promotions` (
  `user_id` int(11) NOT NULL,
  `promotion_id` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `credited_amount` decimal(2,2) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
