package org.stream.core.resource;

import lombok.Data;
import lombok.NonNull;

/**
 * Encapsulation of logical resource authority of a resource that can be used by Stream workflow nodes.
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
