package com.orientsec.idap.core.base;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;

/** Test bootstrap that preserves IDAP package boundaries without enabling Dubbo. */
@SpringBootApplication(
        scanBasePackageClasses = {
            com.orientsec.idap.core.PackageBase.class,
            com.orientsec.genesis.auth.PackageBase.class
        },
        exclude = {
            DataSourceAutoConfiguration.class,
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            WebFluxAutoConfiguration.class
        })
public class IdapTestServer {}
