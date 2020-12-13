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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

import static java.lang.Class.forName;
import static java.lang.String.format;

/**
 * Contains lazy cache maps for start line numbers of templates in files,
 * and for class name to Class lookup.
 *
 * Provides methods to register AttributeRenders and ModelAdapters in an STGroup,
 * and to try and replace a STMessage with a relative line numbers for one with an absolute line number.
 *
 * @author rwperrott
 */
public class STContext implements Closeable {

    private final Map<Object, Object2IntMap<String>> ST_GROUP_META_MAP = new HashMap<>();
    private final ClassLoader registerClassLoader;
    private final BiConsumer<String, Throwable> errorLog;
    private final Map<String, Class<?>> registerClassCache = new HashMap<>();
    public STContext(final ClassLoader registerClassLoader,
                     final BiConsumer<String, Throwable> errorLog) {
        Objects.requireNonNull(registerClassLoader, "registerClassLoader");
        Objects.requireNonNull(errorLog, "errorLog");
        this.registerClassLoader = registerClassLoader;
        this.errorLog = errorLog;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() throws IOException {
        // Not use currently, but may decide to
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
        synchronized (registerClassCache) {
            Class<?> cls = registerClassCache.get(className);
            if (null != cls) {
                return cls;
            }

            // Expand simple names for java.lang package
            final int p = className.lastIndexOf('.');
            if (-1 == p)
                try {
                    final String key = JAVA_LANG_PREFIX + className;
                    cls = forName(key); // With default ClassLoader
                    if (!type.isAssignableFrom(cls)) {
                        errorLog.accept(format("%s '%s' not an instanceof %s",
                                               label, className, type.getTypeName()), null);
                        cls = null;
                    }
                    registerClassCache.put(className, cls);
                    // Also cache full key
                    registerClassCache.put(key, cls);
                    return cls;
                } catch (ClassNotFoundException ignore) {
                }

            try {
                cls = registerClassLoader.loadClass(className);
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
            registerClassCache.put(className, cls);
            // Also cache simple key if java.lang package
            if (className.regionMatches(0, JAVA_LANG_PREFIX, 0, JAVA_LANG_PREFIX.length())) {
                registerClassCache.put(className.substring(p + 1), cls);
            }
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
            final InstanceScope scope = strm.scope;
            if (null != scope) {
                final ST st = scope.st;
                final String name = st.getName().substring(1);
                final STGroup stGroup = st.groupThatCreatedThisInstance;
                final int n = getTemplateLineNumber(stGroup, encoding, name);
                if (n == -1) {
                    throw new IllegalStateException(format("start line not found for template %s in %s:%s",
                                                           name, stGroup.getClass().getSimpleName(), STGroupType.of(stGroup).getSource(stGroup)));
                } else {
                    return new STRuntimeMessagePatch((STRuntimeMessage) msg, n);
                }
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
        synchronized (ST_GROUP_META_MAP) {
            return ST_GROUP_META_MAP.computeIfAbsent(templateSource, k -> {
                final Object2IntMap<String> templateLineNumbers = new Object2IntLinkedOpenHashMap<>();
                try (final LineNumberReader lnr = new LineNumberReader(type.openReader(stGroup, templateSource, encoding))) {
                    lnr.setLineNumber(1);
                    for (String line; (line = lnr.readLine()) != null;) {
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
    static final String JAVA_LANG_PREFIX = "java.lang.";
}
