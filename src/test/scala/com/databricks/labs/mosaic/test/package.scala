package com.databricks.labs.mosaic

import com.databricks.labs.mosaic.core.geometry.MosaicGeometry
import com.databricks.labs.mosaic.core.geometry.api.GeometryAPI
import com.databricks.labs.mosaic.core.index._
import com.databricks.labs.mosaic.core.types.model.Coordinates
import com.databricks.labs.mosaic.functions.MosaicContext
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

import java.nio.file.{Files, Paths}

package object test {

    // noinspection ScalaStyle
    object mocks {

        import org.apache.spark.sql._
        import org.apache.spark.sql.types.{StructField, StructType}

        val hex_rows_epsg4326: List[List[Any]] =
            List(
              List(
                1,
                "00000000030000000100000005403E0000000000004024000000000000404400000000000040440000000000004034000000000000404400000000000040240000000000004034000000000000403E0000000000004024000000000000"
              ),
              List(
                2,
                "000000000600000001000000000300000001000000040000000000000000000000000000000000000000000000003FF00000000000004000000000000000400000000000000000000000000000000000000000000000"
              ),
              List(
                3,
                "0000000003000000030000000540240000000000004024000000000000405B8000000000004024000000000000405B800000000000405B8000000000004024000000000000405B8000000000004024000000000000402400000000000000000005403400000000000040340000000000004034000000000000403E000000000000403E000000000000403E000000000000403E00000000000040340000000000004034000000000000403400000000000000000005404400000000000040340000000000004044000000000000403E0000000000004049000000000000403E0000000000004049000000000000403400000000000040440000000000004034000000000000"
              ),
              List(
                4,
                "000000000600000002000000000300000001000000044044000000000000404E000000000000403400000000000040468000000000004046800000000000403E0000000000004044000000000000404E00000000000000000000030000000200000006403400000000000040418000000000004024000000000000403E00000000000040240000000000004024000000000000403E0000000000004014000000000000404680000000000040340000000000004034000000000000404180000000000000000004403E00000000000040340000000000004034000000000000402E00000000000040340000000000004039000000000000403E0000000000004034000000000000"
              ),
              List(5, "0000000001C052F1F0ED3D859D4041983D46B26BF8"),
              List(
                6,
                "00000000040000000400000000014024000000000000404400000000000000000000014044000000000000403E0000000000000000000001403400000000000040340000000000000000000001403E0000000000004024000000000000"
              ),
              List(7, "000000000200000003403E00000000000040240000000000004024000000000000403E00000000000040440000000000004044000000000000"),
              List(
                8,
                "00000000050000000200000000020000000340240000000000004024000000000000403400000000000040340000000000004024000000000000404400000000000000000000020000000440440000000000004044000000000000403E000000000000403E00000000000040440000000000004034000000000000403E0000000000004024000000000000"
              )
            )
        val hex_rows_epsg27700: List[List[Any]] =
            hex_rows_epsg4326.map {
                case id :: hex :: _ => List(id, JTS.geometry(hex, "HEX").mapXY((x, y) => (math.abs(x) * 1000, math.abs(y) * 1000)).toHEX)
                case _              => throw new Error("Unexpected test data format!")
            }
        val wkt_rows_epsg4326: List[List[Any]] =
            List(
              List(1, "POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))"),
              List(2, "MULTIPOLYGON (((0 0, 0 1, 2 2, 0 0)))"),
              List(
                3,
                """POLYGON ((10 10, 110 10, 110 110, 10 110, 10 10),
                  | (20 20, 20 30, 30 30, 30 20, 20 20),
                  | (40 20, 40 30, 50 30, 50 20, 40 20))""".stripMargin.filter(_ >= ' ')
              ),
              List(
                4,
                """MULTIPOLYGON (((40 60, 20 45, 45 30, 40 60)),
                  | ((20 35, 10 30, 10 10, 30 5, 45 20, 20 35),
                  | (30 20, 20 15, 20 25, 30 20)))""".stripMargin.filter(_ >= ' ')
              ),
              List(5, "POINT (-75.78033 35.18937)"),
              List(6, "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))"),
              List(7, "LINESTRING (30 10, 10 30, 40 40)"),
              List(8, "MULTILINESTRING ((10 10, 20 20, 10 40), (40 40, 30 30, 40 20, 30 10))")
            )
        val wkt_rows_epsg27700: List[List[Any]] =
            List(
              List(1, "POLYGON ((30000 10000, 40000 40000, 20000 40000, 10000 20000, 30000 10000))"),
              List(2, "MULTIPOLYGON (((0 0, 0 1000, 2000 2000, 0 0)))"),
              List(
                3,
                """POLYGON ((10000 10000, 110000 10000, 110000 110000, 10000 110000, 10000 10000),
                  | (20000 20000, 20000 30000, 30000 30000, 30000 20000, 20000 20000),
                  | (40000 20000, 40000 30000, 50000 30000, 50000 20000, 40000 20000))""".stripMargin.filter(_ >= ' ')
              ),
              List(
                4,
                """MULTIPOLYGON (((40000 60000, 20000 45000, 45000 30000, 40000 60000)),
                  | ((20000 35000, 10000 30000, 10000 10000, 30000 5000, 45000 20000, 20000 35000),
                  | (30000 20000, 20000 15000, 20000 25000, 30000 20000)))""".stripMargin.filter(_ >= ' ')
              ),
              List(5, "POINT (75780 35189)"),
              List(6, "MULTIPOINT ((10000 40000), (40000 30000), (20000 20000), (30000 10000))"),
              List(7, "LINESTRING (30000 10000, 10000 30000, 40000 40000)"),
              List(8, "MULTILINESTRING ((10000 10000, 20000 20000, 10000 40000), (40000 40000, 30000 30000, 40000 20000, 30000 10000))")
            )
        val geoJSON_rows: List[List[Any]] =
            List(
              List(
                1,
                """{"type":"Polygon","coordinates":[[[30,10],[40,40],[20,40],[10,20],[30,10]]],"crs":{"type":"name","properties":{"name":"EPSG:0"}}}"""
              ),
              List(
                2,
                """{"type":"MultiPolygon","coordinates":[[[[0,0],[0,1],[2,2],[0,0]]]],"crs":{"type":"name","properties":{"name":"EPSG:0"}}}"""
              ),
              List(
                3,
                """{"type":"Polygon","coordinates":[[[10,10],[110,10],[110,110],[10,110],[10,10]],[[20,20],[20,30],[30,30],[30,20],[20,20]],[[40,20],[40,30],[50,30],[50,20],[40,20]]],"crs":{"type":"name","properties":{"name":"EPSG:0"}}}"""
              ),
              List(
                4,
                """{"type":"MultiPolygon","coordinates":[[[[40,60],[20,45],[45,30],[40,60]]],[[[20,35],[10,30],[10,10],[30,5],[45,20],[20,35]],[[30,20],[20,15],[20,25],[30,20]]]],"crs":{"type":"name","properties":{"name":"EPSG:0"}}}"""
              ),
              List(5, """{"type":"Point","coordinates":[-75.78033,35.18937],"crs":{"type":"name","properties":{"name":"EPSG:0"}}}"""),
              List(
                6,
                """{"type":"MultiPoint","coordinates":[[10,40],[40,30],[20,20],[30,10]],"crs":{"type":"name","properties":{"name":"EPSG:0"}}}"""
              ),
              List(
                7,
                """{"type":"LineString","coordinates":[[30,10],[10,30],[40,40]],"crs":{"type":"name","properties":{"name":"EPSG:0"}}}"""
              ),
              List(
                8,
                """{"type":"MultiLineString","coordinates":[[[10,10],[20,20],[10,40]],[[40,40],[30,30],[40,20],[30,10]]],"crs":{"type":"name","properties":{"name":"EPSG:0"}}}"""
              )
            )
        val wkt_rows_boroughs_epsg4326: List[List[Any]] =
            List(
              List(
                1,
                "POLYGON ((-73.15203987512825 41.65493888808187, -73.15276005327304 41.654464534276144, -73.1534774913585 41.65398408823101, -73.15419392499267 41.65350553754797, -73.15490927428783 41.65302455163131, -73.15562571110856 41.65254793798908, -73.15634368724263 41.65206744602062, -73.15705807062369 41.65159004880297, -73.15731239180201 41.65141993549811, -73.15777654592605 41.65110813492959, -73.15849148815478 41.65062931467526, -73.15920857202283 41.650150840155845, -73.15992302044982 41.64967211027974, -73.16063898461574 41.64919378876085, -73.16135519740746 41.648713437678566, -73.16207164924467 41.64823497334122, -73.1627885741791 41.647755144686975, -73.16350625871115 41.647275319743144, -73.16422169735793 41.64679722055354, -73.16494134072073 41.64631906203407, -73.16565369192317 41.64583842224602, -73.16636983326426 41.645358003568795, -73.16708766647704 41.64487976843594, -73.16780256963874 41.644399682754724, -73.16854605445398 41.643902823959415, -73.17033751716234 41.644983274281614, -73.17253823425115 41.64631261117375, -73.17193389459365 41.646894728269224, -73.17135689834309 41.64745620467151, -73.17076909435258 41.648015175832654, -73.17018706340953 41.64857422903216, -73.1696037457729 41.649134448673664, -73.16902267733688 41.649696981284, -73.16843897396495 41.65025478289252, -73.16785747123949 41.65081511159219, -73.1672752495212 41.651375887324555, -73.16669360977347 41.65193549858073, -73.16610628498582 41.652494188624, -73.16552602032272 41.653055406663334, -73.16494093450561 41.653618385728294, -73.16436105223679 41.65417640070095, -73.16377902055123 41.65473719420326, -73.16319536474295 41.65529794703389, -73.1626200103856 41.65586153432125, -73.16202888149982 41.656416992938276, -73.1614469289715 41.656978874154134, -73.16086400864474 41.65753817612168, -73.16028157563906 41.658091666569824, -73.16000331309866 41.658357439331475, -73.15979488017096 41.65853007132134, -73.15923429499004 41.65899462327653, -73.15902680427884 41.65916030197158, -73.15691038908221 41.6578889582749, -73.15478938601524 41.6566036161547, -73.15203987512825 41.65493888808187))"
              ),
              List(
                2,
                "POLYGON ((-73.56235277075633 39.15300458448253, -73.56187982440873 39.152765556097556, -73.56123679243437 39.152389892258334, -73.56025725573296 39.15181762539406, -73.55908147720491 39.15113068578863, -73.56005314321054 39.14998280260329, -73.55836085802869 39.148982271965366, -73.5588005318875 39.14804664871961, -73.5589701212708 39.147802585224845, -73.55899084725938 39.147770658198375, -73.55901409600605 39.147724583035185, -73.5588993969599 39.147696859381604, -73.55868819359598 39.14763265265065, -73.5581290080893 39.14746265672036, -73.5569539724615 39.14709701956742, -73.55683739862177 39.14706074482676, -73.55678108223934 39.147043219317794, -73.5567143203941 39.147021797957116, -73.55628927118747 39.146890662360384, -73.55675229943505 39.146463009256095, -73.55675633843144 39.14645818156648, -73.55680695220889 39.146410760700725, -73.5569688840089 39.14622034773482, -73.5570030571936 39.146180164801095, -73.5573231767441 39.146393395716984, -73.5576798239216 39.146568138927094, -73.5578223705353 39.146632904277034, -73.55796929826334 39.146691707632904, -73.55812017826726 39.14674437736086, -73.55827457016954 39.146790759729335, -73.55843202333921 39.146830719357986, -73.5585920782074 39.14686413961279, -73.55875426760916 39.14689092294671, -73.55891811814733 39.14691099118442, -73.5590831515747 39.146924285750615, -73.56272213077807 39.14739171068307, -73.56688427692357 39.147907650514995, -73.56762691377294 39.148067760265086, -73.56846056046193 39.148493150708745, -73.56859157100894 39.14856000056833, -73.5690101648959 39.14883805276128, -73.56937178624052 39.14908886104908, -73.56943584827047 39.14911977118178, -73.56970704380902 39.1492104222148, -73.57028226309495 39.14963965249121, -73.57116768911617 39.150391193891984, -73.57135895508857 39.15056293684171, -73.57184825369085 39.151026065186045, -73.57193080868898 39.151131560861295, -73.57200549459178 39.151240409622076, -73.572072077664 39.151352270874696, -73.57211474515398 39.15144914180766, -73.57216558866945 39.15154369286198, -73.57222439297175 39.15163552370587, -73.57229090911304 39.15172424552332, -73.5723648554899 39.151809482660674, -73.57244591903529 39.15189087421716, -73.57253375654373 39.151968075573215, -73.57262799612437 39.15204075984987, -73.57272823877521 39.152108619292875, -73.57283406007264 39.152171366576106, -73.57287916081053 39.152196821721766, -73.57292715073446 39.152218997128564, -73.57297762048636 39.152237703637205, -73.57305260263297 39.152253635213675, -73.57312485808473 39.152275703329146, -73.57319352506092 39.152303644781234, -73.57325778457711 39.15233712631693, -73.5733168702127 39.152375748607, -73.57337007725165 39.15241905100848, -73.57361522607525 39.152604225455, -73.57357154647099 39.152617657377526, -73.57330990013257 39.15269811155836, -73.57283523563343 39.15285469672319, -73.57266577380229 39.15291059956592, -73.572625015474 39.15292404732427, -73.5725638303784 39.152944255554274, -73.57241480522278 39.15299131699475, -73.57202009049745 39.153121719733434, -73.57111787637662 39.15340950935181, -73.57072108751393 39.15353607499735, -73.57066877156423 39.15361821813746, -73.57063304357986 39.153671720588136, -73.57040270649328 39.15374140561407, -73.57021960782376 39.153829940031265, -73.57007861589398 39.15389784011041, -73.56949171945728 39.15413464326359, -73.56947269565252 39.15414653572976, -73.56902183372368 39.154428400960434, -73.56881541231328 39.15459599408252, -73.56868387526444 39.15471908341296, -73.56855900306586 39.15484614061648, -73.56844100161437 39.15497695623263, -73.56833006548342 39.15511131460388, -73.56822637760222 39.155248994230774, -73.56806614790393 39.15548193977366, -73.56769215206711 39.155913807692656, -73.5673916154186 39.15614394146521, -73.5671329105966 39.156353182556394, -73.5666214016349 39.15676688523694, -73.56658428608434 39.156796900211944, -73.5665476538435 39.156824717408725, -73.5662731651565 39.157109963201535, -73.56617531122572 39.157225344146546, -73.56605918549774 39.15738879140227, -73.56596030267085 39.15755808673604, -73.56556944470182 39.15792531464056, -73.5651928853132 39.158301256975534, -73.56498073118041 39.158526932981395, -73.56483112013758 39.15868541502952, -73.56441718718062 39.15906567617598, -73.56421347351214 39.15925282567818, -73.56405455439416 39.15942913912508, -73.56386520514326 39.15963788670296, -73.5637531215215 39.159580448371955, -73.56372906286936 39.15956811819494, -73.5636675023606 39.15953675654002, -73.56332533889682 39.159366809648596, -73.5636455803234 39.15879525340545, -73.56382277205552 39.15841336784076, -73.56409007977581 39.15797891328463, -73.56494425347604 39.156543102561685, -73.56511842090157 39.156245770159714, -73.56459870518916 39.15598258880011, -73.56410773046338 39.155733957221216, -73.56395153886761 39.155759189855175, -73.56468205386413 39.153552542777255, -73.56255477350777 39.1530505616413, -73.56235277075633 39.15300458448253))"
              ),
              List(
                3,
                "POLYGON ((-74.8137149473433 40.87152674578831, -74.81303062900967 40.87111037699551, -74.81082920282513 40.86978071614845, -74.81037789493061 40.86950853133973, -74.80975346278758 40.86913192571649, -74.80862203689101 40.86844951820328, -74.80891985710187 40.86816791995567, -74.80920647054273 40.8678909598745, -74.80700561724747 40.86656133290565, -74.80480806576382 40.86523245692151, -74.80393259353669 40.86470543733995, -74.80417596274863 40.86393659636803, -74.80440551243532 40.86309702022793, -74.80601282532922 40.86407199715848, -74.80663857237198 40.86346737291672, -74.80722179740965 40.862906689395, -74.80789414616204 40.8622594631665, -74.8083857240464 40.86178917763156, -74.80896778533965 40.86123102473893, -74.8095489388813 40.86067039415398, -74.81012749350626 40.86011215415955, -74.8107109915535 40.85955293938097, -74.81129099225348 40.85899360431007, -74.81191858239676 40.85838990433847, -74.8125451218216 40.85778722757246, -74.81312587240065 40.85723146721382, -74.81370723868882 40.85667069674902, -74.81428999137555 40.8561112116838, -74.81486879799036 40.85554993101825, -74.81545232242863 40.85499257425766, -74.81603356746139 40.85443184635177, -74.81661564241048 40.85387339346004, -74.81719595835392 40.85331336230701, -74.81777840352092 40.85275579451416, -74.8184039380371 40.85215177949946, -74.82059848688203 40.85346911232904, -74.82114299638 40.85379694914571, -74.82281016263585 40.85480078656643, -74.82501771218313 40.85614280284762, -74.82653356214082 40.85706072556648, -74.82721800942433 40.85747459528865, -74.82790740512627 40.85788873559117, -74.82869558747787 40.85835542610192, -74.82956790991463 40.85888289822074, -74.8289657084795 40.8595258099377, -74.82849346487157 40.86001991642085, -74.82842117779481 40.86009620295064, -74.82786891708065 40.86067460717189, -74.8275956052216 40.86094291717428, -74.82731523282146 40.86122300187544, -74.82730020399046 40.86134292451229, -74.82711602155707 40.86158006067824, -74.82704893830979 40.86153974723015, -74.82682933500945 40.86176029145383, -74.82677859516487 40.86183770297382, -74.82639683805107 40.86160171532001, -74.82607895175649 40.86230856951167, -74.82574493205215 40.86301777118307, -74.82583919636807 40.8630734209662, -74.82564648966675 40.86347131035809, -74.82554983729175 40.86369081234196, -74.82453385616054 40.86600744468851, -74.82391480413989 40.86751743966481, -74.82366147299979 40.8681455362238, -74.82361837047796 40.86825238899756, -74.82333528787801 40.86899282900694, -74.82303514638994 40.86970234624308, -74.82274012648766 40.87044963895666, -74.82243996650502 40.87118102002588, -74.82211602116853 40.87192988940918, -74.82183822994068 40.87263868879912, -74.8222939728318 40.87293372825336, -74.82255343173273 40.87280807699048, -74.82255785421022 40.87305434369991, -74.82258709203629 40.87329877622275, -74.82263746784666 40.87354147707521, -74.8227086384383 40.8737808643054, -74.82284606362833 40.87408885418022, -74.82308087313457 40.87444935040451, -74.82319724991096 40.87457942519204, -74.82327983983531 40.87466733328505, -74.82330748304234 40.87469577346027, -74.82340917285165 40.87479947546519, -74.82346588448698 40.87485189375082, -74.82338738661066 40.87493822542071, -74.82299740557913 40.87531242450498, -74.82080070107769 40.87398074812494, -74.8186001602882 40.87265115509361, -74.81801841122788 40.87321124727138, -74.81743412338449 40.87377329913746, -74.81522896358014 40.87244491169576, -74.8137149473433 40.87152674578831))"
              ),
              List(
                4,
                "POLYGON ((-73.7433326819638921 40.7388830992603985, -73.7437139888894251 40.7394032488948525, -73.7440155883505781 40.7407748661890210, -73.7499027983064650 40.7397660065436895, -73.7514517303028754 40.7402341513596795, -73.7534931436411227 40.7423878817946772, -73.7484426826701736 40.7433414452265339, -73.7457349867485306 40.7429503781368751, -73.7447645572185877 40.7433316862888120, -73.7464795803781499 40.7468463207216089, -73.7437632921849939 40.7475585906716375, -73.7441027238367610 40.7481863340343153, -73.7456631440826982 40.7515780164934043, -73.7460203083420396 40.7526644103624847, -73.7475059433906921 40.7561384089298997, -73.7504626791590567 40.7593922093765286, -73.7524216045020893 40.7600876090127926, -73.7534776328631523 40.7588130243055105, -73.7564507049811766 40.7588915767399058, -73.7579784666999245 40.7586649336612936, -73.7597774230309255 40.7578476406518675, -73.7606719303619229 40.7569937242137001, -73.7607268443749433 40.7559019923299672, -73.7601500600110001 40.7550493069326762, -73.7606661687627252 40.7548748677083807, -73.7614213585374898 40.7557447401175921, -73.7613109712959840 40.7569624717956671, -73.7616629882570862 40.7581497156257058, -73.7609327945658180 40.7590315370508662, -73.7596247050563960 40.7592198242993931, -73.7588654466414368 40.7599420439581479, -73.7580538753354915 40.7601269448370971, -73.7579223460616475 40.7604249738937270, -73.7583978580822617 40.7620084949657482, -73.7569924246160440 40.7622746205692295, -73.7500250134239934 40.7637489926651782, -73.7492870519646999 40.7630876909040154, -73.7462167073649368 40.7642256499076510, -73.7449603503156368 40.7621534123618545, -73.7444216818426099 40.7604909608152965, -73.7426837874691472 40.7587853704986856, -73.7408405198126644 40.7560089046367438, -73.7392920808643311 40.7570547567895574, -73.7391620778007280 40.7562952062720782, -73.7397603039244984 40.7554447937863742, -73.7409832571666470 40.7548727104215160, -73.7413307390569059 40.7545925764364227, -73.7418468418436817 40.7534650599401758, -73.7413480742687710 40.7524692490958031, -73.7409132119359754 40.7519838138741903, -73.7398945907203256 40.7513378510539752, -73.7382982358692658 40.7492218807730779, -73.7373691337662081 40.7483450331937860, -73.7367232256816436 40.7479174775296684, -73.7358776472692625 40.7475273848981132, -73.7354179904088767 40.7471420011437004, -73.7351583251506213 40.7465564344430788, -73.7350435381085703 40.7461150344244842, -73.7350506640354695 40.7448748495469459, -73.7347560460864315 40.7441178513729767, -73.7337456012040064 40.7428385758763270, -73.7332075020618731 40.7417961908928703, -73.7337938214375583 40.7389875325014827, -73.7384772699255535 40.7381168525342190, -73.7401553662552516 40.7385312242947180, -73.7412865288292636 40.7388136410812791, -73.7430322642033786 40.7384417601234929, -73.7433326819638921 40.7388830992603985))"
              ),
              List(
                5,
                "POLYGON ((-73.7433326819638921 40.7388830992603985, -73.7437139888894251 40.7394032488948525, -73.7440155883505781 40.7407748661890210, -73.7499027983064650 40.7397660065436895, -73.7514517303028754 40.7402341513596795, -73.7534931436411227 40.7423878817946772, -73.7484426826701736 40.7433414452265339, -73.7457349867485306 40.7429503781368751, -73.7447645572185877 40.7433316862888120, -73.7464795803781499 40.7468463207216089, -73.7437632921849939 40.7475585906716375, -73.7441027238367610 40.7481863340343153, -73.7456631440826982 40.7515780164934043, -73.7460203083420396 40.7526644103624847, -73.7475059433906921 40.7561384089298997, -73.7504626791590567 40.7593922093765286, -73.7524216045020893 40.7600876090127926, -73.7534776328631523 40.7588130243055105, -73.7564507049811766 40.7588915767399058, -73.7579784666999245 40.7586649336612936, -73.7597774230309255 40.7578476406518675, -73.7606719303619229 40.7569937242137001, -73.7607268443749433 40.7559019923299672, -73.7601500600110001 40.7550493069326762, -73.7606661687627252 40.7548748677083807, -73.7614213585374898 40.7557447401175921, -73.7613109712959840 40.7569624717956671, -73.7616629882570862 40.7581497156257058, -73.7609327945658180 40.7590315370508662, -73.7596247050563960 40.7592198242993931, -73.7588654466414368 40.7599420439581479, -73.7580538753354915 40.7601269448370971, -73.7579223460616475 40.7604249738937270, -73.7583978580822617 40.7620084949657482, -73.7569924246160440 40.7622746205692295, -73.7500250134239934 40.7637489926651782, -73.7492870519646999 40.7630876909040154, -73.7462167073649368 40.7642256499076510, -73.7449603503156368 40.7621534123618545, -73.7444216818426099 40.7604909608152965, -73.7426837874691472 40.7587853704986856, -73.7408405198126644 40.7560089046367438, -73.7392920808643311 40.7570547567895574, -73.7391620778007280 40.7562952062720782, -73.7397603039244984 40.7554447937863742, -73.7409832571666470 40.7548727104215160, -73.7413307390569059 40.7545925764364227, -73.7418468418436817 40.7534650599401758, -73.7413480742687710 40.7524692490958031, -73.7409132119359754 40.7519838138741903, -73.7398945907203256 40.7513378510539752, -73.7382982358692658 40.7492218807730779, -73.7373691337662081 40.7483450331937860, -73.7367232256816436 40.7479174775296684, -73.7358776472692625 40.7475273848981132, -73.7354179904088767 40.7471420011437004, -73.7351583251506213 40.7465564344430788, -73.7350435381085703 40.7461150344244842, -73.7350506640354695 40.7448748495469459, -73.7347560460864315 40.7441178513729767, -73.7337456012040064 40.7428385758763270, -73.7332075020618731 40.7417961908928703, -73.7337938214375583 40.7389875325014827, -73.7384772699255535 40.7381168525342190, -73.7401553662552516 40.7385312242947180, -73.7412865288292636 40.7388136410812791, -73.7430322642033786 40.7384417601234929, -73.7433326819638921 40.7388830992603985))"
              ),
              List(
                6,
                "POLYGON ((-74.1597481587429570 40.6414165257901772, -74.1599787569963098 40.6414464808363576, -74.1603665567036785 40.6415795387926551, -74.1611124252219156 40.6418354537373574, -74.1611789710470646 40.6420066123307677, -74.1613480397163300 40.6435009886979728, -74.1614598592411625 40.6442914295759721, -74.1614603600531836 40.6442949697649141, -74.1579875954066665 40.6438617889660350, -74.1574334920100711 40.6433028577790125, -74.1575530521179331 40.6432482917784057, -74.1579158940678411 40.6430826944529571, -74.1581345160336980 40.6426325306630645, -74.1582754371735149 40.6425633838383860, -74.1584034767242173 40.6425416038819449, -74.1584849583912842 40.6425384668857674, -74.1585504377056566 40.6424975013064795, -74.1585761059831157 40.6424293511026207, -74.1586612596052674 40.6423051054572042, -74.1587437814761898 40.6420038398483285, -74.1588109374531115 40.6417586347403272, -74.1592002453457866 40.6416504039773514, -74.1594560243820524 40.6414483333241208, -74.1597481587429570 40.6414165257901772))"
              )
            )
        // We are generating acceptable data for EPSG:27700
        // by making sure all coordinates are positive
        // and since EPSG:27700 represents meters from CRS
        // origin we multiply long/lat values by 1000 (arbitrary choice)
        val wkt_rows_boroughs_epsg27700: List[List[Any]] =
            wkt_rows_boroughs_epsg4326.map {
                case id :: wkt :: _ => List(id, JTS.geometry(wkt, "WKT").mapXY((x, y) => (math.abs(x) * 1000, math.abs(y) * 1000)).toWKT)
                case _              => throw new Error("Unexpected test data format!")
            }
        val geotiffBytes: Array[Byte] = fileBytes("/modis/MCD43A4.A2018185.h10v07.006.2018194033728_B01.TIF")
        val gribBytes: Array[Byte] =
            fileBytes("/binary/grib-cams/adaptor.mars.internal-1650626995.380916-11651-14-ca8e7236-16ca-4e11-919d-bdbd5a51da35.grib")
        val netcdfBytes: Array[Byte] = fileBytes("/binary/netcdf-coral/ct5km_baa-max-7d_v3.1_20220101.nc")

        def polyDf(sparkSession: SparkSession, mosaicContext: MosaicContext): DataFrame = {
            val mc = mosaicContext
            import mc.functions._

            val indexSystem = mc.getIndexSystem
            val df = sparkSession.read
                .json("src/test/resources/NYC_Taxi_Zones.geojson")
                .withColumn("geometry", st_geomfromgeojson(to_json(col("geometry"))))
            indexSystem match {
                case H3IndexSystem  => df
                case BNGIndexSystem => df
                        .withColumn("greenwich", st_point(lit(0.0098), lit(51.4934)))
                        .withColumn(
                          "geometry",
                          st_translate(
                            col("geometry"),
                            st_xmax(col("greenwich")) - st_xmax(col("geometry")),
                            st_ymax(col("greenwich")) - st_ymax(col("geometry"))
                          )
                        )
                        .withColumn(
                          "geometry",
                          st_setsrid(col("geometry"), lit(4326))
                        )
                        .withColumn(
                          "geometry",
                          st_transform(col("geometry"), lit(27700))
                        )
                        .drop("greenwich")
                case _              => df
            }
        }

        def pointDf(sparkSession: SparkSession, mosaicContext: MosaicContext): DataFrame = {
            val mc = mosaicContext
            import mc.functions._

            val indexSystem = mc.getIndexSystem
            val df = sparkSession.read
                .options(
                  Map(
                    "header" -> "true",
                    "inferSchema" -> "true"
                  )
                )
                .csv("src/test/resources/nyctaxi_yellow_trips.csv")
                .withColumn("geometry", st_point(col("pickup_longitude"), col("pickup_latitude")))
            indexSystem match {
                case H3IndexSystem  => df
                case BNGIndexSystem => df
                        .withColumn("greenwich", st_point(lit(0.0098), lit(51.4934)))
                        .withColumn(
                          "geometry",
                          st_translate(
                            col("geometry"),
                            st_xmax(col("greenwich")) - st_xmax(col("geometry")),
                            st_ymax(col("greenwich")) - st_ymax(col("geometry"))
                          )
                        )
                        .withColumn(
                          "geometry",
                          st_setsrid(col("geometry"), lit(4326))
                        )
                        .withColumn(
                          "geometry",
                          st_transform(col("geometry"), lit(27700))
                        )
                        .drop("greenwich")
                case _              => df
            }
        }

        def getHexRowsDf(mosaicContext: MosaicContext): DataFrame = {
            val mc = mosaicContext

            val indexSystem = mc.getIndexSystem
            val spark = SparkSession.builder().getOrCreate()
            val rows = indexSystem match {
                case BNGIndexSystem => hex_rows_epsg27700.map { x => Row(x: _*) }
                case _              => hex_rows_epsg4326.map { x => Row(x: _*) }
            }
            val rdd = spark.sparkContext.makeRDD(rows)
            val schema = StructType(
              List(
                StructField("id", IntegerType),
                StructField("hex", StringType)
              )
            )
            spark.createDataFrame(rdd, schema)
        }

        def getWKTRowsDf(indexSystem: IndexSystem = H3IndexSystem): DataFrame = {
            val spark = SparkSession.builder().getOrCreate()
            val rows = indexSystem match {
                case H3IndexSystem  => wkt_rows_epsg4326.map { x => Row(x: _*) }
                case BNGIndexSystem => wkt_rows_epsg27700.map { x => Row(x: _*) }
                case _              => wkt_rows_epsg4326.map { x => Row(x: _*) }
            }
            val rdd = spark.sparkContext.makeRDD(rows)
            val schema = StructType(
              List(
                StructField("id", IntegerType),
                StructField("wkt", StringType)
              )
            )
            spark.createDataFrame(rdd, schema)
        }

        def getBoroughs(mosaicContext: MosaicContext): DataFrame = {
            val mc = mosaicContext

            val indexSystem = mc.getIndexSystem
            val spark = SparkSession.builder().getOrCreate()
            val rows = indexSystem match {
                case H3IndexSystem  => wkt_rows_boroughs_epsg4326.map { x => Row(x: _*) }
                case BNGIndexSystem => wkt_rows_boroughs_epsg27700.map { x => Row(x: _*) }
                case _              => wkt_rows_boroughs_epsg4326.map { x => Row(x: _*) }
            }
            val rdd = spark.sparkContext.makeRDD(rows)
            val schema = StructType(
              List(
                StructField("id", IntegerType),
                StructField("wkt", StringType)
              )
            )
            spark.createDataFrame(rdd, schema)
        }

        def getGeoJSONDf(mosaicContext: MosaicContext): DataFrame = {
            val mc = mosaicContext
            import mc.functions._

            val indexSystem = mc.getIndexSystem
            val spark = SparkSession.builder().getOrCreate()
            val rows = geoJSON_rows.map { x => Row(x: _*) }
            val rdd = spark.sparkContext.makeRDD(rows)
            val schema = StructType(
              List(
                StructField("id", IntegerType),
                StructField("geojson", StringType)
              )
            )
            val df = spark.createDataFrame(rdd, schema)
            indexSystem match {
                case BNGIndexSystem => df
                        .withColumn("greenwich", st_point(lit(0.0098), lit(51.4934)))
                        .withColumn(
                          "geojson",
                          st_translate(
                            col("geojson"),
                            st_xmax(col("greenwich")) - st_xmax(col("geojson")),
                            st_ymax(col("greenwich")) - st_ymax(col("geojson"))
                          )
                        )
                        .withColumn(
                          "geojson",
                          st_setsrid(st_geomfromwkt(col("geojson")), lit(4326))
                        )
                        .withColumn(
                          "geojson",
                          st_asgeojson(st_transform(col("geojson"), lit(27700)))
                        )
                        .drop("greenwich")
                case _              => df
            }
        }

        def fileBytes(resourcePath: String): Array[Byte] = {
            val inFile = getClass.getResource(resourcePath)
            Files.readAllBytes(Paths.get(inFile.getPath))
        }

        def filePath(resourcePath: String): String = {
            val inFile = getClass.getResource(resourcePath)
            Paths.get(inFile.getPath).toAbsolutePath.toString
        }

    }

    // noinspection NotImplementedCode, ScalaStyle
    object MockIndexSystem extends IndexSystem(LongType) {

        override def crsID: Int = ???

        override def name: String = "MOCK"

        override def polyfill(geometry: MosaicGeometry, resolution: Int, geometryAPI: GeometryAPI): Seq[Long] = ???

        override def format(id: Long): String = ???

        override def getResolutionStr(resolution: Int): String = ???

        override def pointToIndex(lon: Double, lat: Double, resolution: Int): Long = ???

        override def kLoop(index: Long, n: Int): Seq[Long] = ???

        override def kRing(index: Long, n: Int): Seq[Long] = ???

        override def getResolution(res: Any): Int = ???

        override def resolutions: Set[Int] = ???

        override def indexToGeometry(index: Long, geometryAPI: GeometryAPI): MosaicGeometry = ???

        override def getBufferRadius(geometry: MosaicGeometry, resolution: Int, geometryAPI: GeometryAPI): Double = ???

        override def parse(id: String): Long = ???

        override def indexToCenter(index: Long): Coordinates = ???
        override def indexToBoundary(index: Long): Seq[Coordinates] = ???
        override def distance(cellId: Long, cellId2: Long): Long = ???

    }

}
