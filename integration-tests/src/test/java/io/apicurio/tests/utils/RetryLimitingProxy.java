/*
 * Copyright 2023 Red Hat
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

package io.apicurio.tests.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Block `n` first requests, then allow the rest through.
 * 
 * @author Carles Arnal
 * @author Jakub Senko <em>m@jsenko.net</em>
 */
public class RetryLimitingProxy extends LimitingProxy {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private int failures;

    public RetryLimitingProxy(int failures, String destinationHost, int destinationPort) {
        super(destinationHost, destinationPort);
        this.failures = failures;
    }

    @Override
    protected boolean allowed() {
        if (failures > 0) {
            failures--;
            return false;
        }
        return true;
    }
}