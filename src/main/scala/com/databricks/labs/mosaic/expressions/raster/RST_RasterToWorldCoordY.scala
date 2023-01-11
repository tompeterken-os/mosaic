package com.databricks.labs.mosaic.expressions.raster

import com.databricks.labs.mosaic.core.raster.MosaicRaster
import com.databricks.labs.mosaic.expressions.base.{GenericExpressionFactory, WithExpressionInfo}
import com.databricks.labs.mosaic.expressions.raster.base.Raster2ArgExpression
import com.databricks.labs.mosaic.functions.MosaicExpressionConfig
import org.apache.spark.sql.catalyst.analysis.FunctionRegistry.FunctionBuilder
import org.apache.spark.sql.catalyst.expressions.{Expression, NullIntolerant}
import org.apache.spark.sql.catalyst.expressions.codegen.CodegenFallback
import org.apache.spark.sql.types._

/** Returns the world coordinates of the raster (x,y) pixel. */
case class RST_RasterToWorldCoordY(
    path: Expression,
    x: Expression,
    y: Expression,
    expressionConfig: MosaicExpressionConfig
) extends Raster2ArgExpression[RST_RasterToWorldCoordY](path, x, y, DoubleType, expressionConfig)
      with NullIntolerant
      with CodegenFallback {

    /** Returns the world coordinates of the raster (x,y) pixel. */
    override def rasterTransform(raster: MosaicRaster, arg1: Any, arg2: Any): Any = {
        val x = arg1.asInstanceOf[Int]
        val y = arg2.asInstanceOf[Int]
        val gt = raster.getRaster.GetGeoTransform()

        val (_, yGeo) = rasterAPI.toWorldCoord(gt, x, y)
        yGeo
    }

}

/** Expression info required for the expression registration for spark SQL. */
object RST_RasterToWorldCoordY extends WithExpressionInfo {

    override def name: String = "rst_rastertoworldcoordy"

    override def usage: String =
        """
          |_FUNC_(expr1) - Returns the y coordinate of the pixel in world coordinates using geo transform of the raster.
          |""".stripMargin

    override def example: String =
        """
          |    Examples:
          |      > SELECT _FUNC_(a, b);
          |        11.2
          |  """.stripMargin

    override def builder(expressionConfig: MosaicExpressionConfig): FunctionBuilder = {
        GenericExpressionFactory.getBaseBuilder[RST_RasterToWorldCoordY](3, expressionConfig)
    }

}
