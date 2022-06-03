package org.evomaster.core.search.gene

import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.utils.NumberCalculationUtil.calculateIncrement
import org.evomaster.core.utils.NumberCalculationUtil.valueWithPrecisionAndScale
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


/**
 * contains a set of utilities in order to facilitate number mutations
 */
object NumberMutatorUtils {

    /**
     * 19 with Long.MAX
     */
    const val MAX_INTEGER_PRECISION = 308

    /**
     * with, IEEE 754
     * 15-16 digits for double
     */
    const val MAX_DOUBLE_PRECISION = 15

    /**
     * with, IEEE 754
     * 6-7 digits for float
     */
    const val MAX_FLOAT_PRECISION = 6

    /**
     * note that
     * with IEEE 754
     * the [Double.MAX_VALUE] is 0x7fefffffffffffff
     * ie, 0 11111111110 1111111111111111111111111111111111111111111111111111
     * in case that max value is Double.Max and its inclusive is false,
     * its max inclusive value here is considered as 0x7fdfffffffffffff
     * (minus smallest positive normal number)
     * ie, 0 11111111101 1111111111111111111111111111111111111111111111111111
     *
     * https://en.wikipedia.org/wiki/Double-precision_floating-point_format
     */
    const val MAX_DOUBLE_EXCLUSIVE : Double = 8.988465674311579E307

    /**
     * note that
     * the [Float.MAX_VALUE] is 0x7f7fffff
     * ie, 0 11111110 11111111111111111111111
     * in case that max value is Float.Max and its inclusive is false,
     * its max inclusive value here is considered as 0x7f7effff
     * (minus smallest positive normal number)
     * ie, 0 11111101 11111111111111111111111
     *
     * https://en.wikipedia.org/wiki/Single-precision_floating-point_format
     */
    const val MAX_FLOAT_EXCLUSIVE : Float = 3.3895312E38F


    /**
     * @return the maximum range of the [value] that can be changed based on
     * @param direction specified direction, >0 means + and <0 means -
     * @param min the lower bound of [value]
     * @param max the upper bound of [value]
     * @param value to be further modified
     */
    private fun <N:Number> getDeltaRange(direction: Double, min: N, max: N, value : N): Long {
        return if (direction > 0)
            calculateIncrement(value.toDouble(), max.toDouble()).toLong()
        else
            calculateIncrement(min.toDouble(), value.toDouble()).toLong()
    }

    /**
     * mutate double/float number
     */
    fun <N: Number> mutateFloatingPointNumber(randomness: Randomness,
                                              sdirection: Boolean? = null,
                                              maxRange: Long? = null,
                                              apc: AdaptiveParameterControl,
                                              value: N, smin: N, smax: N, scale: Int?): N{

        val direction = when{
            smax == value -> false
            smin == value -> true
            else -> sdirection
        }

        val gaussianDelta = getGaussianDeltaWithDirection(randomness, direction)

        val range = maxRange?:getDeltaRange(gaussianDelta, smin, smax, value)

        // the jumpDelta is at least 1
        val jumpDelta = GeneUtils.getDelta(randomness, apc, max(range, 1))

        var res = modifyValue(randomness, value.toDouble(), delta = gaussianDelta, maxRange = range, specifiedJumpDelta = jumpDelta,scale == null)

        if (scale != null && getFormattedValue(value, scale) == getFormattedValue(res, scale)){
            res += (if (gaussianDelta>0) 1.0 else -1.0).times(getDecimalEpsilon(scale, value).toDouble())
        }

        return if (res > smax.toDouble()) smax
        else if (res < smin.toDouble()) smin
        else getFormattedValue(res as N, scale)
    }

    /**
     * @param randomness
     * @param sDirection specify a direction, null means that the direction would be decided at random
     * @return direction info generated by Gaussian
     *          < 0 means - modification
     *          > 0 means + modification
     */
    private fun getGaussianDeltaWithDirection(randomness: Randomness, sDirection: Boolean?) : Double{
        var gaussianDelta = randomness.nextGaussian()
        if (gaussianDelta == 0.0)
            gaussianDelta = randomness.nextGaussian()

        if (sDirection != null && ((sDirection && gaussianDelta < 0) || (!sDirection && gaussianDelta > 0))){
            gaussianDelta *= -1.0
        }

        return gaussianDelta
    }

