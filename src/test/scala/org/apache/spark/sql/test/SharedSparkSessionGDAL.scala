package org.apache.spark.sql.test

import com.databricks.labs.mosaic.gdal.MosaicGDAL
import com.databricks.labs.mosaic.utils.FileUtils
import com.databricks.labs.mosaic.{MOSAIC_GDAL_NATIVE, MOSAIC_RASTER_CHECKPOINT}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.gdal.gdal.gdal

import java.nio.file.{Files, Paths}
import scala.util.Try

trait SharedSparkSessionGDAL extends SharedSparkSession {

    override def sparkConf: SparkConf = {
        super.sparkConf
            .set(MOSAIC_GDAL_NATIVE, "true")
    }

    override def createSparkSession: TestSparkSession = {
        val conf = sparkConf
        conf.set(MOSAIC_RASTER_CHECKPOINT, FileUtils.createMosaicTempDir())
        SparkSession.cleanupAnyExistingSession()
        val session = new MosaicTestSparkSession(conf)
        session.sparkContext.setLogLevel("INFO")
        Try {
            //val tempPath = Files.createTempDirectory("mosaic-gdal")
            // prepareEnvironment no longer exists
            // - only have python calls now
            //MosaicGDAL.prepareEnvironment(session, tempPath.toAbsolutePath.toString)
            MosaicGDAL.enableGDAL(session)
        }
        session
    }

    override def beforeEach(): Unit = {
        super.beforeEach()
        MosaicGDAL.enableGDAL(this.spark)
        gdal.AllRegister()
    }

}
