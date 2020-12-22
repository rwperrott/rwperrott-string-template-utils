package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.ints.IntArrays;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Only for package use
 *
 * @author rwperrott
 */
final class MemberInvokersImpl implements MemberInvokers, Consumer<MemberInvoker>, Iterable<MemberInvoker> {
    private final ArrayList<MemberInvoker> list = new ArrayList<>();
    final String name;
    private int maxTypeConverterCount = -1;
    /**
     * Eliminates the need for an array for each number of parameters.
     */
    private int[] subIndex = IntArrays.DEFAULT_EMPTY_ARRAY;

    public MemberInvokersImpl(String name) {
        this.name = name;
    }

    public void clear() {
        list.clear();
        unindex();
    }

    @Override
    public void accept(final MemberInvoker memberInvoker) {
        list.add(memberInvoker);
    }

    public void ensureCapacity(int minCapacity) {
        list.ensureCapacity(minCapacity);
    }

    public int size() {
        return list.size();
    }

    @Override
    public Iterator<MemberInvoker> iterator() {
        return list.iterator();
    }

    public Stream<MemberInvoker> stream() {
        return list.stream();
    }

    @Override
    public void forEach(final Consumer<? super MemberInvoker> action) {
        list.forEach(action);
    }

    @Override
    public Spliterator<MemberInvoker> spliterator() {
        return list.spliterator();
    }

    private void unindex() {
        maxTypeConverterCount = -1;
    }

    public void sort(Comparator<? super MemberInvoker> c) {
        list.sort(c);
        if (-1 == maxTypeConverterCount)
            index0();
    }

    /**
     * Get all member invokers with a parameter matching valueType.
     *
     * Only supported by MemberInvoker.WithValueType<br/>
     * i.e. MemberInvoker.ForStaticMethod and MemberInvoker.ForConstructor
     */
    Stream<MemberInvoker> functionStream(final Class<?> valueType) {
        return list.stream()
                .filter(MemberInvoker.WithValueType.class::isInstance)
                .map(mi -> ((MemberInvoker.WithValueType) mi).forValueType(valueType))
                .filter(Objects::nonNull);
    }

    void sort() {
        sort(Comparator.naturalOrder());
    }

    private void index0() {
        if (list.isEmpty())
            throw new IllegalArgumentException("empty");
        //
        final int size = list.size();
        final int maxTypeConverterCount = list.get(size - 1).typeConverterCount();
        final int[] subIndex_ = IntArrays.ensureCapacity(subIndex, (1 + maxTypeConverterCount) << 1);
        Arrays.fill(subIndex, -1);
        int iSubIndex = 0, iSubIndexP = -1, i = 0;
        while (i < size) {
            final MemberInvoker mi = list.get(i);
            iSubIndex = mi.typeConverterCount() << 1;
            if (iSubIndex != iSubIndexP) {
                if (iSubIndexP != -1)
                    subIndex_[iSubIndexP + 1] = i;
                iSubIndexP = iSubIndex;
            }
            subIndex_[iSubIndex] = i++;
        }
        subIndex_[iSubIndex + 1] = i;

        this.subIndex = subIndex_;
        this.maxTypeConverterCount = maxTypeConverterCount;
    }

    @Override
    public synchronized String toString() {
        ToStringBuilder t = new ToStringBuilder("MemberInvokersImpl",true);
        t.add("name",name);
        t.add("maxTypeConverterCount", maxTypeConverterCount);
        t.add("subIndex", subIndex);
        t.add("list", list);
        t.complete();
        return t.toString();
    }

    /**
     * Used by AbstractInvokeAdaptor
     */
    public synchronized int maxTypeConverterCount() {
        ensureIndexed();
        return maxTypeConverterCount;
    }

    private void ensureIndexed() {
        if (-1 == maxTypeConverterCount)
            throw new IllegalStateException("Not indexed");
    }

    /**
     * Used by AbstractInvokeAdaptor and StringInvokeRenderer
     */
    public synchronized MemberInvoker find(final boolean onlyPublic,
                                           final Class<?> returnType,
                                           final List<Object> args,
                                           final int extrasLen) {
        ensureIndexed();
        final int typeConverterCount = args.size();
        if (typeConverterCount > maxTypeConverterCount)
            return null;
        int x = typeConverterCount << 1;
        int i = subIndex[x];
        int n = subIndex[x + 1];
        while (i < n) {
            MemberInvoker mi = list.get(i++);
            if (mi.isAccessible(onlyPublic) &&
                mi.isReturnTypeInstanceOf(returnType) &&
                mi.convert(args, extrasLen))
                return mi;
        }
        return null;
    }
}
