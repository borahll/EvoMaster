package org.evomaster.core.search.impact.impactinfocollection


/**
 * @property shared shared impacts. over a course of mutation, some gene impact info (e.g., times of manipulating the gene) should be shared that are collected by [shared].
 * @property specific there exist some impact info specific to 'current' gene, e.g., times of no impact from impact
 *
 * for instance, an evolution of a gene is 0:A-1:B-2:(C)-3:D-4:(E)-5:F-6:G-7:(H)-8:(I)-9:($J$), where
 *      (?) represents a gene is mutated, but no improvement
 *      $?$ represents a gene is mutated, but no impact
 *      number represents the order to mutate during search, and a gene is originated from sampling, represented by 0.
 *
 * regarding genes A, B, D, F, G, timesToManipulate (i.e.,9), timesOfNoImpact (i.e.,1), timesOfImpact(i.e.,8) are shared,
 * regarding gene G, noImpactFromImpact(i.e., 1) and noImprovement(i.e.,3) are specific
 */
open class Impact(
        val shared : SharedImpactInfo,
        val specific : SpecificImpactInfo
){
    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Double> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImprovement : MutableMap<Int, Double> = mutableMapOf()
    ) : this(SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact), SpecificImpactInfo(noImpactFromImpact, noImprovement))



    fun getId() = shared.id
    fun getTimesOfNoImpact() = shared.timesOfNoImpacts
    fun getTimesOfNoImpactWithTargets() = shared.timesOfNoImpactWithTargets
    fun getTimesToManipulate() = shared.timesToManipulate
    fun getDegree() = shared.degree

    fun recentImprovement() = getNoImprovementCounter().any { it.value < 2 }

    fun getDegree(property: ImpactProperty, target: Int) = if (getTimesToManipulate() == 0) -1.0 else getValueByImpactProperty(property, target)/getTimesToManipulate().toDouble()
    fun getCounter(property: ImpactProperty, target: Int) = getValueByImpactProperty(property, target)

    fun getDegree(property: ImpactProperty, targets: Set<Int>, by: By) : Double{
        return targets.map { getDegree(property, it) }.filter { it != -1.0 }.run {
            if (isEmpty()) -1.0
            else{
                when(by){
                    By.MIN -> this.min()!!
                    By.MAX -> this.max()!!
                    By.AVG -> this.average()!!
                }
            }
        }
    }

    fun getCounter(property: ImpactProperty, targets: Set<Int>, by: By) : Double{
        val list = targets.map { getCounter(property, it) }.filter { it != -1.0 }
        if (list.isEmpty()) return -1.0
        return when(by){
            By.MIN -> list.min()?: throw IllegalArgumentException("min is null")
            By.MAX -> list.max()?: throw IllegalArgumentException("max is null")
            By.AVG -> list.average()
        }
    }

    fun getTimesOfImpacts() = shared.timesOfImpact

    fun getNoImpactsFromImpactCounter() = specific.noImpactFromImpact
    fun getNoImprovementCounter() = specific.noImprovement


    open fun copy(): Impact {
        return Impact(
                shared.copy(), specific.copy()
        )
    }

    open fun clone() : Impact{
        return Impact(
                shared, specific.copy()
        )
    }

    fun countImpactAndPerformance(noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean, num: Int){
        shared.timesToManipulate += 1
        val hasImpact = impactTargets.isNotEmpty()

        if (hasImpact) {
            impactTargets.forEach { target ->
                if (onlyManipulation){
                    initMap(target, shared.timesOfImpact)
                }else{
                    shared.singleImpact.merge(target, num == 1){old, delta -> (old || delta)}
                    plusMap(target, shared.timesOfImpact, num)
                    assignMap(target, specific.noImpactFromImpact, 0.0)
                    if (improvedTargets.contains(target))
                        assignMap(target,specific.noImprovement, 0.0)
                    else
                        plusMap(target, specific.noImprovement)
                }
            }
            if (!onlyManipulation){
                specific.noImpactFromImpact.keys.filter { !impactTargets.contains(it) }.forEach { k->
                    plusMap(k, specific.noImpactFromImpact)
                }
                specific.noImprovement.keys.filter { !impactTargets.contains(it) }.forEach { k->
                    plusMap(k, specific.noImprovement)
                }
            }
        } else {
            specific.noImpactFromImpact.keys.forEach { target->
                plusMap(target, specific.noImpactFromImpact)
            }
            specific.noImprovement.keys.forEach { target->
                plusMap(target, specific.noImprovement)
            }
            shared.timesOfNoImpacts +=1
            noImpactTargets.forEach {
                plusMap(it, shared.timesOfNoImpactWithTargets)
            }
        }
    }

    private fun plusMap(key : Int, map: MutableMap<Int, Double>, num: Int = 1){
        map.merge(key, 1.0/num){old, delta -> (old + delta)}
    }

    private fun assignMap(key : Int, map: MutableMap<Int, Double>, value : Double){
        map.getOrPut(key){0.0}
        map.replace(key, value)
    }

    private fun initMap(key : Int, map: MutableMap<Int, Double>){
        map.getOrPut(key){0.0}
    }

    fun increaseDegree(delta : Double){
        shared.degree += delta
    }

    open fun maxTimesOfNoImpact() : Int = 10


    companion object{
        fun toCSVHeader() : List<String> = listOf("id", "degree", "timesToManipulate", "timesOfNoImpacts","timesOfImpact","noImpactFromImpact","noImprovement")
    }
    fun toCSVCell(targets : Set<Int>? = null) : List<String> = listOf(
            getId(),
            getDegree().toString(),
            "CM:${getTimesToManipulate()}",
            "CNI:${getTimesOfNoImpact()}",
            "I:${getTimesOfImpacts().filter { targets?.contains(it.key)?:true }.map { "${it.key}->${it.value}" }.joinToString(";")}",
            "NI:${getTimesOfNoImpactWithTargets().filter { targets?.contains(it.key)?:true }.map { "${it.key}->${it.value}" }.joinToString(";")}",
            "I->NI:${getNoImpactsFromImpactCounter().filter { targets?.contains(it.key)?:true }.map { "${it.key}->${it.value}" }.joinToString(";")}",
            "NV:${getNoImprovementCounter().filter { targets?.contains(it.key)?:true }.map { "${it.key}->${it.value}" }.joinToString(";")}"
    )

    fun getMaxImpact() : Double = shared.timesOfImpact.values.max()?:0.0

    fun getValueByImpactProperty(property: ImpactProperty, target : Int) : Double{
        return when(property){
            ImpactProperty.TIMES_NO_IMPACT -> shared.timesOfNoImpacts.toDouble()
            ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET -> shared.timesOfNoImpactWithTargets[target]
            ImpactProperty.TIMES_IMPACT -> shared.timesOfImpact[target]
            ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT -> specific.noImpactFromImpact[target]
            ImpactProperty.TIMES_CONS_NO_IMPROVEMENT -> specific.noImpactFromImpact[target]
        }?: -1.0
    }
}