    private fun modifyValue(randomness: Randomness, value: Double, delta: Double, maxRange: Long, specifiedJumpDelta: Int, precisionChangeable: Boolean): Double{
        val strategies = FloatingPointNumber.ModifyStrategy.values().filter{
            precisionChangeable || it != FloatingPointNumber.ModifyStrategy.REDUCE_PRECISION
        }
        return when(randomness.choose(strategies)){
            FloatingPointNumber.ModifyStrategy.SMALL_CHANGE-> value + min(1, maxRange) * delta
            FloatingPointNumber.ModifyStrategy.LARGE_JUMP -> value + specifiedJumpDelta * delta
            FloatingPointNumber.ModifyStrategy.REDUCE_PRECISION -> BigDecimal(value).setScale(randomness.nextInt(15), RoundingMode.HALF_EVEN).toDouble()
        }
    }

    /**
     * @return minimal delta if it has.
     * this is typically used when the precision/scale is specified
     */
    fun <N: Number> getDecimalEpsilon(scale: Int?, value: N): N {

        val f = (if (value is Float) MAX_FLOAT_PRECISION else MAX_DOUBLE_PRECISION).run { min(this, scale?:this) }
        val bd = BigDecimal(1.0/(10.0.pow(f))).setScale(f, RoundingMode.HALF_UP)

        return  when (value) {
            is Float -> bd.toFloat() as N
            is Double -> bd.toDouble() as N
            is BigDecimal -> bd as N
            else -> throw Exception("valueToFormat must be Double, Float or BigDecimal, but it is ${value::class.java.simpleName}")
        }
    }

    /**
     * @return formatted value based on precision if it has
     */
    fun <N: Number> getFormattedValue(valueToFormat: N, scale: Int?, roundingMode: RoundingMode= RoundingMode.HALF_UP) : N {
        if (scale == null)
            return valueToFormat
        return when (valueToFormat) {
            is Double -> valueWithPrecisionAndScale(valueToFormat.toDouble(), scale, roundingMode).toDouble() as N
            is Float -> valueWithPrecisionAndScale(valueToFormat.toDouble(), scale, roundingMode).toFloat() as N
            is BigDecimal -> valueWithPrecisionAndScale(valueToFormat.toDouble(), scale, roundingMode) as N
            else -> throw Exception("valueToFormat must be Double, Float or BigDecimal, but it is ${valueToFormat::class.java.simpleName}")
        }
    }

    /**
     * mutate a long with [max], [min] with adaptive parameter control
     */
    fun mutateLong(value: Long, min: Long?, max: Long?, randomness: Randomness, apc: AdaptiveParameterControl): Long {

        //choose an i for 2^i modification
        val delta = GeneUtils.getDelta(randomness, apc, max(longDeltaRange(min, value), longDeltaRange(value, max)))

        return mutateLong(value, min, max, delta, randomness)
    }

    /**
     * mutate a long with [max], [min], specified [delta]
     */
    fun mutateLong(value: Long, min: Long?, max: Long?, delta: Int, randomness: Randomness): Long {

        val sign = when {
            max != null && (value >= max || ((value + delta) > max)) -> -1
            min != null && (value <= min || ((value - delta) < min)) -> +1
            else -> randomness.choose(listOf(-1, +1))
        }

        return value + (sign * delta)
    }

    /**
     * calculate the delta based on [min] and [max] that is used for eg, mutation
     * note the delta is less than [Long.MAX_VALUE]
     */
    private fun longDeltaRange(min: Long?, max : Long?) : Long{
        return if (min != null || max != null){
            try{
                min(Long.MAX_VALUE, Math.subtractExact(max?: Long.MAX_VALUE, min?: Long.MIN_VALUE))
            }catch (e : ArithmeticException) {
                Long.MAX_VALUE
            }
        }else
            Long.MAX_VALUE
    }

    /**
     * randomize a long with [max], [min]
     */
    fun randomizeLong(value: Long, min: Long?, max: Long?, randomness: Randomness, forceNewValue: Boolean) : Long {

        /*
            if one of min or max is specified,
            we employ [randomness.randomizeBoundedIntAndLong] for randomizing long that is same as randomizing int
         */
        if (min != null || max != null){
            return randomness.randomizeBoundedIntAndLong(value, min?: Long.MIN_VALUE, max?: Long.MAX_VALUE, forceNewValue)
        }

        var k = if (randomness.nextBoolean(0.1)) {
            randomness.nextLong()
        } else if (randomness.nextBoolean(0.1)) {
            randomness.nextInt().toLong()
        } else {
            randomness.nextInt(1000).toLong()
        }

        while (forceNewValue && k == value) {
            k = randomness.nextInt().toLong()
        }

        return k
    }

    /**
     * randomize a double with [max], [min] and [scale]
     */
    fun randomizeDouble(min: Double, max: Double, scale: Int?, randomness: Randomness): Double{
        var rand = randomness.nextDouble()
        if (rand < min || rand > max){
            rand = randomness.nextDouble(min, max)
        }
        return getFormattedValue(rand, scale)
    }

}