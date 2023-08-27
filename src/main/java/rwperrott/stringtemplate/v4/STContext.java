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

/**
 * Contains lazy cache maps for start line numbers of templates in files, and for class name to Class lookup.
 * <ul>
 * <li>Supports registration of AttributeRenders and ModelAdapters in an STGroup</li>
 * <li>Supports replacing a STMessage with a, stinky, relative line number, with one with an absolute line number.</li>
 * <li>Thread-Safe, to allow concurrent rendering of STGroup(s), and hopefully templates, later.</li>
 * </ul>
 *
 * @author rwperrott
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class STContext implements Closeable {
  private static final String[] DEFAULT_PACKAGE_NAMES = {
    // Core Java packages
    "java.lang",
    "java.util",
    // String Template package
    "org.stringtemplate.v4",
    // Own package
    "rwperrott.stringtemplate.v4"
  };
  private static final Class<?>[] DEFAULT_CLASSES = {
    // Possible attribute types
    CharSequence.class,
    String.class,
    Number.class,
    Collection.class,
    List.class,
    Map.class,
    Set.class,
    // Own Renderers
    StringRenderer.class,
    StringInvokeRenderer.class,
    // Own Adapters
    ObjectInvokeAdaptor.class,
    NumberInvokeAdaptor.class,
    StringInvokeAdaptor.class
  };
  private final Set<Option> options = EnumSet.noneOf(Option.class);
  private final Map<Object, Object2IntMap<String>> stGroupMetaMap = new HashMap<>();
  private final Set<String> packageNames = new LinkedHashSet<>();
  private final Map<String, Class<?>> classCache = new HashMap<>();
  private final Object lock = new Object();
  // A lazy initialisation flag to allow adding more options before configuration locked.
  private boolean initialised;
  // Class loader off Thread, to allow for custom classloader, e.g., for a Maven plugin.
  private ClassLoader classLoader;

  public STContext() {
    this.classLoader = Thread.currentThread().getContextClassLoader();
  }

  /**
   * Set a custom ClassLoader.
   *
   * @param classLoader a custom ClassLoader
   * @return this
   */
  public STContext classLoader(final @NonNull ClassLoader classLoader) {
    synchronized (lock) {
      ensureUninitialised();
      this.classLoader = classLoader;
    }
    return this;
  }

  private void ensureUninitialised() {
    if (initialised)
      throw new IllegalStateException("too late, already initialised");
  }

  /**
   * Clear all options.
   *
   * @return this
   */
  public STContext clearOptions() {
    synchronized (lock) {
      ensureUninitialised();
      options.clear();
    }
    return this;
  }

  /**
   * Set some options.
   *
   * @param options .
   * @return this
   */
  public STContext options(final @NonNull Option... options) {
    synchronized (lock) {
      ensureUninitialised();
      if (0 != options.length)
        Collections.addAll(this.options, options);
    }
    return this;
  }

  /**
   * Set all options.
   *
   * @return this
   */
  public STContext allOptions() {
    synchronized (lock) {
      ensureUninitialised();
      options.addAll(Option.ALL);
    }
    return this;
  }

  /**
   * Add package names, to allow ShortName class lookup.
   *
   * @param packageNames .
   */
  public STContext packages(@NonNull String... packageNames) {
    if (0 != packageNames.length)
      synchronized (lock) {
        if (!initialised)
          init();
        Collections.addAll(this.packageNames, packageNames);
      }
    return this;
  }

  private void init() {
    if (options.contains(Option.ADD_DEFAULT_PACKAGES))
      Collections.addAll(packageNames, DEFAULT_PACKAGE_NAMES);
    if (options.contains(Option.ADD_DEFAULT_CLASSES))
      for (Class<?> cls : DEFAULT_CLASSES)
        cacheClass(cls);
    initialised = true;
  }

  private void cacheClass(Class<?> cls) {
    final String className = cls.getName();
    cacheClass(className, cls, className.lastIndexOf('.'));
  }

  // Adds ShortName too when package prefix provided in className.
  private void cacheClass(String className, Class<?> cls, int p) {
    classCache.put(className, cls);
    final String packageName = className.substring(0, p);
    if (packageNames.contains(packageName))
      classCache.put(className.substring(p + 1), cls);
  }

  /**
   * Some classes to classCache.
   *
   * @param classes one of more class to register
   */
  public STContext classes(Class<?>... classes) {
    synchronized (lock) {
      if (!initialised)
        init();
      for (Class<?> cls : classes)
        cacheClass(cls);
    }
    return this;
  }

  @SuppressWarnings("RedundantThrows")
  @Override
  public void close() throws IOException {
    synchronized (stGroupMetaMap) {
      stGroupMetaMap.clear();
    }
  }

  /**
   * @param stGroup           target
   * @param attributeTypeName .
   * @param rendererClassName .
   */
  public final void registerRenderer(@NonNull final STGroup stGroup,
                                     @NonNull final String attributeTypeName,
                                     @NonNull final String rendererClassName) {
    registerAttributeExtension(attributeTypeName,
                               rendererClassName,
                               AttributeRenderer.class,
                               stGroup::registerRenderer);
  }

  /**
   * @param stGroup the target
   * @param map     a map of AttributeRenderer class name keyed by attribute type name.
   */
  public final void registerRenderers(@NonNull final STGroup stGroup,
                                      @NonNull final Map<String, String> map) {
    registerAttributeExtensions(map,
                                AttributeRenderer.class,
                                stGroup::registerRenderer);
  }

  /**
   * @param stGroup the target
   * @param map     a map of ModelAdaptor class name keyed by attribute type name.
   */
  public final void registerModelAdaptors(@NonNull final STGroup stGroup,
                                          @NonNull final Map<String, String> map) {
    registerAttributeExtensions(map,
                                ModelAdaptor.class,
                                stGroup::registerModelAdaptor);
  }

  public final void registerModelAdaptor(@NonNull final STGroup stGroup,
                                         @NonNull final String attributeType,
                                         @NonNull final String modelAdapterClassName) {
    registerAttributeExtension(attributeType,
                               modelAdapterClassName,
                               ModelAdaptor.class,
                               stGroup::registerModelAdaptor);
  }

  private <T> void registerAttributeExtensions(
    final Map<String, String> typeExtensionMap,
    final Class<T> extensionTypeName,
    final BiConsumer<Class<?>, T> registerer) {
    typeExtensionMap.forEach((attributeType, extensionClassName) ->
                               registerAttributeExtension(attributeType, extensionClassName, extensionTypeName, registerer));
  }

  private <T> void registerAttributeExtension(
    final String attributeTypeName,
    final String extensionClassName,
    final Class<T> extensionType,
    final BiConsumer<Class<?>, T> registerer) {
    final Class<?> keyType = getClass("attributeType", attributeTypeName, null);
    final T valueInstance = instanceOf(extensionType.getSimpleName(), extensionClassName, extensionType);
    registerer.accept(keyType, valueInstance);
  }

  // Used by Group to create instances of AttributeRender and ModelAdapter
  @SuppressWarnings({"unchecked", "UseSpecificCatch"})
  private <T> T instanceOf(final String label, final String className, Class<?> type) {
    final Class<?> cls = getClass(label, className, type);
    try {
      return (T) cls.getConstructor().newInstance();
    } catch (Exception ex) {
      throw new IllegalStateException(format("%s: failed to create instance of %s, a %s",
                                             label, className, type.getTypeName()), ex);
    }
  }

  // Used by Group to get type for registerRenderer and registerModelAdaptor
  // Used by StContext.instanceOf.
  private Class<?> getClass(final String label, final String className, Class<?> baseType) {
    // Make operation full atomic to avoid pointless puts
    synchronized (lock) {
      if (!initialised)
        init();
      Class<?> cls = classCache.get(className);
      if (null != cls) {
        return cls;
      }

      // Expand simple names for java.lang package
      final int p = className.lastIndexOf('.');
      if (-1 == p) {
        cls = getPackageClass(label, className, baseType);
      } else {
        try {
          cls = classLoader.loadClass(className);
        } catch (ClassNotFoundException ex) {
          throw new IllegalArgumentException(format("%s: %s class not found", label, className), ex);
        }
      }
      if (null != baseType && !baseType.isAssignableFrom(cls)) {
        throw new IllegalArgumentException(format("%s: %s not an instanceof %s",
                                                  label, className, baseType.getTypeName()));
      }
      cacheClass(className, cls, p);
      return cls;
    }
  }

  private Exception addSuppressed(Exception fail, Exception newFail) {
    if (null == fail)
      fail = newFail;
    else
      fail.addSuppressed(newFail);
    return fail;
  }

  private Class<?> getPackageClass(final String label, final String className, Class<?> type) {
    Exception fail = null; // Store them all, maybe useful for a duf package name
    for (String packageName : packageNames) {
      final String key = Fmt.useF(true, f ->
        f.append(packageName).append('.').append(className).toString());
      Class<?> cls;
      try {
        cls = classLoader.loadClass(key);
      } catch (ClassNotFoundException ex) {
        fail = addSuppressed(fail, ex);
        continue;
      } catch (Exception e) {
        fail = addSuppressed(fail, e);
        break;
      }
      if (!type.isAssignableFrom(cls)) {
        fail = addSuppressed(fail,
                             new IllegalArgumentException(format("%s: class %s not an instanceof %s",
                                                                 label, className, type.getTypeName()), null));
        continue;
      }
      classCache.put(className, cls);
      // Also cache full key
      classCache.put(key, cls);
      return cls;
    }
    throw new IllegalArgumentException(format("%s: class %s not found",
                                              label, className), fail);
  }

  public STMessage patch(@NonNull STMessage msg, @NonNull String encoding) {
    if (msg instanceof STRuntimeMessagePatch) {
      throw new IllegalArgumentException("Already patched: " + msg);
    }
    //
    if (msg instanceof STRuntimeMessage) {
      final STRuntimeMessage stRuntimeMessage = (STRuntimeMessage) msg;
      InstanceScope scope = stRuntimeMessage.scope;
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
        scope = stRuntimeMessage.scope;
        final ST st = scope.st;
        final String name = st.getName().substring(1);
        final STGroup stGroup = st.groupThatCreatedThisInstance;
        throw new IllegalStateException(format("start line not found for template %s in %s:%s",
                                               name, stGroup.getClass().getSimpleName(), STGroupType.of(stGroup).getSource(stGroup)));
      }
    }
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

  /**
   * Current options, which may expand, thus the ALL constant.
   */
  public enum Option {
    ADD_DEFAULT_PACKAGES,
    ADD_DEFAULT_CLASSES;
    private static final EnumSet<Option> ALL = EnumSet.allOf(Option.class);
  }
}