/**
 * @property id of impact, always refer to Gene of Action or structure of individual
 * @property degree of the impact
 * @property timesToManipulate presents how many times [value] the element is manipulated
 * @property timesOfImpact presents how many times [value] the change of the element (i.e., Gene, structure of individual) impacts the [Archive] with regards to target id [key]
 * @property timesOfNoImpacts presents how many times [value] the change of the element (i.e., Gene, structure of individual) did not impact the [Archive]
 */
class SharedImpactInfo(
        val id: String,
        var degree: Double = 0.0,
        var timesToManipulate: Int = 0,
        var timesOfNoImpacts: Int = 0,
        val timesOfNoImpactWithTargets: MutableMap<Int, Double> = mutableMapOf(),
        val timesOfImpact: MutableMap<Int, Double> = mutableMapOf(),
        val singleImpact : MutableMap<Int, Boolean> = mutableMapOf()){



    fun copy() : SharedImpactInfo{
        return SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact.toMutableMap(), singleImpact.toMutableMap())
    }

    fun clone() = this
}

/**
 * @property noImpactFromImpact continuous times [value] of no impact but it had impact with regards to target id [key]
 * @property noImprovement continuous times [value] of results which does not contribute to an improvement with regards to target id [key]
 */
class SpecificImpactInfo(

        val noImpactFromImpact: MutableMap<Int, Double> = mutableMapOf(),
        val noImprovement: MutableMap<Int, Double> = mutableMapOf()
){
    fun copy() : SpecificImpactInfo{
        return SpecificImpactInfo(noImpactFromImpact.toMutableMap(), noImprovement.toMutableMap())
    }

    fun clone() : SpecificImpactInfo = copy()
}

enum class ImpactProperty{
    TIMES_NO_IMPACT,
    TIMES_NO_IMPACT_WITH_TARGET,
    TIMES_IMPACT,
    TIMES_CONS_NO_IMPACT_FROM_IMPACT,
    TIMES_CONS_NO_IMPROVEMENT
}

enum class By{
    MIN,
    MAX,
    AVG
}