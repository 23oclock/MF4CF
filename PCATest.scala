package org.apache.spark.mllib.feature

import org.apache.spark.{HashPartitioner, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.mllib.linalg.{DenseVector, SparseMatrix, SparseVector, Vector, Vectors}
import breeze.linalg.{*, CSCMatrix => BSM, DenseMatrix => BDM, DenseVector => BDV, Matrix => BM}
import org.apache.spark.mllib.stat.Statistics

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object PCATest {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local").appName("pca").getOrCreate()
    val sc = spark.sparkContext
    sc.setLogLevel("ERROR")

    val m = 5
    val n = 3
    val k = 4
    val pt = 3
    val data = generateData(sc, m, n, pt).setName("origin-data").cache()
    data.foreach(println)
    println(s"origin partitioner: ${data.partitioner}")

    val c = computeMeanVector(data)
    println(s"mean vector: $c")

    val A = partitionData(data, n).setName("block-mat").cache()
    A.foreach(x => {
      println(s"-----partition ${x._1}-----")
      println(x._2.toDense)
    })
    println(s"A partitioner: ${A.partitioner}")

    val p = k / 2
    val Omega = generateOmega(A, k + p)   // n * (k + p)

    val p1 = 1
    val GG2 = generateGaussionDistri(A, k + p + p1)

    var X = cov_tran_mul(A, c, Omega)
    println("A^T * Omega")
    println(X)

    val GG1 = generateGaussionSingle(X, k + p + p1)

    val Y = cov_mul(A, c, X).setName("Y").cache()
    println("AX")
    Y.foreach(x => {
      println(s"-----${x._1}-----")
      println(x._2)
    })

    for(_ <- 0 until 3) {
      val Y = ggrqr4(cov_mul(A, c, X), GG2)
      X = ggrqr4(cov_tran_mul(A, c, Y), GG1)
      println("X")
      println(X)
    }









    Thread.sleep(1000000)
    spark.stop()
  }

  def generateData(sc: SparkContext, numSamples: Int, numFeatures: Int, pt: Int): RDD[SparseVector] = {
    sc.parallelize(0 until numSamples, pt).mapPartitions(iter => {
      iter.map(_ => {
        Vectors.fromBreeze(BDV.rand[Double](numFeatures)).toSparse
      })
    })
  }

  def computeMeanVector(A: RDD[SparseVector]): BDV[Double] = {
    val summary = Statistics.colStats(A.map((_, 1.0)), Seq("mean"))
    summary.mean.asBreeze.asInstanceOf[BDV[Double]]
  }

  def partitionData(data: RDD[SparseVector], numFeatures: Int): RDD[(Int, BSM[Double])] = {
    val pt = data.partitions.length
    data.mapPartitionsWithIndex((blockId, iter) => {
      Iterator((blockId, iter2mat(iter, numFeatures)))
    }).partitionBy(new HashPartitioner(pt))
  }

  def iter2mat(iter: Iterator[SparseVector], nCols: Int): BSM[Double] = {
    val array = iter.toArray
    val nRows = array.length
    val entries = array.zipWithIndex.flatMap{ case (row, i) =>
      row.activeIterator.map{ case (j, v) =>
        (i, j, v)
      }
    }
    SparseMatrix.fromCOO(nRows, nCols, entries).asBreeze.asInstanceOf[BSM[Double]]
  }

  /**
   * compute AX - c^T^X
   * @param A M x N
   * @param c N x 1
   * @param X N x K
   */
  def cov_mul(A: RDD[(Int, BSM[Double])], c: BDV[Double], X: BDM[Double]): RDD[(Int, BDM[Double])] = {
    val brX = A.sparkContext.broadcast(X)
    val brCX = A.sparkContext.broadcast(X.t * c)  // (c.t * X).t
    A.mapValues(mat => {
      val vX = brX.value
      val vCX = brCX.value
      val AX = mat * vX
      AX(*, ::) - vCX
    })
  }

  def cov_tran_mul(A: RDD[(Int, BSM[Double])], c: BDV[Double], Y: RDD[(Int, BDM[Double])]): BDM[Double] = {
    require(A.partitioner.isDefined && A.partitioner == Y.partitioner)
    Y.cache()
    val AY = A.join(Y).mapValues{ case (a, y) =>
      a.t * y
    }.map(_._2).treeReduce(_ + _)
    val sumY = Y.map(_._2).map(mat => {
      breeze.linalg.sum(mat(::, *)).t
    }).treeReduce(_ + _)
    Y.unpersist()
    AY - c * sumY.t
  }

  def ggrqr4(Y: RDD[(Int, BDM[Double])], GG1: RDD[(Int, BDM[Double])]) = {
    require(Y.partitioner.isDefined && Y.partitioner == GG1.partitioner)
    val array = GG1.join(Y).mapValues{ case (g, y) => g * y }.collect().sortBy(_._1).map(_._2)
    val X = BDM.vertcat(array:_*)
    val R = breeze.linalg.qr.reduced(X).r
    val bcRinv = Y.sparkContext.broadcast(breeze.linalg.inv(R))
    Y.mapPartitions(iter => {
      val vRinv = bcRinv.value
      iter.map{ case (blockId, mat) =>
        (blockId, mat * vRinv)
      }
    }, true)
  }

  def ggrqr4(X: BDM[Double], GG2: BSM[Double]) = {
    val GX = GG2* X
    val R = breeze.linalg.qr.reduced(GX).r
    val Rinv = breeze.linalg.inv(R)
    X * Rinv
  }

  def generateOmega(A: RDD[(Int, BSM[Double])], nCols: Int): RDD[(Int, BDM[Double])] = {
    A.mapPartitions(iter => {
      iter.map{ case (blockId, mat) =>
        val nRows = mat.rows
        val gaussion = BDM.rand(nRows, nCols, breeze.stats.distributions.Rand.gaussian)
        (blockId, gaussion)
      }
    }, preservesPartitioning = true)
  }

  def generateGaussionDistri(Y: RDD[(Int, BSM[Double])], expectedDim: Int): RDD[(Int, BDM[Double])] = {
    val nRowsPer = math.max(expectedDim / Y.partitions.length, 1)
    Y.mapPartitions(iter => {
      iter.map{ case (blockId, mat) =>
        val nColsPer = mat.rows
        val gaussion = BDM.rand(nRowsPer, nColsPer, breeze.stats.distributions.Rand.gaussian)
        (blockId, gaussion)
      }
    }, preservesPartitioning = true)
  }

  def generateGaussionSingle(X: BDM[Double], expectedDim: Int) = {
    val nRowsPer = math.max(math.sqrt(expectedDim).toInt, 1)
    val nColsPer = math.max(X.cols / nRowsPer, 1)   // nColBlocks = nRowsPer
    val entries = new ArrayBuffer[(Int, Int, Double)]()
    var row_start = 0
    var col_start = 0
    var nBlocks = 0
    while (col_start < X.rows) {
      val curCols = math.min(nColsPer, X.rows - col_start)
      val gaussion = BDM.rand(nRowsPer, curCols, breeze.stats.distributions.Rand.gaussian)
      gaussion.activeIterator.foreach{ case ((i, j), v) =>
         entries.append((i + row_start, j + col_start, v))
      }
      nBlocks += 1
      row_start += nRowsPer
      col_start += curCols
    }
    SparseMatrix.fromCOO(nBlocks * nRowsPer, X.rows, entries.toArray.toIterable).asBreeze.asInstanceOf[BSM[Double]]
  }



}
