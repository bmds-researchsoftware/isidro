# --- !Ups

DROP TABLE IF EXISTS "request";
CREATE TABLE "request" (
  "requestId" SERIAL,
  "userId" int NOT NULL DEFAULT '0',
  "title" varchar(254) NOT NULL,
  "description" varchar(254) NOT NULL,
  "status" int NOT NULL DEFAULT '0',
  "email" varchar(254) NOT NULL,
  "phone" varchar(64) NOT NULL,
  "cphs" varchar(64) NOT NULL,
  "pi" varchar(254) NOT NULL,
  PRIMARY KEY ("requestId")
);

DROP TABLE IF EXISTS "request_requirement";
CREATE TABLE "request_requirement" (
  "requestId" int NOT NULL,
  "requirementId" int NOT NULL,
  "completed" boolean NOT NULL default FALSE,
  PRIMARY KEY ("requestId", "requirementId")
);

DROP TABLE IF EXISTS "requirement";
CREATE TABLE "requirement" (
  "requirementId" SERIAL,
  "requirementOrder" int NOT NULL,
  "requirementName" varchar(254) NOT NULL,
  "requirementText" varchar(254) NOT NULL,
  PRIMARY KEY ("requirementId")
);
INSERT INTO "requirement" VALUES (1,14,'End-User Licensing Agreement',''),(2,16,'Data Use Agreement',''),(3,18,'Acceptable Use Agreement',''),(4,20,'Omnibus Privacy and Security Policy',''),(5,22,'CITI Researcher Training Completion Attestation',''),(6,24,'HIPAA Privacy Training',''),(7,26,'HIPAA Security Training',''),(8,28,'Researcher Specific Privacy and Security Training',''),(9,30,'REDCap Privacy and Security Training','');

DROP TABLE IF EXISTS "status";
CREATE TABLE "status" (
  "statusId" SERIAL,
  "statusName" varchar(254) NOT NULL,
  PRIMARY KEY ("statusId")
);

DROP TABLE IF EXISTS "isidro_user";
CREATE TABLE "isidro_user" (
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

DROP TABLE IF EXISTS "unique_file";
CREATE TABLE "unique_file" (
  "isDeleted" boolean NOT NULL DEFAULT FALSE,
  "password" varchar(254) DEFAULT NULL,
  "fileLocation" varchar(254) NOT NULL,
  "uniqueName" varchar(254) NOT NULL,
  "requestId" int NOT NULL,
  "fileName" varchar(254) NOT NULL DEFAULT '',
  "dateCreated" date NOT NULL DEFAULT now(),
  PRIMARY KEY ("uniqueName")
);


INSERT INTO "isidro_user" VALUES
    (1,'','Test','User','test.user@example.com',TRUE,'$2a$10$Mtcq4iFwL6tRhekgpH3jxeckRUqu8tKcM0JpmdsUz7srLtAbvDW06','broker');

DELETE FROM "users";
DELETE FROM "login_info";
DELETE FROM "password_info";
DELETE FROM "user_login_info";


INSERT INTO "users" VALUES
    ('37928d32-d011-4266-8a22-c35a4ff27129','Test','User','Test User','test.user@example.com',NULL,true);
INSERT INTO "login_info" VALUES
    (1, 'credentials', 'test.user@example.com');
INSERT INTO "password_info" VALUES
    ('bcrypt', '$2a$10$Mtcq4iFwL6tRhekgpH3jxeckRUqu8tKcM0JpmdsUz7srLtAbvDW06', NULL, 1);
INSERT INTO "user_login_info" VALUES
    ('37928d32-d011-4266-8a22-c35a4ff27129', 1);

DROP TABLE IF EXISTS "request_log";
CREATE TABLE "request_log" (
  "id" SERIAL,
  "requestId" int NOT NULL,
  "userId" CHARACTER VARYING,
  "notes" varchar(4096) NOT NULL,
  "timeMod" timestamp NOT NULL default now(),
  PRIMARY KEY ("id")
);

# --- !Downs

DROP TABLE IF EXISTS "request_requirement";
DROP TABLE IF EXISTS "request";
DROP TABLE IF EXISTS "requirement";
DROP TABLE IF EXISTS "status";
DROP TABLE IF EXISTS "isidro_user";
DROP TABLE IF EXISTS "user_requirement_log";
DROP TABLE IF EXISTS "unique_file";
