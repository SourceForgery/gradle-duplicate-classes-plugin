package com.sourceforgery.gradle.duplicateclasses

import java.util.HashMap
import java.util.TreeSet

class SimpleMultimap<K, V> : HashMap<K, TreeSet<V>>() {

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun set(key: K, value: V) = put(key, value)

    fun put(key: K, value: V): TreeSet<V> =
        getOrPut(key) { TreeSet() }
            .also {
                it.add(value)
            }
}

fun <K, V> Sequence<Pair<K, V>>.toSimpleMap(): SimpleMultimap<K, V> {
    val dest = SimpleMultimap<K, V>()
    forEach { (k, v) -> dest[k] = v }
    return dest
}

fun <T> Collection<T>.toTreeSet(): TreeSet<T> {
    return toCollection(TreeSet())
}
