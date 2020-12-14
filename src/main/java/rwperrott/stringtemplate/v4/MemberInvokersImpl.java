package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.ints.IntArrays;

import java.util.*;
import java.util.stream.Stream;

/**
 * Only for package use
 */
final class MemberInvokersImpl extends ArrayList<MemberInvoker> implements MemberInvokers {
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
        super.clear();
        unindex();
    }

    private void unindex() {
        maxTypeConverterCount = -1;
    }

    @Override
    public void sort(Comparator<? super MemberInvoker> c) {
        super.sort(c);
        if (-1 == maxTypeConverterCount)
            index0();
    }

    /**
     * Push out all
     */
    Stream<MemberInvoker.ForStaticMethod.ForValueType> functionStream(final Class<?> valueType) {
        return stream()
                .filter(MemberInvoker.ForStaticMethod.class::isInstance)
                .map(mi -> ((MemberInvoker.ForStaticMethod) mi).forValueType(valueType))
                .filter(Objects::nonNull);
    }

    void sort() {
        sort(Comparator.naturalOrder());
    }

    private void index0() {
        if (isEmpty())
            throw new IllegalArgumentException("empty");
        //
        final int size = size();
        final int maxTypeConverterCount = get(size - 1).typeConverterCount();
        final int[] subIndex_ = IntArrays.ensureCapacity(subIndex, (1 + maxTypeConverterCount) << 1);
        Arrays.fill(subIndex, -1);
        int iSubIndex = 0, iSubIndexP = -1, i = 0;
        while (i < size) {
            final MemberInvoker mi = get(i);
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
        return "MemberInvokersImpl{" +
               "name=" + name +
               ", maxTypeConverterCount=" + maxTypeConverterCount +
               ", subIndex=" + Arrays.toString(subIndex) +
               ", list=" + super.toString() +
               '}';
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
            MemberInvoker mi = get(i++);
            if (mi.isAccessible(onlyPublic) &&
                mi.isReturnTypeInstanceOf(returnType) &&
                mi.convert(args, extrasLen))
                return mi;
        }
        return null;
    }
}
