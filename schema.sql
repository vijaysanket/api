CREATE DATABASE socialeaze;

use socialeaze;

create table AuthAsset(
    state varchar(200) primary key,
    codeChallenge varchar(200),
    codeVerifier varchar(200),
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
    emailId varchar(75),
    name varchar(100),
    addedAt datetime,
    isActive boolean
    password varchar(75)
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

CREATE TABLE media (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pa_id INT NOT NULL,
    media_url VARCHAR(255) NOT NULL,
    media_type ENUM('image', 'video') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pa_id) REFERENCES postaccounts(id) ON DELETE CASCADE
);
CREATE TABLE content (
    id INT PRIMARY KEY,
    postid INT,
    pa_id INT,
    content_type VARCHAR(255),
    post_type VARCHAR(255),
    text TEXT,
    FOREIGN KEY (postid) REFERENCES post(id),
    FOREIGN KEY (pa_id) REFERENCES postaccounts(id)
);
