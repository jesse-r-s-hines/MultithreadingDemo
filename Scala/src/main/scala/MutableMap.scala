package threadingdemo

// Creates a mutable map.
class MutableMap[K, V]() extends Iterable[(K, V)] {
    private var map: Map[K, V] = Map()

    override def equals(other: Any): Boolean = other match { 
        case other: MutableMap[K, V] => this.map == other.map
        case _ => false
    }

    // The indexing operation. 
    def apply(key: K): V = {
        return map(key);
    }

    // Optionally gets the element 
    def get(key: K): Option[V] = {
        return map.get(key)
    }

    def remove(key: K): Option[V] = {
        val originalVal = map.get(key)
        map = map - key
        return originalVal
    }

    override def iterator = map.iterator
    def values: Iterable[V] = map.values
    def keys: Iterable[K] = map.keys

    override def size: Int = map.size

    // The indexing set operation.
    def update(key: K, value: V): Unit = {
        map = map + (key -> value)
    }
}