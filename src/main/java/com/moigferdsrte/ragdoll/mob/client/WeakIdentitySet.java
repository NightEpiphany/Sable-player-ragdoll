package com.moigferdsrte.ragdoll.mob.client;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** A weak set whose membership checks use object identity instead of equals/hashCode. */
final class WeakIdentitySet<T> {
    private final ReferenceQueue<T> staleReferences = new ReferenceQueue<>();
    private final Set<IdentityReference<T>> references = new HashSet<>(16);

    synchronized boolean add(T value) {
        Objects.requireNonNull(value, "value");
        expungeStaleReferences();
        return references.add(new IdentityReference<>(value, staleReferences));
    }

    synchronized boolean contains(T value) {
        if (value == null) return false;
        expungeStaleReferences();
        return references.contains(new IdentityReference<>(value));
    }

    synchronized boolean remove(T value) {
        if (value == null) return false;
        expungeStaleReferences();
        return references.remove(new IdentityReference<>(value));
    }

    @SuppressWarnings("unchecked")
    private void expungeStaleReferences() {
        Reference<? extends T> reference;
        while ((reference = staleReferences.poll()) != null) {
            references.remove((IdentityReference<T>) reference);
        }
    }

    private static final class IdentityReference<T> extends WeakReference<T> {
        private final int identityHashCode;

        private IdentityReference(T referent, ReferenceQueue<T> queue) {
            super(referent, queue);
            identityHashCode = System.identityHashCode(referent);
        }

        private IdentityReference(T referent) {
            super(referent);
            identityHashCode = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return identityHashCode;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof IdentityReference<?> other)) return false;
            Object referent = get();
            return referent != null && referent == other.get();
        }
    }
}
