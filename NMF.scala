package org.apache.spark.ml.recommendation

import org.apache.spark.{HashPartitioner, Partitioner}
import org.apache.spark.ml.recommendation.ALS.Rating
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ArrayBuffer
import com.github.fommil.netlib.BLAS.{getInstance => blas}

import scala.collection.mutable
import scala.util.Random

case class RatingBlock(colIds: Array[Int], rowIndices: Array[Array[Int]], values: Array[Array[Float]])
case class FactorBlock(colIds: Array[Int], factors: Array[Array[Float]])

object NMF {
  def main(args: Array[String]): Unit = {
    val dataPath = "D:\\codes\\nmf\\data\\ml-100k\\u.data"
    val rank = 10
    val maxIter = 10
    val lambda = 0.1.toFloat
    val pt = 3

    val spark = SparkSession.builder().master("local").appName("nmf").getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

    val ratings = readML(spark, dataPath, pt)
    val Array(trainDF, testDF) = ratings.randomSplit(Array(0.8, 0.2))
    val training = trainDF.rdd.map(row => Rating(row.getInt(0), row.getInt(1), row.getFloat(2))).cache()
    val test = testDF.rdd.map(row => Rating(row.getInt(0), row.getInt(1), row.getFloat(2))).cache()
    training.count()
    test.count()

    val time0 = System.currentTimeMillis()
    train(training, test, rank, maxIter, lambda, pt)
    val time1 = System.currentTimeMillis()
    println(s"time: ${(time1 - time0) / 1000.0}")

    Thread.sleep(100000)
    spark.stop()
  }

  def train(ratings: RDD[Rating[Int]], test: RDD[Rating[Int]], rank: Int, maxIter: Int, lambda: Float, numBlocks: Int) = {
    ratings.cache()

    val userPartitioner = new HashPartitioner(numBlocks)
    val itemPartitioner = new HashPartitioner(numBlocks)

    val forItem = partitionByCol(ratings, itemPartitioner).cache()
    val withItem = giveDest(forItem, userPartitioner).cache()

    val forUser = partitionByCol(ratings.map(x => Rating(x.item, x.user, x.rating)), userPartitioner).cache()
    val withUser = giveDest(forUser, itemPartitioner).cache()

    var itemFactors = initialize(forItem, rank)
    var userFactors = initialize(forUser, rank)

    for (iter <- 0 until maxIter) {
      userFactors = updateFactors(itemFactors, withItem, userFactors, forUser, rank, lambda).cache()
      itemFactors = updateFactors(userFactors, withUser, itemFactors, forItem, rank, lambda).cache()
      val rmse = computeRMSE(test, userFactors, itemFactors, rank)
      println(s"$iter = $rmse")
    }
    userFactors.count()
    itemFactors.count()
  }

  def computeRMSE(test: RDD[Rating[Int]], userFactors: RDD[(Int, FactorBlock)], itemFactors: RDD[(Int, FactorBlock)], rank: Int) = {
    val userIdAndFactors = userFactors.flatMap{case (blockId, FactorBlock(colIds, factors)) => {
      colIds.indices.map(i => (colIds(i), factors(i)))
    }}
    val itemIdAndFactors = itemFactors.flatMap{case (blockId, FactorBlock(colIds, factors)) => {
      colIds.indices.map(i => (colIds(i), factors(i)))
    }}

    test.map(x => (x.user, (x.item, x.rating)))
      .join(userIdAndFactors).map{case (userId, ((itemId, rating), userFactor)) => (itemId, (rating, userFactor))}
      .join(itemIdAndFactors).map{case (itemId, ((rating, userFactor), itemFactor)) =>
      math.pow(rating - blas.sdot(rank, userFactor, 1, itemFactor, 1), 2)
    }.mean()
  }



