/*
 * Copyright (C) 2021 guanxiongwei
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

package org.stream.core.resource;

import lombok.Data;
import lombok.NonNull;

/**
 * Encapsulation of logical resource authority of a resource that can be used by Stream work-flow.
 */
@Data
public class ResourceAuthority {

    /**
     * String value representing the resource authority.
     */
    @NonNull
    private String value;

    /**
     * Class value representing the resource class.
     */
    @NonNull
    private Class<?> clazz;

}
