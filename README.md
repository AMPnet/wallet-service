# Wallet Service

![Release](https://github.com/AMPnet/wallet-service/workflows/Release/badge.svg?branch=master) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/6f9d691a5abe49469ede30a68ae20e8a)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=AMPnet/wallet-service&amp;utm_campaign=Badge_Grade) [![codecov](https://codecov.io/gh/AMPnet/wallet-service/branch/master/graph/badge.svg)](https://codecov.io/gh/AMPnet/wallet-service)

Wallet service is a part of the AMPnet crowdfunding project. Service contains blockchain wallet data for users, organizations and projects.
Using gRPC, service connects to other crowdfunding services:

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

## Application Properties

### Online Storage

Online storage uses for storing project data like documents and images. It is implemented using AWS SDK S3 library which relies on following environment variables:

  * `AWS_ACCESS_KEY_ID`
  * `AWS_SECRET_ACCESS_KEY`

Set online storage URL: `com.ampnet.walletservice.file-storage.url`

### JWT

Set public key property to verify JWT: `com.ampnet.walletservice.jwt.public-key`
