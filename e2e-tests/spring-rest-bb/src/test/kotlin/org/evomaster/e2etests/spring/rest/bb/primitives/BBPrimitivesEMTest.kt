package org.evomaster.e2etests.spring.rest.bb.primitives

import com.foo.rest.examples.bb.primitives.BBPrimitivesController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBPrimitivesEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBPrimitivesController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "bbprimitives",
            50,
            3,
            listOf("A","B","C","D", "E")
        ){ args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbprimitives/boolean", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbprimitives/int", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbprimitives/unquoted", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbprimitives/quoted", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbprimitives/text", null)
        }
    }
}
