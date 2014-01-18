/*
 * Copyright 2013 That Amazing Web Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taw.dashtube.model;

import com.google.api.client.util.Key;
import com.google.api.client.xml.GenericXml;

/**
 * Simple class encapsulating the &lt;Line&gt; element.
 */
public class Line extends GenericXml {
    @Key("@ID")
    public String id;
    @Key("@Name")
    public String name;
}
