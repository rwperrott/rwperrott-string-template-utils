package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.NonNull;
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
import static rwperrott.stringtemplate.v4.Utils.fmt;

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
    public final BiConsumer<String, Throwable> errorLog;
    private final Map<Object, Object2IntMap<String>> stGroupMetaMap = new HashMap<>();
    private final ClassLoader classLoader;
    private final Map<String, Class<?>> classCache = new HashMap<>();

    public STContext(final BiConsumer<String, Throwable> errorLog) {
        this(Thread.currentThread().getContextClassLoader(), errorLog);
    }

    public STContext(final @NonNull ClassLoader classLoader,
                     final @NonNull BiConsumer<String, Throwable> errorLog) {
        this.classLoader = classLoader;
        this.errorLog = errorLog;
    }

    /**
     * Add default packages and classes.
     * <p>
     * This is separate, to allow testing with full class-names.
     */
    public void addDefaults() {
        for (String packageName : DEFAULT_PACKAGE_NAMES)
            addPackage(packageName);
        for (Class<?> cls : DEFAULT_CACHED_CLASSES)
            addClass0(cls);
    }

    /**
     * Allow more package prefixes to be added, for class name to Class lookup.
     *
     * @param packageName .
     */
    public void addPackage(@NonNull String packageName) {
        if (packageName.isEmpty())
            throw new IllegalArgumentException("packageName is blank");
        // No more validation is necessary, because classLoader won't find a stupid package name.
        synchronized (PACKAGE_NAMES) {
            PACKAGE_NAMES.add(packageName);
        }
    }

    private void addClass0(Class<?> cls) {
        final String className = cls.getName();
        cacheClass(className, cls, className.lastIndexOf('.'));
    }

    private void cacheClass(String className, Class<?> cls, int p) {
        classCache.put(className, cls);
        final String packagePrefix = className.substring(p + 1);
        if (PACKAGE_NAMES.contains(className.substring(p + 1)))
            classCache.put(className.substring(p + 1), cls);
    }

    /**
     * Should be called after all addPackage calls.
     *
     * @param cls a class to register
     */
    public void addClass(Class<?> cls) {
        synchronized (classCache) {
            addClass0(cls);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() throws IOException {
        synchronized (stGroupMetaMap) {
            stGroupMetaMap.clear();
        }
    }

    private <T> void registerAttributePlugin(
            final STGroup stGroup,
            final String attributeType,
            final String pluginClassName,
            final Class<T> pluginType,
            final BiConsumer<Class<?>, T> registerer) {
        final Class<?> keyType = getClass("attributeType", attributeType, Object.class);
        final T valueInstance = instanceOf(pluginType.getSimpleName(), pluginClassName, pluginType);
        registerer.accept(keyType, valueInstance);
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
                Exception fail = null; // Store them all, maybe useful for a duf package name
                for (String packageName : PACKAGE_NAMES) {
                    final String key = fmt().concat(packageName, ".", className);
                    try {
                        cls = classLoader.loadClass(key);
                        if (!type.isAssignableFrom(cls)) {
                            throw new IllegalArgumentException(format("%s '%s' not an instanceof %s",
                                                                      label, className, type.getTypeName()), null);
                        }
                        classCache.put(className, cls);
                        // Also cache full key
                        classCache.put(key, cls);
                        return cls;
                    } catch (ClassNotFoundException ex) {
                        if (null == fail)
                            fail = ex;
                        else
                            fail.addSuppressed(ex);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(format("unexpected failure for label \"%s\" className: \"%s.%s\" ",
                                                                  label, packageName, className), e);
                    }
                }
                throw new IllegalArgumentException(format("%s '%s' not found",
                                                          label, className), fail);
            } else {
                try {
                    cls = classLoader.loadClass(className);
                    if (!type.isAssignableFrom(cls)) {
                        throw new IllegalArgumentException(format("%s '%s' not an instanceof %s",
                                                                  label, className, type.getTypeName()));
                    }
                } catch (ClassNotFoundException ex) {
                    throw new IllegalArgumentException(format("%s '%s' not found", label, className), ex);
                }
            }
            cacheClass(className, cls, p);
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
            throw new IllegalStateException(format("%s '%s' (%s) instance construction failed",
                                                   label, className, type.getTypeName()), ex);
        }
    }

    public final void registerRenderer(final STGroup stGroup,
                                       final String attributeType,
                                       final String rendererClassName) {
        registerAttributePlugin(stGroup,
                                attributeType,
                                rendererClassName,
                                AttributeRenderer.class,
                                stGroup::registerRenderer);
    }

    public final void registerModelAdaptor(final STGroup stGroup,
                                           final String attributeType,
                                           final String modelAdapterClassName) {
        registerAttributePlugin(stGroup,
                                attributeType,
                                modelAdapterClassName,
                                ModelAdaptor.class,
                                stGroup::registerModelAdaptor);
    }

    public STMessage patch(@NonNull STMessage msg, @NonNull String encoding) {
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
                }
                while (scope != null);
                //
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

    private static final Set<String> PACKAGE_NAMES = new LinkedHashSet<>();
    private static final String[] DEFAULT_PACKAGE_NAMES = {
            "java.lang",
            "java.util",
            "org.stringtemplate.v4",
            "rwperrott.stringtemplate.v4"
    };
    private static final Class<?>[] DEFAULT_CACHED_CLASSES = {
            // Possible attribute types
            CharSequence.class,
            String.class,
            Number.class,
            Collection.class,
            List.class,
            Map.class,
            Set.class,
            // Renderers
            StringRenderer.class,
            StringInvokeRenderer.class,
            // Adapters
            ObjectInvokeAdaptor.class,
            NumberInvokeAdaptor.class,
            StringInvokeAdaptor.class
    };
}