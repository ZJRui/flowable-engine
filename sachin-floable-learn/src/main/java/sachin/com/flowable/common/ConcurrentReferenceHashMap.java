package sachin.com.flowable.common;

import sachin.com.flowable.common.utils.ObjectUtils;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ConcurrentReferenceHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {


    public static final int DEFAULT_INITIAL_CAPACITY = 16;

    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    //default_concurrency_level
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    private static final ReferenceType DEFAULT_REFERENCE_TYPE = ReferenceType.SOFT;

    /**
     * <<表示二进制左移，即将该数字在二进制下每一位向左移动n个，右面补充零，数值上扩大了2^n倍；65536
     */
    private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;
    /**
     * 1 073 741 824
     * <p>
     * 左移的运算规则：按二进制形式把所有的数字向左移动对应的位数，高位移出（舍弃），低位的空位补零。
     * <p>
     * 计算过程已1<<30为例,首先把1转为二进制数字 0000 0000 0000 0000 0000 0000 0000 0001
     * <p>
     * 然后将上面的二进制数字向左移动30位后面补0得到 0010 0000 0000 0000 0000 0000 0000 0000
     * <p>
     * 最后将得到的二进制数字转回对应类型的十进制
     */
    private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;


    private final Segment[]segments;

    private final float loadFactor;

    /**
     * soft or weak
     */
    private final ReferenceType referenceType;

    /**
     *	 * The shift value used to calculate the size of the segments array and an index from the hash.
     * 	 * 用于计算片段数组大小和哈希值的索引的移位值。
     */
    private final int shift;


    /**
     *Late binding entry set.后期绑定条目集。
     */
    private volatile Set<Map.Entry<K,V>>entrySet;


    /**
     * Create a new {@code ConcurrentReferenceHashMap} instance.
     */
    public ConcurrentReferenceHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
    }

    /**
     * Create a new {@code ConcurrentReferenceHashMap} instance.
     * @param initialCapacity the initial capacity of the map
     */
    public ConcurrentReferenceHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
    }

    /**
     * Create a new {@code ConcurrentReferenceHashMap} instance.
     * @param initialCapacity the initial capacity of the map
     * @param loadFactor the load factor. When the average number of references per table
     * exceeds this value resize will be attempted
     */
    public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
    }

    /**
     * Create a new {@code ConcurrentReferenceHashMap} instance.
     * @param initialCapacity the initial capacity of the map
     * @param concurrencyLevel the expected number of threads that will concurrently
     * write to the map
     */
    public ConcurrentReferenceHashMap(int initialCapacity, int concurrencyLevel) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
    }

    /**
     * Create a new {@code ConcurrentReferenceHashMap} instance.
     * @param initialCapacity the initial capacity of the map
     * @param referenceType the reference type used for entries (soft or weak)
     */
    public ConcurrentReferenceHashMap(int initialCapacity, org.springframework.util.ConcurrentReferenceHashMap.ReferenceType referenceType) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, referenceType);
    }

    /**
     * Create a new {@code ConcurrentReferenceHashMap} instance.
     * @param initialCapacity the initial capacity of the map
     * @param loadFactor the load factor. When the average number of references per
     * table exceeds this value, resize will be attempted.
     * @param concurrencyLevel the expected number of threads that will concurrently
     * write to the map
     */
    public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this(initialCapacity, loadFactor, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
    }


    public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, ReferenceType referenceType) {

        this.loadFactor=loadFactor;
        this.shift=calculateShift(concurrencyLevel,MAXIMUM_CONCURRENCY_LEVEL);
        int size=1<<this.shift;
        this.referenceType = referenceType;
        int roundedUpSegmentCapacity=(int)((initialCapacity+size-1L)/size);
        int initialSize=1 << calculateShift(roundedUpSegmentCapacity,MAXIMUM_SEGMENT_SIZE);
        Segment[] segments = (Segment[]) Array.newInstance(Segment.class, size);
        int resizeThreshold=(int)(initialSize*getLoadFactor());
        for(int i=0;i<segments.length;i++){
            segments[i] = new Segment(initialSize, resizeThreshold);
        }
        this.segments = segments;


    }

    protected final  float getLoadFactor(){
        return this.loadFactor;
    }

    protected static int calculateShift(int minimumValue, int maximumValue){

        int shift=0;
        int value=1;
        /**
         * 找到value，这个value是2的shift次方，同时value 大于最小值 小于最大值
         */
        while (value < minimumValue && value < maximumValue) {
            /**
             * 1、左移运算符（<<）
             *
             * 左移运算符是用来将一个数的各二进制位左移若干位，移动的位数由右操作数指定（右操作数必须是非负值），其右边空出的位用0填补，高位左移溢出则舍弃该高位。
             * 左移1位相当于该数乘以2，左移2位相当于该数乘以2*2＝4，15＜＜2=60，即乘了4。但此结论只适用于该数左移时被溢出舍弃的高位中不包含1的情况。
             */
            value <<=1;//例如 a << =2相当于a = a << 2
            shift++;
        }
        return shift;
    }

    protected ReferenceManager createReferenceManager() {
        return new ReferenceManager();
    }

    protected Reference<K, V>[] createReferenceArray(int size) {
        return new Reference[size];
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return ConcurrentMap.super.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        ConcurrentMap.super.forEach(action);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return false;
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        ConcurrentMap.super.replaceAll(function);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return ConcurrentMap.super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return ConcurrentMap.super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return ConcurrentMap.super.compute(key, remappingFunction);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return ConcurrentMap.super.merge(key, value, remappingFunction);
    }


    public enum ReferenceType {
        /**
         * use softReferences
         */
        SOFT,
        /**
         * Use WeakReference
         */
        WEAK
    }

    public enum Restructure {
        WHEN_NECESSARY, NEVER;
    }

    /**
     * A single segment used to divide the map to allow better concurrent performance.
     * <p>
     * 用于划分映射的单个段，以获得更好的并发性能。
     */
    protected final class Segment extends ReentrantLock {


        private final ReferenceManager referenceManager;
        private final int initialSize;
        /**
         * Array of references indexed using the low order bits from the hash.
         * This property should only be set along with {@code resizeThreshold}.
         * 使用来自哈希的低阶位建立索引的引用数组。此属性只能与resizeThreshold一起设置。
         */
        private volatile Reference<K, V>[] references;

        /**
         * The total number of references contained in this segment. This includes chained
         * references and references that have been garbage collected but not purged.
         * *本段所包含的参考文献总数。这包括链接
         * *已被垃圾回收但未清除的引用。
         */
        private final AtomicInteger count = new AtomicInteger();


        /**
         * The threshold when resizing of the references should occur. When {@code count}
         * exceeds this value references will be resized.
         * *调整引用大小时的阈值。当{@code数}
         * *超过此值的引用将被调整大小。
         */
        private int resizeThreshold;


        public Segment(int initialSize, int resizeThreshold) {
            this.referenceManager = createReferenceManager();
            this.initialSize = initialSize;
            this.references = createReferenceArray(this.initialSize);
            this.resizeThreshold = resizeThreshold;

        }


    }

    protected class ReferenceManager {

        private final ReferenceQueue<Entry<K, V>> queue = new ReferenceQueue<>();

        public Reference<K, V> createReference(Entry<K, V> entry, int hash, Reference<K, V> next) {

        }
    }

    protected interface Reference<K, V> {
        Entry<K, V> get();

        int getHash();

        Reference<K, V> getNext();

        void release();
    }

    protected static final class Entry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private volatile V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V value) {
            V previous = this.value;
            this.value = value;
            return previous;
        }

        @Override
        public String toString() {
            return (this.key + "=" + this.value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry otherEntry = (Map.Entry) obj;
            return ObjectUtils.nullSafeEquals(getKey(), otherEntry.getKey()) && ObjectUtils.nullSafeEquals(getValue(), otherEntry.getValue());
        }

        @Override
        public int hashCode() {
            return ObjectUtils.nullSafeHashCode(this.key) ^ ObjectUtils.nullSafeHashCode(this.value);
        }
    }


    /**
     * 对于一个软引用，当其所引用的对象 不存在任何强引用引用了该对象 ，且内存出现紧张的时候，JVM会回收这个对象及其所有的软引用。
     * <p>
     * 那么我们还需要关心的一个问题是 ：如何主动触发 放弃对这个软引用，也就是说 我想单独只清除这个软引用，而不关心 这个软引用的对象， 在release方法中
     * 实现了这个功能
     *
     * @param <K>
     * @param <V>
     */
    private static final class SoftEntryReference<K, V> extends SoftReference<Entry<K, V>> implements Reference<K, V> {
        private final int hash;
        private final Reference<K, V> nextReference;

        public SoftEntryReference(Entry<K, V> referent, int hash, Reference<K, V> nextReference, ReferenceQueue<Entry<K, V>> queue) {
            super(referent, queue);
            this.hash = hash;
            this.nextReference = nextReference;
        }

        @Override
        public int getHash() {
            return this.hash;
        }

        @Override
        public Reference<K, V> getNext() {
            return nextReference;
        }

        /**
         * 考虑当我我们从map中移除一个元素的时候，我们需要将引用和 这个引用所关联的对象 之间的关系解除。从而确保即便 对象存在的情况下
         * 这个引用对象仍然能够被及时释放
         */
        @Override
        public void release() {

            /**
             * 问题：这个地方为什么 要调用enqueue方法？
             * 原因是SoftReference 的父类Reference中定义了enqueue 方法，当Reference对象内部所包装的referent被垃圾回收的时候 我们会将
             * 这个Reference对象 放置到其属性queu所指定的ReferenceQueue中。
             *
             * 这里的release内部会通过调用clear方法将 referent设置为null，同时调用enqueue方法将reference对象放置到其queue属性指定的ReferenceQueue中。
             *
             * 在 Reference类中对属性 T referent 有如下注释：
             *     * To ensure that a concurrent collector can discover active Reference
             *      * objects without interfering with application threads that may apply
             *      * the enqueue() method to those objects, collectors should link
             *      * discovered objects through the discovered field. The discovered
             *      * field is also used for linking Reference objects in the pending list.
             *      * 保证一个并发收集器可以发现活跃的Reference
             *       * 对象不干扰可能适用的应用程序线程
             *       * enqueue() 方法到这些对象，收集者应该链接
             *       * 通过发现的字段发现的对象。 发现的
             *       * 字段还用于链接挂起列表中的参考对象。
             *       注意这个属性并不是static类型的，pending属性是static类型的
             *
             */
            enqueue();
            /**
             * clear方法会将 Reference对象中的T referent属性设置为null
             */
            clear();
        }

    }

    private static final class WeakEntryReference<K, V> extends WeakReference<Entry<K, V>> implements Reference<K, V> {
        private final int hash;

        private final Reference<K, V> nextReference;

        public WeakEntryReference(Entry<K, V> referent, int hash, Reference<K, V> nextReference, ReferenceQueue<Entry<K, V>> queue) {

            super(referent, queue);
            this.hash = hash;
            this.nextReference = nextReference;
        }

        @Override
        public int getHash() {
            return this.hash;
        }

        @Override
        public Reference<K, V> getNext() {
            return this.nextReference;
        }

        @Override
        public void release() {

            enqueue();
            clear();
        }
    }
}
