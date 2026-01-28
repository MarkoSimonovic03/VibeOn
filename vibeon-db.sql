-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jan 28, 2026 at 08:50 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `vibeon-db`
--

-- --------------------------------------------------------

--
-- Table structure for table `chats`
--

CREATE TABLE `chats` (
  `id` bigint(20) NOT NULL,
  `last_message_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `chats`
--

INSERT INTO `chats` (`id`, `last_message_id`) VALUES
(1, 9),
(2, 22);

-- --------------------------------------------------------

--
-- Table structure for table `chats_users`
--

CREATE TABLE `chats_users` (
  `chat_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `chats_users`
--

INSERT INTO `chats_users` (`chat_id`, `user_id`) VALUES
(1, 1),
(1, 2),
(2, 1),
(2, 3);

-- --------------------------------------------------------

--
-- Table structure for table `follows`
--

CREATE TABLE `follows` (
  `id` bigint(20) NOT NULL,
  `followee_id` bigint(20) DEFAULT NULL,
  `follower_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `follows`
--

INSERT INTO `follows` (`id`, `followee_id`, `follower_id`) VALUES
(1, 1, 3),
(2, 2, 1);

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--

CREATE TABLE `messages` (
  `id` bigint(20) NOT NULL,
  `content` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `chat_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `messages`
--

INSERT INTO `messages` (`id`, `content`, `created_at`, `chat_id`, `user_id`) VALUES
(1, 'Zdravo Mare!', '2026-01-28 20:33:04.000000', 1, 2),
(2, 'Cao Ogi!', '2026-01-28 20:33:21.000000', 1, 1),
(3, 'Kako napreduje projekat iz SE425 kod Andjele? :D', '2026-01-28 20:33:42.000000', 1, 2),
(4, 'Odlicno, sve bolje i bolje, upravo ga završavamo', '2026-01-28 20:34:02.000000', 1, 1),
(5, 'Kad je odbrana?', '2026-01-28 20:34:13.000000', 1, 2),
(6, 'Ne znam', '2026-01-28 20:34:23.000000', 1, 1),
(7, 'Može na vežbama, a može i u petak :D', '2026-01-28 20:35:03.000000', 1, 1),
(8, 'Uuu super...', '2026-01-28 20:35:16.000000', 1, 2),
(9, 'Andrej i ja završavamo dokumentaciju :D', '2026-01-28 20:35:37.000000', 1, 2),
(10, 'Cao Mareeee', '2026-01-28 20:36:25.000000', 2, 3),
(11, 'Kako si?', '2026-01-28 20:36:30.000000', 2, 3),
(12, 'Evo brate, odlično', '2026-01-28 20:37:01.000000', 2, 1),
(13, 'Gde si ti, šta radiš?', '2026-01-28 20:37:08.000000', 2, 1),
(14, 'Evo vratio sam se sa skijanja. Bilo je super :D', '2026-01-28 20:37:36.000000', 2, 3),
(15, 'Gde si skijao, koliko dana si bio? Kakvo je bilo vreme?', '2026-01-28 20:37:58.000000', 2, 1),
(16, 'U Austriji, 10 dana, vreme bilo TOP :D', '2026-01-28 20:38:26.000000', 2, 3),
(17, 'Lepo lepo', '2026-01-28 20:38:39.000000', 2, 1),
(18, 'Šta je ostalo još za projekat kod Anđele?', '2026-01-28 20:39:10.000000', 2, 1),
(19, 'Sve gotovo, samo jos dokumentacija i to je to', '2026-01-28 20:39:31.000000', 2, 3),
(20, 'Napokon... :\'(', '2026-01-28 20:39:51.000000', 2, 3),
(21, '👌', '2026-01-28 20:40:08.000000', 2, 1),
(22, 'Odličnoooo, svaka čast!', '2026-01-28 20:40:17.000000', 2, 1);

-- --------------------------------------------------------

--
-- Table structure for table `posts`
--

CREATE TABLE `posts` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `posts`
--

INSERT INTO `posts` (`id`, `created_at`, `deleted_at`, `description`, `image_url`, `is_deleted`, `user_id`) VALUES
(1, '2026-01-28 19:04:16.000000', NULL, ';)', 'bdfde3e1-f89e-4d54-b34a-0ce7dbe52506.png', b'0', 2),
(2, '2026-01-28 20:01:53.000000', NULL, '...', 'f1fe92e4-e03b-4f20-bc60-379aed6f8cb6.jpg', b'0', 1),
(3, '2026-01-28 20:04:04.000000', NULL, '...', '2d008d06-71ee-40fe-8205-b032bfaccb10.jpg', b'0', 3),
(4, '2026-01-28 20:04:35.000000', NULL, '...', '7fd3468a-0b5d-4f1e-b5db-d8ac85795231.jpg', b'0', 3),
(5, '2026-01-28 20:05:14.000000', NULL, '...', '880b97b6-c5d2-48be-aefd-69cae0a15338.jpg', b'0', 1),
(6, '2026-01-28 20:05:53.000000', NULL, '...', '03201a51-af1b-482b-8825-0897c1d03eca.jpg', b'0', 2),
(7, '2026-01-28 20:06:12.000000', '2026-01-28 20:06:21.000000', '...', '9df295e0-15ce-4a49-a07e-52f77dda5f04.jpg', b'1', 2),
(8, '2026-01-28 20:06:33.000000', NULL, '...', 'cf4ad5fb-9d2a-4f00-8b68-767e70df5f86.jpg', b'0', 2),
(9, '2026-01-28 20:06:45.000000', NULL, '...', '2818ef2e-5ca6-4386-81d0-7b6e8fbaa362.jpg', b'0', 2),
(10, '2026-01-28 20:07:16.000000', NULL, '...', '92341267-bb00-44cc-8048-f6097575c36c.jpg', b'0', 3),
(11, '2026-01-28 20:07:26.000000', NULL, '...', 'c13b75d1-00c1-4eff-b1c2-19f34fa83a88.jpg', b'0', 3),
(12, '2026-01-28 20:07:43.000000', NULL, '...', '036006a4-0d6a-439b-ab76-c66a7fdafe81.jpg', b'0', 1),
(13, '2026-01-28 20:08:03.000000', NULL, '...', 'b5f20ac2-3186-4ae9-a8b0-e59d0831732a.jpg', b'0', 1),
(14, '2026-01-28 20:08:24.000000', NULL, '...', '380e1221-fcf7-482d-af23-90a64070c476.jpg', b'0', 2),
(15, '2026-01-28 20:08:41.000000', NULL, '...', 'dc78d1b2-496f-42bb-a90f-bad0dfb5cd4c.jpg', b'0', 2),
(16, '2026-01-28 20:08:54.000000', NULL, '...', '78f4c0c6-5218-45a2-ae11-9725d7fde824.jpg', b'0', 2),
(17, '2026-01-28 20:09:20.000000', '2026-01-28 20:10:09.000000', '...', '24fd7bf2-5b84-44cc-abbc-b68ded124c93.jpg', b'1', 3),
(18, '2026-01-28 20:10:17.000000', NULL, '...', 'c5d1a136-2f56-4364-b73c-3081f7434b60.png', b'0', 3),
(19, '2026-01-28 20:10:31.000000', NULL, '...', '37bc23b4-5af0-45e8-afe9-c34cafc43dd1.jpg', b'0', 3),
(20, '2026-01-28 20:11:22.000000', NULL, '...', '6643b1e8-074d-49c1-88df-4ff48f151774.png', b'0', 1),
(21, '2026-01-28 20:11:44.000000', NULL, '...', '3e4467d6-070d-441c-8358-274c8d88352b.jpg', b'0', 1),
(22, '2026-01-28 20:11:53.000000', NULL, '...', '9a074b25-f8fa-4c33-961a-ea72f634ee26.jpg', b'0', 1),
(23, '2026-01-28 20:12:47.000000', NULL, '...', '760b78f3-9596-411f-b189-020c52046d7d.jpg', b'0', 2),
(24, '2026-01-28 20:12:55.000000', NULL, '...', 'db2b853f-b75e-4153-b73a-75784e85e899.jpg', b'0', 2),
(25, '2026-01-28 20:13:09.000000', NULL, '...', 'bd1d66c8-b683-45b8-af5e-b7cfa027ff35.jpg', b'0', 2),
(26, '2026-01-28 20:13:24.000000', NULL, '...', '881ccbda-b7c5-4eed-a1aa-bd19013bfb64.jpg', b'0', 2),
(27, '2026-01-28 20:13:57.000000', NULL, '...', 'b4c91a7e-d5b4-434b-a66a-3ff8523368f8.jpg', b'0', 3),
(28, '2026-01-28 20:14:04.000000', NULL, '...', '823de1f1-d532-4c6a-bcbe-c42abde220c6.jpg', b'0', 3),
(29, '2026-01-28 20:15:19.000000', NULL, '...', '7b6b4788-90df-4484-9fa6-94487d0137c7.jpg', b'0', 1),
(30, '2026-01-28 20:15:29.000000', NULL, '...', '09c034d6-f309-4037-b022-740f0bd10bda.jpg', b'0', 1),
(31, '2026-01-28 20:15:44.000000', NULL, '...', 'f7b9dfd7-f973-4596-8bc2-e4dc1b671776.jpg', b'0', 1),
(32, '2026-01-28 20:16:01.000000', NULL, '...', '25c292e6-a5b0-47b0-ac91-eab05db31285.jpg', b'0', 1),
(33, '2026-01-28 20:17:19.000000', NULL, '...', 'abda417e-8497-4363-8a06-55ce49c7e410.jpg', b'0', 1),
(34, '2026-01-28 20:18:36.000000', NULL, '...', 'c67906d9-2af9-436a-b46b-a44ffe346d38.jpg', b'0', 2),
(35, '2026-01-28 20:18:44.000000', NULL, '...', 'bd109c98-5172-4f65-bcbd-fb363ac67358.jpg', b'0', 2);

-- --------------------------------------------------------

--
-- Table structure for table `roles`
--

CREATE TABLE `roles` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `roles`
--

INSERT INTO `roles` (`id`, `name`) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_USER');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `birth_date` date DEFAULT NULL,
  `created_at` date DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `gender` bit(1) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `profile_image_url` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `birth_date`, `created_at`, `email`, `gender`, `last_name`, `name`, `password`, `profile_image_url`, `username`) VALUES
(1, '2003-05-03', '2026-01-28', 'marko.simonovic.5349@metropolitan.ac.rs', b'0', 'Simonovic', 'Marko', '$2a$10$6dprirAqdELiDqxPtU8BreO6YCwJTZrxKFlZhVd8KklSmq8ForaEe', 'b2ae64fa-f145-4fcf-a5ae-e08767db3d5d.jpg', 'marko.simonovicc'),
(2, '2003-08-12', '2026-01-28', 'ognjen.stevic.5378@metropolitan.ac.rs', b'0', 'Stevic', 'Ognjen', '$2a$10$VUFs83ur3B8N3y48syTboemrGKtHu7lwz52T/AVuiqHBin5iyJVmW', 'e8ec628f-6bff-4222-8ef4-37ea86f569e1.png', 'ognjen._.stevic'),
(3, '2003-06-08', '2026-01-28', 'andrej.trailovic.5444@metropolitan.ac.r.s', b'0', 'Trailovic', 'Andrej', '$2a$10$B5j8uMaf6CcfYmrGCcNt/eWwB6hsVxctsxf/nZvyqkjatkEhbvt.K', '4b8502bf-e1f6-4892-9ac3-0b473c3222dc.jpg', 'andrejtrailovic');

-- --------------------------------------------------------

--
-- Table structure for table `users_roles`
--

CREATE TABLE `users_roles` (
  `user_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users_roles`
--

INSERT INTO `users_roles` (`user_id`, `role_id`) VALUES
(1, 2),
(2, 2),
(3, 2);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `chats`
--
ALTER TABLE `chats`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK8y0mndjgabg4cq2x9o0dj72p` (`last_message_id`);

--
-- Indexes for table `chats_users`
--
ALTER TABLE `chats_users`
  ADD PRIMARY KEY (`chat_id`,`user_id`),
  ADD KEY `FKel3edus31mycq8mm6gis7ei9` (`user_id`);

--
-- Indexes for table `follows`
--
ALTER TABLE `follows`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKeo7hqi2bt2vdwk6mpu0w2ihyb` (`followee_id`),
  ADD KEY `FKqnkw0cwwh6572nyhvdjqlr163` (`follower_id`);

--
-- Indexes for table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK64w44ngcpqp99ptcb9werdfmb` (`chat_id`),
  ADD KEY `FKpsmh6clh3csorw43eaodlqvkn` (`user_id`);

--
-- Indexes for table `posts`
--
ALTER TABLE `posts`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKamevt5he2wrp4h64wu75hjj4u` (`image_url`),
  ADD KEY `FK5lidm6cqbc7u4xhqpxm898qme` (`user_id`);

--
-- Indexes for table `roles`
--
ALTER TABLE `roles`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  ADD UNIQUE KEY `UKcema79jnjd0b5yei1k1gi19e1` (`profile_image_url`),
  ADD UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`);

--
-- Indexes for table `users_roles`
--
ALTER TABLE `users_roles`
  ADD PRIMARY KEY (`user_id`,`role_id`),
  ADD KEY `FKj6m8fwv7oqv74fcehir1a9ffy` (`role_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `chats`
--
ALTER TABLE `chats`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `follows`
--
ALTER TABLE `follows`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `messages`
--
ALTER TABLE `messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- AUTO_INCREMENT for table `posts`
--
ALTER TABLE `posts`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=36;

--
-- AUTO_INCREMENT for table `roles`
--
ALTER TABLE `roles`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `chats`
--
ALTER TABLE `chats`
  ADD CONSTRAINT `FK8y0mndjgabg4cq2x9o0dj72p` FOREIGN KEY (`last_message_id`) REFERENCES `messages` (`id`);

--
-- Constraints for table `chats_users`
--
ALTER TABLE `chats_users`
  ADD CONSTRAINT `FKel3edus31mycq8mm6gis7ei9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKq1qn1fsgj8yr4ftcsrtxn7t6k` FOREIGN KEY (`chat_id`) REFERENCES `chats` (`id`);

--
-- Constraints for table `follows`
--
ALTER TABLE `follows`
  ADD CONSTRAINT `FKeo7hqi2bt2vdwk6mpu0w2ihyb` FOREIGN KEY (`followee_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKqnkw0cwwh6572nyhvdjqlr163` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `FK64w44ngcpqp99ptcb9werdfmb` FOREIGN KEY (`chat_id`) REFERENCES `chats` (`id`),
  ADD CONSTRAINT `FKpsmh6clh3csorw43eaodlqvkn` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `posts`
--
ALTER TABLE `posts`
  ADD CONSTRAINT `FK5lidm6cqbc7u4xhqpxm898qme` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `users_roles`
--
ALTER TABLE `users_roles`
  ADD CONSTRAINT `FK2o0jvgh89lemvvo17cbqvdxaa` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKj6m8fwv7oqv74fcehir1a9ffy` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
