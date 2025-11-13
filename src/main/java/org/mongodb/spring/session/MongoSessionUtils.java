/*
 * Copyright 2025-present MongoDB, Inc.
 * Copyright 2014-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.spring.session;

import com.mongodb.ConnectionString;
import com.mongodb.DBObject;
import org.bson.Document;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * Utility for MongoSession.
 *
 * @author Greg Turnquist
 */
final class MongoSessionUtils {

    private static final String DEFAULT_URI = "mongodb://localhost:27017";
    private static final String URI_SYSTEM_PROPERTY_NAME = "org.mongodb.test.uri";
    public static final String DEFAULT_DATABASE_NAME = "MongoSpringSessionTest";

    private MongoSessionUtils() {}

    @Nullable static DBObject convertToDBObject(AbstractMongoSessionConverter mongoSessionConverter, MongoSession session) {

        return (DBObject) mongoSessionConverter.convert(
                session, TypeDescriptor.valueOf(MongoSession.class), TypeDescriptor.valueOf(DBObject.class));
    }

    @Nullable static MongoSession convertToSession(AbstractMongoSessionConverter mongoSessionConverter, Document session) {

        return (MongoSession) mongoSessionConverter.convert(
                session, TypeDescriptor.valueOf(Document.class), TypeDescriptor.valueOf(MongoSession.class));
    }

    static ConnectionString getConnectionString() {
        String connectionString = System.getProperty(URI_SYSTEM_PROPERTY_NAME, DEFAULT_URI);
        return new ConnectionString(connectionString);
    }
}
