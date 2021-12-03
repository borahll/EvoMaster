package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * FIXME: this needs to be refactored, as at the moment the
 * keys are strings that are fixed.
 * Keys should be of any basic type, and should be modifiable.
 *
 */
class MapGene<K, V>(
        name: String,
        val template: PairGene<K, V>,
        var maxSize: Int = MAX_SIZE,
        private var elements: MutableList<PairGene<K, V>> = mutableListOf()
) : CollectionGene, Gene(name, elements)
        where K : Gene, V: Gene {

    constructor(name : String, key: K, value: V, maxSize: Int = MAX_SIZE): this(name, PairGene("template", key, value), maxSize)

    private var keyCounter = 0

    init {
        if (elements.size > maxSize) {
            throw IllegalArgumentException(
                    "More elements (${elements.size}) than allowed ($maxSize)")
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(MapGene::class.java)
        const val MAX_SIZE = 5
    }

    override fun getChildren(): MutableList<PairGene<K, V>> {
        return elements
    }

    override fun copyContent(): Gene {
        return MapGene(name,
                template.copyContent() as PairGene<K, V>,
                maxSize,
                elements.map { e -> e.copyContent() as PairGene<K, V> }.toMutableList()
        )
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is MapGene<*,*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        clearElements()
        this.elements = other.elements.map { e -> e.copyContent() as PairGene<K, V> }.toMutableList()
        addChildren(this.elements)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is MapGene<*,*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.elements.size == other.elements.size
                && this.elements.zip(other.elements) { thisElem, otherElem ->
            thisElem.containsSameValueAs(otherElem)
        }.all { it }
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        //maybe not so important here to complicate code to enable forceNewValue

        elements.clear()
        log.trace("Randomizing MapGene")
        val n = randomness.nextInt(maxSize)
        (0 until n).forEach {
            val gene = addRandomElement(randomness, false)
            elements.add(gene)
        }
        addChildren(elements)
    }

    override fun isMutable(): Boolean {
        //it wouldn't make much sense to have 0, but let's just be safe here
        return maxSize > 0
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        if(!isMutable()){
            throw IllegalStateException("Cannot mutate a immutable array")
        }
        val mutable = elements.filter { it.isMutable() }
        if ( mutable.isEmpty() || mutable.size > maxSize){
            return listOf()
        }

        val p = probabilityToModifySize(selectionStrategy, additionalGeneMutationInfo?.impact)

        return if (randomness.nextBoolean(p)) listOf() else mutable
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        /*
            element is dynamically modified, then we do not collect impacts for it now.
            thus for the internal genes, adaptive gene selection for mutation is not applicable
        */
        val s = randomness.choose(internalGenes)
        return listOf(s to additionalGeneMutationInfo.copyFoInnerGene(null, s))
    }

    /**
     * leaf mutation for arrayGene is size mutation, i.e., 'remove' or 'add'
     */
    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        if(elements.isEmpty() || (elements.size < maxSize && randomness.nextBoolean())){
            val gene = addRandomElement(randomness, false)
            elements.add(gene)
            addChild(gene)
        } else {
            log.trace("Removing gene in mutation")
            val removed = elements.removeAt(randomness.nextInt(elements.size))
            removed.removeThisFromItsBindingGenes()
        }
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return "{" +
                elements.filter { f ->
                    isPrintable(f)
                }.map {f->
                    """
                    ${getKeyValueAsPrintableString(f.first, targetFormat)}:${f.second.getValueAsPrintableString(targetFormat = targetFormat)}
                    """
                }.joinToString(",") +
                "}"
    }

    private fun getKeyValueAsPrintableString(key: Gene, targetFormat: OutputFormat?): String {
        val keyString = key.getValueAsPrintableString(targetFormat = targetFormat)
        if (!keyString.startsWith("\""))
            return "\"$keyString\""
        return keyString
    }

    private fun isPrintable(pairGene: PairGene<K, V>): Boolean {
        val keyT = ParamUtil.getValueGene(pairGene.first)
        val valueT = pairGene.second
        return (keyT is LongGene || keyT is StringGene || keyT is IntegerGene) &&
                (valueT !is CycleObjectGene && (valueT !is OptionalGene || valueT.isActive))
    }

    override fun isPrintable(): Boolean {
        return isPrintable(template)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(elements.flatMap { g -> g.flatView(excludePredicate) })
    }

    /**
     * 1 is for 'remove' or 'add' element
     */
    override fun mutationWeight(): Double {
        return 1.0 + elements.map { it.mutationWeight() }.sum()
    }

    override fun innerGene(): List<Gene> = elements


    /*
        Note that value binding cannot be performed on the [elements]
     */
    override fun bindValueBasedOn(gene: Gene): Boolean {
        if(gene is MapGene<*,*> && gene.template::class.java.simpleName == template::class.java.simpleName){
            clearElements()
            elements = gene.elements.mapNotNull { it.copyContent() as? PairGene<K, V> }.toMutableList()
            addChildren(elements)
            return true
        }
        LoggingUtil.uniqueWarn(log, "cannot bind the MapGene with the template (${template::class.java.simpleName}) with the gene ${gene::class.java.simpleName}")
        return false
    }

    override fun clearElements() {
        elements.forEach { it.removeThisFromItsBindingGenes() }
        elements.clear()
    }

    /**
     * remove an existing element [element] (key to value) from [elements]
     */
    fun removeExistingElement(element: PairGene<K, V>){
        //this is a reference heap check, not based on `equalsTo`
        if (elements.contains(element)){
            elements.remove(element)
            element.removeThisFromItsBindingGenes()
        }else{
            log.warn("the specified element (${if (element.isPrintable()) element.getValueAsPrintableString() else "not printable"})) does not exist in this map")
        }
    }

    /**
     * add [element] (key to value) to [elements],
     * if the key of [element] exists in [elements],
     * we replace the existing one with [element]
     */
    fun addElement(element: PairGene<K, V>){
        getElementsBy(element).forEach { e->
            removeExistingElement(e)
        }
        elements.add(element)
        addChild(element)
    }

    /**
     * @return all elements
     */
    fun getAllElements() = elements

    /**
     * @return whether the key of [pairGene] exists in [elements] of this map
     */
    fun containsKey(pairGene: PairGene<K, V>): Boolean{
        return getElementsBy(pairGene).isNotEmpty()
    }

    /**
     * @return a list of elements from [elements] which has the same key with [pairGene]
     */
    private fun getElementsBy(pairGene: PairGene<K, V>): List<PairGene<K, V>>{
        val geneValue = ParamUtil.getValueGene(pairGene.first)
        /*
            currently we only support Integer, String and LongGene
            TODO support other types if needed
         */
        if (geneValue is IntegerGene || geneValue is StringGene || geneValue is LongGene){
            return elements.filter { ParamUtil.getValueGene(it.first).containsSameValueAs(geneValue) }
        }
        return listOf()
    }

    private fun addRandomElement(randomness: Randomness, forceNewValue: Boolean) : PairGene<K, V>{
        val keyName = "key_${keyCounter++}"

        val gene = template.copy() as PairGene<K, V>
        gene.randomize(randomness, forceNewValue)

        gene.name = keyName
        gene.first.name = keyName
        gene.second.name = keyName

        if (containsKey(gene)){
            // try one more time
            gene.first.randomize(randomness, true, elements.map { it.first })
        }

        return gene
    }
}