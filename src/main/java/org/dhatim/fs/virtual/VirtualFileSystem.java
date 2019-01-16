package org.dhatim.fs.virtual;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.dhatim.fs.base.AbstractVirtualFileSystem;
import org.dhatim.fs.base.AbstractVirtualFileSystemProvider;
import org.dhatim.fs.base.VirtualPath;
import org.dhatim.fs.util.BasicDirectoryStream;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public abstract class VirtualFileSystem extends AbstractVirtualFileSystem {

    public class AttributeView implements PosixFileAttributeView {

        private final VirtualPath path;

        public AttributeView(VirtualPath path) {
            this.path = path;
        }

        @Override
        public String name() {
            return "posix";
        }

        @Override
        public PosixFileAttributes readAttributes() throws IOException {
            VirtualUserPrincipalLookupService lookupService = getUserPrincipalLookupService();
            VirtualFile file = resolve(path);
            return new VirtualPosixFileAttributes(file, lookupService.lookupUserPrincipal(file), lookupService.lookupGroupPrincipal(file));
        }

        @Override
        public UserPrincipal getOwner() throws IOException {
            return getUserPrincipalLookupService().lookupUserPrincipal(resolve(path));
        }

        @Override
        public void setOwner(UserPrincipal owner) throws IOException {
            resolve(path).setOwner(owner);
        }

        @Override
        public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
            resolve(path).setTimes(lastModifiedTime, lastAccessTime, createTime);
        }

        @Override
        public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
            resolve(path).setPermissions(perms);
        }

        @Override
        public void setGroup(GroupPrincipal group) throws IOException {
            resolve(path).setGroup(group);
        }

    }

    private VirtualDirectory root;
    private VirtualPath rootPath = create("/");

    private final LoadingCache<VirtualPath, VirtualFile> cache = CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<VirtualPath, VirtualFile>() {
        @Override
        public VirtualFile load(VirtualPath key) throws Exception {
            if (key.equals(rootPath)) {
                return root;
            } else {
                VirtualFile parent = cache.get(key.getParent());
                if (parent instanceof VirtualDirectory) {
                    return ((VirtualDirectory) parent).find(key.getFileName().toString()).orElseThrow(() -> new NoSuchFileException(key.toString()));
                } else {
                    throw new IllegalStateException();
                }
            }
        }
    });

    public VirtualFileSystem(AbstractVirtualFileSystemProvider fileSystemProvider, URI uri) {
        super(fileSystemProvider, uri);
    }

    protected final void setRoot(VirtualDirectory newRoot) {
        this.root = newRoot;
        this.rootPath = create("/");
    }

    @Override
    protected void free() {
        cache.cleanUp();
    }

    @Override
    protected DirectoryStream<Path> newDirectoryStream(VirtualPath vDir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        log.debug("treefs.newDirectoryStream {}", vDir);
        VirtualDirectory directory = asDir(resolve(vDir));
        return new BasicDirectoryStream<>(directory.getChildren(), filter, this::create);
    }

    @Override
    protected void createDirectory(VirtualPath vDir, FileAttribute<?>... attrs) throws IOException {
        log.debug("treefs.createDirectory {}", vDir);
        throw new UnsupportedOperationException("createDirectory");
    }

    @Override
    protected void delete(VirtualPath vPath) throws IOException {
        log.debug("treefs.delete {}", vPath);
        throw new UnsupportedOperationException("delete");
    }

    @Override
    protected void copy(VirtualPath src, VirtualPath dst, CopyOption... options) throws IOException {
        log.debug("treefs.copy {} {}", src, dst);
        throw new UnsupportedOperationException("copy");
    }

    @Override
    protected void move(VirtualPath src, VirtualPath dst, CopyOption... options) throws IOException {
        log.debug("treefs.move {} {}", src, dst);
        throw new UnsupportedOperationException("move");
    }

    @Override
    protected FileChannel newFileChannel(VirtualPath vPath, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        log.debug("treefs.newFileChannel {} with {}", vPath, options);
        if (isNewFile(options)) {
            VirtualPath parentPath = vPath.getParent();
            VirtualDirectory dir = asDir(resolve(parentPath));
            return dir.createFile(vPath, options);
        } else {
            VirtualFile file = resolve(vPath);
            return file.open(options);
        }
    }

    @Override
    protected PosixFileAttributeView getFileAttributeView(VirtualPath path, LinkOption... options) {
        log.debug("treefs.getPosixFileAttributeView {}", path);
        return new AttributeView(path);
    }

    @Override
    public abstract VirtualUserPrincipalLookupService getUserPrincipalLookupService();

    private VirtualPath create(VirtualFile element) {
        Objects.requireNonNull(root);
        List<String> list = new ArrayList<>();
        VirtualFile current = element;
        while (current != root) {
            list.add(current.getName());
            current = current.getParent();
        }
        Collections.reverse(list);
        return create("/", list);
    }

    protected VirtualFile resolve(VirtualPath path) throws IOException {
        try {
            return cache.get(path.toAbsolutePath());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw new IOException(e);
            }
        }
    }

    private static VirtualDirectory asDir(VirtualFile file) throws IOException {
        if (file instanceof VirtualDirectory) {
            return (VirtualDirectory) file;
        } else {
            throw new IOException(file.getName() + " is not a directory");
        }
    }

    private static boolean isNewFile(Set<? extends OpenOption> options) {
        return options.contains(WRITE) && (options.contains(CREATE) || options.contains(CREATE_NEW) || options.contains(TRUNCATE_EXISTING));
    }

}
