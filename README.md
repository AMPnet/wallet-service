# Wallet Service

[![CircleCI](https://circleci.com/gh/AMPnet/wallet-service/tree/master.svg?style=svg&circle-token=6f6bd7fe37596c217dfb4269da2055144c831811)](https://circleci.com/gh/AMPnet/wallet-service/tree/master) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/6f9d691a5abe49469ede30a68ae20e8a)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=AMPnet/wallet-service&amp;utm_campaign=Badge_Grade) [![codecov](https://codecov.io/gh/AMPnet/wallet-service/branch/master/graph/badge.svg)](https://codecov.io/gh/AMPnet/wallet-service)

Wallet service is a part of the AMPnet crowdfunding project. Service contains blockchain wallet data for users, organizations and projects. 
Using gRPC, service is connected to other crowdfunding services:
* [user service](https://github.com/AMPnet/user-service)
* [project service](https://github.com/AMPnet/project-service)
* [mail service](https://github.com/AMPnet/mail-service)
* [blockchain service](https://github.com/AMPnet/ampnet-ae-middleware)

## Requirements

Service must have running and initialized database. Default database url is `locahost:5432`.
To change database url set configuration: `spring.datasource.url` in file `application.properties`.
To initialize database run script in the project root folder:

```sh
./initialize-local-database.sh
```

## Start

Application is running on port: `8128`. To change default port set configuration: `server.port`.

### Build

```sh
./gradlew build
```

### Run

```sh
./gradlew bootRun
```

After starting the application, API documentation is available at: `localhost:8128/docs/index.html`.
If documentation is missing generate it by running gradle task:
```sh
./gradlew copyDocs
```

### Test

```sh
./gradlew test
```
