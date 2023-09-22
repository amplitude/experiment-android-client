package com.amplitude.experiment.evaluation

internal object Murmur3 {

    private const val C1_32 = -0x3361d2af
    private const val C2_32 = 0x1b873593
    private const val R1_32 = 15
    private const val R2_32 = 13
    private const val M_32 = 5
    private const val N_32 = -0x19ab949c

    internal fun hash32x86(data: ByteArray, length: Int, seed: Int): Int {
        var hash = seed
        val nblocks = length shr 2

        // body
        for (i in 0 until nblocks) {
            val index = (i shl 2)
            val k: Int = data.readIntLe(index)
            hash = mix32(k, hash)
        }

        // tail
        val index = (nblocks shl 2)
        var k1 = 0

        when (length - index) {
            3 -> {
                k1 = k1 xor ((data[index + 2].toInt() and 0xff) shl 16)
                k1 = k1 xor ((data[index + 1].toInt() and 0xff) shl 8)
                k1 = k1 xor ((data[index].toInt() and 0xff))

                // mix functions
                k1 *= C1_32
                k1 = k1.rotateLeft(R1_32)
                k1 *= C2_32
                hash = hash xor k1
            }
            2 -> {
                k1 = k1 xor ((data[index + 1].toInt() and 0xff) shl 8)
                k1 = k1 xor ((data[index].toInt() and 0xff))
                k1 *= C1_32
                k1 = k1.rotateLeft(R1_32)
                k1 *= C2_32
                hash = hash xor k1
            }
            1 -> {
                k1 = k1 xor ((data[index].toInt() and 0xff))
                k1 *= C1_32
                k1 = k1.rotateLeft(R1_32)
                k1 *= C2_32
                hash = hash xor k1
            }
        }
        hash = hash xor length
        return fmix32(hash)
    }

    private fun mix32(k: Int, hash: Int): Int {
        var kResult = k
        var hashResult = hash
        kResult *= C1_32
        kResult = kResult.rotateLeft(R1_32)
        kResult *= C2_32
        hashResult = hashResult xor kResult
        return hashResult.rotateLeft(
            R2_32
        ) * M_32 + N_32
    }

    private fun fmix32(hash: Int): Int {
        var hashResult = hash
        hashResult = hashResult xor (hashResult ushr 16)
        hashResult *= -0x7a143595
        hashResult = hashResult xor (hashResult ushr 13)
        hashResult *= -0x3d4d51cb
        hashResult = hashResult xor (hashResult ushr 16)
        return hashResult
    }

    private fun Int.reverseBytes(): Int {
        return (this and -0x1000000 ushr 24) or
            (this and 0x00ff0000 ushr 8) or
            (this and 0x0000ff00 shl 8) or
            (this and 0x000000ff shl 24)
    }

    private fun ByteArray.readIntLe(index: Int = 0): Int {
        return (
            this[index].toInt() and 0xff shl 24
                or (this[index + 1].toInt() and 0xff shl 16)
                or (this[index + 2].toInt() and 0xff shl 8)
                or (this[index + 3].toInt() and 0xff)
            ).reverseBytes()
    }
}
