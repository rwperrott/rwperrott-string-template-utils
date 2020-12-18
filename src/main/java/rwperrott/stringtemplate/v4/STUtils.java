package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.ST;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Paths.get;
import static java.util.regex.Pattern.*;
import static org.stringtemplate.v4.STGroup.GROUP_FILE_EXTENSION;
import static rwperrott.stringtemplate.v4.STGroupType.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class STUtils {

    private STUtils() {
    }

    /**
     * Used by plugin
     */
    public static class TypeAndURL {

        public final STGroupType type;
        public final URL url;

        private TypeAndURL(STGroupType type, URL url) {
            this.type = type;
            this.url = url;
        }

    }

    private static final Pattern TEMPLATE_PATTERN = compile("^([a-z][^( :]*)\\([^ :)]*\\) *::= *.*$", CASE_INSENSITIVE | MULTILINE);

    /**
     * Used by plugin
     */
    @SuppressWarnings({"UseSpecificCatch", "unused"})
    public static TypeAndURL resolveTypeAndURL(final @NonNull String source,
                                               final @NonNull Path defaultDir) throws IOException {
        final STGroupType type;
        // Check contains a template String
        if (STUtils.templateMatcher(source).find()) {
            return new TypeAndURL(string, null);
        }

        // Must be a URL or a filesystem path
        Path path;
        if (".".equals(source)) {
            path = defaultDir;
            type = directory;
        } else {
            type = source.endsWith(GROUP_FILE_EXTENSION)
                   ? file
                   : directory;
            // Try direct conversion to a URL
            try {
                return new TypeAndURL(type, new URI(source).normalize().toURL());
            } catch (Exception ignore) {
            }

            // Resolve relative filesystem path
            path = get(source);
            if (!path.isAbsolute()) {
                path = defaultDir.resolve(path);
            }
        }

        // Validate filesystem paths
        path = path.toRealPath();
        if (!exists(path)) {
            throw new FileNotFoundException(path.toString());
        }
        if (type == file && !isRegularFile(path)) {
            throw new IOException(source + " is not a file");
        }

        // Convert to URL, for STGroup creation
        return new TypeAndURL(type, path.toUri().toURL());
    }

    /**
     * Used to identify a source string as a template, and to identity and extract names for start line number mapping.
     * Group 1 is the name of the template.
     */
    public static Matcher templateMatcher(final @NonNull CharSequence cs) {
        return TEMPLATE_PATTERN.matcher(Objects.requireNonNull(cs, "cs"));
    }

    @SuppressWarnings("unchecked")
    public static <V> Map<String, V> validateAttributes(final @NonNull Map<?, ?> map,
                                                        final @NonNull String label,
                                                        final int checkDepth) {
        map.forEach((k, v) -> {
            if (k.getClass() != String.class)
                throw new IllegalArgumentException(format("non-String key %s:%s in %s",
                                                          k.getClass().getName(), k.toString(), label));
            if (checkDepth > 0 && v instanceof Map) {
                validateAttributes((Map<?, ?>) v, label, checkDepth - 1);
            }
        });
        return (Map<String, V>) map;
    }

    public static void clearAttributes(final @NonNull ST st, Map<String, ?> attributes) {
        removeAttributes(st, st.getAttributes());
    }

    public static void removeAttributes(final @NonNull ST st, Map<String, ?> attributes) {
        if (null != attributes)
            attributes.forEach((k, v) -> st.remove(k));
    }

    public static void applyAttributes(final @NonNull ST st, Map<String, ?> attributes) {
        if (null != attributes)
            attributes.forEach((k, v) -> {
                if (null == v)
                    st.remove(k);
                else
                    st.add(k, v);
            });
    }
}
