package rwperrott.stringtemplate.v4;

import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.STGroupString;
import org.stringtemplate.v4.misc.Misc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.stringtemplate.v4.STGroup.GROUP_FILE_EXTENSION;
import static org.stringtemplate.v4.STGroup.TEMPLATE_FILE_EXTENSION;

/**
 * The type of STGroup to create.
 */
public enum STGroupType implements MultilineAppender {
    /**
     * If source contains "::=", new STGroupString(null,source,delimiterStartChar,delimiterStopChar) will be used.
     */
    string(STGroupString.class) {
        private STGroupString as(final Object o) {
            return (STGroupString) Objects.requireNonNull(o,"o");
        }
        @Override
        public STGroup getSTGroup(String sourceName, String source, URL url, String encoding) {
            return new STGroupString(Objects.requireNonNull(sourceName,"sourceName"), Objects.requireNonNull(source,"source"));
        }

        @Override
        public boolean appendTo(final MultiLineJoiner mlj, final Object o) {
            final STGroupString stg = as(o);
            final ToStringBuilder ts = new ToStringBuilder(mlj, "STGroupString");
            ts.add("sourceName", stg.sourceName);
            ts.add("text", stg.text);
            ts.complete();
            return true;
        }

        @Override
        public String getSourceName(final STGroup stGroup) {
            return as(stGroup).sourceName;
        }

        @Override
        public Object getSource(final STGroup stGroup) {
            return as(stGroup).text;
        }

        @Override
        public Object getTemplateSource(final STGroup stGroup, final String templateName) {
            return as(stGroup).sourceName;
        }

        @SuppressWarnings("RedundantThrows")
        @Override
        Reader openReader(final STGroup stGroup, final Object source, final String encoding) throws IOException {
            return new StringReader(as(stGroup).text);
        }
    },
    /**
     * If not String and source ends with ".st" or ".stg" new STGroupFile(url,encoding,
     * delimiterStartChar,delimiterStopChar) will be used.
     */
    file(STGroupFile.class) {
        private STGroupFile as(final Object o) {
            return (STGroupFile) Objects.requireNonNull(o,"o");
        }
        @Override
        public STGroup getSTGroup(String sourceName, String source, URL url, String encoding) {
            return new STGroupFile(url, encoding,
                                   STGroup.defaultGroup.delimiterStartChar,
                                   STGroup.defaultGroup.delimiterStopChar);
        }

        @Override
        public boolean appendTo(final MultiLineJoiner mlj, final Object o) {
            final STGroupFile stg = as(o);
            final ToStringBuilder ts = new ToStringBuilder(mlj, "STGroupFile");
            //ts.add("fileName", stg.fileName);
            ts.add("url", stg.url);
            ts.add("importedGroups",stg.getImportedGroups());
            ts.complete();
            return true;
        }

        @Override
        public String getSourceName(final STGroup stGroup) {
            return as(stGroup).url.toString();
        }

        @Override
        public Object getSource(final STGroup stGroup) {
            return as(stGroup).url;
        }

        @SuppressWarnings("RedundantThrows")
        @Override
        public Object getTemplateSource(final STGroup stGroup, final String templateName) throws IOException {
            return as(stGroup).url;
        }

        @Override
        Reader openReader(final STGroup stGroup, final Object source, final String encoding) throws IOException {
            return new InputStreamReader(((URL)source).openStream(),encoding);
        }
    },
    /**
     * If not string or file, new STGroupDir(url,encoding, delimiterStartChar,delimiterStopChar) will be used.
     */
    directory(STGroupDir.class) {
        private STGroupDir as(final Object o) {
            return (STGroupDir) Objects.requireNonNull(o,"o");
        }
        @Override
        public STGroup getSTGroup(String sourceName, String source, URL url, String encoding) {
            return new STGroupDir(url, encoding,
                                  STGroup.defaultGroup.delimiterStartChar,
                                  STGroup.defaultGroup.delimiterStopChar);
        }
        @Override
        public boolean appendTo(final MultiLineJoiner mlj, final Object o) {
            final STGroupDir stg = as(o);
            final ToStringBuilder ts = new ToStringBuilder(mlj, "STGroupDir");
            //ts.add("groupDirName", stg.groupDirName);
            ts.add("root", stg.root);
            ts.add("importedGroups",stg.getImportedGroups());
            ts.complete();
            return true;
        }

        @Override
        public String getSourceName(final STGroup stGroup) {
            return as(stGroup).root.toString();
        }

        @Override
        public Object getSource(final STGroup stGroup) {
            return as(stGroup).root;
        }

        @Override
        public Object getTemplateSource(final STGroup stGroup, final String templateName) throws IOException {
            final URL root = as(stGroup).root;
            URL url = new URL(root + Misc.getParent(templateName) + GROUP_FILE_EXTENSION);
            try {
                url.openStream().close();
                return url;
            } catch (IOException ioe) {
                url = new URL(root + Misc.getPrefix(templateName) + Misc.getFileName(templateName) + TEMPLATE_FILE_EXTENSION);
                url.openStream().close();
                return url;
            }
        }

        @Override
        Reader openReader(final STGroup stGroup, final Object source, final String encoding) throws IOException {
            return new InputStreamReader(((URL)source).openStream(),encoding);
        }
    };
    //
    public final Class<?> stGroupClass;

    STGroupType(Class<?> stGroupClass) {
        this.stGroupClass = stGroupClass;
    }

    public abstract STGroup getSTGroup(String sourceName, String source, URL url, String encoding);

    public abstract String getSourceName(STGroup stGroup);

    public abstract Object getSource(STGroup stGroup);

    public abstract Object getTemplateSource(final STGroup stGroup, final String templateName) throws IOException;

    abstract Reader openReader(final STGroup stGroup, final Object source, final String encoding) throws IOException;

    private static final Map<Class<?>, STGroupType> MAP;

    static {
        final STGroupType[] a = values();
        final Map<Class<?>, STGroupType> map = new HashMap<>(a.length);
        for (STGroupType v : a)
            map.put(v.stGroupClass, v);
        MAP = map;
    }

    public static STGroupType of(final STGroup stGroup) {
        Objects.requireNonNull(stGroup, "stGroup");
        return MAP.computeIfAbsent(stGroup.getClass(),
                                   k-> {throw  new IllegalArgumentException(k.toString());});
    }
}
