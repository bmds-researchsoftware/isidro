# --- !Ups

DROP TABLE IF EXISTS "request";
CREATE TABLE "request" (
  "requestId" SERIAL,
  "userId" int NOT NULL DEFAULT '0',
  "requestText" varchar(254) NOT NULL,
  "reasonForRequest" varchar(254) NOT NULL,
  PRIMARY KEY ("requestId")
);

INSERT INTO "request" VALUES (2,1,'Aging Study','Please send me data on how studies age.'),(3,1,'Mouse Data','Please send me the mouse data.'),(4,2,'My data request','I would like as much data as you can send me, please.'),(5,2,'My other request','Also, please send the data which you can not send.'),(6,2,'Last request','If anyone else can send me data, that would also be nice.'),(7,3,'Cloning Study','Please send me all information about the clones, and why some of them attempt to take over the world.');

DROP TABLE IF EXISTS "request_requirement";
CREATE TABLE "request_requirement" (
  "requestRequirementId" SERIAL,
  "requestId" int NOT NULL,
  "requirementId" int NOT NULL,
  "requirementTime" int NOT NULL,
  PRIMARY KEY ("requestRequirementId")
);

DROP TABLE IF EXISTS "requirement";
CREATE TABLE "requirement" (
  "requirementId" SERIAL,
  "requirementOrder" int NOT NULL,
  "requirementName" varchar(254) NOT NULL,
  "requirementText" varchar(254) NOT NULL,
  PRIMARY KEY ("requirementId")
);
INSERT INTO "requirement" VALUES (1,2,'Water Mark',''),(2,4,'Fingerprint',''),(3,6,'Signature Certificate',''),(4,8,'Encryption',''),(5,10,'Excel File Format',''),(6,12,'PDF File Format',''),(7,14,'End-User Licensing Agreement',''),(8,16,'Data Use Agreement',''),(9,18,'Acceptable Data Use Policy',''),(10,20,'Omnibus Privacy and Security Policy Agreement',''),(11,22,'CITI Researcher Training Completion Attestation',''),(12,24,'HIPAA Privacy Training',''),(13,26,'HIPAA Security Training',''),(14,28,'Researcher Specific Privacy and Security Training',''),(15,30,'REDCap Privacy and Security Training',''),(16,1,'','File Requirements (Security)'),(17,9,'','File Requirements (Format)'),(18,13,'','Legal Requirements'),(19,23,'','Educational Requirements');

DROP TABLE IF EXISTS "status";
CREATE TABLE "status" (
  "statusId" SERIAL,
  "statusName" varchar(254) NOT NULL,
  PRIMARY KEY ("statusId")
);

DROP TABLE IF EXISTS "user";
CREATE TABLE "user" (
  "userId" SERIAL,
  "netId" varchar(254) NOT NULL DEFAULT '',
  "firstName" varchar(254) NOT NULL,
  "lastName" varchar(254) NOT NULL,
  "email" varchar(254) NOT NULL,
  "emailConfirmed" boolean NOT NULL DEFAULT FALSE,
  "password" varchar(254) NOT NULL DEFAULT '',
  "services" varchar(254) NOT NULL DEFAULT '',
  PRIMARY KEY ("userId")
);

INSERT INTO "user" VALUES (1,'','William','Adama','databroker@isidro.dartmouth.edu',TRUE,'$2a$10$Mtcq4iFwL6tRhekgpH3jxeckRUqu8tKcM0JpmdsUz7srLtAbvDW06','master'),(2,'','Mickey','Mouse','mmouse@isidro.dartmouth.edu',TRUE,'$2a$10$Mtcq4iFwL6tRhekgpH3jxeckRUqu8tKcM0JpmdsUz7srLtAbvDW06',''),(3,'','Daffy','Duck','winner@isidro.dartmouth.edu',TRUE,'$2a$10$Mtcq4iFwL6tRhekgpH3jxeckRUqu8tKcM0JpmdsUz7srLtAbvDW06',''),(4,'','Arya','Stark','arya@gmail.com',TRUE,'$2a$10$Mtcq4iFwL6tRhekgpH3jxeckRUqu8tKcM0JpmdsUz7srLtAbvDW06','x');

DROP TABLE IF EXISTS "user_requirement_log";
CREATE TABLE "user_requirement_log" (
  "id" SERIAL,
  "requestId" int NOT NULL,
  "requirementId" int NOT NULL,
  "userId" int NOT NULL,
  "logTime" int NOT NULL,
  PRIMARY KEY ("id")
);

# --- !Downs

DROP TABLE IF EXISTS "request_requirement";
DROP TABLE IF EXISTS "request";
DROP TABLE IF EXISTS "requirement";
DROP TABLE IF EXISTS "status";
DROP TABLE IF EXISTS "user";
DROP TABLE IF EXISTS "user_requirement_log";
