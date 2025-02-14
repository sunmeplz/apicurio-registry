/*
 * Copyright 2022 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.registry;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@QuarkusTest
public class MigrationTest extends AbstractResourceTestBase {


    @Test
    public void migrateData() throws Exception {

        InputStream originalData = getClass().getResource("rest/v2/destination_original_data.zip").openStream();
        InputStream migratedData = getClass().getResource("rest/v2/migration_test_data_dump.zip").openStream();

        clientV2.admin().importEscaped().post(originalData, config -> {
            // TODO: this header should be injected by Kiota
            config.headers.add("Content-Type", "application/zip");
        }).get(10, TimeUnit.SECONDS);
        clientV2.admin().importEscaped().post(migratedData, config -> {
            // TODO: this header should be injected by Kiota
            config.headers.add("Content-Type", "application/zip");
            config.headers.add("X-Registry-Preserve-GlobalId", "false");
            config.headers.add("X-Registry-Preserve-ContentId", "false");
        }).get(40, TimeUnit.SECONDS);
    }
}
