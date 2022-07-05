package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents a collection of points without lines between them.
 * Column types MULTIPOINT of databases H2 and PostgreSQL are supported by
 * this gene.
 */
class SqlMultiPointGene(
        name: String,
        /**
         * The database type of the source column for this gene
         */
        val databaseType: DatabaseType = DatabaseType.POSTGRES,
        val points: ArrayGene<SqlPointGene> = ArrayGene(
                name = "points",
                minSize = 0,
                template = SqlPointGene("p", databaseType = databaseType))
) : CompositeFixedGene(name, mutableListOf(points)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlMultiPointGene::class.java)
    }

    override fun copyContent(): Gene = SqlMultiPointGene(
            name,
            databaseType = this.databaseType,
            points = points.copy() as ArrayGene<SqlPointGene>
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean, allGenes: List<Gene>) {
        points.randomize(randomness, tryToForceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            allGenes: List<Gene>,
            selectionStrategy: SubsetGeneSelectionStrategy,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(points)
    }

    override fun getValueAsPrintableString(
            previousGenes: List<Gene>,
            mode: GeneUtils.EscapeMode?,
            targetFormat: OutputFormat?,
            extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.POSTGRES -> {
                "\"LINESTRING(${
                    points.getViewOfElements().joinToString(" , ") {
                        it.x.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) +
                                " " + it.y.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                    }
                })\""
            }
            DatabaseType.H2 -> {
                if (points.isEmpty()) "\"MULTIPOINT EMPTY\""
                else
                    "\"MULTIPOINT(${
                        points.getViewOfElements().joinToString(" , ") {
                            it.x.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) +
                                    " " + it.y.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                        }
                    })\""
            }
            else -> {
                throw IllegalArgumentException("Unsupported SqlMultiPointGene.getValueAsPrintableString() for $databaseType")
            }
        }
    }

    override fun getValueAsRawString(): String {
        return "( ${
            points.getViewOfElements()
                    .map { it.getValueAsRawString() }
                    .joinToString(" , ")
        } ) "
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlMultiPointGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.points.copyValueFrom(other.points)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlMultiPointGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.points.containsSameValueAs(other.points)
    }


    override fun innerGene(): List<Gene> = listOf(points)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlMultiPointGene -> {
                points.bindValueBasedOn(gene.points)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

}