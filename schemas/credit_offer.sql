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
