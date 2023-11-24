package com.databricks.labs.mosaic.expressions.raster

import com.databricks.labs.mosaic.core.geometry.api.GeometryAPI
import com.databricks.labs.mosaic.core.index.IndexSystem
import com.databricks.labs.mosaic.functions.MosaicContext
import org.apache.spark.sql.QueryTest
import org.apache.spark.sql.functions.lit
import org.scalatest.matchers.should.Matchers._

trait RST_TessellateBehaviors extends QueryTest {

    // noinspection MapGetGet
    def tessellateBehavior(indexSystem: IndexSystem, geometryAPI: GeometryAPI): Unit = {
        val mc = MosaicContext.build(indexSystem, geometryAPI)
        mc.register()
        val sc = spark
        import mc.functions._
        import sc.implicits._

        val rastersInMemory = spark.read
            .format("gdal")
            .option("raster_storage", "in-memory")
            .load("src/test/resources/modis")

        val gridTiles = rastersInMemory
            .withColumn("tiles", rst_tessellate($"tile", 3))
            .select("tiles")

        rastersInMemory
            .createOrReplaceTempView("source")

        noException should be thrownBy spark.sql("""
                                                   |select rst_tessellate(tile, 3) from source
                                                   |""".stripMargin)

        noException should be thrownBy rastersInMemory
            .withColumn("tiles", rst_tessellate($"tile", 3))
            .select("tiles")

        val result = gridTiles.collect()

        result.length should be(980)

        val netcdf = spark.read
            .format("gdal")
            .option("raster.read.strategy", "in-memory")
            .load("src/test/resources/binary/netcdf-CMIP5/prAdjust_day_HadGEM2-CC_SMHI-DBSrev930-GFD-1981-2010-postproc_rcp45_r1i1p1_20201201-20201231.nc")
            .withColumn("tile", rst_separatebands($"tile"))
            .withColumn("tile", rst_setsrid($"tile", lit(4326)))

        val netcdfGridTiles = netcdf
            .select(rst_tessellate($"tile", lit(3)).alias("tile"))

        val netcdfResult = netcdfGridTiles.collect()

        netcdfResult.length should be(4495)

    }

}
