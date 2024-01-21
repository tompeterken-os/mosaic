package com.databricks.labs.mosaic.expressions.geometry

import com.databricks.labs.mosaic.core.geometry.api.GeometryAPI
import com.databricks.labs.mosaic.core.index._
import com.databricks.labs.mosaic.functions.MosaicContext
import com.databricks.labs.mosaic.test.mocks
import com.databricks.labs.mosaic.test.mocks.getWKTRowsDf
import org.apache.spark.sql.QueryTest
import org.apache.spark.sql.catalyst.expressions.codegen.{CodegenContext, CodeGenerator}
import org.apache.spark.sql.execution.WholeStageCodegenExec
import org.apache.spark.sql._
import org.apache.spark.sql.functions.{col, lit}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.matchers.should.Matchers.{an, be, convertToAnyShouldWrapper}

import scala.collection.JavaConverters._

trait ST_BufferBehaviors extends QueryTest {

    def bufferBehavior(indexSystem: IndexSystem, geometryAPI: GeometryAPI): Unit = {
        spark.sparkContext.setLogLevel("FATAL")
        val mc = MosaicContext.build(indexSystem, geometryAPI)
        import mc.functions._
        val sc = spark
        import sc.implicits._
        mc.register(spark)

        val referenceGeoms = mocks
            .getWKTRowsDf()
            .orderBy("id")
            .select("wkt")
            .as[String]
            .collect()
            .map(mc.getGeometryAPI.geometry(_, "WKT"))

        val expected = referenceGeoms.map(_.buffer(1).getLength)
        val result = mocks
            .getWKTRowsDf()
            .orderBy("id")
            .select(st_length(st_buffer($"wkt", lit(1))))
            .as[Double]
            .collect()

        result.zip(expected).foreach { case (l, r) => math.abs(l - r) should be < 1e-8 }

        mocks.getWKTRowsDf().createOrReplaceTempView("source")

        val sqlResult = spark
            .sql("select id, st_length(st_buffer(wkt, 1.0)) from source")
            .orderBy("id")
            .drop("id")
            .as[Double]
            .collect()

        sqlResult.zip(expected).foreach { case (l, r) => math.abs(l - r) should be < 1e-8 }

        mocks.getWKTRowsDf().select(st_buffer_cap_style($"wkt", lit(1), lit("round"))).collect()
        mocks.getWKTRowsDf().select(st_buffer_cap_style($"wkt", 1, "round")).collect()

        val sourceDf = testData(spark)

        val noExtraParamResult = sourceDf
            .where($"bufferStyleParameters" === "")
            .withColumn("geomBufferedTest", st_buffer(convert_to($"geom", "COORDS"), $"buffer"))
            .select(
              st_distance($"geomBufferedRef", $"geomBufferedTest"),
              st_area(st_intersection($"geomBufferedRef", $"geomBufferedTest")),
              st_area(st_union($"geomBufferedRef", $"geomBufferedTest")),
              st_area($"geomBufferedRef")
            )
            .as[(Double, Double, Double, Double)]
            .collect()

        noExtraParamResult.foreach({ case (d, a1, a2, a3) =>
            d should be < 1e-9
            a1 shouldBe a2 +- 1e-9
            a1 shouldBe a3 +- 1e-9
        })

        val extraParamResult = sourceDf
            .withColumn("geomBufferedTest", st_buffer(convert_to($"geom", "COORDS"), $"buffer", $"bufferStyleParameters"))
            .select(
              st_distance($"geomBufferedRef", $"geomBufferedTest"),
              st_area(st_intersection($"geomBufferedRef", $"geomBufferedTest")),
              st_area(st_union($"geomBufferedRef", $"geomBufferedTest")),
              st_area($"geomBufferedRef"),
            )
            .as[(Double, Double, Double, Double)]
            .collect()

        extraParamResult.foreach({ case (d, a1, a2, a3) =>
            d should be < 1e-9
            a1 shouldBe a2 +- 1e-9
            a1 shouldBe a3 +- 1e-9
        })

    }

