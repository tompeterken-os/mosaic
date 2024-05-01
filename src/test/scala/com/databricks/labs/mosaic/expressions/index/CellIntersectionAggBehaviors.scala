package com.databricks.labs.mosaic.expressions.index

import com.databricks.labs.mosaic.test.MosaicSpatialQueryTest
import com.databricks.labs.mosaic.functions.MosaicContext
import com.databricks.labs.mosaic.test.mocks
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions.{col, lit, struct}
import org.apache.spark.sql.types._
import org.scalatest.matchers.should.Matchers._

trait CellIntersectionAggBehaviors extends MosaicSpatialQueryTest {

    def behaviorComputedColumns(mosaicContext: MosaicContext): Unit = {
        spark.sparkContext.setLogLevel("ERROR")
        val mc = mosaicContext
        import mc.functions._
        mc.register(spark)
        val sc = spark
        import sc.implicits._

        // AggregateIntersection of exclusively core chips should return the first element in the buffer
        val onlyCoreChips =
            Seq(Row(1, true, 1L, "POLYGON ((0 0, 2 0, 2 1, 0 1, 0 0))"), Row(1, true, 1L, "POLYGON ((1 0, 3 0, 3 1, 1 1, 1 0))"))
        // AggregateIntersection of a mix of core and boundary chips should ignore the core chips
        val ignoresCoreChips =
            Seq(Row(2, true, 1L, "POLYGON ((0 0, 2 0, 2 1, 0 1, 0 0))"), Row(2, false, 1L, "POLYGON ((1 0, 3 0, 3 1, 1 1, 1 0))"))
        // AggregateIntersection with multiple boundary chips should calculate the WKB intersection
        val intersectBoundaryChips =
            Seq(Row(3, false, 1L, "POLYGON ((0 0, 2 0, 2 1, 0 1, 0 0))"), Row(3, false, 1L, "POLYGON ((1 0, 3 0, 3 1, 1 1, 1 0))"))

        val testCases: Seq[Row] = Seq(onlyCoreChips, ignoresCoreChips, intersectBoundaryChips).flatten
        val testCaseSchema = StructType(
          Array(
            StructField("case_id", IntegerType, true),
            StructField("is_core", BooleanType, true),
            StructField("index_id", LongType, true),
            StructField("wkt", StringType, true)
          )
        )

        val expected = Seq(
          Row(1, "POLYGON ((0 0, 2 0, 2 1, 0 1, 0 0))"),
          Row(2, "POLYGON ((1 0, 3 0, 3 1, 1 1, 1 0))"),
          Row(3, "POLYGON ((1 0, 2 0, 2 1, 1 1, 1 0))")
        )
        val expectedSchema = StructType(
          Array(
            StructField("case_id", IntegerType, true),
            StructField("expected_wkt", StringType, true)
          )
        )

        val in_df = spark
            .createDataFrame(spark.sparkContext.parallelize(testCases), testCaseSchema)
            .withColumn("wkb", st_aswkb($"wkt"))
            .select($"case_id", struct("is_core", "index_id", "wkb").alias("chip"))
        val exp_df = spark.createDataFrame(spark.sparkContext.parallelize(expected), expectedSchema)

        val res = in_df
            .groupBy("case_id", "chip.index_id")
            .agg(grid_cell_intersection_agg($"chip").alias("intersection_chips"))
            .select($"case_id", st_aswkt($"intersection_chips.wkb").alias("actual_wkt"))
            .join(exp_df, "case_id")
            .select($"actual_wkt", $"expected_wkt")
            .as[(String, String)]
            .collect()
            .map(r => (mc.getGeometryAPI.geometry(r._1, "WKT"), mc.getGeometryAPI.geometry(r._2, "WKT")))

        res.foreach { case (actual, expected) => actual.equalsTopo(expected) shouldEqual true }

        in_df.createOrReplaceTempView("source")

        noException should be thrownBy spark
            .sql("""with subquery (
                   | select grid_cell_intersection_agg(chip) as intersection_chips from source
                   | group by case_id, chip.index_id
                   |) select st_aswkt(intersection_chips.wkb) from subquery""".stripMargin)
            .as[String]
            .collect()
            .map(wkt => mc.getGeometryAPI.geometry(wkt, "WKT"))

    }

    def columnFunctionSignatures(mosaicContext: MosaicContext): Unit = {
        val funcs = mosaicContext.functions
        noException should be thrownBy funcs.grid_cell_intersection_agg(col("wkt"))
    }

    def auxiliaryMethods(mosaicContext: MosaicContext): Unit = {
        spark.sparkContext.setLogLevel("ERROR")
        val sc = spark
        import sc.implicits._
        val mc = mosaicContext
        mc.register(spark)

        val wkt = mocks.getWKTRowsDf(mc.getIndexSystem).limit(1).select("wkt").as[String].collect().head

        val cellIntersectionAggExpr = CellIntersectionAgg(
          lit(wkt).expr,
          mc.getGeometryAPI.name,
          mc.getIndexSystem
        )

        noException should be thrownBy mc.functions.grid_cell_intersection_agg(lit(""))
        noException should be thrownBy mc.functions.grid_cell_intersection_agg(lit(""))
    }

}
