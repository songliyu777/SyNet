package com.synet.cache.annotation;

import com.synet.cache.interceptor.AbstractFallbackCacheOperationSource;
import com.synet.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationReactiveCacheOperationSource extends AbstractFallbackCacheOperationSource implements Serializable {

    private final boolean publicMethodsOnly;

    private final Set<CacheAnnotationParser> annotationParsers;


    /**
     * Create a default AnnotationCacheOperationSource, supporting public methods
     * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
     */
    public AnnotationReactiveCacheOperationSource() {
        this(true);
    }

    /**
     * Create a default {@code AnnotationCacheOperationSource}, supporting public methods
     * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
     * @param publicMethodsOnly whether to support only annotated public methods
     * typically for use with proxy-based AOP), or protected/private methods as well
     * (typically used with AspectJ class weaving)
     */
    public AnnotationReactiveCacheOperationSource(boolean publicMethodsOnly) {
        this.publicMethodsOnly = publicMethodsOnly;
        this.annotationParsers = Collections.singleton(new SpringReactiveCacheAnnotationParser());
    }

    /**
     * Create a custom AnnotationCacheOperationSource.
     * @param annotationParser the CacheAnnotationParser to use
     */
    public AnnotationReactiveCacheOperationSource(CacheAnnotationParser annotationParser) {
        this.publicMethodsOnly = true;
        Assert.notNull(annotationParser, "CacheAnnotationParser must not be null");
        this.annotationParsers = Collections.singleton(annotationParser);
    }

    /**
     * Create a custom AnnotationCacheOperationSource.
     * @param annotationParsers the CacheAnnotationParser to use
     */
    public AnnotationReactiveCacheOperationSource(CacheAnnotationParser... annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
        this.annotationParsers = new LinkedHashSet<>(Arrays.asList(annotationParsers));
    }

    /**
     * Create a custom AnnotationCacheOperationSource.
     * @param annotationParsers the CacheAnnotationParser to use
     */
    public AnnotationReactiveCacheOperationSource(Set<CacheAnnotationParser> annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
        this.annotationParsers = annotationParsers;
    }


//    @Override
//    public boolean isCandidateClass(Class<?> targetClass) {
//        for (CacheAnnotationParser parser : this.annotationParsers) {
//            if (parser.isCandidateClass(targetClass)) {
//                return true;
//            }
//        }
//        return false;
//    }

    @Override
    @Nullable
    protected Collection<CacheOperation> findCacheOperations(Class<?> clazz) {
        return determineCacheOperations(parser -> parser.parseCacheAnnotations(clazz));
    }

    @Override
    @Nullable
    protected Collection<CacheOperation> findCacheOperations(Method method) {
        return determineCacheOperations(parser -> parser.parseCacheAnnotations(method));
    }

    /**
     * Determine the cache operation(s) for the given {@link AnnotationCacheOperationSource.CacheOperationProvider}.
     * <p>This implementation delegates to configured
     * {@link CacheAnnotationParser CacheAnnotationParsers}
     * for parsing known annotations into Spring's metadata attribute class.
     * <p>Can be overridden to support custom annotations that carry caching metadata.
     * @param provider the cache operation provider to use
     * @return the configured caching operations, or {@code null} if none found
     */
    @Nullable
    protected Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
        Collection<CacheOperation> ops = null;
        for (CacheAnnotationParser parser : this.annotationParsers) {
            Collection<CacheOperation> annOps = provider.getCacheOperations(parser);
            if (annOps != null) {
                if (ops == null) {
                    ops = annOps;
                }
                else {
                    Collection<CacheOperation> combined = new ArrayList<>(ops.size() + annOps.size());
                    combined.addAll(ops);
                    combined.addAll(annOps);
                    ops = combined;
                }
            }
        }
        return ops;
    }

    /**
     * By default, only public methods can be made cacheable.
     */
    @Override
    protected boolean allowPublicMethodsOnly() {
        return this.publicMethodsOnly;
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnnotationReactiveCacheOperationSource)) {
            return false;
        }
        AnnotationReactiveCacheOperationSource otherCos = (AnnotationReactiveCacheOperationSource) other;
        return (this.annotationParsers.equals(otherCos.annotationParsers) &&
                this.publicMethodsOnly == otherCos.publicMethodsOnly);
    }

    @Override
    public int hashCode() {
        return this.annotationParsers.hashCode();
    }


    /**
     * Callback interface providing {@link CacheOperation} instance(s) based on
     * a given {@link CacheAnnotationParser}.
     */
    @FunctionalInterface
    protected interface CacheOperationProvider {

        /**
         * Return the {@link CacheOperation} instance(s) provided by the specified parser.
         * @param parser the parser to use
         * @return the cache operations, or {@code null} if none found
         */
        @Nullable
        Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser);
    }
}
