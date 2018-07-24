package org.dhatim.fs.base;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.apache.sshd.common.util.GenericUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractVirtualFileSystemProvider extends FileSystemProvider {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    private final HashMap<URI, AbstractVirtualFileSystem> fileSystems = new HashMap<>();
    
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        synchronized (fileSystems) {
            if (fileSystems.containsKey(uri)) {
                throw new FileSystemAlreadyExistsException();
            }
            AbstractVirtualFileSystem vfs = createFileSystem(uri, env);
            fileSystems.put(uri, vfs);
            return vfs;
        }
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        synchronized (fileSystems) {
            AbstractVirtualFileSystem fs = fileSystems.get(uri);
            if (fs == null) {
                throw new FileSystemNotFoundException();
            }
            return fs;
        }
    }
    
    void removeFileSystem(AbstractVirtualFileSystem virtualFileSystem) {
        synchronized (fileSystems) {
            fileSystems.remove(virtualFileSystem.getURI());
        }
    }

    @Override
    public Path getPath(URI uri) {
        FileSystem fs = getFileSystem(uri);
        return fs.getPath(uri.getPath());
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        log.trace("provider.newByteChannel {}", path);
        VirtualPath vPath = toVirtualPath(path);
        return vPath.getFileSystem().newFileChannel(vPath, options, attrs);
    }
    
    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        log.trace("provider.newFileChannel {}", path);
        VirtualPath vPath = toVirtualPath(path);
        return vPath.getFileSystem().newFileChannel(vPath, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        log.trace("provider.newDirectoryStream {}", dir);
        VirtualPath vDir = toVirtualPath(dir);
        return vDir.getFileSystem().newDirectoryStream(vDir, filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        log.trace("provider.createDirectory {}", dir);
        VirtualPath vDir = toVirtualPath(dir);
        vDir.getFileSystem().createDirectory(vDir, attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        log.trace("provider.delete {}", path);
        VirtualPath vPath = toVirtualPath(path);
        vPath.getFileSystem().delete(vPath);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        log.trace("provider.copy {} {}", source, target);
        VirtualPath src = toVirtualPath(source);
        VirtualPath dst = toVirtualPath(target);
        if (src.getFileSystem() != dst.getFileSystem()) {
            throw new ProviderMismatchException("Mismatched file system providers for " + src + " vs. " + dst);
        }
        src.getFileSystem().copy(src, dst, options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        log.trace("provider.move {} {}", source, target);
        VirtualPath src = toVirtualPath(source);
        VirtualPath dst = toVirtualPath(target);
        if (src.getFileSystem() != dst.getFileSystem()) {
            throw new ProviderMismatchException("Mismatched file system providers for " + src + " vs. " + dst);
        }
        src.getFileSystem().move(src, dst, options);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        log.trace("provider.isSameFile {} {}", path, path2);
        VirtualPath p1 = toVirtualPath(path);
        VirtualPath p2 = toVirtualPath(path2);
        if (p1.getFileSystem() != p2.getFileSystem()) {
            throw new ProviderMismatchException("Mismatched file system providers for " + p1 + " vs. " + p2);
        }
        checkAccess(p1);
        checkAccess(p2);
        return p1.equals(p2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        log.trace("provider.isHidden {}", path);
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        log.trace("provider.getFileStore {}", path);
        VirtualPath vPath = toVirtualPath(path);
        return vPath.getFileSystem().getFileStores().iterator().next();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        log.trace("provider.checkAccess {}", path);
        VirtualPath p = toVirtualPath(path);
        boolean w = false;
        boolean x = false;
        if (modes.length > 0) {
            for (AccessMode mode : modes) {
                switch (mode) {
                    case READ:
                        break;
                    case WRITE:
                        w = true;
                        break;
                    case EXECUTE:
                        x = true;
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported mode: " + mode);
                }
            }
        }

        BasicFileAttributes attrs = getFileAttributeView(p, BasicFileAttributeView.class).readAttributes();
        if ((attrs == null) && !(p.isAbsolute() && p.getNameCount() == 0)) {
            throw new NoSuchFileException(path.toString());
        }

        AbstractVirtualFileSystem fs = p.getFileSystem();
        if (x || (w && fs.isReadOnly())) {
            throw new AccessDeniedException("Filesystem is read-only: " + path.toString());
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        log.trace("provider.getFileAttributeView {} for type {}", path, type);
        if (isSupportedFileAttributeView(path, type)) {
            VirtualPath vPath = toVirtualPath(path);
            if (BasicFileAttributeView.class.isAssignableFrom(type) || FileOwnerAttributeView.class.isAssignableFrom(type) || PosixFileAttributeView.class.isAssignableFrom(type)) {
                return type.cast(vPath.getFileSystem().getFileAttributeView(vPath, options));
            } 
        }
        throw new UnsupportedOperationException("getFileAttributeView(" + path + ") view not supported: " + type.getSimpleName());
    }
    
    private static boolean isSupportedFileAttributeView(Path path, Class<? extends FileAttributeView> type) {
        return isSupportedFileAttributeView(toVirtualPath(path).getFileSystem(), type);
    }

    private static boolean isSupportedFileAttributeView(AbstractVirtualFileSystem fs, Class<? extends FileAttributeView> type) {
        boolean result;
        Collection<String> views = fs.supportedFileAttributeViews();
        if ((type == null) || (views == null) || views.isEmpty()) {
            result = false;
        } else if (PosixFileAttributeView.class.isAssignableFrom(type)) {
            result = views.contains("posix");
        } else if (FileOwnerAttributeView.class.isAssignableFrom(type)) {
            result = views.contains("owner");
        } else if (BasicFileAttributeView.class.isAssignableFrom(type)) {
            result = views.contains("basic"); // must be last
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        log.trace("provider.readAttributes {} for type {}", path, type);
        if (type.isAssignableFrom(PosixFileAttributes.class) || type.isAssignableFrom(BasicFileAttributes.class)) {
            return type.cast(getFileAttributeView(path, PosixFileAttributeView.class, options).readAttributes());
        } 
        throw new UnsupportedOperationException("readAttributes(" + path + ")[" + type.getSimpleName() + "] N/A");
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        String view;
        String attrs;
        int i = attributes.indexOf(':');
        if (i == -1) {
            view = "basic";
            attrs = attributes;
        } else {
            view = attributes.substring(0, i++);
            attrs = attributes.substring(i);
        }

        return readAttributes(path, view, attrs, options);
    }
    
    private Map<String, Object> readAttributes(Path path, String view, String attrs, LinkOption... options) throws IOException {
        VirtualPath p = toVirtualPath(path);
        AbstractVirtualFileSystem fs = p.getFileSystem();
        Collection<String> views = fs.supportedFileAttributeViews();
        if (GenericUtils.isEmpty(views) || (!views.contains(view))) {
            throw new UnsupportedOperationException("readAttributes(" + path + ")[" + view + ":" + attrs + "] view not supported: " + views);
        }

        if ("basic".equalsIgnoreCase(view) || "posix".equalsIgnoreCase(view) || "owner".equalsIgnoreCase(view)) {
            return readPosixViewAttributes(p, view, attrs, options);
        } else  {
            throw new UnsupportedOperationException("readCustomViewAttributes(" + path + ")[" + view + ":" + attrs + "] view not supported");
        }
    }
    
    private Map<String, Object> readPosixViewAttributes(VirtualPath path, String view, String attrs, LinkOption... options) throws IOException {
        PosixFileAttributes v = readAttributes(path, PosixFileAttributes.class, options);
        if ("*".equals(attrs)) {
            attrs = "lastModifiedTime,lastAccessTime,creationTime,size,isRegularFile,isDirectory,isSymbolicLink,isOther,fileKey,owner,permissions,group";
        }

        Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String attr : attrs.split(",")) {
            switch (attr) {
                case "lastModifiedTime":
                    map.put(attr, v.lastModifiedTime());
                    break;
                case "lastAccessTime":
                    map.put(attr, v.lastAccessTime());
                    break;
                case "creationTime":
                    map.put(attr, v.creationTime());
                    break;
                case "size":
                    map.put(attr, v.size());
                    break;
                case "isRegularFile":
                    map.put(attr, v.isRegularFile());
                    break;
                case "isDirectory":
                    map.put(attr, v.isDirectory());
                    break;
                case "isSymbolicLink":
                    map.put(attr, v.isSymbolicLink());
                    break;
                case "isOther":
                    map.put(attr, v.isOther());
                    break;
                case "fileKey":
                    map.put(attr, v.fileKey());
                    break;
                case "owner":
                    map.put(attr, v.owner());
                    break;
                case "permissions":
                    map.put(attr, v.permissions());
                    break;
                case "group":
                    map.put(attr, v.group());
                    break;
                default:
                    log.debug("readPosixViewAttributes({})[{}:{}] ignored for {}", path, view, attr, attrs);
            }
        }
        return map;
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        String view;
        String attr;
        int i = attribute.indexOf(':');
        if (i == -1) {
            view = "basic";
            attr = attribute;
        } else {
            view = attribute.substring(0, i++);
            attr = attribute.substring(i);
        }

        setAttribute(path, view, attr, value, options);
    }
    
    private void setAttribute(Path path, String view, String attr, Object value, LinkOption... options) throws IOException {
        log.trace("provider.setAttribute {}", path);
        VirtualPath p = toVirtualPath(path);
        AbstractVirtualFileSystem fs = p.getFileSystem();
        Collection<String> views = fs.supportedFileAttributeViews();
        if (GenericUtils.isEmpty(views) || (!views.contains(view))) {
            throw new UnsupportedOperationException("setAttribute(" + path + ")[" + view + ":" + attr + "=" + value + "] view " + view + " not supported: " + views);
        }

        PosixFileAttributeView v = fs.getFileAttributeView(p, options);
        switch (attr) {
            case "lastModifiedTime":
                v.setTimes((FileTime) value, null, null);
                break;
            case "lastAccessTime":
                v.setTimes(null, (FileTime) value, null);
                break;
            case "creationTime":
                v.setTimes(null, null, (FileTime) value);
                break;
            case "permissions":
                @SuppressWarnings("unchecked")
                Set<PosixFilePermission> attrSet = (Set<PosixFilePermission>) value;
                v.setPermissions(attrSet);
                break;
            case "owner":
                v.setOwner((UserPrincipal) value);
                break;
            case "group":
                v.setGroup((GroupPrincipal) value);
                break;
            case "acl":
            case "isRegularFile":
            case "isDirectory":
            case "isSymbolicLink":
            case "isOther":
            case "fileKey":
            case "size":
                throw new UnsupportedOperationException("setAttribute(" + path + ")[" + view + ":" + attr + "=" + value + "] modification N/A");
            default:
                if (log.isTraceEnabled()) {
                    log.trace("setAttribute({})[{}] ignore {}:{}={}", fs, path, view, attr, value);
                }
        }
    }
    
    private static VirtualPath toVirtualPath(Path path) {
        Objects.requireNonNull(path, "No path provided");
        if (!(path instanceof VirtualPath)) {
            throw new ProviderMismatchException("Path is not virtual: " + path);
        }
        return (VirtualPath) path;
    }
    
    protected abstract AbstractVirtualFileSystem createFileSystem(URI uri, Map<String, ?> env) throws IOException;

}