    def bufferCodegen(indexSystem: IndexSystem, geometryAPI: GeometryAPI): Unit = {
        spark.sparkContext.setLogLevel("FATAL")
        val mc = MosaicContext.build(indexSystem, geometryAPI)
        val sc = spark
        import mc.functions._
        import sc.implicits._
        mc.register(spark)

        val result = mocks
            .getWKTRowsDf()
            .select(st_length(st_buffer($"wkt", lit(1))))

        val queryExecution = result.queryExecution
        val plan = queryExecution.executedPlan

        val wholeStageCodegenExec = plan.find(_.isInstanceOf[WholeStageCodegenExec])

        wholeStageCodegenExec.isDefined shouldBe true

        val codeGenStage = wholeStageCodegenExec.get.asInstanceOf[WholeStageCodegenExec]
        val (_, code) = codeGenStage.doCodeGen()

        noException should be thrownBy CodeGenerator.compile(code)

        val stBuffer = ST_Buffer(lit(1).expr, lit(1).expr, mc.expressionConfig)
        val ctx = new CodegenContext
        an[Error] should be thrownBy stBuffer.genCode(ctx)
    }

    def testData(spark: SparkSession): DataFrame = {
        // Comparison vs. PostGIS
        val testDataWKT = List(
          (
            "POLYGON((30 10,40 40,20 40,10 20,30 10))",
            2.0,
            "",
            "POLYGON((29.105572809000083 8.211145618000169,9.105572809000083 18.211145618000167,8.773770799250563 18.420012042061902,8.489092090008587 18.689596517279742,8.262476724420662 19.009539063455783,8.102633403898972 19.367544467966326,8.015704817609999 19.749854783887773,8.005031582374714 20.14177804018136,8.071023864637592 20.528252845900816,8.211145618000169 20.894427190999917,18.211145618000167 40.89442719099991,18.40562298012387 41.207460938701914,18.65423405443726 41.47949789447759,18.948537775761732 41.70130161670408,19.278541681368626 41.865341227356964,19.63304118399603 41.96604710710526,20 42,40 42,40.37625388633239 41.96428944227163,40.73907153653638 41.85843301301978,41.07549652996949 41.68621090437216,41.37351494248246 41.45377326388176,41.622484370351124 41.16942056932753,41.813513976448974 40.84330721402359,41.939781986999634 40.4870788877705,41.99677930092308 40.11345670277741,41.9824705123501 39.73578291565012,41.89736659610103 39.367544467966326,31.897366596101026 9.367544467966324,31.743483999644496 9.02006962339989,31.5266535144039 8.707974827273778,31.25470373030189 8.44252815461514,30.937453300545492 8.233313466034124,30.5863564425368 8.087884385740342,30.214089387302234 8.011491580544716,29.83409270854116 8.006893186344296,29.46008605534576 8.074255226576508,29.105572809000083 8.211145618000169))"
          ),
          (
            "POLYGON((30 10,40 40,20 40,10 20,30 10))",
            3.0,
            "",
            "POLYGON((28.658359213500127 7.316718427000252,8.658359213500127 17.316718427000254,8.160656198875845 17.630018063092855,7.733638135012881 18.034394775919612,7.393715086630994 18.51430859518367,7.153950105848459 19.051316701949485,7.023557226414999 19.624782175831655,7.007547373562073 20.212667060272036,7.106535796956389 20.792379268851224,7.316718427000252 21.341640786499873,17.316718427000254 41.34164078649987,17.60843447018581 41.81119140805288,17.981351081655895 42.21924684171639,18.4228066636426 42.55195242505612,18.91781252205294 42.798011841035446,19.44956177599405 42.949070660657895,20 43,40 43,40.56438082949858 42.94643416340744,41.10860730480457 42.78764951952966,41.613244794954234 42.52931635655824,42.06027241372369 42.18065989582263,42.43372655552668 41.7541308539913,42.72027096467346 41.26496082103538,42.90967298049945 40.73061833165575,42.99516895138463 40.17018505416612,42.97370576852514 39.60367437347518,42.84604989415154 39.05131670194949,32.84604989415154 9.051316701949487,32.615225999466745 8.530104435099837,32.28998027160585 8.061962240910669,31.882055595452837 7.663792231922711,31.40617995081824 7.349970199051186,30.879534663805202 7.131826578610514,30.321134080953357 7.017237370817074,29.75113906281174 7.010339779516444,29.19012908301864 7.111382839864762,28.658359213500127 7.316718427000252))"
          ),
          (
            "POLYGON((30 10,40 40,20 40,10 20,30 10))",
            3.0,
            "quad_segs=2",
            "POLYGON((28.658359213500127 7.316718427000252,8.658359213500127 17.316718427000254,7.153950105848459 19.051316701949485,7.316718427000252 21.341640786499873,17.316718427000254 41.34164078649987,20 43,40 43,42.43372655552668 41.7541308539913,42.84604989415154 39.05131670194949,32.84604989415154 9.051316701949487,31.14805029709527 7.22836140246614,28.658359213500127 7.316718427000252))"
          ),
          (
            "MULTIPOLYGON(((0 0,0 1,2 2,0 0)))",
            2.0,
            "",
            "POLYGON((1.414213562373095 -1.414213562373095,1.111140466039204 -1.662939224605091,0.76536686473018 -1.847759065022573,0.390180644032257 -1.961570560806461,-3.673940397442059e-16 -2,-0.390180644032257 -1.961570560806461,-0.765366864730181 -1.847759065022573,-1.111140466039204 -1.66293922460509,-1.414213562373095 -1.414213562373095,-1.662939224605091 -1.111140466039204,-1.847759065022574 -0.765366864730179,-1.961570560806461 -0.390180644032257,-2 0,-2 1,-1.966047107105261 1.366958816003967,-1.865341227356966 1.721458318631373,-1.70130161670408 2.051462224238267,-1.479497894477594 2.345765945562737,-1.207460938701918 2.594377019876128,-0.894427190999916 2.788854381999832,1.105572809000084 3.788854381999832,1.481529468137407 3.931628408258203,1.878448906059273 3.996302915782525,2.280282892709963 3.980262987598904,2.670784493205818 3.884157149410477,3.034164963224574 3.711871148433408,3.355732118853176 3.470370845028517,3.622484370351121 3.16942056932753,3.823636401881189 2.821188330246984,3.951055242946718 2.439753839062402,3.999589100313399 2.040539239112909,3.9672756550971 1.639685558348996,3.855421403158644 1.253400095967856,3.668548830917987 0.897301129572439,3.414213562373095 0.585786437626905,1.414213562373095 -1.414213562373095))"
          ),
          (
            "MULTIPOLYGON(((40 60,20 45,45 30,40 60)), ((20 35,10 30,10 10,30 5,45 20,20 35), (30 20,20 15,20 25,30 20)))",
            2.0,
            "",
            "MULTIPOLYGON(((21.028991510855054 36.71498585142509,46.02899151085505 21.714985851425087,46.351780955338434 21.474004154941333,46.619733646867054 21.173227562413512,46.82197973593542 20.824857467591027,46.95031484684309 20.443025956557175,46.999532898867834 20.043222521273073,46.9676372977026 19.641665708186324,46.85592192990094 19.25464519179602,46.66891867468572 18.897860962815827,46.41421356237309 18.585786437626904,31.414213562373096 3.585786437626905,31.094914714141236 3.326332838119534,30.72928238054107 3.137703780427949,30.332789018157783 3.02788147683929,29.92221302915125 3.001513275709299,29.514928749927336 3.059714999709336,9.514928749927334 8.059714999709335,9.15830172540112 8.185738713818413,8.83177905204805 8.376651664994155,8.547039206017194 8.625625621910517,8.314266250382676 8.923755731536227,8.141785589878605 9.260379011914079,8.035766202785066 9.623455726535903,8 10,8 30,8.033952892894739 30.36695881600397,8.134658772643034 30.721458318631374,8.298698383295921 31.051462224238268,8.520502105522406 31.345765945562736,8.792539061298083 31.594377019876127,9.105572809000083 31.788854381999833,19.105572809000083 36.78885438199983,19.480363108468328 36.93131496679317,19.87603797663762 36.99615465752629,20.27669496489004 36.9807675018549,20.66623138973048 36.88577192028564,21.028991510855054 36.71498585142509),(25.527864045000417 20,22 21.763932022500207,22 18.236067977499793,25.527864045000417 20)),((41.972787847664286 60.328797974610715,46.972787847664286 30.328797974610715,46.9993974080564 29.950908201730538,46.954274213176696 29.574779704491174,46.83903715633888 29.21390691543241,46.657820619013656 28.881236935194416,46.417126140850286 28.58870502696326,46.12558916272103 28.346806412798255,45.79366921139157 28.164219734584478,45.43327464112035 28.047495688772486,45.05733539540255 28.000822005814882,44.67933911696567 28.025873206176552,44.31284724908532 28.121750523250334,43.97100848914495 28.285014148574913,18.971008489144946 43.28501414857491,18.65706596538611 43.51793111540804,18.394426953504244 43.80746690093468,18.19312493490509 44.14256049826342,18.060850161109478 44.51041047567288,18.00265586649343 44.89696402401638,18.0207652218219 45.28745380994221,18.114486403444406 45.666962125764265,18.280239022726388 46.02099078401664,18.511690906044425 46.33601498526369,18.8 46.6,38.8 61.6,39.15068013648958 61.81070587601815,39.53689868768618 61.94564569604366,39.94249496357223 61.99917312176446,40.3504974446115 61.969048384707904,40.74383392750859 61.85653200572658,41.106045883917275 61.66633205054384,41.4219771409141 61.406407128365665,41.678408066517704 61.08763337676274,41.86460872378755 60.72334936730143,41.972787847664286 60.328797974610715)))"
          ),
          (
            "POINT(-75.78033 35.18937)",
            2.0,
            "",
            "POLYGON((-73.78033 35.18937,-73.81875943919354 34.79918935596774,-73.93257093497743 34.424003135269814,-74.11739077539491 34.078229533960794,-74.36611643762691 33.775156437626904,-74.6691895339608 33.52643077539491,-75.01496313526982 33.341610934977425,-75.39014935596775 33.22779943919353,-75.78033 33.18937,-76.17051064403226 33.22779943919353,-76.54569686473019 33.341610934977425,-76.89147046603921 33.52643077539491,-77.1945435623731 33.775156437626904,-77.4432692246051 34.078229533960794,-77.62808906502258 34.424003135269814,-77.74190056080647 34.79918935596774,-77.78033 35.18937,-77.74190056080647 35.57955064403225,-77.62808906502258 35.95473686473018,-77.4432692246051 36.3005104660392,-77.1945435623731 36.60358356237309,-76.89147046603921 36.85230922460509,-76.54569686473019 37.03712906502257,-76.17051064403226 37.150940560806454,-75.78033 37.18937,-75.39014935596775 37.15094056080646,-75.01496313526982 37.03712906502257,-74.6691895339608 36.85230922460509,-74.36611643762691 36.60358356237309,-74.11739077539491 36.3005104660392,-73.93257093497743 35.95473686473018,-73.81875943919354 35.57955064403225,-73.78033 35.18937))"
          ),
          (
            "MULTIPOINT(10 40,40 30,20 20,30 10)",
            2.0,
            "",
            "MULTIPOLYGON(((42 30,41.961570560806464 29.609819355967744,41.84775906502257 29.23463313526982,41.66293922460509 28.888859533960797,41.41421356237309 28.585786437626904,41.1111404660392 28.33706077539491,40.76536686473018 28.152240934977428,40.390180644032256 28.03842943919354,40 28,39.609819355967744 28.03842943919354,39.23463313526982 28.152240934977428,38.8888595339608 28.33706077539491,38.58578643762691 28.585786437626904,38.33706077539491 28.888859533960797,38.15224093497743 29.23463313526982,38.038429439193536 29.609819355967744,38 30,38.038429439193536 30.390180644032256,38.15224093497743 30.76536686473018,38.33706077539491 31.111140466039203,38.58578643762691 31.414213562373096,38.8888595339608 31.66293922460509,39.23463313526982 31.847759065022572,39.609819355967744 31.96157056080646,40 32,40.390180644032256 31.96157056080646,40.76536686473018 31.847759065022572,41.1111404660392 31.66293922460509,41.41421356237309 31.414213562373096,41.66293922460509 31.111140466039203,41.84775906502257 30.76536686473018,41.96157056080646 30.390180644032256,42 30)),((32 10,31.96157056080646 9.609819355967744,31.847759065022572 9.23463313526982,31.66293922460509 8.888859533960796,31.414213562373096 8.585786437626904,31.111140466039206 8.33706077539491,30.76536686473018 8.152240934977426,30.390180644032256 8.03842943919354,30 8,29.609819355967744 8.03842943919354,29.23463313526982 8.152240934977426,28.888859533960797 8.33706077539491,28.585786437626904 8.585786437626904,28.33706077539491 8.888859533960796,28.152240934977428 9.23463313526982,28.03842943919354 9.609819355967742,28 10,28.03842943919354 10.390180644032256,28.152240934977428 10.76536686473018,28.33706077539491 11.111140466039204,28.585786437626904 11.414213562373096,28.888859533960797 11.66293922460509,29.23463313526982 11.847759065022572,29.609819355967744 11.96157056080646,30 12,30.390180644032256 11.96157056080646,30.76536686473018 11.847759065022574,31.111140466039203 11.662939224605092,31.414213562373096 11.414213562373096,31.66293922460509 11.111140466039204,31.847759065022572 10.765366864730181,31.96157056080646 10.390180644032258,32 10)),((22 20,21.96157056080646 19.609819355967744,21.847759065022572 19.23463313526982,21.66293922460509 18.888859533960797,21.414213562373096 18.585786437626904,21.111140466039206 18.33706077539491,20.76536686473018 18.152240934977428,20.390180644032256 18.03842943919354,20 18,19.609819355967744 18.03842943919354,19.23463313526982 18.152240934977428,18.888859533960797 18.33706077539491,18.585786437626904 18.585786437626904,18.33706077539491 18.888859533960797,18.152240934977428 19.23463313526982,18.03842943919354 19.609819355967744,18 20,18.03842943919354 20.390180644032256,18.152240934977428 20.76536686473018,18.33706077539491 21.111140466039203,18.585786437626904 21.414213562373096,18.888859533960797 21.66293922460509,19.23463313526982 21.847759065022572,19.609819355967744 21.96157056080646,20 22,20.390180644032256 21.96157056080646,20.76536686473018 21.847759065022572,21.111140466039203 21.66293922460509,21.414213562373096 21.414213562373096,21.66293922460509 21.111140466039203,21.847759065022572 20.76536686473018,21.96157056080646 20.390180644032256,22 20)),((12 40,11.96157056080646 39.609819355967744,11.847759065022574 39.23463313526982,11.66293922460509 38.8888595339608,11.414213562373096 38.58578643762691,11.111140466039204 38.33706077539491,10.76536686473018 38.15224093497743,10.390180644032256 38.038429439193536,10 38,9.609819355967744 38.038429439193536,9.23463313526982 38.15224093497743,8.888859533960796 38.33706077539491,8.585786437626904 38.58578643762691,8.33706077539491 38.8888595339608,8.152240934977426 39.23463313526982,8.03842943919354 39.609819355967744,8 40,8.03842943919354 40.390180644032256,8.152240934977426 40.76536686473018,8.337060775394908 41.1111404660392,8.585786437626904 41.41421356237309,8.888859533960796 41.66293922460509,9.234633135269819 41.84775906502257,9.609819355967742 41.96157056080646,10 42,10.390180644032256 41.961570560806464,10.76536686473018 41.84775906502257,11.111140466039204 41.66293922460509,11.414213562373094 41.41421356237309,11.66293922460509 41.1111404660392,11.847759065022572 40.76536686473018,11.96157056080646 40.390180644032256,12 40)))"
          ),
          (
            "LINESTRING(30 10,10 30,40 40)",
            2.0,
            "",
            "POLYGON((8.585786437626904 28.585786437626904,8.329219317019618 28.900685709462664,8.141566986980227 29.26092846346362,8.030569554367744 29.651655745250856,8.000805336911114 30.056751203308092,8.05350202106454 30.45950584109472,8.186486023551023 30.843307214023586,8.394272145281546 31.19232464395452,8.668289819689326 31.49216218812062,8.997236623400621 31.730452429438905,9.367544467966324 31.897366596101026,39.367544467966326 41.89736659610103,39.74985478388777 41.98429518239,40.14177804018136 41.99496841762529,40.528252845900816 41.928976135362404,40.89442719099991 41.78885438199983,41.22622920074944 41.5799879579381,41.51090790999141 41.31040348272026,41.737523275579335 40.99046093654422,41.89736659610103 40.632455532033674,41.98429518239 40.25014521611223,41.99496841762529 39.85822195981864,41.928976135362404 39.471747154099184,41.78885438199983 39.10557280900009,41.579987957938094 38.77377079925056,41.31040348272026 38.48909209000859,40.99046093654422 38.262476724420665,40.632455532033674 38.10263340389897,13.702459173643835 29.12596795110236,31.414213562373096 11.414213562373096,31.66293922460509 11.111140466039204,31.847759065022572 10.76536686473018,31.96157056080646 10.390180644032256,32 10,31.96157056080646 9.609819355967744,31.847759065022572 9.23463313526982,31.66293922460509 8.888859533960796,31.414213562373096 8.585786437626904,31.111140466039206 8.33706077539491,30.76536686473018 8.152240934977426,30.390180644032256 8.03842943919354,30 8,29.609819355967744 8.03842943919354,29.23463313526982 8.152240934977426,28.888859533960797 8.33706077539491,28.585786437626904 8.585786437626904,8.585786437626904 28.585786437626904))"
          ),
          (
            "LINESTRING(30 10,10 30,40 40)",
            2.0,
            "endcap=square",
            "POLYGON((8.585786437626904 28.585786437626904,8.329219317019618 28.900685709462664,8.141566986980227 29.26092846346362,8.030569554367744 29.651655745250856,8.000805336911114 30.056751203308092,8.05350202106454 30.45950584109472,8.186486023551023 30.843307214023586,8.394272145281546 31.19232464395452,8.668289819689326 31.49216218812062,8.997236623400621 31.730452429438905,9.367544467966324 31.897366596101026,39.367544467966326 41.89736659610103,41.264911064067356 42.529822128134704,42.529822128134704 38.735088935932644,13.702459173643835 29.12596795110236,31.414213562373096 11.414213562373096,32.82842712474619 10,30 7.171572875253809,8.585786437626904 28.585786437626904))"
          ),
          (
            "LINESTRING(30 10,10 30,40 40)",
            2.0,
            "quad_segs=8 endcap=flat",
            "POLYGON((8.585786437626904 28.585786437626904,8.329219317019618 28.900685709462664,8.141566986980227 29.26092846346362,8.030569554367744 29.651655745250856,8.000805336911114 30.056751203308092,8.05350202106454 30.45950584109472,8.186486023551023 30.843307214023586,8.394272145281546 31.19232464395452,8.668289819689326 31.49216218812062,8.997236623400621 31.730452429438905,9.367544467966324 31.897366596101026,39.367544467966326 41.89736659610103,40.632455532033674 38.10263340389897,13.702459173643835 29.12596795110236,31.414213562373096 11.414213562373096,28.585786437626904 8.585786437626904,8.585786437626904 28.585786437626904))"
          )
        ).map({ case (f: String, b: Double, s: String, t: String) => Row(f, b, s, t) })
        val testSchema = StructType(
          Seq(
            StructField("geom", StringType),
            StructField("buffer", DoubleType),
            StructField("bufferStyleParameters", IntegerType),
            StructField("geomBufferedRef", StringType)
          )
        )
        val sourceDf = spark
            .createDataFrame(testDataWKT.asJava, testSchema)
        sourceDf

    }

    def auxiliaryMethods(indexSystem: IndexSystem, geometryAPI: GeometryAPI): Unit = {
        spark.sparkContext.setLogLevel("FATAL")
        val mc = MosaicContext.build(indexSystem, geometryAPI)
        mc.register(spark)
        import mc.functions._


        val df = getWKTRowsDf()

        val stBuffer = ST_Buffer(df.col("wkt").expr, lit(1).expr, mc.expressionConfig)

        stBuffer.left shouldEqual df.col("wkt").expr
        stBuffer.right shouldEqual lit(1).expr
        stBuffer.dataType shouldEqual df.col("wkt").expr.dataType
        noException should be thrownBy stBuffer.makeCopy(Array(stBuffer.left, stBuffer.right))

        st_buffer(col("wkt"), 1).expr.children(1) shouldEqual lit(1.0).cast("double").expr

    }

}
