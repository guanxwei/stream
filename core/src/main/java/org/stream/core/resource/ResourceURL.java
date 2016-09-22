package org.stream.core.resource;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Encapsulation of resource uri, used to uniquely mark a resource.
 */
@AllArgsConstructor
@Data
public class ResourceURL {

    private String path;

    private ResourceAuthority resourceAuthority;

}
