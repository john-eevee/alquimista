package ex.alquimista.expert.observer

internal class AllocatorPeaks {
    private val peaks = mutableMapOf<String, Long>()
    fun observe(name: String, carrierBytes: Long): Long {
        val peak = maxOf(peaks[name] ?: 0L, carrierBytes)
        peaks[name] = peak
        return peak
    }
    fun clear() = peaks.clear()
}
