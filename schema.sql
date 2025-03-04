CREATE DATABASE socialeaze;

use socialeaze;

create table AuthAsset(
    state varchar(200) primary key,
    codeChallenge varchar(200),
    codeVerifier varchar(200)
    status varchar(30)
);

Create table Organisation(
    id int primary key auto_increment,
    name varchar(200),
    addedAt datetime
);

Create table User(
    id int primary key auto_increment,
    orgId int,
    name varchar(100),
    addedAt datetime,
    isActive boolean
);

CREATE TABLE `Accounts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `accountHandle` varchar(200) DEFAULT NULL,
  `accessToken` text,
  `refreshToken` text,
  `validTill` datetime DEFAULT NULL,
  `connectedAt` datetime DEFAULT NULL,
  `followerCount` int DEFAULT NULL,
  `accountOf` varchar(50) DEFAULT NULL,
  `userId` int DEFAULT NULL,
  `profilePicture` text,
  `accountName` varchar(100) DEFAULT NULL,
  `channelId` text,
  PRIMARY KEY (`id`)
);

CREATE TABLE `Post` (
  `id` int NOT NULL AUTO_INCREMENT,
  `userId` int DEFAULT NULL,
  `postText` mediumtext,
  `addedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `scheduledAt` datetime DEFAULT NULL,
  `status` varchar(30) DEFAULT NULL,
  `orgId` int DEFAULT NULL,
  `channelId` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
)

CREATE TABLE `PostAccounts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `postId` int DEFAULT NULL,
  `accountId` int DEFAULT NULL,
  PRIMARY KEY (`id`)
);