  def updateFactors(srcFactors: RDD[(Int, FactorBlock)],
                    withSrc: RDD[(Int, mutable.Map[Int, Array[Int]])],
                    dstFactors: RDD[(Int, FactorBlock)],
                    forDst: RDD[(Int, RatingBlock)],
                    rank: Int,
                    lambda: Float) = {
    val merged = srcFactors.join(withSrc).flatMap{case (blockId, (FactorBlock(colIds, factors), src2dst)) =>
      src2dst.map{case (dstBlockId, srcLocalIndices) =>
        (dstBlockId, srcLocalIndices.map(localIdx => (colIds(localIdx), factors(localIdx))))
      }
    }.groupByKey(dstFactors.partitions.length).mapValues(_.flatten.toMap)

    merged.join(dstFactors).join(forDst).mapValues{case x =>
      val A = x._1._1
      val X = x._1._2.factors
      val B = x._2
      cd(A, X, B, rank, lambda)
    }
  }

  def cd(A: Map[Int, Array[Float]], X: Array[Array[Float]], B: RatingBlock, rank: Int, lambda: Float) = {
    FactorBlock(B.colIds, B.colIds.indices.map { localIdx =>
      val rowIds = B.rowIndices(localIdx)
      val w = X(localIdx)
      val e = B.values(localIdx)
      for (i <- e.indices) {
        e(i) -= blas.sdot(rank, w, 1, A(rowIds(i)), 1)
      }
      for (k <- 0 until rank) {
        for (i <- e.indices) {
          e(i) += w(k) * A(rowIds(i))(k)
        }
        var g: Float = 0
        var h: Float = lambda * e.length
        for (i <- e.indices) {
          val a = A(rowIds(i))(k)
          g += a * e(i)
          h += a * a
        }
        w(k) = math.max(g / h, 0)
      }
      w
    }.toArray)
  }

  def initialize(forSrc: RDD[(Int, RatingBlock)], rank: Int) = {
    forSrc.mapValues { case RatingBlock(colIds, rowIndices, values) =>
      val factors = colIds.map(_ => {
        Array.fill[Float](rank)(Random.nextFloat())
      })
      FactorBlock(colIds, factors)
    }
  }

  def giveDest(forSrc: RDD[(Int, RatingBlock)], dstPartitioner: Partitioner) = {
    forSrc.mapValues { case RatingBlock(colIds, rowIndices, values) =>
      val src2dst = mutable.Map.empty[Int, ArrayBuffer[Int]]
      rowIndices.zipWithIndex.foreach{case (dstIds, localIdx) =>
        dstIds.foreach(dstId => {
          val dstBlockId = dstPartitioner.getPartition(dstId)
          if (src2dst.contains(dstBlockId)) {
            src2dst(dstBlockId).append(localIdx)
          } else {
            src2dst(dstBlockId) = ArrayBuffer(localIdx)
          }
        })
      }
      src2dst.map(x => (x._1, x._2.toArray))
    }
  }

  def partitionByCol(rdd: RDD[Rating[Int]], partitioner: Partitioner) = {
    rdd.mapPartitions(it => {
      it.map{case Rating(user, item, rating) =>
        (partitioner.getPartition(item), (user, item, rating))
      }
    }).groupByKey(partitioner.numPartitions).mapValues(it => {
      val colIds = new ArrayBuffer[Int]()
      val rowIndices = new ArrayBuffer[Array[Int]]()
      val values = new ArrayBuffer[Array[Float]]()
      it.groupBy(_._2).foreach{case (colId, iter) =>
        colIds+= colId
        val iterArr = iter.toArray
        rowIndices += iterArr.map(_._1)
        values += iterArr.map(_._3)
      }
      RatingBlock(colIds.toArray, rowIndices.toArray, values.toArray)
    })
  }

  def readML(spark: SparkSession, dataPath: String, pt: Int, sep: String = "\t") = {
    import spark.implicits._
    spark.sparkContext.textFile(dataPath, pt).map(s => {
      val ss = s.split(sep)
      Rating(ss(0).toInt - 1, ss(1).toInt - 1, ss(2).toFloat)
    }).toDF()
  }

}
