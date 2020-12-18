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
        for (Class<?> cls : PRE_CACHED_CLASSES)
            addClass0(cls);
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

    public final void registerRenderers(final STGroup stGroup, Map<String, String> map) {
        registerAttributePlugins(stGroup,
                                 map,
                                 AttributeRenderer.class,
                                 stGroup::registerRenderer);
    }

    private <T> void registerAttributePlugins(
            final @NonNull STGroup stGroup,
            final @NonNull Map<String, String> map,
            final @NonNull Class<T> pluginType,
            final @NonNull BiConsumer<Class<?>, T> registerer) {
        if (null == map) {
            return;
        }
        boolean failed = false;
        for (Map.Entry<String, String> e : map.entrySet()) {
            final Class<?> keyType = getClass("attributeType", e.getKey(), Object.class);
            final T valueInstance = instanceOf(pluginType.getSimpleName(), e.getValue(), pluginType);
            registerer.accept(keyType, valueInstance);
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
                Exception fail = null;
                for (String packageName : PACKAGE_NAMES) {
                    final String key = fmt().concat(packageName,".",className);
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

    /**
     * Allow more package prefixes to be added, for class name to Class lookup.
     *
     * @param packagePrefix .
     */
    public void addPackage(@NonNull String packagePrefix) {
        if (packagePrefix.isEmpty())
            throw new IllegalArgumentException("packagePrefix is blank");
        synchronized (PACKAGE_NAMES) {
            PACKAGE_NAMES.add(packagePrefix);
        }
    }

    public final void registerModelAdaptors(final STGroup stGroup, Map<String, String> map) {
        registerAttributePlugins(stGroup,
                                 map,
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
                } while (scope != null);
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

    private static final Set<String> PACKAGE_NAMES =
            new LinkedHashSet<>(Arrays.asList(
                    "java.lang",
                    "java.util",
                    "org.stringtemplate.v4",
                    "rwperrott.stringtemplate.v4"
                    ));
    private static final Class<?>[] PRE_CACHED_CLASSES = {
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