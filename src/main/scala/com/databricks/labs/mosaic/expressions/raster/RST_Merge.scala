package com.databricks.labs.mosaic.expressions.raster

import com.databricks.labs.mosaic.core.raster.MosaicRaster
import com.databricks.labs.mosaic.core.raster.gdal_raster.RasterCleaner
import com.databricks.labs.mosaic.core.raster.operator.merge.MergeRasters
import com.databricks.labs.mosaic.expressions.base.{GenericExpressionFactory, WithExpressionInfo}
import com.databricks.labs.mosaic.expressions.raster.base.RasterArrayExpression
import com.databricks.labs.mosaic.functions.MosaicExpressionConfig
import org.apache.spark.sql.catalyst.analysis.FunctionRegistry.FunctionBuilder
import org.apache.spark.sql.catalyst.expressions.codegen.CodegenFallback
import org.apache.spark.sql.catalyst.expressions.{Expression, NullIntolerant}
import org.apache.spark.sql.types.ArrayType

/**
  * Returns a set of new rasters with the specified tile size (tileWidth x
  * tileHeight).
  */
case class RST_Merge(
    rastersExpr: Expression,
    expressionConfig: MosaicExpressionConfig
) extends RasterArrayExpression[RST_Merge](
      rastersExpr,
      null,
      returnsRaster = true,
      expressionConfig = expressionConfig
    )
      with NullIntolerant
      with CodegenFallback {

    /**
      * Returns a set of new rasters with the specified tile size (tileWidth x
      * tileHeight).
      */
    override def rasterTransform(rasters: Seq[MosaicRaster]): Any = {
        val result = MergeRasters.merge(rasters)
        rasters.foreach(RasterCleaner.dispose)
        result
    }

}

/** Expression info required for the expression registration for spark SQL. */
object RST_Merge extends WithExpressionInfo {

    override def name: String = "rst_merge"

    override def usage: String =
        """
          |_FUNC_(expr1) - Returns a raster that is a result of merging an array of rasters.
          |""".stripMargin

    override def example: String =
        """
          |    Examples:
          |      > SELECT _FUNC_(a, b);
          |        /path/to/raster_tile_1.tif
          |        /path/to/raster_tile_2.tif
          |        /path/to/raster_tile_3.tif
          |        ...
          |  """.stripMargin

    override def builder(expressionConfig: MosaicExpressionConfig): FunctionBuilder = {
        GenericExpressionFactory.getBaseBuilder[RST_Merge](1, expressionConfig)
    }

}
