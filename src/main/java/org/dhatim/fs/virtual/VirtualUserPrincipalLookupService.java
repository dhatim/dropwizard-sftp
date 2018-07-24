package org.dhatim.fs.virtual;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;

public abstract class VirtualUserPrincipalLookupService extends UserPrincipalLookupService {

    public abstract UserPrincipal lookupUserPrincipal(VirtualFile file) throws IOException;
    public abstract GroupPrincipal lookupGroupPrincipal(VirtualFile file) throws IOException;

}
