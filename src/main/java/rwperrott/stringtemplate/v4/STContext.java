package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.stringtemplate.v4.*;
import org.stringtemplate.v4.misc.STMessage;
import org.stringtemplate.v4.misc.STRuntimeMessage;
import org.stringtemplate.v4.misc.STRuntimeMessagePatch;

import java.io.Closeable;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

import static java.lang.String.format;

/**
 * Contains lazy cache maps for start line numbers of templates in files, and for class name to Class lookup.
 * <p>
 * Provides methods to register AttributeRenders and ModelAdapters in an STGroup, and to try and replace a STMessage
 * with a relative line numbers for one with an absolute line number.
 *
 * @author rwperrott
 */
@SuppressWarnings("unused")
public class STContext implements Closeable {
    private final Map<Object, Object2IntMap<String>> stGroupMetaMap = new HashMap<>();
    private final BiConsumer<String, Throwable> errorLog;
    private final ClassLoader classLoader;
    private final Map<String, Class<?>> classCache = new HashMap<>();

    public STContext(final BiConsumer<String, Throwable> errorLog) {
        this(Thread.currentThread().getContextClassLoader(), errorLog);
    }

    public STContext(final ClassLoader classLoader,
                     final BiConsumer<String, Throwable> errorLog) {
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(errorLog, "errorLog");

        this.classLoader = classLoader;
        this.errorLog = errorLog;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() throws IOException {
        synchronized (stGroupMetaMap) {
            stGroupMetaMap.clear();
        }
        synchronized (classCache) {
            classCache.clear();
        }
        synchronized (supportedPackagePrefixes) {
            supportedPackagePrefixes.clear();
        }
    }

    public final void registerRenderers(final STGroup stGroup, Map<String, String> map) {
        registerAttributePlugins(stGroup,
                                 map,
                                 AttributeRenderer.class,
                                 stGroup::registerRenderer);
    }

    private <T> void registerAttributePlugins(
            final STGroup stGroup,
            final Map<String, String> map,
            final Class<T> pluginType,
            final BiConsumer<Class<?>, T> registerer) {
        Objects.requireNonNull(stGroup, "stGroup");
        Objects.requireNonNull(pluginType, "pluginType");
        Objects.requireNonNull(registerer, "registerer");
        if (null == map) {
            return;
        }
        boolean failed = false;
        for (Map.Entry<String, String> e : map.entrySet()) {
            final Class<?> keyType = getClass("attributeType", e.getKey(), Object.class);
            final T valueInstance = instanceOf(pluginType.getSimpleName(), e.getValue(), pluginType);
            if (null != keyType && null != valueInstance) {
                registerer.accept(keyType, valueInstance);
            } else {
                failed = true;
            }
        }
        if (failed) {
            throw new IllegalArgumentException(format("Failed to register all of the %ss", pluginType));
        }
    }

    // Used by Group to get type for registerRenderer and registerModelAdaptor
    // Used by instanceOf
    private Class<?> getClass(final String label, final String className, Class<?> type) {
        // Make operation full atomic to avoid pointless puts
        synchronized (classCache) {
            Class<?> cls = classCache.get(className);
            if (null != cls) {
                return cls;
            }

            // Expand simple names for java.lang package
            final int p = className.lastIndexOf('.');
            if (-1 == p) {
                for (String packagePrefix : supportedPackagePrefixes)
                    try {
                        final String key = packagePrefix + className;
                        cls = classLoader.loadClass(key); // With default ClassLoader
                        if (!type.isAssignableFrom(cls)) {
                            errorLog.accept(format("%s '%s' not an instanceof %s",
                                                   label, className, type.getTypeName()), null);
                            cls = null;
                        }
                        classCache.put(className, cls);
                        // Also cache full key
                        classCache.put(key, cls);
                        return cls;
                    } catch (ClassNotFoundException ignore) {
                    }
            } else {
                try {
                    cls = classLoader.loadClass(className);
                    if (!type.isAssignableFrom(cls)) {
                        errorLog.accept(format("%s '%s' not an instanceof %s",
                                               label, className, type.getTypeName()), null);
                        cls = null;
                    }
                } catch (ClassNotFoundException ex) {
                    errorLog.accept(format("%s '%s' not found",
                                           label, className), ex);
                    cls = null;
                }
            }
            classCache.put(className, cls);
            if (supportedPackagePrefixes.contains(className.substring(p + 1)))
                classCache.put(className.substring(p + 1), cls);
            //
            return cls;
        }
    }

    // Used by Group to create instances of AttributeRender and ModelAdapter
    @SuppressWarnings({"unchecked", "UseSpecificCatch"})
    private <T> T instanceOf(final String label, final String className, Class<?> type) {
        final Class<?> cls = getClass(label, className, type);
        try {
            return (T) cls.getConstructor().newInstance();
        } catch (Exception ex) {
            errorLog.accept(format("%s '%s' (%s) instance construction failed",
                                   label, className, type.getTypeName()), null);
            return null;
        }
    }

    /**
     * Allow more package prefixes to be added, for class name to Class lookup.
     *
     * @param packagePrefix .
     */
    public void addPackagePrefix(String packagePrefix) {
        packagePrefix = Objects.requireNonNull(packagePrefix, "packagePrefix");
        if (packagePrefix.isEmpty())
            throw new IllegalArgumentException("packagePrefix is blank");
        if (packagePrefix.indexOf('.') == -1)
            packagePrefix = packagePrefix.concat(".");
        synchronized (supportedPackagePrefixes) {
            supportedPackagePrefixes.add(packagePrefix);
        }
    }

    public final void registerModelAdaptors(final STGroup stGroup, Map<String, String> map) {
        registerAttributePlugins(stGroup,
                                 map,
                                 ModelAdaptor.class,
                                 stGroup::registerModelAdaptor);
    }

    public STMessage patch(STMessage msg, String encoding) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(encoding, "encoding");
        if (msg instanceof STRuntimeMessage) {
            final STRuntimeMessage strm = (STRuntimeMessage) msg;
            InstanceScope scope = strm.scope;
            if (null != scope) {
                do {
                    final ST st = scope.st;
                    final String name = st.getName().substring(1);
                    final STGroup stGroup = st.groupThatCreatedThisInstance;
                    final int n = getTemplateLineNumber(stGroup, encoding, name);
                    if (n != -1)
                        return new STRuntimeMessagePatch((STRuntimeMessage) msg, n);
                    scope = scope.parent;
                } while (scope != null);
                scope = strm.scope;
                final ST st = scope.st;
                final String name = st.getName().substring(1);
                final STGroup stGroup = st.groupThatCreatedThisInstance;
                throw new IllegalStateException(format("start line not found for template %s in %s:%s",
                                                       name, stGroup.getClass().getSimpleName(), STGroupType.of(stGroup).getSource(stGroup)));
            }
        }
        // Probably don't need to patch
        // STCompiletimeMessage, STGroupCompiletimeMessage and STLexerMessage,
        // because they get the line number from Token.
        return msg;
    }

    private int getTemplateLineNumber(final STGroup stGroup, final String encoding, final String templateName) {
        final STGroupType type = STGroupType.of(stGroup);
        final Object templateSource;
        try {
            templateSource = type.getTemplateSource(stGroup, templateName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        synchronized (stGroupMetaMap) {
            return stGroupMetaMap.computeIfAbsent(templateSource, k -> {
                final Object2IntMap<String> templateLineNumbers = new Object2IntLinkedOpenHashMap<>();
                try (final LineNumberReader lnr = new LineNumberReader(type.openReader(stGroup, templateSource, encoding))) {
                    lnr.setLineNumber(1);
                    for (String line; (line = lnr.readLine()) != null; ) {
                        final Matcher m = STUtils.templateMatcher(line);
                        if (m.matches()) {
                            templateLineNumbers.put(m.group(1), lnr.getLineNumber());
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return templateLineNumbers;
            }).getOrDefault(templateName, -1);
        }
    }

    private static final Set<String> supportedPackagePrefixes =
            new HashSet<>(Arrays.asList(
                    "java.lang.",
                    "java.util."
                                       ));
}