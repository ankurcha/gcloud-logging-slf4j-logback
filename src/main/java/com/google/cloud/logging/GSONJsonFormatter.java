/**
 * Copyright (C) 2016 - Ankur Chauhan <ankur@malloc64.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.logging;

import ch.qos.logback.contrib.json.JsonFormatter;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;

/**
 * Jackson-specific implementation of the {@link JsonFormatter}.
 */
public class GSONJsonFormatter implements JsonFormatter {
    private Gson gson;

    public GSONJsonFormatter() {
        this.gson = new Gson();
    }

    @Override
    public String toJsonString(Map m) throws IOException {
        return gson.toJson(m);
    }
}
