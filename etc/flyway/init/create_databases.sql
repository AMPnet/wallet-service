DROP DATABASE IF EXISTS wallet_service;
CREATE DATABASE wallet_service ENCODING 'UTF-8';

DROP DATABASE IF EXISTS wallet_service_test;
CREATE DATABASE wallet_service_test ENCODING 'UTF-8';

DROP USER IF EXISTS wallet_service;
CREATE USER wallet_service WITH PASSWORD 'password';

DROP USER IF EXISTS wallet_service_test;
CREATE USER wallet_service_test WITH PASSWORD 'password';
